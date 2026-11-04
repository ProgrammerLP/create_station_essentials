package net.adeptstack.cts.migration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Old → new mapping for blocks, block states and items. Every state of every old block is
 * mapped up front, so a lookup during the chunk scan is a single map access.
 */
public final class MigrationTable {

    /** How deep nested item data (shulker boxes in shulker boxes, …) is followed. */
    private static final int MAX_NBT_DEPTH = 16;

    private final Map<Block, Block> blocks;
    private final Map<BlockState, BlockState> states = new IdentityHashMap<>();
    /** States where at least one property could not be carried over, with the reason. */
    private final Map<BlockState, String> lossyStates = new IdentityHashMap<>();
    private final Set<BlockState> warnedStates = ConcurrentHashMap.newKeySet();
    private final Map<Item, Item> items;
    /** The same item mapping as id strings, for item stacks stored as NBT inside other items. */
    private final Map<String, String> itemIds = new HashMap<>();

    MigrationTable(Map<Block, Block> blocks, Map<Item, Item> items) {
        this.blocks = blocks;
        this.items = items;
        blocks.forEach((oldBlock, newBlock) -> {
            for (BlockState oldState : oldBlock.getStateDefinition().getPossibleStates()) {
                List<String> problems = new ArrayList<>();
                states.put(oldState, mapState(oldState, newBlock, problems));
                if (!problems.isEmpty()) {
                    lossyStates.put(oldState, String.join(", ", problems));
                }
            }
        });
        items.forEach((oldItem, newItem) -> itemIds.put(
                BuiltInRegistries.ITEM.getKey(oldItem).toString(),
                BuiltInRegistries.ITEM.getKey(newItem).toString()));
    }

    public boolean isEmpty() {
        return blocks.isEmpty() && items.isEmpty();
    }

    public int blockCount() {
        return blocks.size();
    }

    public int itemCount() {
        return items.size();
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }

    public boolean isOldState(BlockState state) {
        return states.containsKey(state);
    }

    /** Returns the new state, or {@code null} if this state is not an old CTU state. */
    @Nullable
    public BlockState convert(BlockState oldState) {
        BlockState newState = states.get(oldState);
        if (newState != null) {
            String problem = lossyStates.get(oldState);
            if (problem != null && warnedStates.add(oldState)) {
                CtuMigration.LOGGER.warn("Block state {} could not be fully migrated ({}); using defaults for those properties", oldState, problem);
            }
        }
        return newState;
    }

    /**
     * Returns a converted copy of the stack, or {@code null} if nothing in it had to change.
     * Covers the stack itself and old items stored in its NBT (shulker boxes, bundles, toolboxes …).
     */
    @Nullable
    public ItemStack convert(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        boolean direct = items.containsKey(stack.getItem());
        if (!direct && !(stack.hasTag() && rewriteItemIds(stack.getTag(), 0, true))) {
            return null;
        }

        // Saving keeps count, NBT and (on Forge) capability data. The saved compound has the
        // same "id"/"Count" shape as nested stacks, so one rewrite handles everything.
        CompoundTag saved = stack.save(new CompoundTag());
        rewriteItemIds(saved, 0, false);
        ItemStack result = ItemStack.of(saved);

        if (direct) {
            CtuMigration.LOGGER.debug("Converted item {} x{} -> {}",
                    BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount(), BuiltInRegistries.ITEM.getKey(result.getItem()));
        } else {
            CtuMigration.LOGGER.debug("Converted items stored in {}", BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        return result;
    }

    /** Converts every slot of the container in place and returns the number of changed slots. */
    public int convertContainer(Container container) {
        int changed = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack converted = convert(container.getItem(slot));
            if (converted != null) {
                container.setItem(slot, converted);
                changed++;
            }
        }
        if (changed > 0) {
            container.setChanged();
        }
        return changed;
    }

    /**
     * Replaces old item ids in every compound that looks like an item stack ("id" + "Count").
     * With {@code dryRun} nothing is changed and the result says whether anything would be.
     */
    private boolean rewriteItemIds(Tag tag, int depth, boolean dryRun) {
        if (depth > MAX_NBT_DEPTH) {
            return false;
        }
        boolean found = false;
        if (tag instanceof CompoundTag compound) {
            if (compound.get("id") instanceof StringTag idTag && compound.get("Count") instanceof NumericTag) {
                String newId = itemIds.get(idTag.getAsString());
                if (newId != null) {
                    if (dryRun) {
                        return true;
                    }
                    compound.putString("id", newId);
                    found = true;
                }
            }
            for (String key : compound.getAllKeys()) {
                if (rewriteItemIds(compound.get(key), depth + 1, dryRun)) {
                    if (dryRun) {
                        return true;
                    }
                    found = true;
                }
            }
        } else if (tag instanceof ListTag list
                && (list.getElementType() == Tag.TAG_COMPOUND || list.getElementType() == Tag.TAG_LIST)) {
            for (Tag element : list) {
                if (rewriteItemIds(element, depth + 1, dryRun)) {
                    if (dryRun) {
                        return true;
                    }
                    found = true;
                }
            }
        }
        return found;
    }

    /** Copies every property by name. Unknown properties or values fall back to the new block's default. */
    private static BlockState mapState(BlockState oldState, Block newBlock, List<String> problems) {
        BlockState result = newBlock.defaultBlockState();
        StateDefinition<Block, BlockState> definition = newBlock.getStateDefinition();

        for (Property<?> oldProperty : oldState.getProperties()) {
            Property<?> newProperty = definition.getProperty(oldProperty.getName());
            if (newProperty == null) {
                problems.add("property '" + oldProperty.getName() + "' does not exist on the new block");
                continue;
            }
            result = setByName(result, newProperty, valueName(oldState, oldProperty), problems);
        }

        for (Property<?> newProperty : result.getProperties()) {
            if (oldState.getProperties().stream().noneMatch(p -> p.getName().equals(newProperty.getName()))) {
                problems.add("property '" + newProperty.getName() + "' is missing on the old block");
            }
        }
        return result;
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static <T extends Comparable<T>> BlockState setByName(BlockState state, Property<T> property, String value, List<String> problems) {
        Optional<T> parsed = property.getValue(value);
        if (parsed.isEmpty()) {
            problems.add(property.getName() + "=" + value + " is not a valid value");
            return state;
        }
        return state.setValue(property, parsed.get());
    }
}

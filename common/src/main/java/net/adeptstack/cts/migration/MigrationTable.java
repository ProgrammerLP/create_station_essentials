package net.adeptstack.cts.migration;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

    /** How deep item containers inside items (shulker boxes, bundles) are followed. */
    private static final int MAX_NESTING = 4;

    private final Map<Block, Block> blocks;
    private final Map<BlockState, BlockState> states = new IdentityHashMap<>();
    /** States where at least one property could not be carried over, with the reason. */
    private final Map<BlockState, String> lossyStates = new IdentityHashMap<>();
    private final Set<BlockState> warnedStates = ConcurrentHashMap.newKeySet();
    private final Map<Item, Item> items;

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

    /** Returns a converted copy of the stack, or {@code null} if nothing in it had to change. */
    @Nullable
    public ItemStack convert(ItemStack stack) {
        return convert(stack, 0);
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

    @Nullable
    private ItemStack convert(ItemStack stack, int depth) {
        if (stack.isEmpty()) {
            return null;
        }

        ItemStack result = null;
        Item newItem = items.get(stack.getItem());
        if (newItem != null) {
            // Keeps count and all data components (custom name etc.).
            result = stack.transmuteCopy(newItem, stack.getCount());
        }

        if (depth < MAX_NESTING) {
            ItemStack current = result != null ? result : stack;

            ItemContainerContents contents = current.get(DataComponents.CONTAINER);
            if (contents != null) {
                List<ItemStack> inner = new ArrayList<>(contents.stream().toList());
                if (convertAll(inner, depth)) {
                    result = result != null ? result : stack.copy();
                    result.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inner));
                }
            }

            BundleContents bundle = current.get(DataComponents.BUNDLE_CONTENTS);
            if (bundle != null) {
                List<ItemStack> inner = new ArrayList<>();
                bundle.itemsCopy().forEach(inner::add);
                if (convertAll(inner, depth)) {
                    result = result != null ? result : stack.copy();
                    result.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(inner));
                }
            }
        }

        if (result != null && newItem != null) {
            CtuMigration.LOGGER.debug("Converted item {} x{} -> {}",
                    BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount(), BuiltInRegistries.ITEM.getKey(newItem));
        }
        return result;
    }

    private boolean convertAll(List<ItemStack> stacks, int depth) {
        boolean changed = false;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack converted = convert(stacks.get(i), depth + 1);
            if (converted != null) {
                stacks.set(i, converted);
                changed = true;
            }
        }
        return changed;
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

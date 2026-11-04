package net.adeptstack.cts.migration;

import net.adeptstack.cts.Main;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Blocks that moved from Create Train Utilities (mod id "trainutilities") into this mod.
 * The ids keep their path, only the namespace changes.
 */
public final class CtuMigration {

    public static final Logger LOGGER = LoggerFactory.getLogger("tracksta/ctu-migration");

    public static final String OLD_NAMESPACE = "trainutilities";

    /** Paths that exist as block and block item in both mods. None of them has a block entity. */
    public static final List<String> MOVED_PATHS = List.of(
            "de_platform_block",
            "nl_platform_block",
            "ch_platform_block",
            "iso_wall_block",
            "iso_wall_block_green"
    );

    private CtuMigration() {
    }

    public static ResourceLocation oldId(String path) {
        return new ResourceLocation(OLD_NAMESPACE, path);
    }

    public static ResourceLocation newId(String path) {
        return Main.asResource(path);
    }

    /** New id for an old CTU id, or {@code null} if the id did not move. */
    @Nullable
    public static ResourceLocation aliasTarget(ResourceLocation id) {
        if (OLD_NAMESPACE.equals(id.getNamespace()) && MOVED_PATHS.contains(id.getPath())) {
            return newId(id.getPath());
        }
        return null;
    }

    /**
     * Looks up the old entries by id and returns an empty table when CTU is not loaded.
     * The lookup checks that the entry is really registered under the old id: on Forge
     * {@code containsKey} follows aliases, and on Fabric our alias mixin redirects {@code get}.
     */
    public static MigrationTable resolve() {
        Map<Block, Block> blocks = new IdentityHashMap<>();
        Map<Item, Item> items = new IdentityHashMap<>();

        for (String path : MOVED_PATHS) {
            ResourceLocation oldId = oldId(path);
            ResourceLocation newId = newId(path);

            Block oldBlock = registered(BuiltInRegistries.BLOCK, oldId);
            if (oldBlock != null) {
                Block newBlock = registered(BuiltInRegistries.BLOCK, newId);
                if (newBlock == null) {
                    LOGGER.warn("Cannot migrate block {}: target {} is not registered", oldId, newId);
                } else if (oldBlock instanceof EntityBlock || newBlock instanceof EntityBlock) {
                    // The converter writes block states straight into the chunk and does not move
                    // block entity data. Refuse instead of silently dropping data.
                    LOGGER.warn("Cannot migrate block {}: block entities are not supported by the converter", oldId);
                } else {
                    blocks.put(oldBlock, newBlock);
                }
            }

            Item oldItem = registered(BuiltInRegistries.ITEM, oldId);
            if (oldItem != null) {
                Item newItem = registered(BuiltInRegistries.ITEM, newId);
                if (newItem == null) {
                    LOGGER.warn("Cannot migrate item {}: target {} is not registered", oldId, newId);
                } else {
                    items.put(oldItem, newItem);
                }
            }
        }

        return new MigrationTable(blocks, items);
    }

    /** The entry registered under exactly this id, ignoring aliases and default values. */
    @Nullable
    private static <T> T registered(Registry<T> registry, ResourceLocation id) {
        T value = registry.get(id);
        return value != null && id.equals(registry.getKey(value)) ? value : null;
    }
}

package net.adeptstack.cts.migration;

import net.adeptstack.cts.Main;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
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
        return ResourceLocation.fromNamespaceAndPath(OLD_NAMESPACE, path);
    }

    public static ResourceLocation newId(String path) {
        return Main.asResource(path);
    }

    /**
     * Looks up the old entries by id. {@code containsKey} ignores registry aliases, so this only
     * finds entries that CTU itself still registers. Returns an empty table when CTU is not loaded.
     */
    public static MigrationTable resolve() {
        Map<Block, Block> blocks = new IdentityHashMap<>();
        Map<Item, Item> items = new IdentityHashMap<>();

        for (String path : MOVED_PATHS) {
            ResourceLocation oldId = oldId(path);
            ResourceLocation newId = newId(path);

            if (BuiltInRegistries.BLOCK.containsKey(oldId)) {
                if (!BuiltInRegistries.BLOCK.containsKey(newId)) {
                    LOGGER.warn("Cannot migrate block {}: target {} is not registered", oldId, newId);
                } else {
                    Block oldBlock = BuiltInRegistries.BLOCK.get(oldId);
                    Block newBlock = BuiltInRegistries.BLOCK.get(newId);
                    // The converter writes block states straight into the chunk and does not move
                    // block entity data. Refuse instead of silently dropping data.
                    if (oldBlock instanceof EntityBlock || newBlock instanceof EntityBlock) {
                        LOGGER.warn("Cannot migrate block {}: block entities are not supported by the converter", oldId);
                    } else {
                        blocks.put(oldBlock, newBlock);
                    }
                }
            }

            if (BuiltInRegistries.ITEM.containsKey(oldId)) {
                if (!BuiltInRegistries.ITEM.containsKey(newId)) {
                    LOGGER.warn("Cannot migrate item {}: target {} is not registered", oldId, newId);
                } else {
                    items.put(BuiltInRegistries.ITEM.get(oldId), BuiltInRegistries.ITEM.get(newId));
                }
            }
        }

        return new MigrationTable(blocks, items);
    }
}

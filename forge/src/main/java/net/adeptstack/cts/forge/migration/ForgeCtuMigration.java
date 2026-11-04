package net.adeptstack.cts.forge.migration;

import net.adeptstack.cts.migration.CtuMigration;
import net.adeptstack.cts.migration.CtuMigrationHandler;
import net.adeptstack.cts.migration.MigrationConfig;
import net.adeptstack.cts.migration.MigrationTable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;

/** Forge glue for the CTU migration: config, chunk loads, item handlers and registry remaps. */
public final class ForgeCtuMigration {

    private ForgeCtuMigration() {
    }

    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MigrationConfig.SPEC);
        CtuMigrationHandler.setInventoryAccess(ForgeCtuMigration::convertItemHandler);
        MinecraftForge.EVENT_BUS.addListener(ForgeCtuMigration::onChunkLoad);
        MinecraftForge.EVENT_BUS.addListener(ForgeCtuMigration::onMissingMappings);
    }

    private static void onChunkLoad(ChunkEvent.Load event) {
        // Only chunks read from disk can contain old CTU blocks.
        if (!event.isNewChunk() && event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            CtuMigrationHandler.onChunkLoad(level, chunk);
        }
    }

    /**
     * Fires when a world was saved with ids that are no longer registered, i.e. without CTU v3.x
     * (or with CTD v4). While CTU is installed the old ids exist and this does nothing.
     * Forge stores the remap in the world, so it also applies on every later load.
     */
    private static void onMissingMappings(MissingMappingsEvent event) {
        remap(event, ForgeRegistries.Keys.BLOCKS, ForgeRegistries.BLOCKS);
        remap(event, ForgeRegistries.Keys.ITEMS, ForgeRegistries.ITEMS);
    }

    private static <T> void remap(MissingMappingsEvent event, ResourceKey<? extends Registry<T>> key, IForgeRegistry<T> registry) {
        for (MissingMappingsEvent.Mapping<T> mapping : event.getMappings(key, CtuMigration.OLD_NAMESPACE)) {
            ResourceLocation target = CtuMigration.aliasTarget(mapping.getKey());
            if (target == null) {
                continue;
            }
            T value = registry.getValue(target);
            if (value != null && target.equals(registry.getKey(value))) {
                mapping.remap(value);
                CtuMigration.LOGGER.info("Remapped {} -> {}", mapping.getKey(), target);
            }
        }
    }

    /** Covers modded storage that only exposes an item handler, e.g. Create vaults. */
    private static int convertItemHandler(ServerLevel level, BlockEntity blockEntity, MigrationTable table) {
        IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (!(handler instanceof IItemHandlerModifiable modifiable)) {
            return 0;
        }
        int changed = 0;
        for (int slot = 0; slot < modifiable.getSlots(); slot++) {
            ItemStack converted = table.convert(modifiable.getStackInSlot(slot));
            if (converted != null) {
                modifiable.setStackInSlot(slot, converted);
                changed++;
            }
        }
        return changed;
    }
}

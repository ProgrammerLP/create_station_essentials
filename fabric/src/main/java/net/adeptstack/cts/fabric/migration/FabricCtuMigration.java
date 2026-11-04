package net.adeptstack.cts.fabric.migration;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.adeptstack.cts.Main;
import net.adeptstack.cts.migration.CtuMigrationHandler;
import net.adeptstack.cts.migration.MigrationConfig;
import net.adeptstack.cts.migration.MigrationTable;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.config.ModConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fabric glue for the CTU migration: config, chunk loads and Transfer API storages.
 * The registry aliases live in {@code MappedRegistryMixin}.
 */
public final class FabricCtuMigration {

    private FabricCtuMigration() {
    }

    public static void init() {
        ForgeConfigRegistry.INSTANCE.register(Main.MOD_ID, ModConfig.Type.COMMON, MigrationConfig.SPEC);
        CtuMigrationHandler.setInventoryAccess(FabricCtuMigration::convertStorage);
        // Fabric has no "newly generated" flag here. New chunks cannot contain CTU blocks,
        // so they fail the cheap palette check right away.
        ServerChunkEvents.CHUNK_LOAD.register(CtuMigrationHandler::onChunkLoad);
    }

    /**
     * Covers modded storage that only exposes a Transfer API storage, e.g. Create vaults.
     * Each old item is taken out and the new one put back in one transaction, so nothing is
     * lost if the storage refuses the new item.
     */
    private static int convertStorage(ServerLevel level, BlockEntity blockEntity, MigrationTable table) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, null);
        if (storage == null) {
            return 0;
        }

        // Collect first: changing a storage while iterating it is not allowed.
        Map<ItemVariant, Long> oldResources = new LinkedHashMap<>();
        for (StorageView<ItemVariant> view : storage) {
            if (!view.isResourceBlank() && view.getAmount() > 0 && table.convert(view.getResource().toStack()) != null) {
                oldResources.merge(view.getResource(), view.getAmount(), Long::sum);
            }
        }

        int changed = 0;
        for (Map.Entry<ItemVariant, Long> entry : oldResources.entrySet()) {
            ItemVariant oldVariant = entry.getKey();
            ItemStack converted = table.convert(oldVariant.toStack());
            if (converted == null) {
                continue;
            }
            ItemVariant newVariant = ItemVariant.of(converted);
            try (Transaction transaction = Transaction.openOuter()) {
                long extracted = storage.extract(oldVariant, entry.getValue(), transaction);
                if (extracted > 0 && storage.insert(newVariant, extracted, transaction) == extracted) {
                    transaction.commit();
                    changed++;
                }
            }
        }
        return changed;
    }
}

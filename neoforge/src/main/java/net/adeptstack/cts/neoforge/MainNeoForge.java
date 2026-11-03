package net.adeptstack.cts.neoforge;

import net.adeptstack.cts.Main;
import net.adeptstack.cts.migration.CtuMigration;
import net.adeptstack.cts.neoforge.migration.CtuMigrationHandler;
import net.adeptstack.cts.neoforge.migration.MigrationConfig;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Main.MOD_ID)
public final class MainNeoForge {
    private static ModContainer modContainer;

    public MainNeoForge(ModContainer container) {
        modContainer = container;
        IEventBus modBus = ModLoadingContext.get().getActiveContainer().getEventBus();
        Main.REGISTRATE.registerEventListeners(modBus);
        Main.init();

        container.registerConfig(ModConfig.Type.COMMON, MigrationConfig.SPEC);
        modBus.addListener(MainNeoForge::registerCtuAliases);
        NeoForge.EVENT_BUS.register(new CtuMigrationHandler());
    }

    /**
     * Old CTU ids → ids of this mod, for worlds loaded without CTU v3.x (or with CTD v4).
     * NeoForge only uses an alias when the old id is not registered, so while CTU v3.x is
     * installed the aliases do nothing and the runtime converter does the work.
     */
    private static void registerCtuAliases(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK) || event.getRegistryKey().equals(Registries.ITEM)) {
            for (String path : CtuMigration.MOVED_PATHS) {
                event.getRegistry().addAlias(CtuMigration.oldId(path), CtuMigration.newId(path));
            }
        }
    }

    static ModContainer getModContainer() {
        return modContainer;
    }
}

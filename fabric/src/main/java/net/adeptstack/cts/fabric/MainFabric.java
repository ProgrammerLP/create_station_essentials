package net.adeptstack.cts.fabric;

import net.fabricmc.api.ModInitializer;

import net.adeptstack.cts.Main;
import net.adeptstack.cts.fabric.migration.FabricCtuMigration;

public final class MainFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        Main.init();
        // Chunk scan hook and config for the CTU migration.
        FabricCtuMigration.init();

        Main.REGISTRATE.register();
    }
}

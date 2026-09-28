package net.adeptstack.cts;

import com.simibubi.create.foundation.data.CreateRegistrate;
import net.adeptstack.cts.network.ModNetwork;
import net.adeptstack.cts.registry.*;
import net.minecraft.resources.ResourceLocation;

public final class Main {
    public static final String MOD_ID = "tracksta";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    public static void init() {
        ModNetwork.networkInit();
        ModTabs.CREATIVE_MODE_TABS.register();
        // Registrate defaults every item to the vanilla search tab; point it at ours instead,
        // otherwise items land in both and the search tab rejects the duplicate.
        REGISTRATE.defaultCreativeTab(ModTabs.TRACKSTA_TAB.getKey());
        ModBlocks.register();
        ModBlockEntities.register();
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
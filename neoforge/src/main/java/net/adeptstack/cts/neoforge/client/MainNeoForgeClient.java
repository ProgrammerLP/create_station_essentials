package net.adeptstack.cts.neoforge.client;

import net.neoforged.api.distmarker.Dist;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = "tracksta", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class MainNeoForgeClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
    }
}
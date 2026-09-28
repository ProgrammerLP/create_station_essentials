package net.adeptstack.cts.network;

import dev.architectury.networking.NetworkChannel;
import net.adeptstack.cts.network.packets.MastStationSignPacket;
import net.adeptstack.cts.network.packets.PlatformBlockPacket;
import net.adeptstack.cts.network.packets.StationSignDoublePacket;
import net.adeptstack.cts.network.packets.WallStationSignPacket;
import net.minecraft.resources.ResourceLocation;

import static net.adeptstack.cts.Main.MOD_ID;

public class ModNetwork {

    public static final NetworkChannel CHANNEL = NetworkChannel.create(new ResourceLocation(MOD_ID, MOD_ID + "_network"));

    public static void init() {
        CHANNEL.register(PlatformBlockPacket.class, PlatformBlockPacket::encode, PlatformBlockPacket::new, PlatformBlockPacket::apply);
        CHANNEL.register(WallStationSignPacket.class, WallStationSignPacket::encode, WallStationSignPacket::new, WallStationSignPacket::apply);
        CHANNEL.register(MastStationSignPacket.class, MastStationSignPacket::encode, MastStationSignPacket::new, MastStationSignPacket::apply);
        CHANNEL.register(StationSignDoublePacket.class, StationSignDoublePacket::encode, StationSignDoublePacket::new, StationSignDoublePacket::apply);
    }
}

package net.adeptstack.cts.network;

import de.mrjulsen.mcdragonlib.network.DLNetworkManager;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.network.NetworkPacketType;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.adeptstack.cts.network.packets.PlatformBlockPacket;
import net.adeptstack.cts.network.packets.StationSignPacket;

import static net.adeptstack.cts.Main.MOD_ID;

public class ModNetwork {

    public static final DLNetworkManager CTU_NETWORK_MANAGER = new DLNetworkManager(DLUtils.resourceLocation(MOD_ID, MOD_ID + "_network"), "v1");

    public static final NetworkPacketType.Send<NetworkDirection.C2S, PlatformBlockPacket> PLATFORM_PACKET =
            CTU_NETWORK_MANAGER.registerSendOnlyPacket(
                    "send_platform_packet",
                    NetworkDirection.C2S,
                    PlatformBlockPacket::handle,
                    PlatformBlockPacket::new
            );

    public static final NetworkPacketType.Send<NetworkDirection.C2S, StationSignPacket> STATION_SIGN_PACKET =
            CTU_NETWORK_MANAGER.registerSendOnlyPacket(
                    "send_station_sign_packet",
                    NetworkDirection.C2S,
                    StationSignPacket::handle,
                    StationSignPacket::new
            );

    public static void networkInit() { }
}

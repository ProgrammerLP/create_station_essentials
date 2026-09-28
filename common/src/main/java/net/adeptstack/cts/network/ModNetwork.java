package net.adeptstack.cts.network;

import de.mrjulsen.mcdragonlib.network.DLNetworkManager;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.network.NetworkPacketType;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.adeptstack.cts.network.packets.MastStationSignPacket;
import net.adeptstack.cts.network.packets.PlatformBlockPacket;
import net.adeptstack.cts.network.packets.StationSignDoublePacket;
import net.adeptstack.cts.network.packets.WallStationSignPacket;

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

    public static final NetworkPacketType.Send<NetworkDirection.C2S, WallStationSignPacket> WALL_STATION_SIGN_PACKET =
            CTU_NETWORK_MANAGER.registerSendOnlyPacket(
                    "send_wall_station_sign_packet",
                    NetworkDirection.C2S,
                    WallStationSignPacket::handle,
                    WallStationSignPacket::new
            );

    public static final NetworkPacketType.Send<NetworkDirection.C2S, MastStationSignPacket> MAST_STATION_SIGN_PACKET =
            CTU_NETWORK_MANAGER.registerSendOnlyPacket(
                    "send_mast_station_sign_packet",
                    NetworkDirection.C2S,
                    MastStationSignPacket::handle,
                    MastStationSignPacket::new
            );

    public static final NetworkPacketType.Send<NetworkDirection.C2S, StationSignDoublePacket> STATION_SIGN_DOUBLE_PACKET =
            CTU_NETWORK_MANAGER.registerSendOnlyPacket(
                    "send_station_sign_double_packet",
                    NetworkDirection.C2S,
                    StationSignDoublePacket::handle,
                    StationSignDoublePacket::new
            );

    public static void networkInit() { }
}

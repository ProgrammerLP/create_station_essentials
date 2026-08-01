package net.adeptstack.cts.client;

import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockCH;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockDE;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockNL;
import net.adeptstack.cts.network.ModNetwork;
import net.adeptstack.cts.network.packets.PlatformBlockPacket;
import net.adeptstack.cts.ui.screens.platformBlocks.PlatformBlockCHPlacementScreen;
import net.adeptstack.cts.ui.screens.platformBlocks.PlatformBlockDEPlacementScreen;
import net.adeptstack.cts.ui.screens.platformBlocks.PlatformBlockNLPlacementScreen;
import net.adeptstack.cts.utils.screenUtils.TextureNames;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import static net.adeptstack.cts.Main.MOD_ID;

public class ClientWrapper {

    //Platform Screens
    public static void openPlatformBlockDEScreen(BlockPos pos, BlockState blockState) {
        Minecraft.getInstance().setScreen(
                new PlatformBlockDEPlacementScreen(
                        blockState.getValue(PlatformBlockDE.SIGN_BLOCKS),
                        PlatformBlockDE.SIGN_BLOCKS,
                        (variant) -> {
                            String name = TextureNames.GetDEPlatformBlockTextureName(variant);
                            return new PlatformBlockDEPlacementScreen.TextureResult(new ResourceLocation(MOD_ID, "textures/block/platformblocks/" + name), 256, 256);
                        }, (variant) -> {
                    ModNetwork.CHANNEL.sendToServer(new PlatformBlockPacket(pos, variant));
                        },
                        "gui." + MOD_ID + ".selection_screen.blockplacementscreen_de", 1
                )
        );
    }

    public static void openPlatformBlockNLScreen(BlockPos pos, BlockState blockState) {
        Minecraft.getInstance().setScreen(
                new PlatformBlockNLPlacementScreen(
                        blockState.getValue(PlatformBlockNL.SIGN_BLOCKS),
                        PlatformBlockNL.SIGN_BLOCKS,
                        (variant) -> {
                            String name = TextureNames.GetNLPlatformBlockTextureName(variant);
                            return new PlatformBlockNLPlacementScreen.TextureResult(new ResourceLocation(MOD_ID, "textures/block/nl_platformblocks/" + name), 256, 256);
                        }, (variant) -> {
                    ModNetwork.CHANNEL.sendToServer(new PlatformBlockPacket(pos, variant));
                        },
                        "gui." + MOD_ID + ".selection_screen.blockplacementscreen_nl", 2
                )
        );
    }

    public static void openPlatformBlockCHScreen(BlockPos pos, BlockState blockState) {
        Minecraft.getInstance().setScreen(
                new PlatformBlockCHPlacementScreen(
                        blockState.getValue(PlatformBlockCH.SIGN_BLOCKS),
                        PlatformBlockCH.SIGN_BLOCKS,
                        (variant) -> {
                            String name = TextureNames.GetCHPlatformBlockTextureName(variant);
                            return new PlatformBlockCHPlacementScreen.TextureResult(new ResourceLocation(MOD_ID, "textures/block/ch_platformblocks/" + name), 256, 256);
                        }, (variant) -> {
                    ModNetwork.CHANNEL.sendToServer(new PlatformBlockPacket(pos, variant));
                        },
                    "gui." + MOD_ID + ".selection_screen.blockplacementscreen_ch", 3
                )
        );
    }
}

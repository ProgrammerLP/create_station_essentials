package net.adeptstack.cts.registry;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.adeptstack.cts.blocks.panelBlocks.IsoWallBlock;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockCH;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockDE;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockNL;
import net.minecraft.world.level.material.MapColor;

public class ModBlocks {

    //===PLATFORM BLOCKS===
    public static final BlockEntry<PlatformBlockDE> DE_PLATFORM_BLOCK =
            TrackStaBuilderTransformers.DEPlatformBlock("de_platform_block", MapColor.NONE);

    public static final BlockEntry<PlatformBlockNL> NL_PLATFORM_BLOCK =
            TrackStaBuilderTransformers.NLPlatformBlock("nl_platform_block", MapColor.NONE);

    public static final BlockEntry<PlatformBlockCH> CH_PLATFORM_BLOCK =
            TrackStaBuilderTransformers.CHPlatformBlock("ch_platform_block", MapColor.NONE);

    //noise isolation walls
    public static final BlockEntry<IsoWallBlock> ISO_WALL_BLOCK =
            TrackStaBuilderTransformers.IsoWallBlock("iso_wall_block", MapColor.COLOR_CYAN);

    public static final BlockEntry<IsoWallBlock> ISO_WALL_BLOCK_GREEN =
            TrackStaBuilderTransformers.IsoWallBlock("iso_wall_block_green", MapColor.COLOR_LIGHT_GREEN);

    //TESTBLOCKS

    public static void register() { }
}

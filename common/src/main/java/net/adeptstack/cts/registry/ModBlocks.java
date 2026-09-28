package net.adeptstack.cts.registry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.adeptstack.cts.blocks.panelBlocks.IsoWallBlock;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockCH;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockDE;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockNL;
import net.adeptstack.cts.blocks.signBlocks.StationSignBlock;
import net.adeptstack.cts.blocks.signBlocks.StationSignDoubleBlock;
import net.adeptstack.cts.blocks.signBlocks.StationSignMastBlock;
import net.minecraft.world.level.material.MapColor;

public class ModBlocks {

    //===PLATFORM BLOCKS===
    public static final BlockEntry<PlatformBlockDE> DE_PLATFORM_BLOCK =
            TrackstaBuilderTransformers.DEPlatformBlock("de_platform_block", MapColor.NONE);

    public static final BlockEntry<PlatformBlockNL> NL_PLATFORM_BLOCK =
            TrackstaBuilderTransformers.NLPlatformBlock("nl_platform_block", MapColor.NONE);

    public static final BlockEntry<PlatformBlockCH> CH_PLATFORM_BLOCK =
            TrackstaBuilderTransformers.CHPlatformBlock("ch_platform_block", MapColor.NONE);

    //noise isolation walls
    public static final BlockEntry<IsoWallBlock> ISO_WALL_BLOCK =
            TrackstaBuilderTransformers.IsoWallBlock("iso_wall_block", MapColor.COLOR_CYAN);

    public static final BlockEntry<IsoWallBlock> ISO_WALL_BLOCK_GREEN =
            TrackstaBuilderTransformers.IsoWallBlock("iso_wall_block_green", MapColor.COLOR_LIGHT_GREEN);

    //sign blocks
    public static final BlockEntry<StationSignBlock> STATION_SIGN_BLOCK =
            TrackstaBuilderTransformers.StationSignBlock("station_sign_block", MapColor.COLOR_BLUE);

    public static final BlockEntry<StationSignMastBlock> STATION_SIGN_MAST_BLOCK =
            TrackstaBuilderTransformers.StationSignMastBlock("station_sign_mast_block", MapColor.METAL);

    public static final BlockEntry<StationSignDoubleBlock> STATION_SIGN_DOUBLE_BLOCK =
            TrackstaBuilderTransformers.StationSignDoubleBlock("station_sign_double_block", MapColor.METAL);

    //TESTBLOCKS

    // Loading this class registers the entries above; the items pick up the
    // creative tab from REGISTRATE's default, which Main.init() sets beforehand.
    public static void register() {
    }
}

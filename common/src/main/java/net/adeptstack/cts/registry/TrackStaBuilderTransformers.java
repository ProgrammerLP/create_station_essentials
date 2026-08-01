package net.adeptstack.cts.registry;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.adeptstack.cts.blocks.panelBlocks.IsoWallBlock;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockCH;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockDE;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockNL;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.MapColor;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;
import static net.adeptstack.cts.Main.REGISTRATE;

@SuppressWarnings({"unused","removal"})
public class TrackstaBuilderTransformers {

    public static BlockEntry<PlatformBlockDE> DEPlatformBlock(String id, MapColor color) {
        return REGISTRATE
                .block(id, PlatformBlockDE::new)
                .initialProperties(() -> Blocks.IRON_BARS)
                .properties(p -> p.mapColor(color)
                        .sound(SoundType.METAL))
                .transform(pickaxeOnly())
                .loot((lr, block) -> lr.add(block, lr.createSingleItemTable(block)))
                .item()
                .build()
                .register();
    }

    public static BlockEntry<PlatformBlockNL> NLPlatformBlock(String id, MapColor color) {
        return REGISTRATE
                .block(id, PlatformBlockNL::new)
                .initialProperties(() -> Blocks.IRON_BARS)
                .properties(p -> p.mapColor(color)
                        .sound(SoundType.METAL))
                .transform(pickaxeOnly())
                .loot((lr, block) -> lr.add(block, lr.createSingleItemTable(block)))
                .item()
                .build()
                .register();
    }

    public static BlockEntry<PlatformBlockCH> CHPlatformBlock(String id, MapColor color) {
        return REGISTRATE
                .block(id, PlatformBlockCH::new)
                .initialProperties(() -> Blocks.IRON_BARS)
                .properties(p -> p.mapColor(color)
                        .sound(SoundType.METAL))
                .transform(pickaxeOnly())
                .loot((lr, block) -> lr.add(block, lr.createSingleItemTable(block)))
                .item()
                .build()
                .register();
    }

    public static BlockEntry<Block> DefaultBlock(String id, MapColor color) {
        return REGISTRATE
                .block(id, Block::new)
                .initialProperties(() -> Blocks.GLASS)
                .properties(p -> p.sound(SoundType.STONE).mapColor(color))
                .transform(pickaxeOnly())
                .loot((lr, block) -> lr.add(block, lr.createSingleItemTable(block)))
                .item()
                .build()
                .register();
    }

    public static BlockEntry<IsoWallBlock> IsoWallBlock(String id, MapColor color) {
        return REGISTRATE
                .block(id, IsoWallBlock::new)
                .initialProperties(() -> Blocks.IRON_BARS)
                .properties(p -> p.mapColor(MapColor.WOOL)
                        .sound(SoundType.METAL))
                .transform(pickaxeOnly())
                .loot((lr, block) -> lr.add(block, lr.createSingleItemTable(block)))
                .item()
                .build()
                .register();
    }
}

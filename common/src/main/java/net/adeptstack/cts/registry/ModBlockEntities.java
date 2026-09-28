package net.adeptstack.cts.registry;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.adeptstack.cts.blockentities.MastStationSignBlockEntity;
import net.adeptstack.cts.blockentities.WallStationSignBlockEntity;
import net.adeptstack.cts.blockentities.StationSignDoubleBlockEntity;
import net.adeptstack.cts.client.renderer.MastStationSignBlockEntityRenderer;
import net.adeptstack.cts.client.renderer.WallStationSignBlockEntityRenderer;
import net.adeptstack.cts.client.renderer.StationSignDoubleBlockEntityRenderer;

import static net.adeptstack.cts.Main.REGISTRATE;

public class ModBlockEntities {

    public static final BlockEntityEntry<WallStationSignBlockEntity> WALL_STATION_SIGN_BLOCK_ENTITY = REGISTRATE
            .<WallStationSignBlockEntity>blockEntity("wall_station_sign_block_entity", WallStationSignBlockEntity::new)
            .validBlocks(ModBlocks.WALL_STATION_SIGN_BLOCK)
            .renderer(() -> WallStationSignBlockEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<MastStationSignBlockEntity> MAST_STATION_SIGN_BLOCK_ENTITY = REGISTRATE
            .<MastStationSignBlockEntity>blockEntity("mast_station_sign_block_entity", MastStationSignBlockEntity::new)
            .validBlocks(ModBlocks.MAST_STATION_SIGN_BLOCK)
            .renderer(() -> MastStationSignBlockEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<StationSignDoubleBlockEntity> STATION_SIGN_DOUBLE_BLOCK_ENTITY = REGISTRATE
            .<StationSignDoubleBlockEntity>blockEntity("station_sign_double_block_entity", StationSignDoubleBlockEntity::new)
            .validBlocks(ModBlocks.STATION_SIGN_DOUBLE_BLOCK)
            .renderer(() -> StationSignDoubleBlockEntityRenderer::new)
            .register();

    public static void register() {
    }
}

package net.adeptstack.cts.registry;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.adeptstack.cts.blockentities.StationSignBlockEntity;
import net.adeptstack.cts.blockentities.StationSignDoubleBlockEntity;
import net.adeptstack.cts.client.renderer.StationSignBlockEntityRenderer;
import net.adeptstack.cts.client.renderer.StationSignDoubleBlockEntityRenderer;

import static net.adeptstack.cts.Main.REGISTRATE;

public class ModBlockEntities {

    public static final BlockEntityEntry<StationSignBlockEntity> STATION_SIGN_BLOCK_ENTITY = REGISTRATE
            .<StationSignBlockEntity>blockEntity("station_sign_block_entity", StationSignBlockEntity::new)
            .validBlocks(ModBlocks.STATION_SIGN_BLOCK)
            .renderer(() -> StationSignBlockEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<StationSignDoubleBlockEntity> STATION_SIGN_DOUBLE_BLOCK_ENTITY = REGISTRATE
            .<StationSignDoubleBlockEntity>blockEntity("station_sign_double_block_entity", StationSignDoubleBlockEntity::new)
            .validBlocks(ModBlocks.STATION_SIGN_DOUBLE_BLOCK)
            .renderer(() -> StationSignDoubleBlockEntityRenderer::new)
            .register();

    public static void register() {
    }
}

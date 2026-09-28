package net.adeptstack.cts.client.renderer;

import net.adeptstack.cts.blockentities.MastStationSignBlockEntity;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public class MastStationSignBlockColor implements BlockColor {

    @Override
    public int getColor(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
        if (level == null || pos == null || !(level.getBlockEntity(pos) instanceof MastStationSignBlockEntity be)) {
            return 0xFFFFFF;
        }
        return be.getBgColor().getFireworkColor();
    }
}

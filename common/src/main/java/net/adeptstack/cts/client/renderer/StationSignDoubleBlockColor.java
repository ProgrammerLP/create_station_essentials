package net.adeptstack.cts.client.renderer;

import net.adeptstack.cts.blockentities.StationSignDoubleBlockEntity;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public class StationSignDoubleBlockColor implements BlockColor {

    @Override
    public int getColor(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
        if (level == null || pos == null || !(level.getBlockEntity(pos) instanceof StationSignDoubleBlockEntity be)) {
            return 0xFFFFFF;
        }
        DyeColor color = tintIndex == 1 ? be.getBgColorB() : be.getBgColorA();
        return color.getFireworkColor();
    }
}

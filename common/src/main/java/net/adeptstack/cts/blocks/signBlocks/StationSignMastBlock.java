package net.adeptstack.cts.blocks.signBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StationSignMastBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    // Anything a pole segment should visually connect to when stacked above/below.
    public static boolean isMastLike(BlockState state) {
        return state.getBlock() instanceof StationSignMastBlock
                || state.getBlock() instanceof StationSignDoubleBlock
                || state.getBlock() instanceof MastStationSignBlock;
    }

    // A sign block shows its pole segment when it is part of a mast column (mast-like block above
    // or below) or when it stands on something a pole can plausibly rise from: a wall, a fence or
    // any block with a sturdy top face.
    public static boolean needsPole(BlockGetter level, BlockPos pos) {
        if (isMastLike(level.getBlockState(pos.above())) || isMastLike(level.getBlockState(pos.below()))) {
            return true;
        }
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        return below.is(BlockTags.WALLS) || below.is(BlockTags.FENCES) || below.isFaceSturdy(level, belowPos, Direction.UP);
    }

    public StationSignMastBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}

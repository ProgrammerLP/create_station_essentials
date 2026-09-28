package net.adeptstack.cts.blocks.signBlocks;

import net.adeptstack.cts.blockentities.StationSignDoubleBlockEntity;
import net.adeptstack.cts.client.ClientWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class StationSignDoubleBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction.Axis> AXIS = EnumProperty.create("axis", Direction.Axis.class, Direction.Axis.X, Direction.Axis.Z);
    // Only show the connecting pole segment when this block is actually part of a mast column
    // (a mast or another double sign directly above or below) - otherwise it's a bare double sign.
    public static final BooleanProperty POLE = BooleanProperty.create("pole");

    private static final VoxelShape POLE_SHAPE = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape PLATES_X = Shapes.or(
            Block.box(12, 2, 0, 14, 14, 16),
            Block.box(2, 2, 0, 4, 14, 16)
    );
    private static final VoxelShape PLATES_Z = Shapes.or(
            Block.box(0, 2, 12, 16, 14, 14),
            Block.box(0, 2, 2, 16, 14, 4)
    );
    private static final VoxelShape SHAPE_X_POLE = Shapes.or(POLE_SHAPE, PLATES_X);
    private static final VoxelShape SHAPE_Z_POLE = Shapes.or(POLE_SHAPE, PLATES_Z);

    public StationSignDoubleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AXIS, Direction.Axis.X)
                .setValue(POLE, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS, POLE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(AXIS, context.getHorizontalDirection().getAxis())
                .setValue(POLE, StationSignMastBlock.needsPole(context.getLevel(), context.getClickedPos()));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP || direction == Direction.DOWN) {
            return state.setValue(POLE, StationSignMastBlock.needsPole(level, pos));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide) {
            return;
        }
        if (neighborPos.equals(pos.above()) || neighborPos.equals(pos.below())) {
            boolean needsPole = StationSignMastBlock.needsPole(level, pos);
            if (state.getValue(POLE) != needsPole) {
                level.setBlockAndUpdate(pos, state.setValue(POLE, needsPole));
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean axisX = state.getValue(AXIS) == Direction.Axis.X;
        if (state.getValue(POLE)) {
            return axisX ? SHAPE_X_POLE : SHAPE_Z_POLE;
        }
        return axisX ? PLATES_X : PLATES_Z;
    }

    // The two horizontal directions a run of double signs can extend along, perpendicular to the
    // pair of directions the plates themselves face (X-axis signs form a row along Z, and vice versa).
    public static Direction runAxisDirection(Direction.Axis blockAxis) {
        return blockAxis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }

        Direction runDir = runAxisDirection(state.getValue(AXIS));
        for (Direction side : new Direction[]{runDir, runDir.getOpposite()}) {
            BlockPos neighborPos = pos.relative(side);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.is(this) && neighborState.getValue(AXIS) == state.getValue(AXIS)
                    && level.getBlockEntity(neighborPos) instanceof StationSignDoubleBlockEntity neighborBe
                    && level.getBlockEntity(pos) instanceof StationSignDoubleBlockEntity be) {
                be.setContentA(neighborBe.getTextA(), neighborBe.getTextColorA(), neighborBe.getBgColorA());
                be.setContentB(neighborBe.getTextB(), neighborBe.getTextColorB(), neighborBe.getBgColorB());
                return;
            }
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide && level.getBlockEntity(pos) instanceof StationSignDoubleBlockEntity be) {
            ClientWrapper.openStationSignDoubleScreen(pos, state, be);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StationSignDoubleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return null;
    }
}

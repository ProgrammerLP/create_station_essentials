package net.adeptstack.cts.blocks.signBlocks;

import net.adeptstack.cts.blockentities.MastStationSignBlockEntity;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** One-sided station sign that mounts on a mast: a plate on the facing side plus an optional pole segment. */
public class MastStationSignBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POLE = BooleanProperty.create("pole");

    private static final VoxelShape POLE_SHAPE = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape PLATE_EAST = Block.box(12, 2, 0, 14, 14, 16);
    private static final VoxelShape PLATE_SOUTH = Block.box(0, 2, 12, 16, 14, 14);
    private static final VoxelShape PLATE_WEST = Block.box(2, 2, 0, 4, 14, 16);
    private static final VoxelShape PLATE_NORTH = Block.box(0, 2, 2, 16, 14, 4);

    public MastStationSignBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POLE, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POLE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
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

    // Direction along which neighboring signs with the same facing join into one run.
    public static Direction runDirection(Direction facing) {
        return facing.getClockWise();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }

        Direction facing = state.getValue(FACING);
        Direction runDir = runDirection(facing);
        for (Direction side : new Direction[]{runDir, runDir.getOpposite()}) {
            BlockPos neighborPos = pos.relative(side);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.is(this) && neighborState.getValue(FACING) == facing
                    && level.getBlockEntity(neighborPos) instanceof MastStationSignBlockEntity neighborBe
                    && level.getBlockEntity(pos) instanceof MastStationSignBlockEntity be) {
                be.setContent(neighborBe.getText(), neighborBe.getTextColor(), neighborBe.getBgColor());
                return;
            }
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape plate = switch (state.getValue(FACING)) {
            case SOUTH -> PLATE_SOUTH;
            case WEST -> PLATE_WEST;
            case EAST -> PLATE_EAST;
            default -> PLATE_NORTH;
        };
        return state.getValue(POLE) ? Shapes.or(POLE_SHAPE, plate) : plate;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide && level.getBlockEntity(pos) instanceof MastStationSignBlockEntity be) {
            ClientWrapper.openMastStationSignScreen(pos, be);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MastStationSignBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return null;
    }
}

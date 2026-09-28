package net.adeptstack.cts.blocks.signBlocks;

import net.adeptstack.cts.blockentities.StationSignBlockEntity;
import net.adeptstack.cts.client.ClientWrapper;
import net.adeptstack.cts.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class StationSignBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DyeColor> BG_COLOR = EnumProperty.create("bg_color", DyeColor.class);

    private static final VoxelShape SHAPE_NORTH = Block.box(0, 2, 14, 16, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 2, 0, 16, 14, 2);
    private static final VoxelShape SHAPE_WEST = Block.box(14, 2, 0, 16, 14, 16);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 2, 0, 2, 14, 16);

    public StationSignBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(BG_COLOR, DyeColor.BLUE)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, BG_COLOR);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);

        for (Direction side : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockPos neighborPos = context.getClickedPos().relative(side);
            BlockState neighborState = context.getLevel().getBlockState(neighborPos);
            if (neighborState.is(this) && neighborState.getValue(FACING) == facing) {
                return state.setValue(BG_COLOR, neighborState.getValue(BG_COLOR));
            }
        }

        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }

        Direction facing = state.getValue(FACING);
        for (Direction side : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockPos neighborPos = pos.relative(side);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.is(this) && neighborState.getValue(FACING) == facing
                    && level.getBlockEntity(neighborPos) instanceof StationSignBlockEntity neighborBe
                    && level.getBlockEntity(pos) instanceof StationSignBlockEntity be) {
                be.setContent(neighborBe.getText(), neighborBe.getTextColor());
                return;
            }
        }
    }

    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, BlockHitResult hit) {
        if (level.isClientSide && level.getBlockEntity(pos) instanceof StationSignBlockEntity be) {
            ClientWrapper.openStationSignScreen(pos, state, be);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StationSignBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return null;
    }
}

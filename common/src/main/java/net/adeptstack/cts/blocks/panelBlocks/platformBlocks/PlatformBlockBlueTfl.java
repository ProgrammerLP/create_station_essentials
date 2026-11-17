package net.adeptstack.cts.blocks.panelBlocks.platformBlocks;

import net.adeptstack.cts.blocks.panelBlocks.PanelBlockBase;
import net.adeptstack.cts.client.ClientWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class PlatformBlockBlueTfl extends PanelBlockBase {

    public static final IntegerProperty SIGN_BLOCKS = IntegerProperty.create("signblock", 0, 20);

    public PlatformBlockBlueTfl(Properties p_49795_) {
        super(p_49795_);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(SIGN_BLOCKS, 0)
        );
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(SIGN_BLOCKS);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Players who may not build (adventure, spectator) must not change the sign.
        if (!player.mayBuild()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            ClientWrapper.openPlatformBlockBlueTflScreen(pos, state);
        }
        return InteractionResult.SUCCESS;
    }
}

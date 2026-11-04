package net.adeptstack.cts.network.packets;

import dev.architectury.networking.NetworkManager;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockCH;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockDE;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockNL;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public class PlatformBlockPacket {

    /** Generous reach check (8 blocks) measured from the eyes to the block centre. */
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    public final BlockPos pos;
    public final int signblock;


    public PlatformBlockPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readInt());
    }

    public PlatformBlockPacket(BlockPos pos, int signblock) {
        this.pos = pos;
        this.signblock = signblock;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(signblock);
    }

    public void apply(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            Player player = contextSupplier.get().getPlayer();
            Level level = player.level();
            // The server decides: no changes from players who may not build (adventure, spectator),
            // from too far away, or in unloaded chunks.
            if (!player.mayBuild() || !level.isLoaded(pos)
                    || player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) {
                return;
            }
            BlockState state = level.getBlockState(pos);
            IntegerProperty property;
            if (state.getBlock() instanceof PlatformBlockNL) {
                property = PlatformBlockNL.SIGN_BLOCKS;
            } else if (state.getBlock() instanceof PlatformBlockDE) {
                property = PlatformBlockDE.SIGN_BLOCKS;
            } else if (state.getBlock() instanceof PlatformBlockCH) {
                property = PlatformBlockCH.SIGN_BLOCKS;
            } else {
                return;
            }
            if (!property.getPossibleValues().contains(signblock)) {
                return;
            }
            level.setBlockAndUpdate(pos, state.setValue(property, signblock));
        });
    }
}

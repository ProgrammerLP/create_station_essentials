package net.adeptstack.cts.network.packets;

import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.mcdragonlib.util.NbtUtils;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockCH;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockBlueTfl;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockDE;
import net.adeptstack.cts.blocks.panelBlocks.platformBlocks.PlatformBlockNL;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class PlatformBlockPacket extends NetworkPacketData {

    public BlockPos pos;
    public int signblock;

    public PlatformBlockPacket(DLStatus status) {
        super(status);
    }

    public PlatformBlockPacket(BlockPos pos, int signblock) {
        super(DLStatus.OK);
        this.pos = pos;
        this.signblock = signblock;
    }

    @Override
    protected void write(CompoundTag nbt) {
        NbtUtils.putNbtPos(nbt, "pos", pos);
        nbt.putInt("signblock", signblock);
    }
    @Override
    protected void read(CompoundTag nbt) {
        this.pos = NbtUtils.getNbtBlockPos(nbt, "pos");
        this.signblock = nbt.getInt("signblock");
    }

    public static void handle(PlatformBlockPacket packet, NetworkPacketContext context) {
        apply(packet, context);
    }


    public static void apply(PlatformBlockPacket packet, NetworkPacketContext context) {
        context.queue(() -> {
            Player player = context.getPlayer();
            Level level = player.level();
            // The server decides: no changes from players who may not build (adventure, spectator),
            // from too far away, or in unloaded chunks.
            if (!player.mayBuild() || !level.isLoaded(packet.pos) || !player.canInteractWithBlock(packet.pos, 1.0)) {
                return;
            }
            BlockState state = level.getBlockState(packet.pos);
            IntegerProperty property;
            if (state.getBlock() instanceof PlatformBlockNL) {
                property = PlatformBlockNL.SIGN_BLOCKS;
            } else if (state.getBlock() instanceof PlatformBlockDE) {
                property = PlatformBlockDE.SIGN_BLOCKS;
            } else if (state.getBlock() instanceof PlatformBlockCH) {
                property = PlatformBlockCH.SIGN_BLOCKS;
            } else if (state.getBlock() instanceof PlatformBlockBlueTfl) {
                property = PlatformBlockBlueTfl.SIGN_BLOCKS;
            } else {
                return;
            }
            if (!property.getPossibleValues().contains(packet.signblock)) {
                return;
            }
            level.setBlockAndUpdate(packet.pos, state.setValue(property, packet.signblock));
        });
    }
}

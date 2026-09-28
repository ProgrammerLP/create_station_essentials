package net.adeptstack.cts.network.packets;

import dev.architectury.networking.NetworkManager;
import net.adeptstack.cts.blockentities.StationSignDoubleBlockEntity;
import net.adeptstack.cts.blocks.signBlocks.StationSignDoubleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class StationSignDoublePacket {

    private static final int MAX_TEXT_LENGTH = 64;
    private static final int MAX_RUN_SCAN = 64;

    public final BlockPos pos;
    public final String textA;
    public final int textColorA;
    public final int bgColorA;
    public final String textB;
    public final int textColorB;
    public final int bgColorB;

    public StationSignDoublePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.textA = buf.readUtf(MAX_TEXT_LENGTH);
        this.textColorA = buf.readInt();
        this.bgColorA = buf.readInt();
        this.textB = buf.readUtf(MAX_TEXT_LENGTH);
        this.textColorB = buf.readInt();
        this.bgColorB = buf.readInt();
    }

    public StationSignDoublePacket(BlockPos pos, String textA, DyeColor textColorA, DyeColor bgColorA, String textB, DyeColor textColorB, DyeColor bgColorB) {
        this.pos = pos;
        this.textA = textA.length() > MAX_TEXT_LENGTH ? textA.substring(0, MAX_TEXT_LENGTH) : textA;
        this.textColorA = textColorA.getId();
        this.bgColorA = bgColorA.getId();
        this.textB = textB.length() > MAX_TEXT_LENGTH ? textB.substring(0, MAX_TEXT_LENGTH) : textB;
        this.textColorB = textColorB.getId();
        this.bgColorB = bgColorB.getId();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(textA, MAX_TEXT_LENGTH);
        buf.writeInt(textColorA);
        buf.writeInt(bgColorA);
        buf.writeUtf(textB, MAX_TEXT_LENGTH);
        buf.writeInt(textColorB);
        buf.writeInt(bgColorB);
    }

    public void apply(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            Level level = contextSupplier.get().getPlayer().level();
            BlockState originState = level.getBlockState(pos);
            if (!(originState.getBlock() instanceof StationSignDoubleBlock)) {
                return;
            }

            Direction.Axis axis = originState.getValue(StationSignDoubleBlock.AXIS);
            applyTo(level, pos);

            Direction runDir = StationSignDoubleBlock.runAxisDirection(axis);
            for (Direction side : new Direction[]{runDir, runDir.getOpposite()}) {
                BlockPos cursor = pos.relative(side);
                for (int i = 0; i < MAX_RUN_SCAN; i++) {
                    BlockState state = level.getBlockState(cursor);
                    if (!(state.getBlock() instanceof StationSignDoubleBlock) || state.getValue(StationSignDoubleBlock.AXIS) != axis) {
                        break;
                    }
                    applyTo(level, cursor);
                    cursor = cursor.relative(side);
                }
            }
        });
    }

    private void applyTo(Level level, BlockPos targetPos) {
        if (level.getBlockEntity(targetPos) instanceof StationSignDoubleBlockEntity be) {
            be.setContentA(textA, DyeColor.byId(textColorA), DyeColor.byId(bgColorA));
            be.setContentB(textB, DyeColor.byId(textColorB), DyeColor.byId(bgColorB));
        }
    }
}

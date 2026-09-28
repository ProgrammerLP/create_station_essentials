package net.adeptstack.cts.network.packets;

import dev.architectury.networking.NetworkManager;
import net.adeptstack.cts.blockentities.MastStationSignBlockEntity;
import net.adeptstack.cts.blocks.signBlocks.MastStationSignBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class MastStationSignPacket {

    private static final int MAX_TEXT_LENGTH = 64;
    private static final int MAX_RUN_SCAN = 64;

    public final BlockPos pos;
    public final String text;
    public final int textColorId;
    public final int bgColorId;

    public MastStationSignPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.text = buf.readUtf(MAX_TEXT_LENGTH);
        this.textColorId = buf.readInt();
        this.bgColorId = buf.readInt();
    }

    public MastStationSignPacket(BlockPos pos, String text, DyeColor textColor, DyeColor bgColor) {
        this.pos = pos;
        this.text = text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;
        this.textColorId = textColor.getId();
        this.bgColorId = bgColor.getId();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(text, MAX_TEXT_LENGTH);
        buf.writeInt(textColorId);
        buf.writeInt(bgColorId);
    }

    public void apply(Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            Level level = contextSupplier.get().getPlayer().level();
            BlockState originState = level.getBlockState(pos);
            if (!(originState.getBlock() instanceof MastStationSignBlock)) {
                return;
            }

            Direction facing = originState.getValue(MastStationSignBlock.FACING);
            applyTo(level, pos);

            Direction runDir = MastStationSignBlock.runDirection(facing);
            for (Direction side : new Direction[]{runDir, runDir.getOpposite()}) {
                BlockPos cursor = pos.relative(side);
                for (int i = 0; i < MAX_RUN_SCAN; i++) {
                    BlockState state = level.getBlockState(cursor);
                    if (!(state.getBlock() instanceof MastStationSignBlock) || state.getValue(MastStationSignBlock.FACING) != facing) {
                        break;
                    }
                    applyTo(level, cursor);
                    cursor = cursor.relative(side);
                }
            }
        });
    }

    private void applyTo(Level level, BlockPos targetPos) {
        if (level.getBlockEntity(targetPos) instanceof MastStationSignBlockEntity be) {
            be.setContent(text, DyeColor.byId(textColorId), DyeColor.byId(bgColorId));
        }
    }
}

package net.adeptstack.cts.network.packets;

import dev.architectury.networking.NetworkManager;
import net.adeptstack.cts.blockentities.WallStationSignBlockEntity;
import net.adeptstack.cts.blocks.signBlocks.WallStationSignBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class WallStationSignPacket {

    private static final int MAX_TEXT_LENGTH = 64;
    private static final int MAX_RUN_SCAN = 64;

    public final BlockPos pos;
    public final String text;
    public final int textColorId;
    public final int bgColorId;

    public WallStationSignPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.text = buf.readUtf(MAX_TEXT_LENGTH);
        this.textColorId = buf.readInt();
        this.bgColorId = buf.readInt();
    }

    public WallStationSignPacket(BlockPos pos, String text, DyeColor textColor, DyeColor bgColor) {
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
            if (!(originState.getBlock() instanceof WallStationSignBlock)) {
                return;
            }

            DyeColor textColor = DyeColor.byId(textColorId);
            DyeColor bgColor = DyeColor.byId(bgColorId);
            Direction facing = originState.getValue(WallStationSignBlock.FACING);

            applyTo(level, pos, originState, textColor, bgColor);

            for (Direction side : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
                BlockPos cursor = pos.relative(side);
                for (int i = 0; i < MAX_RUN_SCAN; i++) {
                    BlockState state = level.getBlockState(cursor);
                    if (!(state.getBlock() instanceof WallStationSignBlock) || state.getValue(WallStationSignBlock.FACING) != facing) {
                        break;
                    }
                    applyTo(level, cursor, state, textColor, bgColor);
                    cursor = cursor.relative(side);
                }
            }
        });
    }

    private void applyTo(Level level, BlockPos targetPos, BlockState state, DyeColor textColor, DyeColor bgColor) {
        if (state.getValue(WallStationSignBlock.BG_COLOR) != bgColor) {
            level.setBlockAndUpdate(targetPos, state.setValue(WallStationSignBlock.BG_COLOR, bgColor));
        }
        if (level.getBlockEntity(targetPos) instanceof WallStationSignBlockEntity be) {
            be.setContent(text, textColor);
        }
    }
}

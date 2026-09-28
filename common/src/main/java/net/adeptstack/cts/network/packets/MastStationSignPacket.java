package net.adeptstack.cts.network.packets;

import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.mcdragonlib.util.NbtUtils;
import net.adeptstack.cts.blockentities.MastStationSignBlockEntity;
import net.adeptstack.cts.blocks.signBlocks.MastStationSignBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MastStationSignPacket extends NetworkPacketData {

    private static final int MAX_RUN_SCAN = 64;

    public BlockPos pos;
    public String text;
    public int textColorId;
    public int bgColorId;

    public MastStationSignPacket(DLStatus status) {
        super(status);
    }

    public MastStationSignPacket(BlockPos pos, String text, DyeColor textColor, DyeColor bgColor) {
        super(DLStatus.OK);
        this.pos = pos;
        this.text = text;
        this.textColorId = textColor.getId();
        this.bgColorId = bgColor.getId();
    }

    @Override
    protected void write(CompoundTag nbt) {
        NbtUtils.putNbtPos(nbt, "pos", pos);
        nbt.putString("text", text);
        nbt.putInt("textColor", textColorId);
        nbt.putInt("bgColor", bgColorId);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.pos = NbtUtils.getNbtBlockPos(nbt, "pos");
        this.text = nbt.getString("text");
        this.textColorId = nbt.getInt("textColor");
        this.bgColorId = nbt.getInt("bgColor");
    }

    public static void handle(MastStationSignPacket packet, NetworkPacketContext context) {
        apply(packet, context);
    }

    public static void apply(MastStationSignPacket packet, NetworkPacketContext context) {
        context.queue(() -> {
            Level level = context.getPlayer().level();
            BlockState originState = level.getBlockState(packet.pos);
            if (!(originState.getBlock() instanceof MastStationSignBlock)) {
                return;
            }

            String text = packet.text.length() > 64 ? packet.text.substring(0, 64) : packet.text;
            DyeColor textColor = DyeColor.byId(packet.textColorId);
            DyeColor bgColor = DyeColor.byId(packet.bgColorId);
            Direction facing = originState.getValue(MastStationSignBlock.FACING);

            applyTo(level, packet.pos, text, textColor, bgColor);

            Direction runDir = MastStationSignBlock.runDirection(facing);
            for (Direction side : new Direction[]{runDir, runDir.getOpposite()}) {
                BlockPos cursor = packet.pos.relative(side);
                for (int i = 0; i < MAX_RUN_SCAN; i++) {
                    BlockState state = level.getBlockState(cursor);
                    if (!(state.getBlock() instanceof MastStationSignBlock) || state.getValue(MastStationSignBlock.FACING) != facing) {
                        break;
                    }
                    applyTo(level, cursor, text, textColor, bgColor);
                    cursor = cursor.relative(side);
                }
            }
        });
    }

    private static void applyTo(Level level, BlockPos pos, String text, DyeColor textColor, DyeColor bgColor) {
        if (level.getBlockEntity(pos) instanceof MastStationSignBlockEntity be) {
            be.setContent(text, textColor, bgColor);
        }
    }
}

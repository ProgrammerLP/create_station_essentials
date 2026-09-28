package net.adeptstack.cts.network.packets;

import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.mcdragonlib.util.NbtUtils;
import net.adeptstack.cts.blockentities.StationSignDoubleBlockEntity;
import net.adeptstack.cts.blocks.signBlocks.StationSignDoubleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class StationSignDoublePacket extends NetworkPacketData {

    public BlockPos pos;
    public String textA;
    public int textColorA;
    public int bgColorA;
    public String textB;
    public int textColorB;
    public int bgColorB;

    public StationSignDoublePacket(DLStatus status) {
        super(status);
    }

    public StationSignDoublePacket(BlockPos pos, String textA, DyeColor textColorA, DyeColor bgColorA,
                                    String textB, DyeColor textColorB, DyeColor bgColorB) {
        super(DLStatus.OK);
        this.pos = pos;
        this.textA = textA;
        this.textColorA = textColorA.getId();
        this.bgColorA = bgColorA.getId();
        this.textB = textB;
        this.textColorB = textColorB.getId();
        this.bgColorB = bgColorB.getId();
    }

    @Override
    protected void write(CompoundTag nbt) {
        NbtUtils.putNbtPos(nbt, "pos", pos);
        nbt.putString("textA", textA);
        nbt.putInt("textColorA", textColorA);
        nbt.putInt("bgColorA", bgColorA);
        nbt.putString("textB", textB);
        nbt.putInt("textColorB", textColorB);
        nbt.putInt("bgColorB", bgColorB);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.pos = NbtUtils.getNbtBlockPos(nbt, "pos");
        this.textA = nbt.getString("textA");
        this.textColorA = nbt.getInt("textColorA");
        this.bgColorA = nbt.getInt("bgColorA");
        this.textB = nbt.getString("textB");
        this.textColorB = nbt.getInt("textColorB");
        this.bgColorB = nbt.getInt("bgColorB");
    }

    public static void handle(StationSignDoublePacket packet, NetworkPacketContext context) {
        apply(packet, context);
    }

    private static final int MAX_RUN_SCAN = 64;

    public static void apply(StationSignDoublePacket packet, NetworkPacketContext context) {
        context.queue(() -> {
            Level level = context.getPlayer().level();
            BlockState originState = level.getBlockState(packet.pos);
            if (!(originState.getBlock() instanceof StationSignDoubleBlock)) {
                return;
            }

            String textA = packet.textA.length() > 64 ? packet.textA.substring(0, 64) : packet.textA;
            String textB = packet.textB.length() > 64 ? packet.textB.substring(0, 64) : packet.textB;
            DyeColor textColorA = DyeColor.byId(packet.textColorA);
            DyeColor bgColorA = DyeColor.byId(packet.bgColorA);
            DyeColor textColorB = DyeColor.byId(packet.textColorB);
            DyeColor bgColorB = DyeColor.byId(packet.bgColorB);
            Direction.Axis axis = originState.getValue(StationSignDoubleBlock.AXIS);

            applyTo(level, packet.pos, textA, textColorA, bgColorA, textB, textColorB, bgColorB);

            Direction runDir = StationSignDoubleBlock.runAxisDirection(axis);
            for (Direction side : new Direction[]{runDir, runDir.getOpposite()}) {
                BlockPos cursor = packet.pos.relative(side);
                for (int i = 0; i < MAX_RUN_SCAN; i++) {
                    BlockState state = level.getBlockState(cursor);
                    if (!(state.getBlock() instanceof StationSignDoubleBlock) || state.getValue(StationSignDoubleBlock.AXIS) != axis) {
                        break;
                    }
                    applyTo(level, cursor, textA, textColorA, bgColorA, textB, textColorB, bgColorB);
                    cursor = cursor.relative(side);
                }
            }
        });
    }

    private static void applyTo(Level level, BlockPos pos, String textA, DyeColor textColorA, DyeColor bgColorA,
                                 String textB, DyeColor textColorB, DyeColor bgColorB) {
        if (level.getBlockEntity(pos) instanceof StationSignDoubleBlockEntity be) {
            be.setContentA(textA, textColorA, bgColorA);
            be.setContentB(textB, textColorB, bgColorB);
        }
    }
}

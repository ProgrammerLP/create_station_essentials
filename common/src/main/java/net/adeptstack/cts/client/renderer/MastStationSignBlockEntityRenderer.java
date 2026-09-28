package net.adeptstack.cts.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.adeptstack.cts.blockentities.MastStationSignBlockEntity;
import net.adeptstack.cts.blocks.signBlocks.MastStationSignBlock;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

public class MastStationSignBlockEntityRenderer implements BlockEntityRenderer<MastStationSignBlockEntity> {

    private static final int MAX_RUN_SCAN = 64;
    private static final float RUN_PADDING_BLOCKS = 0.12f;
    // The plate sits at 12-14 (or the mirrored equivalent), so the text anchor has to clear its
    // outer face at 0.875 from center to not render behind the plate as seen from outside.
    private static final float FACE_OFFSET = 0.4f;
    private static final float NATURAL_SCALE = 0.05f;

    // Text-forward world direction per facing, precomputed once - only 4 facings are possible, so
    // there's no reason to redo the quaternion rotation (and its allocations) on every render call.
    private static final Direction[] TEXT_FORWARD_BY_FACING = buildTextForwardTable();

    private final Font font;

    // Per-block-entity run cache, valid for a single game tick. render() and getRenderBoundingBox()
    // are both called independently, once or more per frame, for the leader block of a run - without
    // this they'd redundantly re-scan the whole (up to 64-block) chain on every one of those calls.
    private final Map<MastStationSignBlockEntity, CachedRun> runCache = new WeakHashMap<>();

    public MastStationSignBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(MastStationSignBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof MastStationSignBlock)) {
            return;
        }

        String text = be.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        Direction facing = state.getValue(MastStationSignBlock.FACING);
        RunInfo run = computeRun(level, be, be.getBlockPos(), facing);

        // Only the leading block of a connected run draws, in one call across the whole run.
        if (run.leftCount() != 0) {
            return;
        }
        int totalLength = run.totalLength();

        int textPixelWidth = font.width(text);
        if (textPixelWidth <= 0) {
            return;
        }

        float availableWidth = Math.max(0.1f, totalLength - RUN_PADDING_BLOCKS * 2f);
        float scale = Math.min(NATURAL_SCALE, availableWidth / textPixelWidth);
        float startOffsetBlocks = (totalLength - textPixelWidth * scale) / 2f;

        double anchorX = 0.5 + FACE_OFFSET * facing.getStepX();
        double anchorZ = 0.5 + FACE_OFFSET * facing.getStepZ();

        poseStack.pushPose();
        poseStack.translate(anchorX, 0.5, anchorZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotFor(facing)));
        poseStack.scale(scale, -scale, scale);

        Matrix4f pose = poseStack.last().pose();
        int color = 0xFF000000 | be.getTextColor().getTextColor();
        float centerLineY = -font.lineHeight / 2f;
        float pixelOffset = (startOffsetBlocks - 0.5f) / scale;

        font.drawInBatch(text, pixelOffset, centerLineY, color, false, pose, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(MastStationSignBlockEntity be) {
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        if (level == null || !(be.getBlockState().getBlock() instanceof MastStationSignBlock)) {
            return new AABB(pos);
        }
        Direction facing = be.getBlockState().getValue(MastStationSignBlock.FACING);
        Direction forward = TEXT_FORWARD_BY_FACING[facing.ordinal()];

        // Non-leader blocks never draw anything beyond their own block (see render() above), so
        // there's no need to scan the whole run just to build their culling box - a single scan in
        // the "backward" direction is enough to tell leaders and non-leaders apart.
        int leftCount = countRun(level, pos, forward.getOpposite(), facing);
        if (leftCount != 0) {
            return new AABB(pos);
        }

        RunInfo run = computeRun(level, be, pos, facing);
        BlockPos start = pos.relative(forward.getOpposite(), run.leftCount());
        BlockPos end = pos.relative(forward, run.rightCount());
        return new AABB(start.getX(), start.getY(), start.getZ(), end.getX() + 1, end.getY() + 1, end.getZ() + 1);
    }

    private RunInfo computeRun(Level level, MastStationSignBlockEntity be, BlockPos pos, Direction facing) {
        long tick = level.getGameTime();
        CachedRun cached = runCache.get(be);
        if (cached != null && cached.tick() == tick) {
            return cached.run();
        }

        Direction textForwardDir = TEXT_FORWARD_BY_FACING[facing.ordinal()];
        int leftCount = countRun(level, pos, textForwardDir.getOpposite(), facing);
        int rightCount = countRun(level, pos, textForwardDir, facing);
        RunInfo run = new RunInfo(leftCount, rightCount);
        runCache.put(be, new CachedRun(tick, run));
        return run;
    }

    private static int countRun(Level level, BlockPos origin, Direction step, Direction requiredFacing) {
        int count = 0;
        BlockPos cursor = origin.relative(step);
        for (int i = 0; i < MAX_RUN_SCAN; i++) {
            BlockState state = level.getBlockState(cursor);
            if (!(state.getBlock() instanceof MastStationSignBlock) || state.getValue(MastStationSignBlock.FACING) != requiredFacing) {
                break;
            }
            count++;
            cursor = cursor.relative(step);
        }
        return count;
    }

    private record RunInfo(int leftCount, int rightCount) {
        int totalLength() {
            return leftCount + rightCount + 1;
        }
    }

    private record CachedRun(long tick, RunInfo run) {
    }

    private static Direction[] buildTextForwardTable() {
        // Which world direction the rotated text advances in - same handedness quirk as the other signs.
        Direction[] table = new Direction[Direction.values().length];
        for (Direction facing : Direction.values()) {
            Vector3f localXInWorld = new Quaternionf().rotateY((float) Math.toRadians(yRotFor(facing))).transform(new Vector3f(1, 0, 0));
            table[facing.ordinal()] = Direction.getNearest(localXInWorld.x(), localXInWorld.y(), localXInWorld.z());
        }
        return table;
    }

    private static float yRotFor(Direction facing) {
        return switch (facing) {
            case NORTH -> 180f;
            case WEST -> 270f;
            case EAST -> 90f;
            default -> 0f; // SOUTH
        };
    }
}

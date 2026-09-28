package net.adeptstack.cts.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.adeptstack.cts.blockentities.StationSignDoubleBlockEntity;
import net.adeptstack.cts.blocks.signBlocks.StationSignDoubleBlock;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

public class StationSignDoubleBlockEntityRenderer implements BlockEntityRenderer<StationSignDoubleBlockEntity> {

    private static final int MAX_RUN_SCAN = 64;
    private static final float RUN_PADDING_BLOCKS = 0.12f;
    // Plates sit at x/z 2-4 and 12-14 (touching the pole), so the text anchor has to clear their
    // own outer face (at 0.875 from center) to not render behind the plate as seen from outside.
    private static final float FACE_OFFSET = 0.4f;
    private static final float NATURAL_SCALE = 0.05f;

    // Text-forward world direction per facing, precomputed once - only 4 facings are possible, so
    // there's no reason to redo the quaternion rotation (and its allocations) on every render call.
    private static final Direction[] TEXT_FORWARD_BY_FACING = buildTextForwardTable();

    private final Font font;

    // Per-block-entity run cache (one slot per side), valid for a single game tick. render() and
    // getRenderBoundingBox() are both called independently, once or more per frame, for the leader
    // block of a run - without this they'd redundantly re-scan the whole (up to 64-block) chain,
    // for both sides, on every one of those calls.
    private final Map<StationSignDoubleBlockEntity, CachedRun> runCache = new WeakHashMap<>();

    public StationSignDoubleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(StationSignDoubleBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof StationSignDoubleBlock)) {
            return;
        }

        Direction.Axis axis = state.getValue(StationSignDoubleBlock.AXIS);
        boolean axisX = axis == Direction.Axis.X;
        Direction sideADir = axisX ? Direction.EAST : Direction.SOUTH;
        Direction sideBDir = axisX ? Direction.WEST : Direction.NORTH;

        drawSide(level, be, axis, be.getBlockPos(), poseStack, bufferSource, packedLight, sideADir, be.getTextA(), be.getTextColorA(), 0);
        drawSide(level, be, axis, be.getBlockPos(), poseStack, bufferSource, packedLight, sideBDir, be.getTextB(), be.getTextColorB(), 1);
    }

    private void drawSide(Level level, StationSignDoubleBlockEntity be, Direction.Axis axis, BlockPos pos, PoseStack poseStack,
                           MultiBufferSource bufferSource, int packedLight, Direction facing, String text, DyeColor textColor, int sideIndex) {
        if (text == null || text.isBlank()) {
            return;
        }

        RunInfo run = computeSideRun(level, be, pos, axis, facing, sideIndex);
        if (run.leftCount() != 0) {
            return; // not the leader for this side's run
        }
        int totalLength = run.totalLength();

        int textPixelWidth = font.width(text);
        if (textPixelWidth <= 0) {
            return;
        }

        float availableWidth = Math.max(0.1f, totalLength - RUN_PADDING_BLOCKS * 2f);
        float fitScale = availableWidth / textPixelWidth;
        float scale = Math.min(NATURAL_SCALE, fitScale);

        float renderedTextWidth = textPixelWidth * scale;
        float startOffsetBlocks = (totalLength - renderedTextWidth) / 2f;

        double anchorX = 0.5 + FACE_OFFSET * facing.getStepX();
        double anchorZ = 0.5 + FACE_OFFSET * facing.getStepZ();

        poseStack.pushPose();
        poseStack.translate(anchorX, 0.5, anchorZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotFor(facing)));
        poseStack.scale(scale, -scale, scale);

        Matrix4f pose = poseStack.last().pose();
        int color = 0xFF000000 | textColor.getTextColor();
        float centerLineY = -font.lineHeight / 2f;
        float pixelOffset = (startOffsetBlocks - 0.5f) / scale;

        font.drawInBatch(text, pixelOffset, centerLineY, color, false, pose, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(StationSignDoubleBlockEntity be) {
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        if (level == null) {
            return new AABB(pos);
        }
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof StationSignDoubleBlock)) {
            return new AABB(pos);
        }

        Direction.Axis axis = state.getValue(StationSignDoubleBlock.AXIS);
        boolean axisX = axis == Direction.Axis.X;
        Direction sideADir = axisX ? Direction.EAST : Direction.SOUTH;
        Direction sideBDir = axisX ? Direction.WEST : Direction.NORTH;

        AABB box = new AABB(pos);
        Direction[] sides = {sideADir, sideBDir};
        for (int sideIndex = 0; sideIndex < sides.length; sideIndex++) {
            Direction facing = sides[sideIndex];
            Direction textForwardDir = TEXT_FORWARD_BY_FACING[facing.ordinal()];

            // Non-leader blocks never draw anything beyond their own block, so there's no need to
            // scan the whole run for this side just to build the culling box - a single scan in the
            // "backward" direction is enough to tell leaders and non-leaders apart.
            int leftCount = countRun(level, pos, textForwardDir.getOpposite(), axis);
            if (leftCount != 0) {
                continue;
            }

            RunInfo run = computeSideRun(level, be, pos, axis, facing, sideIndex);
            BlockPos start = pos.relative(textForwardDir.getOpposite(), run.leftCount());
            BlockPos end = pos.relative(textForwardDir, run.rightCount());
            box = box.minmax(new AABB(start.getX(), start.getY(), start.getZ(), end.getX() + 1, end.getY() + 1, end.getZ() + 1));
        }
        return box;
    }

    private RunInfo computeSideRun(Level level, StationSignDoubleBlockEntity be, BlockPos pos, Direction.Axis axis, Direction facing, int sideIndex) {
        long tick = level.getGameTime();
        CachedRun cached = runCache.get(be);
        if (cached == null || cached.tick != tick) {
            cached = new CachedRun(tick);
            runCache.put(be, cached);
        }
        RunInfo run = cached.runs[sideIndex];
        if (run != null) {
            return run;
        }

        Direction textForwardDir = TEXT_FORWARD_BY_FACING[facing.ordinal()];
        int leftCount = countRun(level, pos, textForwardDir.getOpposite(), axis);
        int rightCount = countRun(level, pos, textForwardDir, axis);
        run = new RunInfo(leftCount, rightCount);
        cached.runs[sideIndex] = run;
        return run;
    }

    private static int countRun(Level level, BlockPos origin, Direction step, Direction.Axis requiredAxis) {
        int count = 0;
        BlockPos cursor = origin.relative(step);
        for (int i = 0; i < MAX_RUN_SCAN; i++) {
            BlockState state = level.getBlockState(cursor);
            if (!(state.getBlock() instanceof StationSignDoubleBlock) || state.getValue(StationSignDoubleBlock.AXIS) != requiredAxis) {
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

    private static final class CachedRun {
        final long tick;
        final RunInfo[] runs = new RunInfo[2];

        CachedRun(long tick) {
            this.tick = tick;
        }
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

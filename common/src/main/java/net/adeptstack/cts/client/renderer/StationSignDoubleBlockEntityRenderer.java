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

public class StationSignDoubleBlockEntityRenderer implements BlockEntityRenderer<StationSignDoubleBlockEntity> {

    private static final int MAX_RUN_SCAN = 64;
    private static final float RUN_PADDING_BLOCKS = 0.12f;
    // Plates sit at x/z 2-4 and 12-14 (touching the pole), so the text anchor has to clear their
    // own outer face (at 0.875 from center) to not render behind the plate as seen from outside.
    private static final float FACE_OFFSET = 0.4f;
    private static final float NATURAL_SCALE = 0.05f;

    private final Font font;

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

        drawSide(level, be, axis, be.getBlockPos(), poseStack, bufferSource, packedLight, sideADir, be.getTextA(), be.getTextColorA());
        drawSide(level, be, axis, be.getBlockPos(), poseStack, bufferSource, packedLight, sideBDir, be.getTextB(), be.getTextColorB());
    }

    private void drawSide(Level level, StationSignDoubleBlockEntity be, Direction.Axis axis, BlockPos pos, PoseStack poseStack,
                           MultiBufferSource bufferSource, int packedLight, Direction facing, String text, DyeColor textColor) {
        if (text == null || text.isBlank()) {
            return;
        }

        // Which world direction this side's text actually advances in once rotated - the same
        // handedness quirk as the single sign, worked out at runtime instead of guessed by hand.
        Vector3f localXInWorld = new Quaternionf().rotateY((float) Math.toRadians(yRotFor(facing))).transform(new Vector3f(1, 0, 0));
        Direction textForwardDir = Direction.getNearest(localXInWorld.x(), localXInWorld.y(), localXInWorld.z());

        int leftCount = countRun(level, pos, textForwardDir.getOpposite(), axis);
        if (leftCount != 0) {
            return; // not the leader for this side's run
        }
        int rightCount = countRun(level, pos, textForwardDir, axis);
        int totalLength = leftCount + rightCount + 1;

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

    public static AABB computeRenderBoundingBox(StationSignDoubleBlockEntity be) {
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
        for (Direction facing : new Direction[]{sideADir, sideBDir}) {
            Vector3f localXInWorld = new Quaternionf().rotateY((float) Math.toRadians(yRotFor(facing))).transform(new Vector3f(1, 0, 0));
            Direction textForwardDir = Direction.getNearest(localXInWorld.x(), localXInWorld.y(), localXInWorld.z());
            int leftCount = countRun(level, pos, textForwardDir.getOpposite(), axis);
            int rightCount = countRun(level, pos, textForwardDir, axis);
            BlockPos start = pos.relative(textForwardDir.getOpposite(), leftCount);
            BlockPos end = pos.relative(textForwardDir, rightCount);
            box = box.minmax(new AABB(start.getX(), start.getY(), start.getZ(), end.getX() + 1, end.getY() + 1, end.getZ() + 1));
        }
        return box;
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

    private static float yRotFor(Direction facing) {
        return switch (facing) {
            case NORTH -> 180f;
            case WEST -> 270f;
            case EAST -> 90f;
            default -> 0f; // SOUTH
        };
    }
}

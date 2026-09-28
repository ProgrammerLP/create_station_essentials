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

public class MastStationSignBlockEntityRenderer implements BlockEntityRenderer<MastStationSignBlockEntity> {

    private static final int MAX_RUN_SCAN = 64;
    private static final float RUN_PADDING_BLOCKS = 0.12f;
    // The plate sits at 12-14 (or the mirrored equivalent), so the text anchor has to clear its
    // outer face at 0.875 from center to not render behind the plate as seen from outside.
    private static final float FACE_OFFSET = 0.4f;
    private static final float NATURAL_SCALE = 0.05f;

    private final Font font;

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
        Direction textForwardDir = textForwardDirection(facing);

        // Only the leading block of a connected run draws, in one call across the whole run.
        if (countRun(level, be.getBlockPos(), textForwardDir.getOpposite(), facing) != 0) {
            return;
        }
        int totalLength = countRun(level, be.getBlockPos(), textForwardDir, facing) + 1;

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

    public static AABB computeRenderBoundingBox(MastStationSignBlockEntity be) {
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        if (level == null || !(be.getBlockState().getBlock() instanceof MastStationSignBlock)) {
            return new AABB(pos);
        }
        Direction facing = be.getBlockState().getValue(MastStationSignBlock.FACING);
        Direction forward = textForwardDirection(facing);
        BlockPos start = pos.relative(forward.getOpposite(), countRun(level, pos, forward.getOpposite(), facing));
        BlockPos end = pos.relative(forward, countRun(level, pos, forward, facing));
        return new AABB(start.getX(), start.getY(), start.getZ(), end.getX() + 1, end.getY() + 1, end.getZ() + 1);
    }

    // Which world direction the rotated text advances in - same handedness quirk as the other signs.
    private static Direction textForwardDirection(Direction facing) {
        Vector3f localXInWorld = new Quaternionf().rotateY((float) Math.toRadians(yRotFor(facing))).transform(new Vector3f(1, 0, 0));
        return Direction.getNearest(localXInWorld.x(), localXInWorld.y(), localXInWorld.z());
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

    private static float yRotFor(Direction facing) {
        return switch (facing) {
            case NORTH -> 180f;
            case WEST -> 270f;
            case EAST -> 90f;
            default -> 0f; // SOUTH
        };
    }
}

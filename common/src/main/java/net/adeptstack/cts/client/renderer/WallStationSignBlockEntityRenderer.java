package net.adeptstack.cts.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.adeptstack.cts.blockentities.WallStationSignBlockEntity;
import net.adeptstack.cts.blocks.signBlocks.WallStationSignBlock;
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

public class WallStationSignBlockEntityRenderer implements BlockEntityRenderer<WallStationSignBlockEntity> {

    private static final int MAX_RUN_SCAN = 64;
    private static final float RUN_PADDING_BLOCKS = 0.12f;
    private static final float FACE_OFFSET = 0.35f;
    // Natural (un-stretched) scale: how big text is when it comfortably fits the run.
    // Text only ever shrinks below this to fit a longer string into fewer blocks - it never
    // grows past it just because a run has spare width, so short text doesn't balloon in height.
    private static final float NATURAL_SCALE = 0.05f;

    // Text-forward world direction per facing, precomputed once - only 4 facings are possible, so
    // there's no reason to redo the quaternion rotation (and its allocations) on every render call.
    private static final Direction[] TEXT_FORWARD_BY_FACING = buildTextForwardTable();

    // Per-block-entity run cache, valid for a single game tick. render() and getRenderBoundingBox()
    // (on the block entity, delegating to computeRenderBoundingBox() below) are both called
    // independently, once or more per frame, for the leader block of a run - without this they'd
    // redundantly re-scan the whole (up to 64-block) chain on every one of those calls.
    private static final Map<WallStationSignBlockEntity, CachedRun> RUN_CACHE = new WeakHashMap<>();

    private final Font font;

    public WallStationSignBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(WallStationSignBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof WallStationSignBlock)) {
            return;
        }

        String text = be.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        Direction facing = state.getValue(WallStationSignBlock.FACING);
        RunInfo run = computeRun(level, be, be.getBlockPos(), facing);

        // Only the leftmost (in text-forward terms) block of a connected run draws - it draws the
        // *whole* string in one go, extending across the neighboring blocks in world space. Drawing
        // every glyph from its own block as a separate call caused Minecraft's translucent-geometry
        // depth sort to reshuffle the (near-coplanar) character quads, scrambling the reading order.
        if (run.leftCount() != 0) {
            return;
        }
        int totalLength = run.totalLength();

        int textPixelWidth = font.width(text);
        if (textPixelWidth <= 0) {
            return;
        }

        float availableWidth = Math.max(0.1f, totalLength - RUN_PADDING_BLOCKS * 2f);
        float fitScale = availableWidth / textPixelWidth;
        float scale = Math.min(NATURAL_SCALE, fitScale);

        // Center the (possibly shrunk) text across the whole run, rather than always hugging
        // the padded edges - a short text on a long run just sits centered with empty space around it.
        float renderedTextWidth = textPixelWidth * scale;
        float startOffsetBlocks = (totalLength - renderedTextWidth) / 2f;

        // Anchor point sits just in front of this block's thin plate, in world-axis space
        // (independent of the rotation below, which only orients the text plane).
        double anchorX = 0.5 - FACE_OFFSET * facing.getStepX();
        double anchorZ = 0.5 - FACE_OFFSET * facing.getStepZ();

        poseStack.pushPose();
        poseStack.translate(anchorX, 0.5, anchorZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotFor(facing)));
        poseStack.scale(scale, -scale, scale);

        Matrix4f pose = poseStack.last().pose();
        int color = 0xFF000000 | be.getTextColor().getTextColor();

        float centerLineY = -font.lineHeight / 2f;
        float pixelOffset = (startOffsetBlocks - 0.5f) / scale;

        font.drawInBatch(text, pixelOffset, centerLineY, color, false, pose, bufferSource,
                Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }

    public static AABB computeRenderBoundingBox(WallStationSignBlockEntity be) {
        // The leader block draws text that visually extends across the whole connected run, so its
        // culling box needs to cover that whole run - otherwise the text vanishes as soon as the
        // leader block itself (not the rest of the sign) leaves the screen/frustum.
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        if (level == null) {
            return new AABB(pos);
        }
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof WallStationSignBlock)) {
            return new AABB(pos);
        }

        Direction facing = state.getValue(WallStationSignBlock.FACING);
        Direction textForwardDir = TEXT_FORWARD_BY_FACING[facing.ordinal()];

        // Non-leader blocks never draw anything beyond their own block (see render() above), so
        // there's no need to scan the whole run just to build their culling box - a single scan in
        // the "backward" direction is enough to tell leaders and non-leaders apart.
        int leftCount = countRun(level, pos, textForwardDir.getOpposite(), facing);
        if (leftCount != 0) {
            return new AABB(pos);
        }

        RunInfo run = computeRun(level, be, pos, facing);
        BlockPos start = pos.relative(run.textForwardDir().getOpposite(), run.leftCount());
        BlockPos end = pos.relative(run.textForwardDir(), run.rightCount());
        return new AABB(start.getX(), start.getY(), start.getZ(), end.getX() + 1, end.getY() + 1, end.getZ() + 1);
    }

    private static RunInfo computeRun(Level level, WallStationSignBlockEntity be, BlockPos pos, Direction facing) {
        long tick = level.getGameTime();
        CachedRun cached = RUN_CACHE.get(be);
        if (cached != null && cached.tick() == tick) {
            return cached.run();
        }

        Direction textForwardDir = TEXT_FORWARD_BY_FACING[facing.ordinal()];
        int leftCount = countRun(level, pos, textForwardDir.getOpposite(), facing);
        int rightCount = countRun(level, pos, textForwardDir, facing);
        RunInfo run = new RunInfo(textForwardDir, leftCount, rightCount);
        RUN_CACHE.put(be, new CachedRun(tick, run));
        return run;
    }

    private static int countRun(Level level, BlockPos origin, Direction step, Direction requiredFacing) {
        int count = 0;
        BlockPos cursor = origin.relative(step);
        for (int i = 0; i < MAX_RUN_SCAN; i++) {
            BlockState state = level.getBlockState(cursor);
            if (!(state.getBlock() instanceof WallStationSignBlock) || state.getValue(WallStationSignBlock.FACING) != requiredFacing) {
                break;
            }
            count++;
            cursor = cursor.relative(step);
        }
        return count;
    }

    private record RunInfo(Direction textForwardDir, int leftCount, int rightCount) {
        int totalLength() {
            return leftCount + rightCount + 1;
        }
    }

    private record CachedRun(long tick, RunInfo run) {
    }

    private static Direction[] buildTextForwardTable() {
        // Determine which world direction the text's rotated local +X axis actually points to by
        // transforming it through the exact same rotation used to orient the text plane - hand
        // -derived lookup tables for this kept coming out wrong in one way or another.
        Direction[] table = new Direction[Direction.values().length];
        for (Direction facing : Direction.values()) {
            Vector3f localXInWorld = new Quaternionf().rotateY((float) Math.toRadians(yRotFor(facing))).transform(new Vector3f(1, 0, 0));
            table[facing.ordinal()] = Direction.getNearest(localXInWorld.x(), localXInWorld.y(), localXInWorld.z());
        }
        return table;
    }

    private static float yRotFor(Direction facing) {
        // Empirically determined per-facing text-plane rotation (confirmed in-game): NORTH/SOUTH
        // need a 180-degree offset from the blockstate model's own "y" rotation, EAST doesn't.
        return switch (facing) {
            case NORTH -> 180f;
            case WEST -> 270f;
            case EAST -> 90f;
            default -> 0f; // SOUTH
        };
    }
}

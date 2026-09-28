package net.adeptstack.cts.blockentities;

import net.adeptstack.cts.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class StationSignDoubleBlockEntity extends BlockEntity {

    public static final String DEFAULT_TEXT = "Bahnhof";

    private String textA = DEFAULT_TEXT;
    private DyeColor textColorA = DyeColor.WHITE;
    private DyeColor bgColorA = DyeColor.BLUE;

    private String textB = DEFAULT_TEXT;
    private DyeColor textColorB = DyeColor.WHITE;
    private DyeColor bgColorB = DyeColor.BLUE;

    public StationSignDoubleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public StationSignDoubleBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.STATION_SIGN_DOUBLE_BLOCK_ENTITY.get(), pos, state);
    }

    public String getTextA() {
        return textA;
    }

    public DyeColor getTextColorA() {
        return textColorA;
    }

    public DyeColor getBgColorA() {
        return bgColorA;
    }

    public String getTextB() {
        return textB;
    }

    public DyeColor getTextColorB() {
        return textColorB;
    }

    public DyeColor getBgColorB() {
        return bgColorB;
    }

    public void setContentA(String text, DyeColor textColor, DyeColor bgColor) {
        this.textA = text == null ? "" : text;
        this.textColorA = textColor == null ? DyeColor.WHITE : textColor;
        this.bgColorA = bgColor == null ? DyeColor.BLUE : bgColor;
        sync();
    }

    public void setContentB(String text, DyeColor textColor, DyeColor bgColor) {
        this.textB = text == null ? "" : text;
        this.textColorB = textColor == null ? DyeColor.WHITE : textColor;
        this.bgColorB = bgColor == null ? DyeColor.BLUE : bgColor;
        sync();
    }

    private void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("TextA", textA);
        tag.putInt("TextColorA", textColorA.getId());
        tag.putInt("BgColorA", bgColorA.getId());
        tag.putString("TextB", textB);
        tag.putInt("TextColorB", textColorB.getId());
        tag.putInt("BgColorB", bgColorB.getId());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.textA = tag.contains("TextA") ? tag.getString("TextA") : DEFAULT_TEXT;
        this.textColorA = DyeColor.byId(tag.contains("TextColorA") ? tag.getInt("TextColorA") : DyeColor.WHITE.getId());
        this.bgColorA = DyeColor.byId(tag.contains("BgColorA") ? tag.getInt("BgColorA") : DyeColor.BLUE.getId());
        this.textB = tag.contains("TextB") ? tag.getString("TextB") : DEFAULT_TEXT;
        this.textColorB = DyeColor.byId(tag.contains("TextColorB") ? tag.getInt("TextColorB") : DyeColor.WHITE.getId());
        this.bgColorB = DyeColor.byId(tag.contains("BgColorB") ? tag.getInt("BgColorB") : DyeColor.BLUE.getId());

        // The plate tint is baked into the chunk mesh, so new data arriving on the client has to
        // trigger a re-render or the old background color stays visible.
        if (this.level != null && this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    // Forge calls this (no @Override: it only exists on the Forge BlockEntity) to frustum-cull;
    // the text is drawn by the run's leading block and extends across the whole run.
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return net.adeptstack.cts.client.renderer.StationSignDoubleBlockEntityRenderer.computeRenderBoundingBox(this);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

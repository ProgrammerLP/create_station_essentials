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

public class WallStationSignBlockEntity extends BlockEntity {

    public static final String DEFAULT_TEXT = "Bahnhof";

    private String text = DEFAULT_TEXT;
    private DyeColor textColor = DyeColor.WHITE;

    public WallStationSignBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public WallStationSignBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.WALL_STATION_SIGN_BLOCK_ENTITY.get(), pos, state);
    }

    public String getText() {
        return text;
    }

    public DyeColor getTextColor() {
        return textColor;
    }

    public void setContent(String text, DyeColor textColor) {
        this.text = text == null ? "" : text;
        this.textColor = textColor == null ? DyeColor.WHITE : textColor;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Text", text);
        tag.putInt("TextColor", textColor.getId());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.text = tag.contains("Text") ? tag.getString("Text") : DEFAULT_TEXT;
        this.textColor = DyeColor.byId(tag.contains("TextColor") ? tag.getInt("TextColor") : DyeColor.WHITE.getId());
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
        return net.adeptstack.cts.client.renderer.WallStationSignBlockEntityRenderer.computeRenderBoundingBox(this);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

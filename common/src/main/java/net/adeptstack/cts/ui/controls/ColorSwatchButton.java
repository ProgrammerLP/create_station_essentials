package net.adeptstack.cts.ui.controls;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorSwatchButton extends AbstractButton {

    public static final int SIZE = 14;

    private final DyeColor color;
    private final Supplier<DyeColor> selectedSupplier;
    private final Consumer<DyeColor> onSelect;

    public ColorSwatchButton(int x, int y, DyeColor color, Supplier<DyeColor> selectedSupplier, Consumer<DyeColor> onSelect) {
        super(x, y, SIZE, SIZE, Component.translatable("color.minecraft." + color.getSerializedName()));
        this.color = color;
        this.selectedSupplier = selectedSupplier;
        this.onSelect = onSelect;
    }

    @Override
    public void onPress() {
        onSelect.accept(color);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int rgb = color.getFireworkColor();
        int fill = 0xFF000000 | rgb;
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), fill);

        boolean selected = selectedSupplier.get() == color;
        int borderColor = selected ? 0xFFFFFFFF : (isHovered() ? 0xFFAAAAAA : 0xFF404040);
        int borderThickness = selected ? 2 : 1;
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + borderThickness, borderColor);
        guiGraphics.fill(getX(), getY() + getHeight() - borderThickness, getX() + getWidth(), getY() + getHeight(), borderColor);
        guiGraphics.fill(getX(), getY(), getX() + borderThickness, getY() + getHeight(), borderColor);
        guiGraphics.fill(getX() + getWidth() - borderThickness, getY(), getX() + getWidth(), getY() + getHeight(), borderColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}

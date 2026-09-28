package net.adeptstack.cts.ui.screens.signBlocks;

import net.adeptstack.cts.ui.controls.ColorSwatchButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import static net.adeptstack.cts.Main.MOD_ID;

public class StationSignDoubleScreen extends Screen {

    private static final int WINDOW_WIDTH = 210;
    private static final int PADDING = 10;
    private static final int SWATCH_GAP = 2;
    private static final int SWATCH_PER_ROW = 8;
    private static final int SWATCH_STEP = ColorSwatchButton.SIZE + SWATCH_GAP;

    private final String initialText;
    private DyeColor textColor;
    private DyeColor bgColor;
    private final DoneCallback onDone;

    private EditBox textBox;
    private int guiLeft;
    private int guiTop;
    private int textColorLabelY;
    private int bgColorLabelY;
    private int panelBottom;

    public interface DoneCallback {
        void accept(String text, DyeColor textColor, DyeColor bgColor);
    }

    public StationSignDoubleScreen(String initialText, DyeColor initialTextColor, DyeColor initialBgColor, DoneCallback onDone) {
        super(Component.translatable("gui." + MOD_ID + ".station_sign_screen.title"));
        this.initialText = initialText;
        this.textColor = initialTextColor;
        this.bgColor = initialBgColor;
        this.onDone = onDone;
    }

    @Override
    protected void init() {
        super.init();

        int swatchRowsHeight = (SWATCH_STEP * 2 - SWATCH_GAP) * 2;
        int contentHeight = 12 + PADDING + 18 + PADDING + 12 + swatchRowsHeight + PADDING + 12 + swatchRowsHeight + PADDING + 20;

        guiLeft = width / 2 - WINDOW_WIDTH / 2;
        guiTop = height / 2 - contentHeight / 2;
        panelBottom = guiTop + contentHeight;

        int y = guiTop + 12 + PADDING;

        textBox = new EditBox(font, guiLeft + PADDING, y, WINDOW_WIDTH - PADDING * 2, 18,
                Component.translatable("gui." + MOD_ID + ".station_sign_screen.text"));
        textBox.setMaxLength(64);
        textBox.setValue(initialText);
        addRenderableWidget(textBox);
        setInitialFocus(textBox);

        y += 18 + PADDING;
        textColorLabelY = y;
        y += 12;
        addColorRow(y, () -> textColor, (c) -> textColor = c);

        y += swatchRowsHeight + PADDING;
        bgColorLabelY = y;
        y += 12;
        addColorRow(y, () -> bgColor, (c) -> bgColor = c);

        y += swatchRowsHeight + PADDING;

        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (btn) -> onClose())
                .bounds(guiLeft + WINDOW_WIDTH - PADDING - 90, y, 90, 20)
                .build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (btn) -> {
                    onDone.accept(textBox.getValue(), textColor, bgColor);
                    onClose();
                })
                .bounds(guiLeft + PADDING, y, 90, 20)
                .build());
    }

    private void addColorRow(int y, java.util.function.Supplier<DyeColor> current, java.util.function.Consumer<DyeColor> onSelect) {
        DyeColor[] colors = DyeColor.values();
        int rowWidth = SWATCH_PER_ROW * SWATCH_STEP - SWATCH_GAP;
        int rowLeft = width / 2 - rowWidth / 2;

        for (int i = 0; i < colors.length; i++) {
            int col = i % SWATCH_PER_ROW;
            int row = i / SWATCH_PER_ROW;
            DyeColor color = colors[i];
            addRenderableWidget(new ColorSwatchButton(
                    rowLeft + col * SWATCH_STEP,
                    y + row * SWATCH_STEP,
                    color,
                    current,
                    onSelect
            ));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, guiTop, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("gui." + MOD_ID + ".station_sign_screen.text_color"), guiLeft + PADDING, textColorLabelY, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.translatable("gui." + MOD_ID + ".station_sign_screen.bg_color"), guiLeft + PADDING, bgColorLabelY, 0xFFFFFF, false);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        super.renderBackground(guiGraphics);
        guiGraphics.fill(guiLeft - PADDING, guiTop - PADDING, guiLeft + WINDOW_WIDTH + PADDING, panelBottom + PADDING, 0xC0101010);
        guiGraphics.renderOutline(guiLeft - PADDING, guiTop - PADDING, WINDOW_WIDTH + PADDING * 2, panelBottom - guiTop + PADDING * 2, 0xFF808080);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

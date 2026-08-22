package com.abo47.oresandstuff.client.ui.controls;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.GuiGraphics;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.client.ui.theme.render.SurfaceFactory;

import static com.abo47.oresandstuff.client.ui.theme.tokens.UiThemeTokens.*;

public final class DragScrollBarWidget extends WidgetGroup {
    public static final int WIDTH = GRID_6;
    public static final int RESERVED_WIDTH = GRID_6;
    private static final int RAIL_WIDTH = 2;
    private static final int MIN_KNOB_HEIGHT = GRID_14;

    private final IntSupplier valueSupplier;
    private final IntSupplier maxSupplier;
    private final IntSupplier knobHeightSupplier;
    private final IntConsumer valueConsumer;
    private final BooleanSupplier draggingSupplier;
    private final Consumer<Boolean> draggingConsumer;
    private final Runnable refresh;
    private final int knobVisualWidth;

    public DragScrollBarWidget(
            int x,
            int y,
            int width,
            int height,
            IntSupplier valueSupplier,
            IntSupplier maxSupplier,
            IntSupplier knobHeightSupplier,
            IntConsumer valueConsumer,
            BooleanSupplier draggingSupplier,
            Consumer<Boolean> draggingConsumer,
            Runnable refresh
    ) {
        this(x, y, width, height, valueSupplier, maxSupplier, knobHeightSupplier, valueConsumer, draggingSupplier, draggingConsumer, refresh, width);
    }

    public DragScrollBarWidget(
            int x,
            int y,
            int width,
            int height,
            IntSupplier valueSupplier,
            IntSupplier maxSupplier,
            IntSupplier knobHeightSupplier,
            IntConsumer valueConsumer,
            BooleanSupplier draggingSupplier,
            Consumer<Boolean> draggingConsumer,
            Runnable refresh,
            int knobVisualWidth
    ) {
        super(x, y, width, height);
        this.valueSupplier = valueSupplier;
        this.maxSupplier = maxSupplier;
        this.knobHeightSupplier = knobHeightSupplier;
        this.valueConsumer = valueConsumer;
        this.draggingSupplier = draggingSupplier;
        this.draggingConsumer = draggingConsumer;
        this.refresh = refresh;
        this.knobVisualWidth = Math.max(1, Math.min(width, knobVisualWidth));
    }

    @Override
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        IGuiTexture skinBg = getBackgroundTexture();
        if (skinBg != null && !skinBg.equals(IGuiTexture.EMPTY)) {
            skinBg.draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }

        int x = getPositionX();
        int y = getPositionY();
        int w = getSizeWidth();
        int h = getSizeHeight();
        int railW = Math.min(RAIL_WIDTH, Math.max(1, w));
        int railX = x + Math.max(0, (w - railW) / 2);
        boolean active = draggingSupplier.getAsBoolean();
        boolean hovered = isMouseOverElement(mouseX, mouseY);
        drawVerticalTrack(graphics, mouseX, mouseY, railX, y, railW, h, OasColors.scrollTrack(active));

        int knobH = knobHeight();
        int max = Math.max(0, maxSupplier.getAsInt());
        int current = ScrollMath.clamp(valueSupplier.getAsInt(), max);
        int span = Math.max(0, h - knobH);
        int knobY = y + (max <= 0 || span <= 0 ? 0 : Math.round((float) span * ((float) current / (float) max)));
        int knobW = Math.min(w, active || hovered ? Math.max(knobVisualWidth, WIDTH) : knobVisualWidth);
        int knobX = x + Math.max(0, (w - knobW) / 2);
        drawVerticalThumb(graphics, mouseX, mouseY, knobX, knobY, knobW, knobH, OasColors.scrollThumb(active || hovered));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isMouseOverElement(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        draggingConsumer.accept(true);
        updateFromMouse(mouseY);
        refresh.run();
        return true;
    }

    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (!isMouseOverElement(mouseX, mouseY)) {
            return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
        }
        int max = Math.max(0, maxSupplier.getAsInt());
        if (max <= 0) {
            return true;
        }
        int current = ScrollMath.clamp(valueSupplier.getAsInt(), max);
        int step = Math.max(1, Math.min(GRID_24, Math.max(1, max / GRID_8)));
        int next = ScrollMath.wheel(current, max, step, wheelDelta);
        if (next != current) {
            valueConsumer.accept(next);
            refresh.run();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!draggingSupplier.getAsBoolean()) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        if (updateFromMouse(mouseY)) {
            refresh.run();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!draggingSupplier.getAsBoolean()) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        draggingConsumer.accept(false);
        refresh.run();
        return true;
    }

    private boolean updateFromMouse(double mouseY) {
        int current = valueSupplier.getAsInt();
        int next = ScrollMath.byMouse(
                (int) Math.round(mouseY),
                getPositionY(),
                getSizeHeight(),
                knobHeight(),
                maxSupplier.getAsInt()
        );
        if (next == current) {
            return false;
        }
        valueConsumer.accept(next);
        return true;
    }

    private int knobHeight() {
        int h = getSizeHeight();
        return Math.max(1, Math.min(h, Math.max(MIN_KNOB_HEIGHT, knobHeightSupplier.getAsInt())));
    }

    public static void drawVerticalTrack(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height, int color) {
        drawRect(graphics, x, y, width, height, color);
    }

    public static void drawVerticalThumb(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height, int color) {
        drawRect(graphics, x, y, width, height, color);
    }

    private static void drawRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        SurfaceFactory.fill(color).draw(graphics, 0, 0, x, y, width, height);
    }
}

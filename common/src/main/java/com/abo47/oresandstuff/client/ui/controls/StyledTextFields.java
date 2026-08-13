package com.abo47.oresandstuff.client.ui.controls;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;

import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.client.ui.theme.render.SurfaceFactory;

public final class StyledTextFields {
    private StyledTextFields() {
    }

    public static TextFieldWidget commitField(int x, int y, int width, int height, Supplier<String> textSupplier,
                                              Consumer<String> responder, Runnable commit, Runnable cancel, Runnable blur) {
        Runnable safeCommit = commit == null ? () -> {
        } : commit;
        Runnable safeCancel = cancel == null ? () -> {
        } : cancel;
        Runnable safeBlur = blur == null ? safeCommit : blur;
        TextFieldWidget field = new TextFieldWidget(x, y, width, height, textSupplier, responder) {
            private boolean suppressNextBlur;

            @Override
            public void onFocusChanged(Widget lastFocus, Widget focus) {
                super.onFocusChanged(lastFocus, focus);
                if (lastFocus == this && focus != this) {
                    if (suppressNextBlur) {
                        suppressNextBlur = false;
                        return;
                    }
                    safeBlur.run();
                }
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    safeCommit.run();
                    suppressNextBlur = true;
                    setFocus(false);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    safeCancel.run();
                    suppressNextBlur = true;
                    setFocus(false);
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        };
        field.setClientSideWidget();
        field.setCurrentString(currentText(textSupplier));
        field.setMaxStringLength(64);
        applyStandardStyle(field, OasColors.SURFACE_BASE, OasColors.BORDER_BASE);
        return field;
    }

    public static TextFieldWidget numberField(int x, int y, int width, int height, int current, int min, int max,
                                              int maxLength, Consumer<String> responder, Runnable commit, Runnable cancel, Runnable blur) {
        int value = Math.max(min, Math.min(max, current));
        TextFieldWidget field = commitField(x, y, width, height, () -> Integer.toString(value), responder, commit, cancel, blur);
        field.setMaxStringLength(maxLength);
        field.setNumbersOnly(min, max);
        return field;
    }

    public static void applyStandardStyle(TextFieldWidget field, int fillColor, int borderColor) {
        field.setBordered(false);
        field.setBackground(SurfaceFactory.bordered(fillColor, borderColor));
        field.setTextColor(OasColors.TEXT_PRIMARY);
    }

    private static String currentText(Supplier<String> textSupplier) {
        if (textSupplier == null) {
            return "";
        }
        String value = textSupplier.get();
        return value == null ? "" : value;
    }
}

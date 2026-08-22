package com.abo47.oresandstuff.client.ui.controls;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphics;

import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.client.ui.theme.render.SurfaceFactory;

public final class StyledTextFields {
    private StyledTextFields() {
    }

    public static TextFieldWidget commitField(int x, int y, int width, int height, Supplier<String> textSupplier,
                                              Consumer<String> responder, Runnable commit, Runnable cancel, Runnable blur) {
        return commitField(x, y, width, height, textSupplier, responder, commit, cancel, blur, null);
    }

    public static TextFieldWidget commitField(int x, int y, int width, int height, Supplier<String> textSupplier,
                                              Consumer<String> responder, Runnable commit, Runnable cancel, Runnable blur,
                                              Consumer<Boolean> focusResponder) {
        Runnable safeCommit = commit == null ? () -> {
        } : commit;
        Runnable safeCancel = cancel == null ? () -> {
        } : cancel;
        Runnable safeBlur = blur == null ? safeCommit : blur;
        return new TextFieldWidget(x, y, width, height, textSupplier, responder) {
            private boolean suppressNextBlur;

            @Override
            public void onFocusChanged(Widget lastFocus, Widget focus) {
                super.onFocusChanged(lastFocus, focus);
                if (focusResponder != null) {
                    focusResponder.accept(isFocus());
                }
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

            @Override
            public TextFieldWidget setCurrentString(Object currentString) {
                String newVal = currentString.toString();
                if (isRemote() && textField != null && !textField.getValue().equals(newVal)) {
                    boolean wasEmpty = textField.getValue().isEmpty();
                    int cursorPos = textField.getCursorPosition();
                    super.setCurrentString(newVal);
                    if (wasEmpty && !newVal.isEmpty()) {
                        textField.setCursorPosition(newVal.length());
                        textField.setHighlightPos(newVal.length());
                    } else {
                        int clamped = Math.min(cursorPos, newVal.length());
                        textField.setCursorPosition(clamped);
                        textField.setHighlightPos(clamped);
                    }
                } else {
                    super.setCurrentString(currentString);
                }
                return this;
            }
        };
    }

    public static TextFieldWidget search(
            int x,
            int y,
            int width,
            int height,
            Supplier<String> textSupplier,
            int maxLength,
            Consumer<String> responder,
            Consumer<Boolean> focusResponder
    ) {
        TextFieldWidget field = new TextFieldWidget(x, y, width, height, textSupplier, responder) {
            @Override
            public void onFocusChanged(Widget lastFocus, Widget focus) {
                super.onFocusChanged(lastFocus, focus);
                if (focusResponder != null) {
                    focusResponder.accept(isFocus());
                }
            }

            @Override
            public TextFieldWidget setCurrentString(Object currentString) {
                String newVal = currentString.toString();
                if (isRemote() && textField != null && !textField.getValue().equals(newVal)) {
                    boolean wasEmpty = textField.getValue().isEmpty();
                    int cursorPos = textField.getCursorPosition();
                    super.setCurrentString(newVal);
                    if (wasEmpty && !newVal.isEmpty()) {
                        textField.setCursorPosition(newVal.length());
                        textField.setHighlightPos(newVal.length());
                    } else {
                        int clamped = Math.min(cursorPos, newVal.length());
                        textField.setCursorPosition(clamped);
                        textField.setHighlightPos(clamped);
                    }
                } else {
                    super.setCurrentString(currentString);
                }
                return this;
            }

            @Override
            public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                setTextColor(OasColors.TEXT_PRIMARY);
                super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            }
        };
        field.setClientSideWidget();
        field.setCurrentString(currentText(textSupplier));
        field.setMaxStringLength(maxLength);
        field.setValidator(StyledTextFields::normalizeUserSearch);
        applyStandardStyle(field, OasColors.SURFACE_BASE, OasColors.BORDER_BASE);
        return field;
    }

    public static void applyStandardStyle(TextFieldWidget field, int fillColor, int borderColor) {
        field.setBordered(false);
        field.setBackground(SurfaceFactory.bordered(fillColor, borderColor));
        field.setTextColor(OasColors.TEXT_PRIMARY);
    }

    private static String normalizeUserSearch(String value) {
        if (value == null) {
            return "";
        }
        String raw = value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .toLowerCase(Locale.ROOT);
        while (raw.endsWith("_")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return raw;
    }

    private static String currentText(Supplier<String> textSupplier) {
        if (textSupplier == null) {
            return "";
        }
        String value = textSupplier.get();
        return value == null ? "" : value;
    }
}

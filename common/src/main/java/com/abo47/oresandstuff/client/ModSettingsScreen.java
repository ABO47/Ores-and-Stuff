package com.abo47.oresandstuff.client;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.client.ui.controls.DragScrollBarWidget;
import com.abo47.oresandstuff.client.ui.controls.ScrollMath;
import com.abo47.oresandstuff.client.ui.controls.ScrollState;
import com.abo47.oresandstuff.client.ui.controls.StyledTextFields;
import com.abo47.oresandstuff.client.ui.controls.ToggleSwitchWidget;
import com.abo47.oresandstuff.client.ui.theme.render.GlowShaderHelper;
import com.abo47.oresandstuff.client.ui.theme.render.SurfaceFactory;

import static com.abo47.oresandstuff.client.ui.theme.tokens.UiThemeTokens.*;

public final class ModSettingsScreen {

    private static final int ROOT_W = 563;
    private static final int ROOT_H = 352;
    private static final int ROOT_PAD_X = GRID_16;
    private static final int ROOT_PAD_Y = GRID_8;
    private static final int CONTENT_INSET = GRID_6;
    private static final int TAB_H = GRID_20;
    private static final int TAB_GAP = GRID_4;
    private static final int TAB_ENLARGE = GRID_4;
    private static final int SEARCH_INSET = GRID_9;
    private static final int HEADER_H = 14;
    private static final int HEADER_LIST_GAP = GRID_5;
    private static final int BOTTOM_GUTTER = GRID_6;
    private static final int LIST_INNER_PAD = GRID_6;
    private static final int LIST_V_PAD = GRID_4;
    private static final int ROW_H = ROW_H_26;
    private static final int ROW_INSET = GRID_4;
    private static final int SWITCH_GAP = GRID_8;

    private static final String[] TABS = {"Scanner", "Bio Scan", "Miner", "Worldgen", "Debug"};

    private ModSettingsScreen() {
    }

    public static void open(Player player) {
        WidgetGroup root = new SettingsUi().build();
        ModularUI ui = new ModularUI(root, IUIHolder.EMPTY, player);
        ui.initWidgets();
        Minecraft.getInstance().setScreen(new SettingsContainer(ui, player.containerMenu.containerId));
    }

    static final class SettingsContainer extends ModularUIGuiContainer {
        private SettingsContainer(ModularUI modularUI, int windowId) {
            super(modularUI, windowId);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                Minecraft.getInstance().setScreen(null);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    private record RowSpec(
            String id,
            String label,
            boolean toggle,
            BooleanSupplier boolGet,
            Consumer<Boolean> boolSet,
            DoubleSupplier numGet,
            DoubleConsumer numSet,
            double min,
            double max,
            int maxLen,
            boolean integer
    ) {
    }

    private static final class SettingsUi {
        private final int bodyW = ROOT_W - ROOT_PAD_X * 2;
        private int selectedTab;
        private String search = "";
        private int scrollValue;
        private boolean scrollDragging;

        private WidgetGroup tabLayer;
        private TextFieldWidget searchField;
        private WidgetGroup optionsPanel;

        private final ScrollState scroll = ScrollState.bind(
                () -> scrollValue,
                v -> scrollValue = v,
                () -> scrollDragging,
                d -> scrollDragging = d);

        private final Runnable refresh = () -> {
            rebuildTabs();
            rebuildOptions();
        };

        private int listY() {
            return CONTENT_INSET + TAB_H + TAB_ENLARGE + TAB_GAP + HEADER_H + HEADER_LIST_GAP;
        }

        private int listH() {
            return (ROOT_H - ROOT_PAD_Y * 2) - listY() - BOTTOM_GUTTER;
        }

        private WidgetGroup build() {
            WidgetGroup root = new WidgetGroup(0, 0, ROOT_W, ROOT_H);
            root.setBackground(SurfaceFactory.bordered(OasColors.SURFACE_BASE, OasColors.BORDER_BASE));

            int mainW = ROOT_W - ROOT_PAD_X * 2;
            int mainH = ROOT_H - ROOT_PAD_Y * 2;
            WidgetGroup mainPanel = new WidgetGroup(ROOT_PAD_X, ROOT_PAD_Y, mainW, mainH);
            mainPanel.setBackground(SurfaceFactory.bordered(OasColors.SURFACE_PANEL, OasColors.BORDER_BASE));
            root.addWidget(mainPanel);

            tabLayer = new WidgetGroup(0, CONTENT_INSET, bodyW, TAB_H + TAB_ENLARGE);
            mainPanel.addWidget(tabLayer);

            searchField = StyledTextFields.search(
                    SEARCH_INSET,
                    CONTENT_INSET + TAB_H + TAB_ENLARGE + TAB_GAP,
                    Math.max(40, bodyW - SEARCH_INSET * 2),
                    HEADER_H,
                    () -> search,
                    Integer.MAX_VALUE,
                    raw -> {
                        search = raw;
                        refresh.run();
                    },
                    focused -> {
                    });
            mainPanel.addWidget(searchField);

            optionsPanel = new WidgetGroup(SEARCH_INSET, listY(), bodyW - SEARCH_INSET * 2, listH());
            optionsPanel.setBackground(SurfaceFactory.bordered(OasColors.SURFACE_BASE, OasColors.BORDER_BASE));
            mainPanel.addWidget(optionsPanel);

            refresh.run();
            return root;
        }

        private void rebuildTabs() {
            tabLayer.clearAllWidgets();
            int count = TABS.length;
            int tabAreaW = Math.max(1, bodyW - SEARCH_INSET * 2);
            int tabX = SEARCH_INSET;
            int totalGap = TAB_GAP * (count - 1);
            int tabW = Math.max(1, (tabAreaW - totalGap) / count);
            int remainder = Math.max(0, (tabAreaW - totalGap) - tabW * count);
            for (int i = 0; i < count; i++) {
                int currentW = tabW + (i < remainder ? 1 : 0);
                tabLayer.addWidget(makeTabButton(TABS[i], tabX, currentW, i == selectedTab, i));
                tabX += currentW + TAB_GAP;
            }
        }

        private WidgetGroup makeTabButton(String label, int x, int w, boolean active, int tabIndex) {
            int h = TAB_H;
            int fill = active ? OasColors.withAlpha(OasColors.SURFACE_BASE, 250) : OasColors.withAlpha(OasColors.SURFACE_PANEL_ALT, 142);
            int border = active ? OasColors.BORDER_ACCENT : OasColors.BORDER_BASE;
            WidgetGroup container = new WidgetGroup(x, 0, w, h + TAB_ENLARGE);
            WidgetGroup bg = active
                    ? enlargedTabBg(w, fill, border)
                    : panel(0, TAB_ENLARGE, w, h, fill, border);
            LabelWidget text = label(8, TAB_ENLARGE + 6, crop(tr(label), fontWidth(tr(label), Math.max(8, w - 16))), active ? OasColors.TEXT_PRIMARY : OasColors.TEXT_MUTED);
            ButtonWidget hit = active
                    ? flatHitButton(0, 0, w, h + TAB_ENLARGE, click -> selectTab(tabIndex))
                    : flatHitButton(0, TAB_ENLARGE, w, h, click -> selectTab(tabIndex));
            hit.setHoverTexture(GlowShaderHelper.hoverGlow());
            hit.setClickedTexture(SurfaceFactory.fill(OasColors.withAlpha(OasColors.INTERACTIVE, 82)));
            hit.setHoverTooltips(Component.translatable(label));
            container.addWidget(bg);
            container.addWidget(text);
            container.addWidget(hit);
            return container;
        }

        private WidgetGroup enlargedTabBg(int w, int fill, int border) {
            return new WidgetGroup(0, 0, w, TAB_H + TAB_ENLARGE) {
                @Override
                public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                    SurfaceFactory.bordered(fill, border).draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
                }
            };
        }

        private void selectTab(int tabIndex) {
            if (selectedTab == tabIndex) {
                return;
            }
            selectedTab = tabIndex;
            scrollValue = 0;
            scrollDragging = false;
            refresh.run();
        }

        private void rebuildOptions() {
            optionsPanel.clearAllWidgets();
            List<RowSpec> all = rowsFor(selectedTab);
            List<RowSpec> entries = search.isBlank()
                    ? all
                    : all.stream().filter(r -> tr(r.label).toLowerCase().contains(search.toLowerCase())).toList();
            if (entries.isEmpty()) {
                optionsPanel.addWidget(label(8, LIST_V_PAD, tr("No matching options"), OasColors.TEXT_MUTED));
                return;
            }
            int x = LIST_INNER_PAD;
            int y = LIST_V_PAD;
            int w = optionsPanel.getSizeWidth() - LIST_INNER_PAD * 2;
            int h = optionsPanel.getSizeHeight() - LIST_V_PAD * 2;
            buildList(optionsPanel, x, y, w, h, ROW_H, entries);
        }

        private void buildList(WidgetGroup panel, int x, int y, int w, int h, int rowH, List<RowSpec> entries) {
            int rows = ScrollMath.listRows(h - LIST_V_PAD * 2, rowH, GRID_4);
            int maxStart = Math.max(0, entries.size() - rows);
            scrollValue = ScrollMath.clamp(scrollValue, maxStart);
            boolean showScroll = maxStart > 0;
            int rowW = showScroll ? w - DragScrollBarWidget.RESERVED_WIDTH - GRID_8 : w;
            WidgetGroup list = new WidgetGroup(x, y, w, h) {
                @Override
                public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
                    if (!showScroll) {
                        return false;
                    }
                    int max = Math.max(0, entries.size() - rows);
                    int next = Math.max(0, Math.min(max, scroll.value() + (wheelDelta > 0 ? -1 : 1)));
                    if (next == scroll.value()) {
                        return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
                    }
                    scroll.setValue(next);
                    refresh.run();
                    return true;
                }
            };
            panel.addWidget(list);

            int end = Math.min(entries.size(), scroll.value() + rows);
            int rowY = LIST_V_PAD;
            for (int i = scroll.value(); i < end; i++) {
                renderRow(list, entries.get(i), rowY, rowW);
                rowY += rowH;
            }
            if (showScroll) {
                int barH = Math.max(1, rows * rowH);
                int knobH = Math.max(12, Math.round((float) rows / (float) entries.size() * barH));
                int barX = x + w - DragScrollBarWidget.RESERVED_WIDTH;
                int barY = y + LIST_V_PAD;
                panel.addWidget(new DragScrollBarWidget(
                        barX + 1,
                        barY,
                        DragScrollBarWidget.RESERVED_WIDTH,
                        barH,
                        scroll::value,
                        () -> maxStart,
                        () -> knobH,
                        scroll::setValue,
                        scroll::dragging,
                        scroll::setDragging,
                        refresh,
                        DragScrollBarWidget.WIDTH
                ));
            }
        }

        private void renderRow(WidgetGroup list, RowSpec o, int rowY, int rowW) {
            Component[] tips = tooltipFor(o);
            if (o.toggle) {
                int rowH = ROW_H - ROW_INSET;
                int cardW = rowW;
                boolean enabled = o.boolGet.getAsBoolean();
                int fill = enabled ? OasColors.withAlpha(OasColors.SUCCESS, 28) : OasColors.withAlpha(OasColors.SURFACE_PANEL_ALT, 180);
                int border = enabled ? OasColors.withAlpha(OasColors.SUCCESS, 170) : OasColors.BORDER_BASE;
                list.addWidget(panel(0, rowY, cardW, rowH, fill, border));
                list.addWidget(hoverFill(0, rowY, cardW, rowH));

                int switchX = Math.max(104, cardW - ToggleSwitchWidget.DEFAULT_WIDTH - SWITCH_GAP);
                int textW = Math.max(16, switchX - 14);
                int crop = Math.max(14, textW / 6);
                int titleColor = enabled ? OasColors.TEXT_PRIMARY : OasColors.TEXT_SECONDARY;
                list.addWidget(label(8, rowY + 7, crop(tr(o.label), crop), titleColor));
                list.addWidget(new ToggleSwitchWidget(
                        o.id,
                        switchX,
                        rowY + 3,
                        ToggleSwitchWidget.DEFAULT_WIDTH,
                        ToggleSwitchWidget.DEFAULT_HEIGHT,
                        o.boolGet,
                        v -> {
                            o.boolSet.accept(v);
                            OresAndStuffConfig.save();
                        },
                        refresh,
                        tips));
                ButtonWidget hit = flatHitButton(0, rowY, cardW, rowH, click -> {
                    boolean next = !o.boolGet.getAsBoolean();
                    o.boolSet.accept(next);
                    OresAndStuffConfig.save();
                    refresh.run();
                });
                hit.setHoverTexture(GlowShaderHelper.hoverGlow());
                hit.setHoverTooltips(tips);
                list.addWidget(hit);
                return;
            }

            int rowH = ROW_H - ROW_INSET;
            int cardW = rowW;
            list.addWidget(panel(0, rowY, cardW, rowH, OasColors.withAlpha(OasColors.SURFACE_PANEL_ALT, 180), OasColors.BORDER_BASE));

            int unitW = 18;
            int fieldW = 54;
            int fieldX = Math.max(104, cardW - fieldW - unitW - SWITCH_GAP);
            int textW = Math.max(16, fieldX - 14);
            int crop = Math.max(14, textW / 6);
            list.addWidget(label(8, rowY + 7, crop(o.label, crop), OasColors.TEXT_SECONDARY));

            final TextFieldWidget[] holder = {null};
            Runnable commit = () -> {
                TextFieldWidget f = holder[0];
                if (f == null) {
                    return;
                }
                if (o.integer) {
                    int current = (int) Math.round(o.numGet.getAsDouble());
                    int parsed = clampInt(parseIntSafe(f.getCurrentString(), current), (int) o.min, (int) o.max);
                    if (parsed == current) {
                        f.setCurrentString(String.valueOf(parsed));
                        return;
                    }
                    o.numSet.accept((double) parsed);
                    OresAndStuffConfig.save();
                    refresh.run();
                } else {
                    double current = o.numGet.getAsDouble();
                    double parsed = clampDouble(parseDoubleSafe(f.getCurrentString(), current), o.min, o.max);
                    if (Math.abs(parsed - current) < 1e-9) {
                        f.setCurrentString(String.valueOf(parsed));
                        return;
                    }
                    o.numSet.accept(parsed);
                    OresAndStuffConfig.save();
                    refresh.run();
                }
            };
            String[] live = {o.integer
                    ? String.valueOf((int) Math.round(o.numGet.getAsDouble()))
                    : String.valueOf(o.numGet.getAsDouble())};
            Runnable reset = () -> {
                live[0] = o.integer
                        ? String.valueOf((int) Math.round(o.numGet.getAsDouble()))
                        : String.valueOf(o.numGet.getAsDouble());
                refresh.run();
            };
            TextFieldWidget field = StyledTextFields.commitField(
                    fieldX, rowY + 4, fieldW, GRID_14,
                    () -> live[0],
                    raw -> live[0] = raw,
                    commit,
                    reset,
                    commit);
            StyledTextFields.applyStandardStyle(field, OasColors.SURFACE_BASE, OasColors.BORDER_BASE);
            field.setClientSideWidget();
            if (o.integer) {
                field.setNumbersOnly((int) o.min, (int) o.max);
            } else {
                field.setNumbersOnly((float) o.min, (float) o.max);
            }
            field.setMaxStringLength(o.maxLen);
            field.setHoverTooltips(tips);
            holder[0] = field;
            list.addWidget(field);
        }

        private Component[] tooltipFor(RowSpec o) {
            if (o.toggle) {
                return new Component[]{Component.translatable(o.boolGet.getAsBoolean() ? "ON" : "OFF")};
            }
            return new Component[]{Component.translatable("Range: %s - %s", formatNum(o.min), formatNum(o.max))};
        }

        private static String formatNum(double v) {
            if (Math.abs(v - Math.round(v)) < 1e-9) {
                return String.valueOf((long) Math.round(v));
            }
            return String.valueOf(v);
        }

        private static String tr(String key) {
            return Component.translatable(key).getString();
        }

        private List<RowSpec> rowsFor(int tab) {
            return switch (tab) {
                case 0 -> scannerRows();
                case 1 -> bioScanRows();
                case 2 -> minerRows();
                case 3 -> worldgenRows();
                default -> debugRows();
            };
        }

        private List<RowSpec> scannerRows() {
            var s = OresAndStuffConfig.scanner();
            return List.of(
                    toggle("scanner.hud", "Holographic status HUD", () -> s.holographicStatusHud, v -> s.holographicStatusHud = v),
                    toggle("scanner.scanSound", "Scan sound", () -> s.scanSoundEnabled, v -> s.scanSoundEnabled = v),
                    toggle("scanner.pingSound", "Ping sound", () -> s.pingSoundEnabled, v -> s.pingSoundEnabled = v),
                    toggle("scanner.autoRefresh", "Auto-refresh while held", () -> s.scannerAutoRefresh, v -> s.scannerAutoRefresh = v),
                    number("scanner.radiusCap", "Radius cap", () -> s.radiusCap, v -> s.radiusCap = (int) v, 32, 4096, 4, true),
                    number("scanner.updateTicks", "Update ticks (0 = off)", () -> s.updateTicks, v -> s.updateTicks = (int) v, 0, 100, 3, true),
                    number("scanner.maxResults", "Max results", () -> s.maxResults, v -> s.maxResults = (int) v, 1, 24, 2, true),
                    number("scanner.cooldownTicks", "Cooldown ticks", () -> s.cooldownTicks, v -> s.cooldownTicks = (int) v, 0, 1200, 4, true),
                    number("scanner.pulseDurationMs", "Pulse duration (ms)", () -> s.pulseDurationMs, v -> s.pulseDurationMs = (int) v, 800, 20000, 6, true),
                    number("scanner.pulseRangeBlocks", "Pulse range (blocks)", () -> s.pulseRangeBlocks, v -> s.pulseRangeBlocks = (int) v, 32, 1024, 4, true),
                    number("scanner.pulseWidthBlocks", "Pulse width (blocks)", () -> s.pulseWidthBlocks, v -> s.pulseWidthBlocks = (int) v, 2, 64, 2, true),
                    number("scanner.qualityPreset", "Quality preset", () -> s.qualityPreset, v -> s.qualityPreset = (int) v, 0, 3, 1, true),
                    number("scanner.visualMode", "Visual mode", () -> s.experimentalVisualMode, v -> s.experimentalVisualMode = (int) v, 0, 1, 1, true),
                    number("scanner.holoWidth", "Holographic width", () -> s.holographicWidthBlocks, v -> s.holographicWidthBlocks = (int) v, 1, 256, 3, true),
                    number("scanner.pulseSpeed", "Pulse speed (b/s)", () -> s.pulseSpeedBlocksPerSec, v -> s.pulseSpeedBlocksPerSec = v, 4.0, 512.0, 8, false),
                    number("scanner.scanline", "Scanline strength", () -> s.holographicScanlineStrength, v -> s.holographicScanlineStrength = v, 0.0, 2.0, 8, false),
                    number("scanner.scanVol", "Scan sound volume", () -> s.scanSoundVolume, v -> s.scanSoundVolume = v, 0.0, 2.0, 8, false),
                    number("scanner.scanPitch", "Scan sound pitch", () -> s.scanSoundPitch, v -> s.scanSoundPitch = v, 0.1, 2.0, 8, false),
                    number("scanner.pingVol", "Ping sound volume", () -> s.pingSoundVolume, v -> s.pingSoundVolume = v, 0.0, 2.0, 8, false),
                    number("scanner.pingPitch", "Ping sound pitch", () -> s.pingSoundPitch, v -> s.pingSoundPitch = v, 0.1, 2.0, 8, false)
            );
        }

        private List<RowSpec> bioScanRows() {
            var s = OresAndStuffConfig.bioScan();
            return List.of(
                    number("bio.durationMs", "Duration (ms)", () -> s.durationMs, v -> s.durationMs = (int) v, 300, 15000, 6, true),
                    number("bio.cooldownTicks", "Cooldown ticks", () -> s.cooldownTicks, v -> s.cooldownTicks = (int) v, 0, 1200, 4, true),
                    number("bio.drainMul", "Drain multiplier", () -> s.drainMultiplier, v -> s.drainMultiplier = v, 0.5, 10.0, 8, false),
                    number("bio.rayStyle", "Ray style", () -> s.rayStyle, v -> s.rayStyle = v, 0.5, 3.0, 8, false)
            );
        }

        private List<RowSpec> minerRows() {
            var s = OresAndStuffConfig.miner();
            return List.of(
                    number("miner.fePerTick", "FE per tick", () -> s.fePerTick, v -> s.fePerTick = (int) v, 1, 10000, 6, true),
                    number("miner.bufferFe", "Buffer FE", () -> s.bufferFe, v -> s.bufferFe = (int) v, 100, 500000, 7, true),
                    number("miner.maxReceiveFe", "Max receive FE", () -> s.maxReceiveFe, v -> s.maxReceiveFe = (int) v, 1, 100000, 7, true)
            );
        }

        private List<RowSpec> worldgenRows() {
            var s = OresAndStuffConfig.worldgen();
            return List.of(
                    toggle("world.removeVanilla", "Remove vanilla ores", () -> s.removeVanillaOres, v -> s.removeVanillaOres = v),
                    number("world.spacing", "Min spacing (blocks)", () -> s.nodeMinSpacingBlocks, v -> s.nodeMinSpacingBlocks = (int) v, 16, 4000, 4, true),
                    number("world.attempts", "Attempts per chunk", () -> s.nodeAttemptsPerChunk, v -> s.nodeAttemptsPerChunk = (int) v, 1, 8, 1, true),
                    number("world.clusterRadius", "Cluster radius", () -> s.nodeClusterRadius, v -> s.nodeClusterRadius = (int) v, 1, 8, 1, true),
                    number("world.scatter", "Scatter count", () -> s.nodeScatterCount, v -> s.nodeScatterCount = (int) v, 0, 64, 2, true)
            );
        }

        private List<RowSpec> debugRows() {
            var s = OresAndStuffConfig.debug();
            return List.of(
                    toggle("debug.logging", "Debug logging", () -> s.debugLogging, v -> s.debugLogging = v)
            );
        }

        private RowSpec toggle(String id, String label, BooleanSupplier get, Consumer<Boolean> set) {
            return new RowSpec(id, label, true, get, set, () -> 0.0, v -> {
            }, 0, 0, 0, false);
        }

        private RowSpec number(String id, String label, DoubleSupplier get, DoubleConsumer set, double min, double max, int maxLen, boolean integer) {
            return new RowSpec(id, label, false, () -> false, v -> {
            }, get, set, min, max, maxLen, integer);
        }

        private static WidgetGroup panel(int x, int y, int w, int h, int fill, int border) {
            WidgetGroup p = new WidgetGroup(x, y, w, h);
            p.setBackground(SurfaceFactory.bordered(fill, border));
            return p;
        }

        private static LabelWidget label(int x, int y, String text, int color) {
            LabelWidget l = new LabelWidget(x, y, text);
            l.setColor(color);
            return l;
        }

        private static ButtonWidget flatHitButton(int x, int y, int w, int h, Consumer<ClickData> callback) {
            ButtonWidget button = new ButtonWidget(x, y, w, h, SurfaceFactory.transparentFill(), callback);
            button.setClientSideWidget();
            button.setHoverTexture(SurfaceFactory.transparentFill());
            button.setClickedTexture(SurfaceFactory.transparentFill());
            return button;
        }

        private static WidgetGroup hoverFill(int x, int y, int w, int h) {
            return new WidgetGroup(x, y, w, h) {
                @Override
                public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                    if (isMouseOverElement(mouseX, mouseY)) {
                        SurfaceFactory.fill(OasColors.withAlpha(OasColors.SURFACE_PANEL_ALT, 26))
                                .draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
                    }
                }
            };
        }

        private static String crop(String value, int max) {
            if (value == null || value.length() <= max) {
                return value == null ? "" : value;
            }
            return value.substring(0, Math.max(0, max - 3)) + "...";
        }

        private static int fontWidth(String text, int maxWidth) {
            if (text == null) {
                return 0;
            }
            int width = Minecraft.getInstance().font.width(text);
            if (width <= maxWidth) {
                return text.length();
            }
            int fit = text.length();
            while (fit > 0 && Minecraft.getInstance().font.width(text.substring(0, fit) + "...") > maxWidth) {
                fit--;
            }
            return Math.max(1, fit);
        }

        private static int parseIntSafe(String raw, int fallback) {
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            try {
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        private static double parseDoubleSafe(String raw, double fallback) {
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            try {
                return Double.parseDouble(raw.trim());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        private static int clampInt(int v, int min, int max) {
            return Math.max(min, Math.min(max, v));
        }

        private static double clampDouble(double v, double min, double max) {
            return Math.max(min, Math.min(max, v));
        }
    }
}

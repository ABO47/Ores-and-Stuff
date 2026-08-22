package com.abo47.oresandstuff.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import com.abo47.oresandstuff.client.entity.EntityPreviewRenderer;
import com.abo47.oresandstuff.client.entity.variant.EntityVariantCatalog;
import com.abo47.oresandstuff.client.ui.controls.DragScrollBarWidget;
import com.abo47.oresandstuff.client.ui.controls.IconAtlas;
import com.abo47.oresandstuff.client.ui.controls.PickerTileText;
import com.abo47.oresandstuff.client.ui.controls.ScrollState;
import com.abo47.oresandstuff.client.ui.controls.StyledTextFields;
import com.abo47.oresandstuff.client.ui.controls.picker.TiledPickerPanel;
import com.abo47.oresandstuff.client.ui.theme.render.ChromeFactory;
import com.abo47.oresandstuff.client.ui.theme.render.GlowShaderHelper;
import com.abo47.oresandstuff.client.ui.theme.render.SurfaceFactory;
import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.network.BioScanLibraryPacket;

import static com.abo47.oresandstuff.client.ui.theme.tokens.UiThemeTokens.*;

public final class BioLibraryScreen {
    private BioLibraryScreen() {
    }

    public static void open(Player player, List<BioScanLibraryPacket.Entry> entries) {
        WidgetGroup root = new LibraryUi(player, entries).build();
        ModularUI ui = new ModularUI(root, IUIHolder.EMPTY, player);
        ui.initWidgets();
        Minecraft.getInstance().setScreen(new LibraryContainer(ui, player.containerMenu.containerId));
    }

    static final class LibraryContainer extends ModularUIGuiContainer {
        private LibraryContainer(ModularUI modularUI, int windowId) {
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

    private static final int RIGHT_X = 166;
    private static final int PAD = GRID_8;
    private static final int LEFT_W = 150;
    private static final int TILE_COLUMNS = 3;
    private static final int TILE_ROWS = 3;
    private static final int TILE_GAP = GRID_6;
    private static final int TILE_PAD = GRID_8;
    private static final int CONTROL_GAP = GRID_3;
    private static final int ENTITY_FRONT_YAW = EntityPreviewRenderer.FRONT_ENTITY_YAW;
    private static final int PREVIEW_SPIN_SPEED = 36;

    private static final int ROOT_W = 432;
    private static final int ROOT_H = 260;

    private static final class LibraryUi {
        private final List<BioScanLibraryPacket.Entry> allEntries;
        private String selectedEntityId = "";
        private String previewEntityId = "";
        private String selectedVariantKey = "";
        private String search = "";

        private int gridScrollValue;
        private boolean gridScrollDragging;

        private final ScrollState gridScroll = ScrollState.bind(
                () -> gridScrollValue,
                v -> gridScrollValue = v,
                () -> gridScrollDragging,
                d -> gridScrollDragging = d);

        private WidgetGroup content;
        private WidgetGroup previewPanel;
        private WidgetGroup tilesPanel;

        private long lastClickTime;
        private String lastClickId = "";

        LibraryUi(Player player, List<BioScanLibraryPacket.Entry> entries) {
            this.allEntries = new ArrayList<>(entries);
        }

        private boolean browsingEntity() {
            return !selectedEntityId.isBlank();
        }

        private BioScanLibraryPacket.Entry selectedEntry() {
            for (BioScanLibraryPacket.Entry entry : allEntries) {
                if (entry.entityId().equals(selectedEntityId)) {
                    return entry;
                }
            }
            return null;
        }

        private boolean hasPreview() {
            return !selectedEntityId.isBlank() || !previewEntityId.isBlank();
        }

        private BioScanLibraryPacket.Entry previewEntry() {
            String id = selectedEntityId.isBlank() ? previewEntityId : selectedEntityId;
            for (BioScanLibraryPacket.Entry entry : allEntries) {
                if (entry.entityId().equals(id)) {
                    return entry;
                }
            }
            return null;
        }

        private boolean isUnlocked() {
            BioScanLibraryPacket.Entry entry = selectedEntry();
            return entry != null && entry.unlocked();
        }

        private WidgetGroup build() {
            WidgetGroup root = new WidgetGroup(0, 0, ROOT_W, ROOT_H);
            root.setBackground(SurfaceFactory.bordered(OasColors.SURFACE_BASE, OasColors.BORDER_BASE));
            content = new WidgetGroup(0, 0, ROOT_W, ROOT_H);
            root.addWidget(content);
            rebuild();
            return root;
        }

        private void rebuild() {
            content.clearAllWidgets();
            content.addWidget(label(8, 6, tr("Bio Library"), OasColors.TEXT_PRIMARY));
            content.addWidget(ChromeFactory.closeIconButton(closeButtonX(), Math.max(0, 4 - GRID_3), 18, 18, click -> Minecraft.getInstance().setScreen(null)));

            if (browsingEntity()) {
                addBackButton();
            }
            addPreview();
            addControls();
            addTiles();
        }

        private int closeButtonX() {
            return RIGHT_X + (ROOT_W - 174) - 18 + 1;
        }

        private int backButtonX() {
            return closeButtonX() - CONTROL_GAP - 18;
        }

        private void addBackButton() {
            int backX = backButtonX();
            int backSize = 18;
            content.addWidget(ChromeFactory.iconButton(backX, 1, backSize, backSize, "back", () -> OasColors.INTERACTIVE, click -> {
                selectedEntityId = "";
                selectedVariantKey = "";
                previewEntityId = "";
                rebuild();
            }));
        }

        private void addPreview() {
            previewPanel = new WidgetGroup(PAD, 22, LEFT_W, ROOT_H - 48);
            previewPanel.setBackground(SurfaceFactory.bordered(OasColors.withAlpha(OasColors.SURFACE_PANEL_ALT, 120), OasColors.BORDER_BASE));
            if (hasPreview()) {
                BioScanLibraryPacket.Entry entry = previewEntry();
                if (entry != null) {
                    String entityId = entry.entityId();
                    boolean unlocked = entry.unlocked();
                    previewPanel.addWidget(label(8, 8, crop(EntityPreviewRenderer.entityDisplayName(entityId), 22), OasColors.TEXT_SECONDARY));
                    previewPanel.addWidget(label(8, 22, crop(variantLabel(entityId, selectedVariantKey), 22), unlocked ? OasColors.TEXT_PRIMARY : OasColors.TEXT_MUTED));
                    previewPanel.addWidget(new WidgetGroup(10, 42, LEFT_W - 20, Math.max(48, ROOT_H - 98)) {
                        @Override
                        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                            EntityPreviewRenderer.renderEntityAsset(
                                    graphics,
                                    getPositionX(),
                                    getPositionY(),
                                    getSizeWidth(),
                                    getSizeHeight(),
                                    EntityPreviewRenderer.entityAsset(entityId, selectedVariantKey),
                                    ENTITY_FRONT_YAW,
                                    PREVIEW_SPIN_SPEED,
                                    !unlocked,
                                    partialTicks);
                        }
                    });
                }
            } else {
                long scanned = 0;
                for (BioScanLibraryPacket.Entry entry : allEntries) {
                    if (entry.unlocked()) {
                        scanned++;
                    }
                }
                previewPanel.addWidget(label(8, 8, crop(tr("All entities"), 22), OasColors.TEXT_SECONDARY));
                previewPanel.addWidget(label(8, 22, crop(tr("Scanned %s of %s", scanned, allEntries.size()), 22), OasColors.TEXT_MUTED));
            }
            content.addWidget(previewPanel);
        }

        private void addControls() {
            int rightX = RIGHT_X;
            int backX = backButtonX();
            int closeX = closeButtonX();
            int searchW = browsingEntity() ? Math.max(40, backX - rightX - CONTROL_GAP) : Math.max(40, closeX - rightX - CONTROL_GAP);
            content.addWidget(StyledTextFields.search(
                    rightX, 2, searchW, 16,
                    () -> search, 80,
                    raw -> {
                        search = raw == null ? "" : raw;
                        rebuildTiles();
                    },
                    focused -> {
                    }));
        }

        private void addTiles() {
            tilesPanel = new WidgetGroup(RIGHT_X, 22, ROOT_W - 174, ROOT_H - 48);
            content.addWidget(tilesPanel);
            rebuildTiles();
        }

        private void rebuildTiles() {
            tilesPanel.clearAllWidgets();
            int rightW = ROOT_W - 174;
            int listH = ROOT_H - 48;
            if (browsingEntity()) {
                String entityId = selectedEntityId;
                List<EntityVariantCatalog.VariantEntry> variants = EntityVariantCatalog.search(entityId, search);
                TileMetrics metrics = tileMetrics(rightW, listH, variants.size());
                TiledPickerPanel.add(
                        tilesPanel,
                        0,
                        0,
                        rightW,
                        listH,
                        metrics.tileW(),
                        metrics.tileH(),
                        TILE_GAP,
                        TILE_PAD,
                        TILE_PAD,
                        variants,
                        tr("No known variants"),
                        gridScroll,
                        null,
                        this::rebuild,
                        (surface, entry, index, tileX, tileY, tileW, tileH, layout) -> addVariantTile(surface, entry, tileX, tileY, tileW, tileH));
            } else {
                List<BioScanLibraryPacket.Entry> entries = filtered();
                TileMetrics metrics = tileMetrics(rightW, listH, entries.size());
                TiledPickerPanel.add(
                        tilesPanel,
                        0,
                        0,
                        rightW,
                        listH,
                        metrics.tileW(),
                        metrics.tileH(),
                        TILE_GAP,
                        TILE_PAD,
                        TILE_PAD,
                        entries,
                        tr("No entities"),
                        gridScroll,
                        null,
                        this::rebuild,
                        (surface, entry, index, tileX, tileY, tileW, tileH, layout) -> addEntityTile(surface, entry, tileX, tileY, tileW, tileH));
            }
        }

        private void addEntityTile(WidgetGroup surface, BioScanLibraryPacket.Entry entry, int tileX, int tileY, int tileW, int tileH) {
            boolean unlocked = entry.unlocked();
            boolean hasVariants = EntityVariantCatalog.hasVariants(entry.entityId());
            String entityId = entry.entityId();
            String displayName = EntityPreviewRenderer.entityDisplayName(entityId);

            WidgetGroup tile = new WidgetGroup(tileX, tileY, tileW, tileH);
            surface.addWidget(tile);

            int labelH = 14;
            int previewH = Math.max(28, tileH - labelH - 8);
            tile.addWidget(new WidgetGroup(3, 2, Math.max(16, tileW - 6), previewH) {
                @Override
                public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                    EntityPreviewRenderer.renderTileEntityAsset(
                            graphics,
                            getPositionX(),
                            getPositionY(),
                            getSizeWidth(),
                            getSizeHeight(),
                            EntityPreviewRenderer.entityAsset(entityId),
                            ENTITY_FRONT_YAW,
                            0,
                            0,
                            !unlocked,
                            0.0F);
                }
            });
            tile.addWidget(PickerTileText.centeredLabel(2, tileH - labelH, tileW - 4, displayName, unlocked ? OasColors.TEXT_PRIMARY : OasColors.TEXT_MUTED));

            if (hasVariants) {
                tile.addWidget(new WidgetGroup(tileW - 17, tileH - 17, 15, 15) {
                    @Override
                    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                        IGuiTexture variant = IconAtlas.iconTexture("variant");
                        if (variant != null) {
                            variant.draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
                        }
                    }
                });
            }

            ButtonWidget hit = flatHitButton(tileX, tileY, tileW, tileH, click -> onEntityClick(entry));
            hit.setHoverTexture(GlowShaderHelper.hoverGlow());
            hit.setClickedTexture(SurfaceFactory.fill(OasColors.withAlpha(OasColors.INTERACTIVE, 90)));
            surface.addWidget(hit);
        }

        private void onEntityClick(BioScanLibraryPacket.Entry entry) {
            long now = System.currentTimeMillis();
            boolean doubleClick = entry.entityId().equals(lastClickId) && (now - lastClickTime) < 350;
            lastClickTime = now;
            lastClickId = entry.entityId();
            if (doubleClick) {
                selectedEntityId = entry.entityId();
                List<EntityVariantCatalog.VariantEntry> variants = EntityVariantCatalog.variantsFor(entry.entityId());
                selectedVariantKey = variants.isEmpty() ? "" : variants.get(0).key();
                rebuild();
            } else {
                selectedVariantKey = "";
                previewEntityId = entry.entityId();
                rebuildPreview();
            }
        }

        private void addVariantTile(WidgetGroup surface, EntityVariantCatalog.VariantEntry entry, int tileX, int tileY, int tileW, int tileH) {
            boolean active = entry.key().equals(selectedVariantKey);
            boolean unlocked = isUnlocked();
            String entityId = selectedEntityId;

            WidgetGroup tile = new WidgetGroup(tileX, tileY, tileW, tileH);
            if (active) {
                tile.setBackground(SurfaceFactory.fill(OasColors.withAlpha(OasColors.INTERACTIVE, 86)));
            }
            int labelH = 14;
            int previewH = Math.max(28, tileH - labelH - 8);
            tile.addWidget(new WidgetGroup(3, 2, Math.max(16, tileW - 6), previewH) {
                @Override
                public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                    EntityPreviewRenderer.renderTileEntityAsset(
                            graphics,
                            getPositionX(),
                            getPositionY(),
                            getSizeWidth(),
                            getSizeHeight(),
                            EntityPreviewRenderer.entityAsset(entityId, entry.key()),
                            ENTITY_FRONT_YAW,
                            0,
                            0,
                            !unlocked,
                            0.0F);
                }
            });
            tile.addWidget(PickerTileText.centeredLabel(2, tileH - labelH, tileW - 4, tileLabel(entityId, entry), active ? OasColors.TEXT_PRIMARY : OasColors.TEXT_SECONDARY));
            surface.addWidget(tile);

            ButtonWidget hit = flatHitButton(tileX, tileY, tileW, tileH, click -> {
                selectedVariantKey = entry.key();
                rebuildPreview();
            });
            hit.setHoverTexture(GlowShaderHelper.hoverGlow());
            hit.setClickedTexture(SurfaceFactory.fill(OasColors.withAlpha(OasColors.INTERACTIVE, 90)));
            surface.addWidget(hit);
        }

        private void rebuildPreview() {
            if (previewPanel != null) {
                content.removeWidget(previewPanel);
            }
            addPreview();
        }

        private List<BioScanLibraryPacket.Entry> filtered() {
            String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
            if (q.isBlank()) {
                return allEntries;
            }
            List<BioScanLibraryPacket.Entry> out = new ArrayList<>();
            for (BioScanLibraryPacket.Entry entry : allEntries) {
                if (entry.title().toLowerCase(Locale.ROOT).contains(q)
                        || entry.category().toLowerCase(Locale.ROOT).contains(q)
                        || entry.entityId().toLowerCase(Locale.ROOT).contains(q)) {
                    out.add(entry);
                }
            }
            return out;
        }

        private static TileMetrics tileMetrics(int rightW, int listH, int count) {
            int safeCount = Math.max(1, count);
            boolean showScroll = safeCount > TILE_COLUMNS * TILE_ROWS;
            int contentW = Math.max(1, rightW - TILE_PAD * 2 - (showScroll ? DragScrollBarWidget.RESERVED_WIDTH : 0));
            int contentH = Math.max(1, listH - TILE_PAD * 2);
            int tileW = Math.max(54, (contentW - TILE_GAP * (TILE_COLUMNS - 1)) / TILE_COLUMNS);
            int tileH = Math.max(60, (contentH - TILE_GAP * (TILE_ROWS - 1)) / TILE_ROWS);
            return new TileMetrics(tileW, tileH);
        }

        private static String variantLabel(String entityId, String variantKey) {
            if (variantKey == null || variantKey.isBlank()) {
                return tr("Base form");
            }
            return EntityVariantCatalog.labelFor(entityId, variantKey);
        }

        private static String tileLabel(String entityId, EntityVariantCatalog.VariantEntry entry) {
            String text = entry.label();
            String entityName = EntityPreviewRenderer.entityDisplayName(entityId).toLowerCase(Locale.ROOT);
            String lower = text.toLowerCase(Locale.ROOT);
            String suffix = " " + entityName;
            if (!entityName.isBlank() && lower.endsWith(suffix)) {
                return text.substring(0, text.length() - suffix.length());
            }
            if (entityId.contains("villager") && lower.endsWith(" villager")) {
                return text.substring(0, text.length() - " villager".length());
            }
            return text;
        }

        private static String tr(String key, Object... args) {
            return Component.translatable(key, args).getString();
        }

        private static LabelWidget label(int x, int y, String text, int color) {
            LabelWidget l = new LabelWidget(x, y, text);
            l.setColor(color);
            return l;
        }

        private static ButtonWidget flatHitButton(int x, int y, int w, int h, java.util.function.Consumer<ClickData> callback) {
            ButtonWidget button = new ButtonWidget(x, y, w, h, SurfaceFactory.transparentFill(), callback);
            button.setClientSideWidget();
            button.setHoverTexture(SurfaceFactory.transparentFill());
            button.setClickedTexture(SurfaceFactory.transparentFill());
            return button;
        }

        private static String crop(String value, int max) {
            if (value == null || value.length() <= max) {
                return value == null ? "" : value;
            }
            return value.substring(0, Math.max(0, max - 3)) + "...";
        }

        private record TileMetrics(int tileW, int tileH) {
        }
    }
}

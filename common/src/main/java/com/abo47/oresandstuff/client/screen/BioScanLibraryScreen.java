package com.abo47.oresandstuff.client.screen;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.network.BioScanLibraryPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;

public class BioScanLibraryScreen extends Screen {
    private final List<BioScanLibraryPacket.Entry> entries;
    private List<BioScanLibraryPacket.Entry> filtered;
    private EntryList list;
    private BioScanLibraryPacket.Entry selected;
    private EditBox search;

    public BioScanLibraryScreen(List<BioScanLibraryPacket.Entry> entries) {
        super(Component.literal("Bio Scan Library"));
        this.entries = entries;
        this.filtered = new java.util.ArrayList<>(entries);
    }

    @Override
    protected void init() {
        int left = 22;
        int top = 24;
        int listW = width / 2 - 30;
        search = new EditBox(font, left, 8, listW, 14, Component.literal("Search..."));
        search.setResponder(s -> refreshFilter());
        addRenderableWidget(search);
        list = new EntryList(minecraft, listW, height - 56, top, height - 36, 20);
        for (BioScanLibraryPacket.Entry e : filtered) list.addRow(e);
        addWidget(list);
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width - 94, height - 30, 72, 20).build());
    }

    private void refreshFilter() {
        String q = search.getValue().trim().toLowerCase(java.util.Locale.ROOT);
        filtered = new java.util.ArrayList<>();
        for (BioScanLibraryPacket.Entry e : entries) {
            if (q.isBlank() || e.title().toLowerCase(java.util.Locale.ROOT).contains(q) || e.entityId().toLowerCase(java.util.Locale.ROOT).contains(q) || e.category().toLowerCase(java.util.Locale.ROOT).contains(q)) {
                filtered.add(e);
            }
        }
        if (list != null) {
            list.resetRows();
            for (BioScanLibraryPacket.Entry e : filtered) list.addRow(e);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, "SCANNED LIBRARY", width / 2, 8, OasColors.ACCENT_SOFT);
        g.drawString(font, "Search", 22, -2 + 8, OasColors.TEXT_MUTED, false);
        list.render(g, mx, my, pt);

        int px = width / 2 + 4;
        int py = 24;
        g.fill(px, py, width - 20, height - 36, OasColors.withAlpha(OasColors.BG_0, 0xAA));
        if (selected != null) {
            g.drawString(font, selected.title(), px + 8, py + 8, OasColors.TEXT_PRIMARY, false);
            g.drawString(font, selected.entityId(), px + 8, py + 20, OasColors.TEXT_SECONDARY, false);
            g.drawString(font, selected.category(), px + 8, py + 32, OasColors.ACCENT_MINT, false);
            g.drawWordWrap(font, Component.literal(selected.summary()), px + 8, py + 48, width - px - 30, OasColors.TEXT_SECONDARY);
        } else {
            g.drawCenteredString(font, "Select an entry", px + (width - px - 20) / 2, py + 16, OasColors.TEXT_MUTED);
        }
        super.render(g, mx, my, pt);
    }

    private class EntryList extends ObjectSelectionList<Row> {
        EntryList(Minecraft mc, int w, int h, int y0, int y1, int itemH) {
            super(mc, w, h, y0, y1, itemH);
        }

        void addRow(BioScanLibraryPacket.Entry e) {
            super.addEntry(new Row(e));
        }

        void resetRows() {
            super.clearEntries();
        }
    }

    private class Row extends ObjectSelectionList.Entry<Row> {
        private final BioScanLibraryPacket.Entry e;

        Row(BioScanLibraryPacket.Entry e) {
            this.e = e;
        }

        @Override
        public void render(GuiGraphics g, int idx, int y, int x, int w, int h, int mx, int my, boolean hovered, float pt) {
            int c = (selected == e) ? OasColors.TEXT_PRIMARY : OasColors.TEXT_SECONDARY;
            g.drawString(font, e.title(), x + 4, y + 2, c, false);
            g.drawString(font, e.category(), x + 4, y + 12, OasColors.TEXT_MUTED, false);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int b) {
            selected = e;
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.literal(e.title() + " " + e.category());
        }
    }
}

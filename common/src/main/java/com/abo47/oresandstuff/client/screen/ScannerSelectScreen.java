package com.abo47.oresandstuff.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.node.OreNodeType;

/**
 * Ore type wheel picker: a full circle sliced into one pie wedge per
 * registered node type (360 / n), filled with the type's scanner color and
 * labelled with the item icon of its configured output. Hovered and selected
 * wedges pop outward with an eased animation. Does not pause the game.
 */
public class ScannerSelectScreen extends Screen {
    private static final float POP_OUT = 6.0F;
    private static final double SLICE_OVERLAP = 0.003;

    private static final class PieRenderType extends RenderType {
        private PieRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }

        static final RenderType PIE = new PieRenderType(
                "oas_scanner_pie", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 8192, false, false,
                () -> {
                    RenderStateShard.POSITION_COLOR_SHADER.setupRenderState();
                    RenderStateShard.TRANSLUCENT_TRANSPARENCY.setupRenderState();
                    RenderStateShard.NO_CULL.setupRenderState();
                    RenderStateShard.NO_DEPTH_TEST.setupRenderState();
                    RenderStateShard.COLOR_DEPTH_WRITE.setupRenderState();
                },
                () -> {
                    RenderStateShard.COLOR_DEPTH_WRITE.clearRenderState();
                    RenderStateShard.NO_DEPTH_TEST.clearRenderState();
                    RenderStateShard.NO_CULL.clearRenderState();
                    RenderStateShard.TRANSLUCENT_TRANSPARENCY.clearRenderState();
                    RenderStateShard.POSITION_COLOR_SHADER.clearRenderState();
                });
    }

    private final ItemStack scannerStack;
    private final List<Entry> entries = new ArrayList<>();
    private int centerX;
    private int centerY;
    private int radius;
    private int hovered = -1;
    private int selected = -1;
    private float[] popValues = new float[0];

    private record Entry(ResourceLocation id, ItemStack icon, int color) {
    }

    public ScannerSelectScreen(ItemStack scannerStack) {
        super(Component.translatable("Scanner Targets"));
        this.scannerStack = scannerStack;
    }

    @Override
    protected void init() {
        entries.clear();
        selected = -1;
        String selectedRaw = scannerStack.getOrCreateTag().getString("SelectedType");
        List<ResourceLocation> ids = OreNodeDataManager.INSTANCE.orderedTypeIds();
        for (ResourceLocation id : ids) {
            OreNodeType type = OreNodeDataManager.INSTANCE.getNodeType(id).orElse(null);
            if (type == null) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(type.outputItem());
            ItemStack icon = item != null && item != Items.AIR ? new ItemStack(item) : ItemStack.EMPTY;
            entries.add(new Entry(id, icon, 0xFF000000 | type.scannerColor()));
            if (id.toString().equals(selectedRaw)) {
                selected = entries.size() - 1;
            }
        }
        popValues = new float[entries.size()];
        if (selected >= 0) {
            popValues[selected] = 1.0F;
        }
        centerX = width / 2;
        centerY = height / 2 + 4;
        radius = Math.max(50, Math.min(100, Math.min(width, height) / 4));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int idx = sliceIndexAt(mouseX, mouseY);
            if (idx >= 0) {
                select(idx);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void select(int idx) {
        Entry entry = entries.get(idx);
        scannerStack.getOrCreateTag().putString("SelectedType", entry.id().toString());
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OasColors.DIM_OVERLAY);
        hovered = sliceIndexAt(mouseX, mouseY);
        int n = entries.size();
        if (n == 0) {
            graphics.drawCenteredString(font, Component.translatable("No ore node types configured").getString(), centerX, centerY, OasColors.TEXT_PRIMARY);
            return;
        }
        int mask = 0;
        if (hovered >= 0) {
            mask |= 1 << hovered;
        }
        if (selected >= 0) {
            mask |= 1 << selected;
        }
        for (int i = 0; i < n; i++) {
            float target = (mask & (1 << i)) != 0 ? 1.0F : 0.0F;
            popValues[i] += (target - popValues[i]) * 0.2F;
        }

        double sweep = (Math.PI * 2.0) / n;
        double start = -Math.PI / 2.0;

        drawDisc(graphics, radius, OasColors.SURFACE_BASE);
        for (int i = 0; i < n; i++) {
            double from = start + i * sweep - SLICE_OVERLAP;
            double to = start + (i + 1) * sweep + SLICE_OVERLAP;
            float r = radius + POP_OUT * popValues[i];
            int color = i == hovered ? lighten(entries.get(i).color()) : entries.get(i).color();
            drawSlice(graphics, from, to, r, color);
        }
        drawHub(graphics, entries.get(selected >= 0 ? selected : 0));
        graphics.flush();

        for (int i = 0; i < n; i++) {
            Entry entry = entries.get(i);
            if (entry.icon().isEmpty()) {
                continue;
            }
            double mid = start + (i + 0.5) * sweep;
            double iconR = radius * (0.55 + 0.05 * popValues[i]);
            int x = centerX + (int) (Math.cos(mid) * iconR) - 8;
            int y = centerY + (int) (Math.sin(mid) * iconR) - 8;
            graphics.renderItem(entry.icon(), x, y);
        }

        if (hovered >= 0) {
            graphics.drawCenteredString(font, entries.get(hovered).id().getPath().replace('_', ' '), centerX, centerY + radius + 14, OasColors.TEXT_PRIMARY);
        } else {
            graphics.drawCenteredString(font, Component.translatable("Select Ore for Scanner").getString(), centerX, 18, OasColors.TEXT_PRIMARY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Full disc fill, the base of the wheel. */
    private void drawDisc(GuiGraphics graphics, float r, int color) {
        int segments = Math.max(16, (int) Math.ceil((Math.PI * 2.0) / 0.10));
        float cx = centerX;
        float cy = centerY;
        PoseStack.Pose mat = graphics.pose().last();
        VertexConsumer consumer = graphics.bufferSource().getBuffer(PieRenderType.PIE);
        int alpha = (color >>> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        for (int s = 0; s < segments; s++) {
            double a0 = (Math.PI * 2.0) * s / segments;
            double a1 = (Math.PI * 2.0) * (s + 1) / segments;
            float x0 = cx + (float) Math.cos(a0) * r;
            float y0 = cy + (float) Math.sin(a0) * r;
            float x1 = cx + (float) Math.cos(a1) * r;
            float y1 = cy + (float) Math.sin(a1) * r;
            triangle(consumer, mat, cx, cy, x0, y0, x1, y1, red, green, blue, alpha);
        }
    }

    private void drawSlice(GuiGraphics graphics, double from, double to, float r, int color) {
        int segments = Math.max(4, (int) Math.ceil((to - from) / 0.12));
        float cx = centerX;
        float cy = centerY;
        PoseStack.Pose mat = graphics.pose().last();
        VertexConsumer consumer = graphics.bufferSource().getBuffer(PieRenderType.PIE);
        int alpha = (color >>> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        for (int s = 0; s < segments; s++) {
            double a0 = from + (to - from) * s / segments;
            double a1 = from + (to - from) * (s + 1) / segments;
            float x0 = cx + (float) Math.cos(a0) * r;
            float y0 = cy + (float) Math.sin(a0) * r;
            float x1 = cx + (float) Math.cos(a1) * r;
            float y1 = cy + (float) Math.sin(a1) * r;
            triangle(consumer, mat, cx, cy, x0, y0, x1, y1, red, green, blue, alpha);
        }
    }

/** Center hub showing the current selection. */
    private void drawHub(GuiGraphics graphics, Entry entry) {
        float hubR = Math.max(11.0F, radius * 0.24F);
        drawDisc(graphics, hubR, OasColors.SURFACE_PANEL);
        if (!entry.icon().isEmpty()) {
            graphics.renderItem(entry.icon(), centerX - 8, centerY - 8);
        }
    }

    private static void triangle(VertexConsumer consumer, PoseStack.Pose mat, float cx, float cy, float x0, float y0, float x1, float y1, int red, int green, int blue, int alpha) {
        consumer.vertex(mat.pose(), cx, cy, 0.0F).color(red, green, blue, alpha).endVertex();
        consumer.vertex(mat.pose(), x0, y0, 0.0F).color(red, green, blue, alpha).endVertex();
        consumer.vertex(mat.pose(), x1, y1, 0.0F).color(red, green, blue, alpha).endVertex();
    }

    private int sliceIndexAt(double mouseX, double mouseY) {
        int n = entries.size();
        if (n == 0) {
            return -1;
        }
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > radius || dist < radius * 0.26) {
            return -1;
        }
        double sweep = (Math.PI * 2.0) / n;
        double norm = Math.atan2(dy, dx) + Math.PI / 2.0;
        if (norm < 0) {
            norm += Math.PI * 2.0;
        }
        int idx = (int) (norm / sweep);
        return idx >= 0 && idx < n ? idx : -1;
    }

    private static int lighten(int argb) {
        int red = (((argb >> 16) & 0xFF) + 255) / 2;
        int green = (((argb >> 8) & 0xFF) + 255) / 2;
        int blue = ((argb & 0xFF) + 255) / 2;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }
}
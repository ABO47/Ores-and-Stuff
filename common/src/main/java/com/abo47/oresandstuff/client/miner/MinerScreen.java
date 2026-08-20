package com.abo47.oresandstuff.client.miner;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.layout.Align;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;

import com.abo47.oresandstuff.block.MinerBlock;
import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.client.ui.render.GradientRectTexture;
import com.abo47.oresandstuff.client.ui.render.StripeOverlayTexture;
import com.abo47.oresandstuff.client.ui.widget.PlayerInventoryWidget;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.data.config.MinerTierConfig;
import com.abo47.oresandstuff.miner.CommonItemTransfer;
import com.abo47.oresandstuff.miner.MinerBlockEntity;
import com.abo47.oresandstuff.miner.MinerStatus;
import com.abo47.oresandstuff.node.NodeQuality;

public final class MinerScreen {
    private static final int UI_W = 220;
    private static final int UI_H = 250;

    private final MinerBlockEntity be;

    private MinerScreen(MinerBlockEntity be) {
        this.be = be;
    }

    public static ModularUI create(MinerBlockEntity be, Player player) {
        return new ModularUI(new MinerScreen(be).build(), be, player);
    }

    private WidgetGroup build() {
        final int MACHINE_X = 10;
        final int MACHINE_Y = 22;
        final int MACHINE_W = 200;
        final int MACHINE_H = 118;

        final int ENERGY_X = 18;
        final int ENERGY_Y = 30;
        final int ENERGY_W = 20;
        final int ENERGY_H = 102;
        final int ENERGY_BAR_X = ENERGY_X + 2;
        final int ENERGY_BAR_Y = ENERGY_Y + 2;
        final int ENERGY_BAR_W = ENERGY_W - 4;
        final int ENERGY_BAR_H = ENERGY_H - 4;

        final int CENTER_X = 46;
        final int CENTER_W = 118;
        final int CENTER_Y = 30;
        final int CENTER_H = 102;

        final int OUTPUT_X = 170;
        final int OUTPUT_Y = 30;
        final int OUTPUT_W = 32;
        final int OUTPUT_H = 102;
        final int SLOT_X = OUTPUT_X + (OUTPUT_W - 18) / 2;
        final int SLOT_Y = OUTPUT_Y;
        final int TOGGLE_X = OUTPUT_X;
        final int TOGGLE_Y = OUTPUT_Y + 3 * 22 + 6;
        final int TOGGLE_W = OUTPUT_W;
        final int TOGGLE_H = 24;

        final int INVENTORY_X = 10;
        final int INVENTORY_Y = 150;
        final int INVENTORY_W = 200;
        final int INVENTORY_H = 100;

        WidgetGroup root = new WidgetGroup(0, 0, UI_W, UI_H);
        root.setBackground(bevelPanelTexture(OasColors.BG_0, OasColors.BORDER_STRONG, OasColors.BORDER));

        root.addWidget(new ImageWidget(MACHINE_X, MACHINE_Y, MACHINE_W, MACHINE_H, bevelPanelTexture(OasColors.BG_1, OasColors.BORDER_STRONG, OasColors.BORDER)));
        root.addWidget(new ImageWidget(INVENTORY_X, INVENTORY_Y, INVENTORY_W, INVENTORY_H, bevelPanelTexture(OasColors.BG_1, OasColors.BORDER_STRONG, OasColors.BORDER)));
        root.addWidget(new ImageWidget(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, bevelPanelTexture(OasColors.BG_2, OasColors.BORDER_STRONG, OasColors.BORDER)));
        root.addWidget(new ImageWidget(CENTER_X, CENTER_Y, CENTER_W, CENTER_H, bevelPanelTexture(OasColors.BG_2, OasColors.BORDER_STRONG, OasColors.BORDER)));
        root.addWidget(new ImageWidget(OUTPUT_X, OUTPUT_Y, OUTPUT_W, OUTPUT_H, bevelPanelTexture(OasColors.BG_2, OasColors.BORDER_STRONG, OasColors.BORDER)));

        ProgressTexture energyTexture = new ProgressTexture(
                new ColorRectTexture(OasColors.withAlpha(0xFF4A1010, 220)),
                new GuiTextureGroup(
                        new GradientRectTexture(0xFFFFB3B3, 0xFFD41212, true),
                        new StripeOverlayTexture(OasColors.withAlpha(0xFFFFFFFF, 58), 1, 3, false)
                )
        ).setFillDirection(ProgressTexture.FillDirection.DOWN_TO_UP);
        ProgressWidget energyBar = new ProgressWidget(this::uiEnergyRatio, ENERGY_BAR_X, ENERGY_BAR_Y, ENERGY_BAR_W, ENERGY_BAR_H, energyTexture);
        energyBar.setHoverTooltips(Component.literal("Energy: " + be.getEnergyStored() + " / " + be.getMaxEnergyStored() + " FE"));
        root.addWidget(energyBar);
        root.addWidget(new ImageWidget(ENERGY_BAR_X + 1, ENERGY_BAR_Y, 1, ENERGY_BAR_H, new ColorRectTexture(OasColors.withAlpha(0xFFFFFFFF, 48))));
        root.addWidget(new ImageWidget(ENERGY_BAR_X + ENERGY_BAR_W - 2, ENERGY_BAR_Y, 1, ENERGY_BAR_H, new ColorRectTexture(OasColors.withAlpha(0xFF000000, 64))));

        int qualityColor = qualityColor(be.getNodeQuality());
        ProgressTexture qualityTexture = new ProgressTexture(
                new ColorRectTexture(OasColors.withAlpha(OasColors.BG_0, 165)),
                new GuiTextureGroup(
                        new GradientRectTexture(qualityColor, darken(qualityColor, 0x30), true),
                        new StripeOverlayTexture(OasColors.withAlpha(0xFFFFFFFF, 46), 1, 3, true)
                )
        ).setFillDirection(ProgressTexture.FillDirection.LEFT_TO_RIGHT);
        ProgressWidget qualityBar = new ProgressWidget(this::uiQualityRatio, CENTER_X, 62, 84, 10, qualityTexture);
        qualityBar.setHoverTooltips(Component.literal("Quality: " + (int) be.getNodeQuality() + "%  \u00D7" + String.format("%.2f", NodeQuality.multiplier(be.getNodeQuality()))));
        root.addWidget(qualityBar);

        ProgressTexture progressTexture = new ProgressTexture(
                new ColorRectTexture(OasColors.withAlpha(OasColors.BG_0, 165)),
                new GuiTextureGroup(
                        new GradientRectTexture(0xFF7DEFFF, 0xFF21B7DF, true),
                        new StripeOverlayTexture(OasColors.withAlpha(0xFFFFFFFF, 58), 1, 3, true)
                )
        ).setFillDirection(ProgressTexture.FillDirection.LEFT_TO_RIGHT);
        ProgressWidget miningBar = new ProgressWidget(this::uiProgress01, CENTER_X, 88, 84, 10, progressTexture);
        miningBar.setHoverTooltips(Component.literal("Mining progress"));
        root.addWidget(miningBar);

        SwitchWidget powerToggle = new SwitchWidget(TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H, (clickData, pressed) -> be.setEnabled(pressed));
        powerToggle.setTexture(rockerTexture(true), rockerTexture(false));
        powerToggle.setHoverBorderTexture(1, OasColors.ACCENT_PRIMARY);
        powerToggle.setSupplier(be::isEnabled);
        powerToggle.setHoverTooltips(Component.literal(be.isEnabled() ? "Running" : "Stopped"));
        root.addWidget(powerToggle);

        WidgetGroup titleLane = new WidgetGroup(0, 6, UI_W, 12);
        LabelWidget title = new LabelWidget(0, 0, uiTitleComponent());
        title.setAlign(Align.TOP_CENTER);
        title.setDropShadow(true);
        titleLane.addWidget(title);
        root.addWidget(titleLane);

        Item outputItem = uiOutputItem();
        if (outputItem != null && outputItem != Items.AIR) {
            ImageWidget nodeIcon = new ImageWidget(CENTER_X, 30, 16, 16, new ItemStackTexture(outputItem));
            nodeIcon.setHoverTooltips(Component.literal(uiNodeName()));
            root.addWidget(nodeIcon);
        }

        LabelWidget nodeName = new LabelWidget(CENTER_X + 22, 32, () -> clip(uiNodeName(), 12));
        nodeName.setColor(OasColors.ACCENT_SOFT);
        nodeName.setDropShadow(true);
        root.addWidget(nodeName);

        LabelWidget qualityLabel = new LabelWidget(CENTER_X, 52, tr("Quality"));
        qualityLabel.setColor(OasColors.TEXT_SECONDARY);
        qualityLabel.setDropShadow(true);
        root.addWidget(qualityLabel);

        LabelWidget qualityValue = new LabelWidget(CENTER_X + 84, 60, () -> (int) be.getNodeQuality() + "%");
        qualityValue.setColor(qualityColor);
        qualityValue.setDropShadow(true);
        root.addWidget(qualityValue);

        LabelWidget miningLabel = new LabelWidget(CENTER_X, 78, tr("Mining"));
        miningLabel.setColor(OasColors.TEXT_SECONDARY);
        miningLabel.setDropShadow(true);
        root.addWidget(miningLabel);

        LabelWidget rateValue = new LabelWidget(CENTER_X + 60, 122, () -> String.format("%.2f/s", be.getRatePerSecond()));
        rateValue.setColor(OasColors.TEXT_SECONDARY);
        rateValue.setDropShadow(true);
        root.addWidget(rateValue);

        LabelWidget statusLabel = new LabelWidget(CENTER_X, 106, this::uiStatusStyled);
        statusLabel.setColor(statusColor(be.getStatus()));
        statusLabel.setDropShadow(true);
        statusLabel.setHoverTooltips(Component.literal(be.getStatus().plain()));
        root.addWidget(statusLabel);

        IItemTransfer outputTransfer = be.getOutputTransfer();
        for (int i = 0; i < CommonItemTransfer.SLOT_COUNT; i++) {
            SlotWidget outputSlot = new SlotWidget(outputTransfer, i, SLOT_X, SLOT_Y + i * 22, true, false);
            outputSlot.setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE.copy().setColor(OasColors.withAlpha(OasColors.TEXT_MUTED, 255)));
            outputSlot.setLocationInfo(false, false);
            outputSlot.setCanPutItems(false);
            outputSlot.setHoverTooltips(Component.translatable("Output slot"));
            root.addWidget(outputSlot);
        }

        LabelWidget inventoryLabel = new LabelWidget(INVENTORY_X + 8, INVENTORY_Y + 4, tr("Inventory"));
        inventoryLabel.setColor(OasColors.TEXT_SECONDARY);
        inventoryLabel.setDropShadow(true);
        root.addWidget(inventoryLabel);

        PlayerInventoryWidget playerInventory = new PlayerInventoryWidget(0, 0);
        playerInventory.setSelfPosition(INVENTORY_X + (INVENTORY_W - 176) / 2, INVENTORY_Y + 6);
        root.addWidget(playerInventory);

        return root;
    }

    private static IGuiTexture bevelPanelTexture(int fillColor, int outerBorder, int innerBorder) {
        return new GuiTextureGroup(
                new ColorRectTexture(fillColor),
                new ColorBorderTexture(1, outerBorder),
                new ColorBorderTexture(2, OasColors.withAlpha(innerBorder, 180))
        );
    }

    private static IGuiTexture rockerTexture(boolean on) {
        int plateFill = on ? OasColors.BG_3 : OasColors.BG_2;
        int rockerFill = on ? 0xFF6C8AA9 : 0xFF33485E;
        int lightAlpha = on ? 36 : 60;
        int darkAlpha = on ? 70 : 44;
        float tilt = on ? -2.5f : 2.5f;
        return new GuiTextureGroup(
                bevelPanelTexture(plateFill, OasColors.BORDER_STRONG, OasColors.BORDER),
                new ColorRectTexture(OasColors.BG_1).scale(0.76f),
                new ColorRectTexture(OasColors.withAlpha(0xFF000000, 70)).scale(0.62f).transform(1, tilt + 4),
                new ColorRectTexture(rockerFill).scale(0.58f).transform(0, tilt),
                new ColorRectTexture(OasColors.withAlpha(0xFFFFFFFF, lightAlpha)).scale(0.58f).transform(0, tilt - 4),
                new ColorRectTexture(OasColors.withAlpha(0xFF000000, darkAlpha)).scale(0.58f).transform(0, tilt + 4)
        );
    }

    private String uiNodeName() {
        ResourceLocation id = be.getNodeTypeId();
        if (id == null) {
            return tr("None");
        }
        String path = id.getPath();
        if (path.equals("air") || path.equals("idle")) {
            return tr("None");
        }
        return titleCase(path);
    }

    private Component uiTitleComponent() {
        String tierId = MinerBlock.tierId(be.getBlockState());
        String displayName = MinerTierConfig.displayName(tierId);
        if (MinerTierConfig.get(tierId).isPresent()) {
            return Component.literal(displayName).withStyle(style -> style.withColor(OasColors.TEXT_PRIMARY));
        }
        return Component.translatable("Miner %s", displayName)
                .withStyle(style -> style.withColor(OasColors.TEXT_PRIMARY));
    }

    private Item uiOutputItem() {
        ResourceLocation id = be.getNodeTypeId();
        if (id == null) {
            return null;
        }
        var type = OreNodeDataManager.INSTANCE.getNodeType(id).orElse(null);
        if (type == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.get(type.outputItem());
    }

    private String uiStatusStyled() {
        return be.getStatus().styled();
    }

    private double uiEnergyRatio() {
        double actual = Math.max(0.0, Math.min(1.0, (double) be.getEnergyStored() / (double) Math.max(1, be.getMaxEnergyStored())));
        if (actual <= 0.0) {
            return 0.0;
        }
        if (actual >= 1.0) {
            return 1.0;
        }
        int filledPixels = Math.max(1, Math.min(100, (int) Math.ceil(actual * 100)));
        return filledPixels / 100.0;
    }

    private double uiQualityRatio() {
        return Math.max(0.0, Math.min(1.0, be.getNodeQuality() / 200.0));
    }

    private double uiProgress01() {
        return Math.max(0.0, Math.min(1.0, be.getProgress()));
    }

    private static int statusColor(MinerStatus status) {
        return switch (status) {
            case RUNNING -> OasColors.ACCENT_MINT;
            case NO_POWER -> OasColors.WARNING;
            case OUTPUT_FULL -> OasColors.ERROR;
            case MAX_MINERS -> OasColors.ERROR;
            case STOPPED -> OasColors.TEXT_SECONDARY;
            case NO_NODE -> OasColors.TEXT_MUTED;
        };
    }

    private static int qualityColor(double quality) {
        double t = Math.max(0.0, Math.min(1.0, quality / 200.0));
        int r;
        int g;
        int b;
        if (t < 0.5) {
            double s = t * 2.0;
            r = (int) Math.round(0xD4 + (0x3F - 0xD4) * s);
            g = (int) Math.round(0x12 + (0xC1 - 0x12) * s);
            b = 0x12;
        } else {
            double s = (t - 0.5) * 2.0;
            r = (int) Math.round(0x3F + (0x39 - 0x3F) * s);
            g = (int) Math.round(0xC1 + (0xC1 - 0xC1) * s);
            b = (int) Math.round(0x12 + (0x6C - 0x12) * s);
        }
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int darken(int argb, int amount) {
        int r = Math.max(0, ((argb >> 16) & 0xFF) - amount);
        int g = Math.max(0, ((argb >> 8) & 0xFF) - amount);
        int b = Math.max(0, (argb & 0xFF) - amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static String clip(String value, int maxChars) {
        if (value == null || value.isEmpty()) {
            return tr("None");
        }
        if (value.length() <= maxChars) {
            return value;
        }
        if (maxChars <= 2) {
            return value.substring(0, maxChars);
        }
        return value.substring(0, maxChars - 2) + "..";
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private static String titleCase(String raw) {
        if (raw == null || raw.isEmpty()) {
            return tr("None");
        }
        String[] parts = raw.replace('-', '_').split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.isEmpty() ? tr("None") : out.toString();
    }
}
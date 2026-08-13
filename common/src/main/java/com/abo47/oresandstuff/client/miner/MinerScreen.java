package com.abo47.oresandstuff.client.miner;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.client.ui.render.GradientRectTexture;
import com.abo47.oresandstuff.client.ui.render.StripeOverlayTexture;
import com.abo47.oresandstuff.client.ui.widget.PlayerInventoryWidget;
import com.abo47.oresandstuff.miner.MinerBlockEntity;
import com.abo47.oresandstuff.miner.MinerStatus;
import com.abo47.oresandstuff.node.Purity;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.widget.layout.Align;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public final class MinerScreen {
    private static final int UI_W = 220;
    private static final int UI_H = 246;
    private static final int ENERGY_BAR_PIXELS = 68;

    private final MinerBlockEntity be;

    private MinerScreen(MinerBlockEntity be) {
        this.be = be;
    }

    public static ModularUI create(MinerBlockEntity be, Player player) {
        return new ModularUI(new MinerScreen(be).build(), be, player);
    }

    private WidgetGroup build() {
        final int OUTER = 12;
        final int PAD = 6;
        final int GAP = 8;

        final int TITLE_Y = 8;
        final int TITLE_H = 14;

        final int MACHINE_X = OUTER;
        final int MACHINE_Y = 26;
        final int MACHINE_W = 196;
        final int MACHINE_H = 114;

        final int ENERGY_W = 18;
        final int ENERGY_H = 72;
        final int CLUSTER_W = ENERGY_W + PAD + 118 + PAD + 24;
        final int CLUSTER_X = MACHINE_X + (MACHINE_W - CLUSTER_W) / 2;
        final int ENERGY_X = CLUSTER_X;
        final int ENERGY_Y = MACHINE_Y + PAD + 2;
        final int ENERGY_BAR_X = ENERGY_X + 2;
        final int ENERGY_BAR_Y = ENERGY_Y + 2;
        final int ENERGY_BAR_W = ENERGY_W - 4;
        final int ENERGY_BAR_H = ENERGY_H - 4;

        final int TOGGLE_W = ENERGY_W;
        final int TOGGLE_H = 24;
        final int TOGGLE_X = ENERGY_X;
        final int TOGGLE_Y = ENERGY_Y + ENERGY_H + 6;

        final int CENTER_X = ENERGY_X + ENERGY_W + PAD;
        final int INFO_W = 118;
        final int INFO_H = 38;
        final int INFO_X = CENTER_X;
        final int INFO_Y = ENERGY_Y;
        final int INFO_LABEL_X = INFO_X + PAD;
        final int INFO_VALUE_X = INFO_X + 62;
        final int INFO_ROW_1_Y = INFO_Y + 2;
        final int INFO_ROW_2_Y = INFO_Y + 13;
        final int INFO_ROW_3_Y = INFO_Y + 24;

        final int PROGRESS_W = 118;
        final int PROGRESS_H = 28;
        final int PROGRESS_X = CENTER_X;
        final int PROGRESS_Y = INFO_Y + INFO_H + PAD;
        final int PROGRESS_BAR_H = 10;
        final int PROGRESS_BAR_X = PROGRESS_X + 8;
        final int PROGRESS_BAR_Y = PROGRESS_Y + (PROGRESS_H - PROGRESS_BAR_H) / 2;
        final int PROGRESS_BAR_W = PROGRESS_W - 16;

        final int OUTPUT_FRAME_W = PROGRESS_H;
        final int OUTPUT_FRAME_H = PROGRESS_H;
        final int OUTPUT_FRAME_X = PROGRESS_X + PROGRESS_W + PAD;
        final int OUTPUT_FRAME_Y = PROGRESS_Y;
        final int OUTPUT_SLOT_X = OUTPUT_FRAME_X + 5;
        final int OUTPUT_SLOT_Y = OUTPUT_FRAME_Y + 5;

        final int INVENTORY_X = OUTER;
        final int INVENTORY_Y = MACHINE_Y + MACHINE_H + GAP;
        final int INVENTORY_W = 196;
        final int INVENTORY_H = 92;
        final int PLAYER_INV_W = 172;
        final int PLAYER_INV_H = 86;

        WidgetGroup root = new WidgetGroup(0, 0, UI_W, UI_H);
        root.setBackground(bevelPanelTexture(OasColors.BG_0, OasColors.BORDER_STRONG, OasColors.BORDER));

        root.addWidget(new ImageWidget(MACHINE_X, MACHINE_Y, MACHINE_W, MACHINE_H, bevelPanelTexture(OasColors.BG_1, OasColors.BORDER_STRONG, OasColors.BORDER)));
        root.addWidget(new ImageWidget(INVENTORY_X, INVENTORY_Y, INVENTORY_W, INVENTORY_H, bevelPanelTexture(OasColors.BG_1, OasColors.BORDER_STRONG, OasColors.BORDER)));
        root.addWidget(new ImageWidget(INFO_X, INFO_Y, INFO_W, INFO_H, bevelPanelTexture(OasColors.BG_2, OasColors.BORDER_STRONG, OasColors.BORDER)));
        root.addWidget(new ImageWidget(PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H, bevelPanelTexture(OasColors.BG_2, OasColors.BORDER_STRONG, OasColors.BORDER)));
        root.addWidget(new ImageWidget(OUTPUT_FRAME_X, OUTPUT_FRAME_Y, OUTPUT_FRAME_W, OUTPUT_FRAME_H, bevelPanelTexture(OasColors.BG_2, OasColors.BORDER_STRONG, OasColors.BORDER)));
        root.addWidget(new ImageWidget(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, bevelPanelTexture(OasColors.BG_2, OasColors.BORDER_STRONG, OasColors.BORDER)));
        root.addWidget(new ImageWidget(PROGRESS_BAR_X - 2, PROGRESS_BAR_Y - 2, PROGRESS_BAR_W + 4, PROGRESS_BAR_H + 4, bevelPanelTexture(OasColors.BG_1, OasColors.BORDER_STRONG, OasColors.BORDER)));

        ProgressTexture energyTexture = new ProgressTexture(
                new ColorRectTexture(OasColors.withAlpha(0xFF4A1010, 220)),
                new GuiTextureGroup(
                        new GradientRectTexture(0xFFFFB3B3, 0xFFD41212, true),
                        new StripeOverlayTexture(OasColors.withAlpha(0xFFFFFFFF, 58), 1, 3, false)
                )
        ).setFillDirection(ProgressTexture.FillDirection.DOWN_TO_UP);
        ProgressWidget energyBar = new ProgressWidget(this::uiEnergyRatio, ENERGY_BAR_X, ENERGY_BAR_Y, ENERGY_BAR_W, ENERGY_BAR_H, energyTexture);
        energyBar.setHoverTooltips(Component.literal("Energy"));
        root.addWidget(energyBar);
        root.addWidget(new ImageWidget(ENERGY_BAR_X + 1, ENERGY_BAR_Y, 1, ENERGY_BAR_H, new ColorRectTexture(OasColors.withAlpha(0xFFFFFFFF, 48))));
        root.addWidget(new ImageWidget(ENERGY_BAR_X + ENERGY_BAR_W - 2, ENERGY_BAR_Y, 1, ENERGY_BAR_H, new ColorRectTexture(OasColors.withAlpha(0xFF000000, 64))));

        ProgressTexture progressTexture = new ProgressTexture(
                new ColorRectTexture(OasColors.withAlpha(OasColors.BG_0, 165)),
                new GuiTextureGroup(
                        new GradientRectTexture(0xFF7DEFFF, 0xFF21B7DF, true),
                        new StripeOverlayTexture(OasColors.withAlpha(0xFFFFFFFF, 58), 1, 3, true)
                )
        ).setFillDirection(ProgressTexture.FillDirection.LEFT_TO_RIGHT);
        ProgressWidget miningBar = new ProgressWidget(this::uiProgress01, PROGRESS_BAR_X, PROGRESS_BAR_Y, PROGRESS_BAR_W, PROGRESS_BAR_H, progressTexture);
        miningBar.setHoverTooltips(Component.literal("Mining progress"));
        root.addWidget(miningBar);
        root.addWidget(new ImageWidget(PROGRESS_BAR_X, PROGRESS_BAR_Y, PROGRESS_BAR_W, 2, new ColorRectTexture(OasColors.withAlpha(0xFFFFFFFF, 38))));
        root.addWidget(new ImageWidget(PROGRESS_BAR_X, PROGRESS_BAR_Y + PROGRESS_BAR_H - 2, PROGRESS_BAR_W, 1, new ColorRectTexture(OasColors.withAlpha(0xFF000000, 56))));

        SwitchWidget powerToggle = new SwitchWidget(TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H, (clickData, pressed) -> be.setEnabled(pressed));
        powerToggle.setTexture(rockerTexture(false), rockerTexture(true));
        powerToggle.setHoverBorderTexture(1, OasColors.ACCENT_PRIMARY);
        powerToggle.setSupplier(be::isEnabled);
        powerToggle.setHoverTooltips(Component.literal("Toggle miner"));
        root.addWidget(powerToggle);

        WidgetGroup titleLane = new WidgetGroup(0, TITLE_Y, UI_W, TITLE_H);
        LabelWidget title = new LabelWidget(0, 0, uiTitleComponent());
        title.setAlign(Align.TOP_CENTER);
        title.setDropShadow(true);
        titleLane.addWidget(title);
        root.addWidget(titleLane);

        LabelWidget nodeLabel = new LabelWidget(INFO_LABEL_X, INFO_ROW_1_Y, "Node");
        nodeLabel.setColor(OasColors.TEXT_SECONDARY);
        nodeLabel.setDropShadow(true);
        root.addWidget(nodeLabel);

        LabelWidget nodeValue = new LabelWidget(INFO_VALUE_X, INFO_ROW_1_Y, () -> clip(uiNodeName(), 12));
        nodeValue.setColor(OasColors.ACCENT_SOFT);
        nodeValue.setDropShadow(true);
        root.addWidget(nodeValue);

        LabelWidget purityLabel = new LabelWidget(INFO_LABEL_X, INFO_ROW_2_Y, "Purity");
        purityLabel.setColor(OasColors.TEXT_SECONDARY);
        purityLabel.setDropShadow(true);
        root.addWidget(purityLabel);

        LabelWidget purityValue = new LabelWidget(INFO_VALUE_X, INFO_ROW_2_Y, () -> clip(uiPurityName(), 8));
        purityValue.setColor(OasColors.ACCENT_SOFT);
        purityValue.setDropShadow(true);
        root.addWidget(purityValue);

        LabelWidget stateLabel = new LabelWidget(INFO_LABEL_X, INFO_ROW_3_Y, "State");
        stateLabel.setColor(OasColors.TEXT_SECONDARY);
        stateLabel.setDropShadow(true);
        root.addWidget(stateLabel);

        LabelWidget stateValue = new LabelWidget(INFO_VALUE_X, INFO_ROW_3_Y, this::uiStatusStyled);
        stateValue.setColor(OasColors.VANILLA_TEXT);
        stateValue.setDropShadow(true);
        root.addWidget(stateValue);

        IItemTransfer outputTransfer = be.getOutputTransfer();
        SlotWidget outputSlot = new SlotWidget(outputTransfer, 0, OUTPUT_SLOT_X, OUTPUT_SLOT_Y, true, false);
        outputSlot.setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE.copy().setColor(OasColors.withAlpha(OasColors.TEXT_MUTED, 255)));
        outputSlot.setLocationInfo(false, false);
        outputSlot.setCanPutItems(false);
        outputSlot.setHoverTooltips(Component.literal("Output slot"));
        root.addWidget(outputSlot);

        PlayerInventoryWidget playerInventory = new PlayerInventoryWidget(0, 0);
        playerInventory.setSelfPosition(INVENTORY_X + (INVENTORY_W - PLAYER_INV_W) / 2, INVENTORY_Y + (INVENTORY_H - PLAYER_INV_H) / 2);
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
            return "None";
        }
        String path = id.getPath();
        if (path.equals("air") || path.equals("idle")) {
            return "None";
        }
        return titleCase(path);
    }

    private Component uiTitleComponent() {
        return Component.empty()
                .append(Component.literal("Miner ").withStyle(style -> style.withColor(OasColors.TEXT_PRIMARY)))
                .append(Component.literal(uiTierSuffix()).withStyle(style -> style.withColor(OasColors.ACCENT_PRIMARY)));
    }

    private String uiTierSuffix() {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock());
        String path = id == null ? "mk1" : id.getPath();
        int mkIndex = path.lastIndexOf("_mk");
        if (mkIndex >= 0 && mkIndex + 1 < path.length()) {
            return titleCase(path.substring(mkIndex + 1));
        }
        String[] tokens = path.split("_");
        return tokens.length == 0 ? "Mk1" : titleCase(tokens[tokens.length - 1]);
    }

    private String uiPurityName() {
        return titleCase(be.getNodePurity().name().toLowerCase(Locale.ROOT));
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
        int filledPixels = Math.max(1, Math.min(ENERGY_BAR_PIXELS, (int) Math.ceil(actual * ENERGY_BAR_PIXELS)));
        return filledPixels / (double) ENERGY_BAR_PIXELS;
    }

    private double uiProgress01() {
        return Math.max(0.0, Math.min(1.0, be.getProgress()));
    }

    private static String clip(String value, int maxChars) {
        if (value == null || value.isEmpty()) {
            return "None";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        if (maxChars <= 2) {
            return value.substring(0, maxChars);
        }
        return value.substring(0, maxChars - 2) + "..";
    }

    private static String titleCase(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "None";
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
        return out.isEmpty() ? "None" : out.toString();
    }
}

package com.abo47.oresandstuff.client.miner;

import java.util.Locale;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.utils.Position;

import com.abo47.oresandstuff.block.MinerBlock;
import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.abo47.oresandstuff.client.ui.render.GradientRectTexture;
import com.abo47.oresandstuff.client.ui.render.EnergyBarTexture;
import com.abo47.oresandstuff.client.ui.widget.PlayerInventoryWidget;
import com.abo47.oresandstuff.data.OreNodeDataManager;
import com.abo47.oresandstuff.data.config.MinerTierConfig;
import com.abo47.oresandstuff.miner.CommonItemTransfer;
import com.abo47.oresandstuff.miner.MinerBlockEntity;
import com.abo47.oresandstuff.miner.MinerStatus;
import com.abo47.oresandstuff.node.OreNodeType;

public final class MinerScreen {
    private static final int UI_W = 221;
    private static final int UI_H = 251;
    private static final int ROOT_GAP = 9;
    private static final int GAP = 4;

    private final MinerBlockEntity be;
    private double smoothEnergy = -1.0;
    private boolean initialSnapDone;

    private MinerScreen(MinerBlockEntity be) {
        this.be = be;
    }

    public static ModularUI create(MinerBlockEntity be, Player player) {
        return new ModularUI(new MinerScreen(be).build(), be, player);
    }

    private WidgetGroup build() {
        final int CONT_X = 6;
        final int CONT_W = 208;
        final int CONT_H = 112;
        final int TOP_Y = ROOT_GAP;
        final int BOT_Y = TOP_Y + CONT_H + ROOT_GAP;

        final int CHILD_W = 44;
        final int MID_W = 104;
        final int LEFT_X = CONT_X + GAP;
        final int MID_X = LEFT_X + CHILD_W + GAP;
        final int RIGHT_X = MID_X + MID_W + GAP;
        final int CHILD_Y = TOP_Y + GAP;
        final int CHILD_H = CONT_H - 2 * GAP;

        final int ENERGY_BAR_X = LEFT_X + (CHILD_W - 16) / 2;
        final int ENERGY_BAR_Y = CHILD_Y + 11;
        final int ENERGY_BAR_W = 16;
        final int ENERGY_BAR_H = 70;

        final int ROW_X = MID_X + 4;
        final int ROW_1_Y = CHILD_Y + 4;
        final int ROW_GAP = 13;

        final int SPLIT_GAP = GAP;
        final int SLOT_PANEL_H = (CHILD_H - SPLIT_GAP) * 2 / 3;
        final int SLOT_PANEL_Y = CHILD_Y;
        final int BUTTON_PANEL_Y = SLOT_PANEL_Y + SLOT_PANEL_H + SPLIT_GAP;
        final int BUTTON_PANEL_H = CHILD_Y + CHILD_H - BUTTON_PANEL_Y;
        final int SLOT_X = RIGHT_X + (CHILD_W - 18) / 2;
        final int SLOT_1_Y = SLOT_PANEL_Y + (SLOT_PANEL_H - 58) / 2;
        final int SLOT_GAP = 20;
        final int TOGGLE_W = 34;
        final int TOGGLE_H = 16;
        final int TOGGLE_X = RIGHT_X + (CHILD_W - TOGGLE_W) / 2;
        final int TOGGLE_Y = BUTTON_PANEL_Y + (BUTTON_PANEL_H - TOGGLE_H) / 2;

        WidgetGroup root = new WidgetGroup(0, 0, UI_W, UI_H);
        root.setBackground(bevelPanelTexture(OasColors.BG_0, OasColors.BORDER_STRONG, OasColors.BORDER));

        root.addWidget(new ImageWidget(CONT_X, TOP_Y, CONT_W, CONT_H, bevelPanelTexture(OasColors.BG_1, OasColors.BORDER_STRONG, OasColors.BORDER)));
        root.addWidget(new ImageWidget(CONT_X, BOT_Y, CONT_W, CONT_H, bevelPanelTexture(OasColors.BG_1, OasColors.BORDER_STRONG, OasColors.BORDER)));

        root.addWidget(new ImageWidget(LEFT_X, CHILD_Y, CHILD_W, CHILD_H, bevelPanelTexture(OasColors.BG_2, OasColors.BORDER_STRONG, OasColors.BORDER)));
        root.addWidget(new ImageWidget(MID_X, CHILD_Y, MID_W, CHILD_H, bevelPanelTexture(OasColors.TERMINAL_BG, OasColors.BORDER_STRONG, OasColors.BORDER)));
        root.addWidget(new ImageWidget(RIGHT_X, SLOT_PANEL_Y, CHILD_W, SLOT_PANEL_H, bevelPanelTexture(OasColors.BG_2, OasColors.BORDER_STRONG, OasColors.BORDER)));
        root.addWidget(new ImageWidget(RIGHT_X, BUTTON_PANEL_Y, CHILD_W, BUTTON_PANEL_H, bevelPanelTexture(OasColors.BG_2, OasColors.BORDER_STRONG, OasColors.BORDER)));

        MinerUIState state = new MinerUIState(be);
        root.addWidget(state);

        root.addWidget(new ImageWidget(ENERGY_BAR_X - 1, ENERGY_BAR_Y - 1, ENERGY_BAR_W + 2, ENERGY_BAR_H + 2, bevelPanelTexture(OasColors.BG_0, OasColors.BORDER, OasColors.BORDER)));
        ImageWidget energyBar = new ImageWidget(ENERGY_BAR_X, ENERGY_BAR_Y, ENERGY_BAR_W, ENERGY_BAR_H,
                () -> new EnergyBarTexture(() -> uiEnergySmooth(state), () -> energyColor(state))) {
            @Override
            @Environment(EnvType.CLIENT)
            public void updateScreen() {
                super.updateScreen();
                setHoverTooltips(Component.literal("Energy: " + state.getEnergyStored() + " / " + state.getMaxEnergy() + " FE"));
            }
        };
        energyBar.setClientSideWidget();
        energyBar.setHoverTooltips(Component.literal("Energy: " + state.getEnergyStored() + " / " + state.getMaxEnergy() + " FE"));
        root.addWidget(energyBar);

        EnergyPercentLabel energyPercent = new EnergyPercentLabel(LEFT_X, ENERGY_BAR_Y + ENERGY_BAR_H + 6, CHILD_W, state);
        root.addWidget(energyPercent);

        LabelWidget tierLabel = terminalLabel(ROW_X, ROW_1_Y, state, s -> "tier: " + s.getTierId());
        root.addWidget(tierLabel);
        LabelWidget outputLabel = terminalLabel(ROW_X, ROW_1_Y + ROW_GAP, state, s -> "output: " + nodeName(s));
        root.addWidget(outputLabel);
        LabelWidget qualityLabel = terminalLabel(ROW_X, ROW_1_Y + 2 * ROW_GAP, state, s -> "quality: " + qualityText(s));
        root.addWidget(qualityLabel);
        LabelWidget rateLabel = terminalLabel(ROW_X, ROW_1_Y + 3 * ROW_GAP, state, s -> "rate: " + rateText(s));
        root.addWidget(rateLabel);
        LabelWidget progressLabel = terminalLabel(ROW_X, ROW_1_Y + 4 * ROW_GAP, state, s -> "progress: " + progressText(s));
        root.addWidget(progressLabel);

        // Red blinking alert row at the end of the console: no energy / output full / miner limit hit
        int alertY = ROW_1_Y + 5 * ROW_GAP;
        int alertW = MID_W - 8;
        MinerAlertWidget alertLabel = new MinerAlertWidget(ROW_X, alertY, alertW, 9, state, be);
        root.addWidget(alertLabel);

        LabelWidget promptLabel = new LabelWidget(ROW_X, CHILD_Y + CHILD_H - 12, ">");
        promptLabel.setTextProvider(() -> {
            boolean blink = (be.getLevel() != null ? be.getLevel().getGameTime() : System.currentTimeMillis() / 50L) % 30 < 15;
            return blink ? ">_" : "> ";
        });
        promptLabel.setColor(OasColors.darken(OasColors.TERMINAL_GREEN, 45));
        promptLabel.setDropShadow(true);
        root.addWidget(promptLabel);

        MinerToggleWidget toggle = new MinerToggleWidget(TOGGLE_X, TOGGLE_Y, be, state);
        root.addWidget(toggle);

        IItemTransfer outputTransfer = be.getOutputTransfer();
        for (int i = 0; i < CommonItemTransfer.SLOT_COUNT; i++) {
            SlotWidget outputSlot = new SlotWidget(outputTransfer, i, SLOT_X, SLOT_1_Y + i * SLOT_GAP, true, false);
            outputSlot.setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE.copy().setColor(OasColors.withAlpha(OasColors.TEXT_MUTED, 255)));
            outputSlot.setLocationInfo(false, false);
            outputSlot.setCanPutItems(false);
            outputSlot.setHoverTooltips(Component.translatable("Output slot"));
            root.addWidget(outputSlot);
        }

        PlayerInventoryWidget playerInventory = new PlayerInventoryWidget(0, 0);
        playerInventory.setSelfPosition(CONT_X + (CONT_W - 176) / 2 - 1, BOT_Y + (CONT_H - 72) / 2);
        root.addWidget(playerInventory);

        return root;
    }

    private static LabelWidget terminalLabel(int x, int y, MinerUIState state, java.util.function.Function<MinerUIState, String> text) {
        LabelWidget label = new LabelWidget(x, y, text.apply(state));
        label.setTextProvider(() -> text.apply(state));
        label.setColor(OasColors.TERMINAL_GREEN);
        label.setDropShadow(true);
        return label;
    }

    private final class EnergyPercentLabel extends Widget {
        private final MinerUIState state;
        private int color = OasColors.TEXT_SECONDARY;

        private EnergyPercentLabel(int x, int y, int width, MinerUIState state) {
            super(x, y, width, 10);
            this.state = state;
            setClientSideWidget();
        }

        @Override
        public void updateScreen() {
            super.updateScreen();
            color = energyColor(state);
        }

        @Override
        @Environment(EnvType.CLIENT)
        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            String text = (int) Math.round(uiEnergySmooth(state) * 100) + "%";
            Font font = Minecraft.getInstance().font;
            Position pos = getPosition();
            int x = pos.x + (getSize().width - font.width(text)) / 2;
            int y = pos.y + (getSize().height - font.lineHeight) / 2;
            graphics.drawString(font, text, x, y, color, true);
        }
    }

    private static final class MinerAlertWidget extends Widget {
        private final MinerUIState state;
        private final MinerBlockEntity be;

        private MinerAlertWidget(int x, int y, int w, int h, MinerUIState state, MinerBlockEntity be) {
            super(x, y, w, h);
            this.state = state;
            this.be = be;
            setClientSideWidget();
        }

        private static String alertFor(MinerStatus status) {
            if (status == null) {
                return null;
            }
            return switch (status) {
                case NO_POWER -> "no energy";
                case OUTPUT_FULL -> "output full";
                case MAX_MINERS -> "miner limit hit";
                default -> null;
            };
        }

        @Override
        @Environment(EnvType.CLIENT)
        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            String text = alertFor(state.getStatus());
            if (text == null) {
                return;
            }
            long t = be.getLevel() != null ? be.getLevel().getGameTime() : System.currentTimeMillis() / 50L;
            boolean visible = (t % 20) < 10;
            if (!visible) {
                return;
            }
            Font font = Minecraft.getInstance().font;
            Position pos = getPosition();
            int x = pos.x;
            int y = pos.y + (getSize().height - font.lineHeight) / 2;
            graphics.drawString(font, text, x, y, OasColors.ERROR, true);
        }
    }

    private static IGuiTexture bevelPanelTexture(int fillColor, int outerBorder, int innerBorder) {
        return new GuiTextureGroup(
                new ColorRectTexture(fillColor),
                new ColorBorderTexture(1, outerBorder),
                new ColorBorderTexture(2, OasColors.withAlpha(innerBorder, 180))
        );
    }

    private static String clip(String value, int maxChars) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= maxChars) {
            return value;
        }
        if (maxChars <= 2) {
            return value.substring(0, maxChars);
        }
        return value.substring(0, maxChars - 2) + "..";
    }

    private static String nodeName(MinerUIState state) {
        ResourceLocation id = state.getNodeTypeId();
        if (id == null) {
            return "none";
        }
        String path = id.getPath();
        if (path.equals("air") || path.equals("idle")) {
            return "none";
        }
        return clip(titleCase(path).toLowerCase(Locale.ROOT), 16);
    }

    private static String qualityText(MinerUIState state) {
        ResourceLocation id = state.getNodeTypeId();
        if (id == null || id.getPath().equals("air") || id.getPath().equals("idle")) {
            return "--";
        }
        return (int) state.getNodeQuality() + "%%";
    }

    private static String rateText(MinerUIState state) {
        return String.format(Locale.ROOT, "%.1f/s", state.getRatePerSecond());
    }

    private static String progressText(MinerUIState state) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, state.getDisplayProgress())) * 100) + "%%";
    }

    private double uiEnergySmooth(MinerUIState state) {
        double target = energyRatio(state);
        if (!initialSnapDone) {
            smoothEnergy = target;
            if (state.isSynced()) {
                initialSnapDone = true;
            }
        } else {
            smoothEnergy += (target - smoothEnergy) * 0.25;
        }
        return smoothEnergy;
    }

    private static int energyColor(MinerUIState state) {
        double ratio = energyRatio(state);
        if (ratio < 0.34) {
            return OasColors.ERROR;
        }
        if (ratio < 0.67) {
            return OasColors.WARNING;
        }
        return OasColors.SUCCESS;
    }

    private static double energyRatio(MinerUIState state) {
        double actual = Math.max(0.0, Math.min(1.0, (double) state.getEnergyStored() / (double) Math.max(1, state.getMaxEnergy())));
        if (actual <= 0.0) {
            return 0.0;
        }
        if (actual >= 1.0) {
            return 1.0;
        }
        int filledPixels = Math.max(1, Math.min(100, (int) Math.ceil(actual * 100)));
        return filledPixels / 100.0;
    }

    private static String titleCase(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "none";
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
        return out.isEmpty() ? "none" : out.toString();
    }
}
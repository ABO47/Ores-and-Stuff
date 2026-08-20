package com.abo47.oresandstuff.client.miner;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;

import com.abo47.oresandstuff.client.ui.controls.ToggleSwitchWidget;
import com.abo47.oresandstuff.miner.MinerBlockEntity;

public class MinerToggleWidget extends ToggleSwitchWidget {
    private final MinerUIState state;

    public MinerToggleWidget(int x, int y, MinerBlockEntity be, MinerUIState state) {
        super(x, y, ToggleSwitchWidget.DEFAULT_WIDTH, ToggleSwitchWidget.DEFAULT_HEIGHT,
                () -> state.isEnabled(), pressed -> be.setEnabled(pressed), () -> {}, null);
        this.state = state;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        setHoverTooltips(statusTooltip(state));
    }

    private static Component[] statusTooltip(MinerUIState state) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(state.getStatus().plainKey()).withStyle(style -> style.withColor(0xFFFFFFFF)));
        lines.add(Component.translatable("miner.status.click_hint").withStyle(style -> style.withColor(0xFF8B98A8)));
        return lines.toArray(new Component[0]);
    }
}
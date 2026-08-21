package com.abo47.oresandstuff.client.miner;

import com.abo47.oresandstuff.client.ui.controls.ToggleSwitchWidget;
import com.abo47.oresandstuff.miner.MinerBlockEntity;

public class MinerToggleWidget extends ToggleSwitchWidget {
    public MinerToggleWidget(int x, int y, MinerBlockEntity be, MinerUIState state) {
        super("", x, y, ToggleSwitchWidget.DEFAULT_WIDTH, ToggleSwitchWidget.DEFAULT_HEIGHT,
                () -> state.isEnabled(), pressed -> be.setEnabled(pressed), () -> true, () -> {}, null, false);
    }
}
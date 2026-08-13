package com.abo47.oresandstuff.client.ui.widget;

import com.abo47.oresandstuff.client.theme.tokens.OasColors;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.world.entity.player.Inventory;

public class PlayerInventoryWidget extends WidgetGroup {
    public PlayerInventoryWidget(int x, int y) {
        super(x, y, 176, 86);
        for (int col = 0; col < 9; col++) {
            addWidget(templateSlot("player_inv_" + col, 8 + col * 18, 76));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addWidget(templateSlot("player_inv_" + (col + (row + 1) * 9), 8 + col * 18, 18 + row * 18));
            }
        }
    }

    private SlotWidget templateSlot(String id, int x, int y) {
        SlotWidget slot = new SlotWidget();
        slot.setId(id);
        slot.setSelfPosition(x, y);
        slot.setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE.copy().setColor(OasColors.withAlpha(OasColors.TEXT_MUTED, 255)));
        return slot;
    }

    @Override
    public void initWidget() {
        super.initWidget();
        Inventory inventory = gui.entityPlayer.getInventory();
        for (int i = 0; i < widgets.size(); i++) {
            if (widgets.get(i) instanceof SlotWidget slot) {
                slot.setContainerSlot(inventory, i);
                slot.setLocationInfo(true, i < 9);
                slot.setCanPutItems(true);
                slot.setCanTakeItems(true);
            }
        }
    }
}

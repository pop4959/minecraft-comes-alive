package net.mca.client.gui.widget;

import net.minecraft.text.MutableText;

public class ToggleableTooltipButtonWidget extends TooltipButtonWidget {
    public boolean toggle = false;

    public ToggleableTooltipButtonWidget(int x, int y, int width, int height, String message, PressAction onPress) {
        super(x, y, width, height, message, onPress);
    }

    public ToggleableTooltipButtonWidget(int x, int y, int width, int height, MutableText message, MutableText tooltip, PressAction onPress) {
        super(x, y, width, height, message, tooltip, onPress);
    }

    protected int getYImage(boolean hovered) {
        int i = 1;
        if (!this.toggle) {
            i = 0;
        } else if (hovered) {
            i = 2;
        }
        return i;
    }
}

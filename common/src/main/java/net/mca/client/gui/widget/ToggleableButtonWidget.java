package net.mca.client.gui.widget;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ToggleableButtonWidget extends ButtonWidget {
    public boolean toggle = false;

    public ToggleableButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress);
    }

    public ToggleableButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, TooltipSupplier tooltipSupplier) {
        super(x, y, width, height, message, onPress, tooltipSupplier);
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

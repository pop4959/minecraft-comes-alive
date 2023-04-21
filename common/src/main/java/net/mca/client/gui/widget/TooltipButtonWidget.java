package net.mca.client.gui.widget;

import net.mca.util.localization.FlowingText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class TooltipButtonWidget extends ButtonWidget {
    private String message;

    public TooltipButtonWidget(int x, int y, int width, int height, String message, PressAction onPress) {
        super(x, y, width, height, Text.translatable(message), onPress, (ButtonWidget buttonWidget, MatrixStack matrixStack, int mx, int my) -> {
            assert MinecraftClient.getInstance().currentScreen != null;
            MinecraftClient.getInstance().currentScreen.renderTooltip(matrixStack, FlowingText.wrap(Text.translatable(((TooltipButtonWidget)buttonWidget).message + ".tooltip"), 160), mx, my);
        });
    }

    public TooltipButtonWidget(int x, int y, int width, int height, MutableText message, MutableText tooltip, PressAction onPress) {
        super(x, y, width, height, message, onPress, (ButtonWidget buttonWidget, MatrixStack matrixStack, int mx, int my) -> {
            assert MinecraftClient.getInstance().currentScreen != null;
            MinecraftClient.getInstance().currentScreen.renderTooltip(matrixStack, FlowingText.wrap(tooltip, 160), mx, my);
        });
    }

    public void setMessage(String message) {
        this.message = message;
        super.setMessage(Text.translatable(message));
    }
}

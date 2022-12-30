package net.mca.client.gui.widget;

import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class GeneSliderWidget extends SliderWidget {
    private final Consumer<Double> callback;

    public GeneSliderWidget(int x, int y, int width, int height, Text text, double value, Consumer<Double> callback) {
        super(x, y, width, height, text, value);
        this.updateMessage();
        this.callback = callback;
        setTooltip(Tooltip.of(Text.translatable("gui.test")));
    }

    @Override
    protected void applyValue() {
        callback.accept(value);
    }

    @Override
    protected void updateMessage() {

    }
}

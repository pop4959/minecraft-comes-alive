package net.mca.client.gui.widget;

import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DoubleSliderWidget extends ExtendedSliderWidget<Double> {
    private final double min;
    private final double max;
    private final Function<Double, Text> textFunction;

    public DoubleSliderWidget(int x, int y, int width, int height, double value, double min, double max, Consumer<Double> onApplyValue, Function<Double, Text> function, Supplier<Text> tooltipSupplier) {
        super(x, y, width, height, Text.literal(""), (value - min) / (max - min), onApplyValue, tooltipSupplier);
        this.min = min;
        this.max = max;
        textFunction = function;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(textFunction.apply(getValue()));
    }

    @Override
    Double getValue() {
        return (value * (max - min) + min);
    }

    @Override
    protected double getOpticalValue() {
        return (getValue() - min) / (max - min);
    }
}

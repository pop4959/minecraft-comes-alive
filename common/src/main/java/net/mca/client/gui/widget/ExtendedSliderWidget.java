package net.mca.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.mca.util.localization.FlowingText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class ExtendedSliderWidget<T> extends SliderWidget {
    private T oldValue;
    final Consumer<T> onApplyValue;
    protected final Supplier<Text> tooltipSupplier;

    public ExtendedSliderWidget(int x, int y, int width, int height, Text text, double value, Consumer<T> onApplyValue, Supplier<Text> tooltipSupplier) {
        super(x, y, width, height, text, value);
        this.onApplyValue = onApplyValue;
        this.tooltipSupplier = tooltipSupplier;
    }

    protected double getOpticalValue() {
        return value;
    }

    abstract T getValue();

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int i = (this.isHovered() ? 2 : 1) * 20;
        drawTexture(matrices, this.getX() + (int) (getOpticalValue() * (double) (this.width - 8)), this.getY(), 0, 46 + i, 4, 20);
        drawTexture(matrices, this.getX() + (int) (getOpticalValue() * (double) (this.width - 8)) + 4, this.getY(), 196, 46 + i, 4, 20);

        super.renderButton(matrices, mouseX, mouseY, delta);

        if (this.isHovered()) {
            this.renderTooltip(matrices, mouseX, mouseY);
        }
    }

    @Override
    protected void applyValue() {
        T v = getValue();
        if (v != oldValue) {
            oldValue = v;
            onApplyValue.accept(v);
        }
    }

    public void renderTooltip(MatrixStack matrices, int mouseX, int mouseY) {
        assert MinecraftClient.getInstance().currentScreen != null;
        MinecraftClient.getInstance().currentScreen.renderTooltip(matrices, FlowingText.wrap(tooltipSupplier.get(), 160), mouseX, mouseY);
    }
}

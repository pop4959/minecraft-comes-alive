package net.mca.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.function.Supplier;

public class HorizontalGradientWidget extends HorizontalColorPickerWidget {
    private final Supplier<float[]> startColorSupplier;
    private final Supplier<float[]> endColorSupplier;

    public HorizontalGradientWidget(int x, int y, int width, int height, double valueX, Supplier<float[]> startColorSupplier, Supplier<float[]> endColorSupplier, DualConsumer<Double, Double> consumer) {
        super(x, y, width, height, valueX, null, consumer);

        this.startColorSupplier = startColorSupplier;
        this.endColorSupplier = endColorSupplier;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float[] startColor = startColorSupplier.get();
        float[] endColor = endColorSupplier.get();

        float z = 0.0f;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        builder.vertex(matrix, (float)getX() + width, (float)getY(), z).color(endColor[0], endColor[1], endColor[2], endColor[3]).next();
        builder.vertex(matrix, (float)getX(), (float)getY(), z).color(startColor[0], startColor[1], startColor[2], startColor[3]).next();
        builder.vertex(matrix, (float)getX(), (float)getY() + height, z).color(startColor[0], startColor[1], startColor[2], startColor[3]).next();
        builder.vertex(matrix, (float)getX() + width, (float)getY() + height, z).color(endColor[0], endColor[1], endColor[2], endColor[3]).next();

        tessellator.draw();

        RenderSystem.disableBlend();
        RenderSystem.enableTexture();

        WidgetUtils.drawRectangle(matrices, getX(), getY(), getX() + width, getY() + height, 0xaaffffff);

        RenderSystem.setShaderTexture(0, MCA_GUI_ICONS_TEXTURE);
        DrawableHelper.drawTexture(matrices, (int)(getX() + valueX * width) - 8, (int)(getY() + valueY * height) - 8, 240, 0, 16, 16, 256, 256);
    }
}

package net.mca.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;

import static net.mca.client.gui.InteractScreen.ICON_TEXTURES;

public class ToggleableTooltipIconButtonWidget extends ToggleableTooltipButtonWidget {
    private final int u;
    private final int v;

    public ToggleableTooltipIconButtonWidget(int x, int y, int u, int v, boolean toggle, MutableText tooltip, PressAction onPress) {
        super(x, y, 16, 16, toggle, null, tooltip, onPress);

        this.u = u;
        this.v = v;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        drawIcon(matrices);
        if (this.isHovered()) {
            this.renderTooltip(matrices, mouseX, mouseY);
        }
    }

    private void drawIcon(MatrixStack matrices) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, ICON_TEXTURES);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        int offset = this.toggle ? 0 : 16;
        this.drawTexture(matrices, this.x, this.y, u, v + offset, this.width, this.height);
    }
}

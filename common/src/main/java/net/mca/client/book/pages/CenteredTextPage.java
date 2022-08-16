package net.mca.client.book.pages;

import net.mca.client.gui.ExtendedBookScreen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;

public class CenteredTextPage extends TextPage {
    public CenteredTextPage(String name, int page) {
        super(name, page);
    }

    @Override
    public void render(ExtendedBookScreen screen, MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //prepare page
        if (content != null) {
            TextRenderer textRenderer = screen.getTextRenderer();

            // text
            int l = Math.min(128 / 9, getCachedPage(screen).size());
            int i = (screen.width - 192) / 2;
            for (int m = 0; m < l; ++m) {
                OrderedText orderedText = getCachedPage(screen).get(m);
                float x = i + 36;
                textRenderer.draw(matrices, orderedText, x + 114 / 2.0f - textRenderer.getWidth(orderedText) / 2.0f, (32.0f + (m + 7 - (int)(l / 2.0f)) * 9.0f), 0);
            }
        }
    }
}

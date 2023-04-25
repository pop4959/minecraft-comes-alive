package net.mca.client.book.pages;

import net.mca.client.gui.ExtendedBookScreen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class CenteredListPage extends ListPage {
    final Text title;
    final List<Text> text;

    int page;

    public static final int ENTRIES_PER_PAGE = 11;

    public CenteredListPage(Text title, List<Text> text) {
        this.title = title;
        this.text = text;
    }

    public CenteredListPage(String title, List<Text> text) {
        this(Text.translatable(title).formatted(Formatting.BLACK).formatted(Formatting.BOLD), text);
    }

    @Override
    int getEntriesPerPage() {
        return 11;
    }

    private static void drawCenteredText(MatrixStack matrices, TextRenderer textRenderer, Text text, int centerX, int y, int color) {
        OrderedText orderedText = text.asOrderedText();
        textRenderer.draw(matrices, orderedText, (float)(centerX - textRenderer.getWidth(orderedText) / 2), (float)y, color);
    }

    @Override
    public void render(ExtendedBookScreen screen, MatrixStack matrices, int mouseX, int mouseY, float delta) {
        drawCenteredText(matrices, screen.getTextRenderer(), title, screen.width / 2, 35, 0xFFFFFFFF);

        int y = 48;
        for (int i = page * ENTRIES_PER_PAGE; i < Math.min(text.size(), (page + 1) * ENTRIES_PER_PAGE); i++) {
            drawCenteredText(matrices, screen.getTextRenderer(), text.get(i), screen.width / 2 - 4, y, 0xFFFFFFFF);
            y += 10;
        }
    }
}
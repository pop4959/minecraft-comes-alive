package net.mca.client.book.pages;

import net.mca.client.gui.ExtendedBookScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.List;

public class SimpleListPage extends ListPage {
    public SimpleListPage(List<Text> text) {
        super(text);
    }

    @Override
    int getEntriesPerPage() {
        return 14;
    }

    @Override
    public void render(ExtendedBookScreen screen, MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int y = 20;
        for (int i = page * getEntriesPerPage(); i < Math.min(text.size(), (page + 1) * getEntriesPerPage()); i++) {
            screen.getTextRenderer().draw(matrices, text.get(i), (screen.width - 192) / 2.0f + 36, (float)y, 0xFF000000);
            y += 10;
        }
    }
}

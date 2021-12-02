package mca.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import mca.client.book.Book;
import mca.client.book.pages.Page;
import mca.client.gui.widget.ExtendedPageTurnWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PageTurnWidget;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class ExtendedBookScreen extends Screen {
    private int pageIndex;
    private PageTurnWidget nextPageButton;
    private PageTurnWidget previousPageButton;
    private final Book book;

    public ExtendedBookScreen(Book book) {
        super(NarratorManager.EMPTY);
        this.book = book;
    }

    public boolean setPage(int index) {
        int i = MathHelper.clamp(index, 0, book.getPageCount() - 1);
        if (i != pageIndex) {
            pageIndex = i;
            updatePageButtons();
            return true;
        } else {
            return false;
        }
    }

    protected boolean jumpToPage(int page) {
        return setPage(page);
    }

    @Override
    protected void init() {
        addCloseButton();
        addPageButtons();
    }

    protected void addCloseButton() {
        addDrawableChild(new ButtonWidget(width / 2 - 100, 196, 200, 20, ScreenTexts.DONE, (buttonWidget) -> this.client.setScreen(null)));
    }

    protected void addPageButtons() {
        int i = (width - 192) / 2;
        nextPageButton = addDrawableChild(new ExtendedPageTurnWidget(i + 116, 159, true, (buttonWidget) -> goToNextPage(), book.hasPageTurnSound(), book.getBackground()));
        previousPageButton = addDrawableChild(new ExtendedPageTurnWidget(i + 43, 159, false, (buttonWidget) -> goToPreviousPage(), book.hasPageTurnSound(), book.getBackground()));
        updatePageButtons();
    }

    protected void goToPreviousPage() {
        if (pageIndex > 0) {
            --pageIndex;
        }
        this.updatePageButtons();
    }

    protected void goToNextPage() {
        if (pageIndex < book.getPageCount() - 1) {
            ++pageIndex;
        }
        this.updatePageButtons();
    }

    private void updatePageButtons() {
        this.nextPageButton.visible = this.pageIndex < book.getPageCount() - 1;
        this.previousPageButton.visible = this.pageIndex > 0;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        switch (keyCode) {
            case 266:
                this.previousPageButton.onPress();
                return true;
            case 267:
                this.nextPageButton.onPress();
                return true;
            default:
                return false;
        }
    }

    public void bindTexture(Identifier tex) {
        this.client.getTextureManager().bindTexture(tex);
    }

    public TextRenderer getTextRenderer() {
        return textRenderer;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        // background
        RenderSystem.setShaderColor(1, 1, 1, 1);
        bindTexture(book.getBackground());
        int i = (width - 192) / 2;
        drawTexture(matrices, i, 2, 0, 0, 192, 192);

        // page number
        Text pageIndexText = new TranslatableText("book.pageIndicator", pageIndex + 1, Math.max(book.getPageCount(), 1)).formatted(book.getTextFormatting());
        int k = textRenderer.getWidth(pageIndexText);
        textRenderer.draw(matrices, pageIndexText, i - k + 192 - 44, 18.0f, 0);

        Page page = book.getPage(pageIndex);
        if (page != null) {
            page.render(this, matrices, mouseX, mouseY, delta);
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean handleTextClick(Style style) {
        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent == null) {
            return false;
        }

        if (clickEvent.getAction() == ClickEvent.Action.CHANGE_PAGE) {
            try {
                return jumpToPage(Integer.parseInt(clickEvent.getValue()) - 1);
            } catch (Exception var5) {
                return false;
            }
        }

        boolean handled = super.handleTextClick(style);
        if (handled && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
            client.setScreen(null);
        }

        return handled;
    }
}

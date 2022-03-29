package mca.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import mca.cobalt.network.NetworkHandler;
import mca.network.DestinyMessage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.util.UUID;

import static mca.entity.VillagerLike.VILLAGER_NAME;

public class DestinyScreen extends VillagerEditorScreen {
    private static final Identifier LOGO_TEXTURE = new Identifier("mca:textures/banner.png");

    public DestinyScreen(UUID playerUUID) {
        super(playerUUID, playerUUID);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public void close() {
        if (!page.equals("general")) {
            setPage("destiny");
        }
    }

    @Override
    protected String[] getPages() {
        return new String[] {"general", "body", "head", "traits"};
    }

    @Override
    public void renderBackground(MatrixStack matrices) {
        renderBackgroundTexture((int)MinecraftClient.getInstance().world.getTime());
    }

    private void drawScaledText(MatrixStack transform, Text text, int x, int y, float scale) {
        transform.push();
        transform.scale(scale, scale, scale);
        drawCenteredText(transform, textRenderer, text, (int)(x / scale), (int)(y / scale), 0xffffffff);
        transform.pop();
    }

    @Override
    public void render(MatrixStack transform, int mouseX, int mouseY, float delta) {
        super.render(transform, mouseX, mouseY, delta);

        if (page.equals("general")) {
            drawScaledText(transform, new TranslatableText("gui.destiny.whoareyou"), width / 2, height / 2 - 16, 1.5f);

            transform.push();
            transform.scale(0.25f, 0.25f, 0.25f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShaderTexture(0, LOGO_TEXTURE);
            DrawableHelper.drawTexture(transform, width * 2 - 512, -40, 0, 0, 1024, 512, 1024, 512);
            transform.pop();
        } else if (page.equals("destiny")) {
            drawScaledText(transform, new TranslatableText("gui.destiny.journey"), width / 2, height / 2 - 48, 1.5f);
        }
    }

    @Override
    protected boolean shouldDrawEntity() {
        return !page.equals("general") && !page.equals("destiny") && super.shouldDrawEntity();
    }

    @Override
    protected void setPage(String page) {
        this.page = page;
        clearChildren();
        if (page.equals("general")) {
            drawName(width / 2 - DATA_WIDTH / 2, height / 2 + 8);
            drawGender(width / 2 - DATA_WIDTH / 2, height / 2 + 32);

            addDrawableChild(new ButtonWidget(width / 2 - 32, height / 2 + 64, 64, 20, new TranslatableText("gui.button.accept"), sender -> {
                setPage("body");
                if (villager.getTrackedValue(VILLAGER_NAME).isEmpty()) {
                    villager.setTrackedValue(VILLAGER_NAME, "Nameless Traveller");
                }
            }));
        } else if (page.equals("destiny")) {
            int x = 0;
            int y = 0;
            for (String location : new String[] {"somewhere", "shipwreck_beached", "village_desert", "village_taiga", "village_snowy", "village_plains", "village_savanna"}) {
                addDrawableChild(new ButtonWidget((int)(width / 2 - 96 * 1.5f + x * 96), height / 2 + y * 20 - 16, 96, 20, new TranslatableText("gui.destiny." + location), sender -> {
                    NetworkHandler.sendToServer(new DestinyMessage(location));
                    super.close();
                }));
                x++;
                if (x >= 3) {
                    x = 0;
                    y++;
                }
            }
        } else {
            super.setPage(page);
        }
    }
}

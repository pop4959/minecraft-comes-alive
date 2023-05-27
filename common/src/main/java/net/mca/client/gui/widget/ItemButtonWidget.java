package net.mca.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class ItemButtonWidget extends TooltipButtonWidget {
    final ItemStack item;

    public ItemButtonWidget(int x, int y, int size, MutableText message, ItemStack item, PressAction onPress) {
        super(x, y, size, size, Text.literal(""), message, onPress);

        this.item = item;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderButton(matrices, mouseX, mouseY, delta);

        int size = 16;

        MinecraftClient.getInstance().getItemRenderer().renderInGuiWithOverrides(matrices, item, getX() + (width - size) / 2, getY() + (height - size) / 2);
    }
}

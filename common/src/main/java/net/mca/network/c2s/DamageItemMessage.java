package net.mca.network.c2s;

import net.mca.cobalt.network.Message;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.io.Serial;
import java.util.Arrays;

public class DamageItemMessage implements Message {
    @Serial
    private static final long serialVersionUID = -8975978126445189429L;

    private final String itemIdentifier;

    public DamageItemMessage(Identifier identifier) {
        itemIdentifier = identifier.toString();
    }

    @Override
    public void receive(ServerPlayerEntity player) {
        Arrays.stream(Hand.values()).forEach(hand -> {
            ItemStack stack = player.getStackInHand(hand);
            if (Registry.ITEM.getId(stack.getItem()).toString().equals(itemIdentifier)) {
                stack.damage(1, player, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
            }
        });
    }
}

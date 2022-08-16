package net.mca.network.c2s;

import net.mca.cobalt.network.Message;
import net.mca.server.world.data.BabyTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.Serial;

public class BabyNamingVillagerMessage implements Message {
    @Serial
    private static final long serialVersionUID = -7160822837267592011L;

    private final int slot;
    private final String name;

    public BabyNamingVillagerMessage(int slot, String name) {
        this.slot = slot;
        this.name = name;
    }

    @Override
    public void receive(ServerPlayerEntity player) {
        ItemStack stack = player.getInventory().getStack(slot);
        BabyTracker.getState(stack, player.getWorld()).ifPresent(state -> {
            state.setName(name);
            state.writeToItem(stack);
        });
    }
}

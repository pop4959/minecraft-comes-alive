package mca.network.s2c;

import mca.cobalt.network.Message;
import mca.entity.VillagerLike;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class InteractionVillagerMessage implements Message {
    private static final long serialVersionUID = 2563941495766992462L;

    private final String command;
    private final UUID villagerUUID;

    public InteractionVillagerMessage(String command, UUID villagerUUID) {
        this.command = command.replace("gui.button.", "");
        this.villagerUUID = villagerUUID;
    }

    @Override
    public void receive(ServerPlayerEntity player) {
        Entity v = player.getWorld().getEntity(villagerUUID);
        if (v instanceof VillagerLike<?> villager) {
            if (villager.getInteractions().handle(player, command)) {
                villager.getInteractions().stopInteracting();
            }
        }
    }
}

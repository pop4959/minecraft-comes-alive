package mca.network.s2c;

import mca.cobalt.network.Message;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.Serial;

public class InteractionServerMessage implements Message {
    @Serial
    private static final long serialVersionUID = -7968792276814434450L;

    public InteractionServerMessage(String command) {
    }

    @Override
    public void receive(ServerPlayerEntity player) {
        // TODO: 7.3.0 should either implement this or markForRemoval
    }
}

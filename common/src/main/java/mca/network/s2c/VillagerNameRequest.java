package mca.network.s2c;

import mca.cobalt.network.Message;
import mca.cobalt.network.NetworkHandler;
import mca.entity.ai.relationship.Gender;
import mca.network.c2s.VillagerNameResponse;
import mca.resources.API;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.Serial;

public class VillagerNameRequest implements Message.ServerMessage {
    @Serial
    private static final long serialVersionUID = -7850240766540487322L;

    private final Gender gender;

    public VillagerNameRequest(Gender gender) {
        this.gender = gender;
    }

    @Override
    public void receive(ServerPlayerEntity player) {
        String name = API.getVillagePool().pickCitizenName(gender);
        NetworkHandler.sendToPlayer(new VillagerNameResponse(name), player);
    }
}

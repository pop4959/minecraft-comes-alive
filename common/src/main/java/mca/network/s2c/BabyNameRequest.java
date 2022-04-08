package mca.network.s2c;

import mca.cobalt.network.Message;
import mca.cobalt.network.NetworkHandler;
import mca.entity.ai.relationship.Gender;
import mca.network.c2s.BabyNameResponse;
import mca.resources.API;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.Serial;

public class BabyNameRequest implements Message.ServerMessage {
    @Serial
    private static final long serialVersionUID = 4965378949498898298L;

    private final Gender gender;

    public BabyNameRequest(Gender gender) {
        this.gender = gender;
    }

    @Override
    public void receive(ServerPlayerEntity player) {
        String name = API.getVillagePool().pickCitizenName(gender);
        NetworkHandler.sendToPlayer(new BabyNameResponse(name), player);
    }
}

package mca.network.c2s;

import mca.ClientProxy;
import mca.cobalt.network.Message;
import net.minecraft.entity.player.PlayerEntity;

import java.io.Serial;

public class GetVillageFailedResponse implements Message {
    @Serial
    private static final long serialVersionUID = 4021214184633955444L;

    @Override
    public void receive() {
        ClientProxy.getNetworkHandler().handleVillageDataFailedResponse(this);
    }
}

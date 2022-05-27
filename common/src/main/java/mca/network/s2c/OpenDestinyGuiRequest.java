package mca.network.s2c;

import mca.ClientProxy;
import mca.Config;
import mca.cobalt.network.Message;
import net.minecraft.server.network.ServerPlayerEntity;

public class OpenDestinyGuiRequest implements Message {
    public final int player;
    public boolean allowTeleportation;
    public boolean allowPlayerModel;
    public boolean allowVillagerModel;

    public OpenDestinyGuiRequest(ServerPlayerEntity player) {
        this.player = player.getId();

        allowTeleportation = Config.getInstance().allowDestinyTeleportation;
        allowPlayerModel = Config.getInstance().enableVillagerPlayerModel;
        allowVillagerModel = !Config.getInstance().forceVillagerPlayerModel;
    }

    @Override
    public void receive() {
        ClientProxy.getNetworkHandler().handleDestinyGuiRequest(this);
    }
}

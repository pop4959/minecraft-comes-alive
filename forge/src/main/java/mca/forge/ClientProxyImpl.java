package mca.forge;

import mca.ClientProxyAbstractImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class ClientProxyImpl extends ClientProxyAbstractImpl {
    @Override
    public PlayerEntity getClientPlayer() {
        return MinecraftClient.getInstance().player;
    }
}

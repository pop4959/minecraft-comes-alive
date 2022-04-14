package mca.network.s2c;

import mca.ClientProxy;
import mca.network.NbtDataMessage;
import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public class PlayerDataMessage extends NbtDataMessage {
    public final UUID uuid;

    public PlayerDataMessage(UUID uuid, NbtCompound nbt) {
        super(nbt);
        this.uuid = uuid;
    }

    @Override
    public void receive() {
        ClientProxy.getNetworkHandler().handlePlayerDataMessage(this);
    }
}

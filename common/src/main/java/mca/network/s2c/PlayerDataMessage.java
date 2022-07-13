package mca.network.s2c;

import mca.ClientProxy;
import mca.network.NbtDataMessage;
import net.minecraft.nbt.NbtCompound;

import java.io.Serial;
import java.util.UUID;

public class PlayerDataMessage extends NbtDataMessage {
    @Serial
    private static final long serialVersionUID = 145267688456022788L;

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

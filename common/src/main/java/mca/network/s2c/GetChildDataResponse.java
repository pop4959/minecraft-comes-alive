package mca.network.s2c;

import mca.ClientProxy;
import mca.network.NbtDataMessage;
import mca.server.world.data.BabyTracker.ChildSaveState;
import net.minecraft.nbt.NbtCompound;

import java.io.Serial;
import java.util.UUID;

public class GetChildDataResponse extends NbtDataMessage {
    @Serial
    private static final long serialVersionUID = -4415670234855916259L;

    public final UUID id;

    public GetChildDataResponse(ChildSaveState data) {
        super(data.writeToNbt(new NbtCompound()));
        this.id = data.getId();
    }

    @Override
    public void receive() {
        ClientProxy.getNetworkHandler().handleChildData(this);
    }
}

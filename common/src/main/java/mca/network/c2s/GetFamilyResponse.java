package mca.network.c2s;

import mca.ClientProxy;
import mca.network.NbtDataMessage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.io.Serial;

public class GetFamilyResponse extends NbtDataMessage {
    @Serial
    private static final long serialVersionUID = -8537919427646877115L;

    public GetFamilyResponse(NbtCompound data) {
        super(data);
    }

    @Override
    public void receive() {
        ClientProxy.getNetworkHandler().handleFamilyDataResponse(this);
    }
}

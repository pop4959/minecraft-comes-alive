package mca.network.c2s;

import mca.ClientProxy;
import mca.cobalt.network.Message;
import mca.resources.data.SerializablePair;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;
import java.util.UUID;

public class FamilyTreeUUIDResponse implements Message {
    private final List<SerializablePair<UUID, SerializablePair<String, String>>> list;

    public FamilyTreeUUIDResponse(List<SerializablePair<UUID, SerializablePair<String, String>>> list) {
        this.list = list;
    }

    @Override
    public void receive() {
        ClientProxy.getNetworkHandler().handleFamilyTreeUUIDResponse(this);
    }

    public List<SerializablePair<UUID, SerializablePair<String, String>>> getList() {
        return list;
    }
}

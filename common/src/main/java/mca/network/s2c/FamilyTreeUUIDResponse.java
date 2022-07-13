package mca.network.s2c;

import mca.ClientProxy;
import mca.cobalt.network.Message;
import mca.resources.data.SerializablePair;

import java.io.Serial;
import java.util.List;
import java.util.UUID;

public class FamilyTreeUUIDResponse implements Message {
    @Serial
    private static final long serialVersionUID = 8216277949975695897L;

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

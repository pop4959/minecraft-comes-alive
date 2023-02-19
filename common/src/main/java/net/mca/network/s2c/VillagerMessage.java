package net.mca.network.s2c;

import net.mca.ClientProxy;
import net.mca.cobalt.network.Message;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.io.Serial;
import java.util.UUID;

public class VillagerMessage implements Message {
    @Serial
    private static final long serialVersionUID = -4135222437610000843L;

    private final String message;
    private final UUID uuid;

    public VillagerMessage(MutableText message, UUID uuid) {
        this.message = Text.Serializer.toJson(message);
        this.uuid = uuid;
    }

    @Override
    public void receive() {
        ClientProxy.getNetworkHandler().handleVillagerMessage(this);
    }

    public MutableText getMessage() {
        return Text.Serializer.fromJson(message);
    }

    public UUID getUuid() {
        return uuid;
    }
}

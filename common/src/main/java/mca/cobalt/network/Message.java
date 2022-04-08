package mca.cobalt.network;

import mca.MCA;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.*;

public interface Message extends Serializable {

    static Message decode(PacketByteBuf b) {
        byte[] data = new byte[b.readableBytes()];
        b.readBytes(data);

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (Message)ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("SneakyThrows", e);
        }
    }

    default void encode(PacketByteBuf b) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(this);
        } catch (IOException e) {
            throw new RuntimeException("SneakyThrows", e);
        }

        b.writeBytes(baos.toByteArray());
    }

    default void receive(PlayerEntity e) {
        // N/A
    }

    default void receive(ServerPlayerEntity e) {
        // N/A
    }

    // More Forge-specific bs
    interface ServerMessage extends Message {
        @Override
        default void receive(PlayerEntity e) {
            if (e instanceof ServerPlayerEntity player) {
                receive(player);
            } else {
                MCA.LOGGER.error("Executing " + getClass().getSimpleName() + " from incorrect side!");
            }
        }
    }
}
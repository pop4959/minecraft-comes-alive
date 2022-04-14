package mca.network.c2s;

import mca.cobalt.network.Message;
import mca.cobalt.network.NetworkHandler;
import mca.server.world.data.PlayerSaveData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class PlayerDataRequest implements Message {
    private final UUID uuid;

    public PlayerDataRequest(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public void receive(ServerPlayerEntity player) {
        PlayerSaveData data = PlayerSaveData.get(player.getWorld(), uuid);
        if (data.isEntityDataSet()) {
            NbtCompound nbt = data.getEntityData();
            NetworkHandler.sendToPlayer(new PlayerDataMessage(uuid, nbt), player);
        }
    }
}

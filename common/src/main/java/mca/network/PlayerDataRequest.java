package mca.network;

import mca.cobalt.network.Message;
import mca.cobalt.network.NetworkHandler;
import mca.network.c2s.PlayerDataMessage;
import mca.server.world.data.PlayerSaveData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

public class PlayerDataRequest implements Message {
    private final UUID uuid;

    public PlayerDataRequest(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public void receive(PlayerEntity e) {
        PlayerSaveData data = PlayerSaveData.get((ServerWorld)e.world, uuid);
        if (data.isEntityDataSet()) {
            NbtCompound nbt = data.getEntityData();
            NetworkHandler.sendToPlayer(new PlayerDataMessage(uuid, nbt), (ServerPlayerEntity)e);
        }
    }
}

package mca.network.c2s;

import mca.cobalt.network.Message;
import mca.cobalt.network.NetworkHandler;
import mca.entity.VillagerLike;
import mca.network.s2c.GetFamilyResponse;
import mca.server.world.data.PlayerSaveData;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.Serial;
import java.util.stream.Stream;

public class GetFamilyRequest implements Message {
    @Serial
    private static final long serialVersionUID = -4415670234855916259L;

    @Override
    public void receive(ServerPlayerEntity player) {
        NbtCompound familyData = new NbtCompound();

        PlayerSaveData playerData = PlayerSaveData.get(player.getWorld(), player.getUuid());

        //fetches all members
        //de-loaded members are excluded as they can't teleport anyway

        Stream.concat(
                        playerData.getFamilyEntry().getAllRelatives(4),
                        playerData.getSpouseUuid().stream()
                ).distinct()
                .map(player.getWorld()::getEntity)
                .filter(e -> e instanceof VillagerLike<?>)
                .limit(100)
                .forEach(e -> {
                    NbtCompound nbt = new NbtCompound();
                    ((MobEntity)e).writeCustomDataToNbt(nbt);
                    nbt.remove("Brain");
                    nbt.remove("memories");
                    nbt.remove("Inventory");
                    familyData.put(e.getUuid().toString(), nbt);
                });

        NetworkHandler.sendToPlayer(new GetFamilyResponse(familyData), player);
    }
}

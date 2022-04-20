package mca.network.c2s;

import mca.cobalt.network.Message;
import mca.cobalt.network.NetworkHandler;
import mca.entity.ai.relationship.Gender;
import mca.network.s2c.SkinListResponse;
import mca.resources.ClothingList;
import mca.resources.WeightedPool;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.VillagerProfession;

import java.util.List;

public class SkinListRequest implements Message {
    @Override
    public void receive(ServerPlayerEntity player) {
        List<String> clothing = ClothingList.getInstance()
                .getPool(Gender.NEUTRAL, (VillagerProfession)null).getEntries().stream()
                .map(WeightedPool.Entry::getValue)
                .toList();

        NetworkHandler.sendToPlayer(new SkinListResponse(clothing), player);
    }
}

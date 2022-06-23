package mca.item;

import mca.Config;
import mca.entity.VillagerEntityMCA;
import mca.entity.ai.Relationship;
import mca.server.world.data.PlayerSaveData;
import net.minecraft.server.network.ServerPlayerEntity;

public class EngagementRingItem extends RelationshipItem {
    public EngagementRingItem(Settings properties) {
        super(properties);
    }

    @Override
    protected float getHeartsRequired() {
        return Config.getInstance().engagementHeartsRequirement;
    }

    @Override
    public boolean handle(ServerPlayerEntity player, VillagerEntityMCA villager) {
        PlayerSaveData playerData = PlayerSaveData.get(player);
        String response;
        boolean consume = false;

        if (super.handle(player, villager)) {
            return false;
        } else if (Relationship.IS_ENGAGED.test(villager, player)) {
            response = "interaction.engage.fail.engaged";
        } else {
            response = "interaction.engage.success";
            playerData.engage(villager);
            villager.getRelationships().engage(player);
            villager.getVillagerBrain().modifyMoodValue(10);
            consume = true;
        }

        villager.sendChatMessage(player, response);
        return consume;
    }
}

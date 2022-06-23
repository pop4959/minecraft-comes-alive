package mca.item;

import mca.Config;
import mca.entity.VillagerEntityMCA;
import mca.entity.ai.Memories;
import mca.entity.ai.Relationship;
import mca.entity.ai.relationship.MarriageState;
import mca.server.world.data.PlayerSaveData;
import net.minecraft.server.network.ServerPlayerEntity;

public class EngagementRingItem extends TooltippedItem implements SpecialCaseGift {
    public EngagementRingItem(Settings properties) {
        super(properties);
    }

    protected float getHeartsRequired() {
        return Config.getInstance().engagementHeartsRequirement;
    }

    @Override
    public boolean handle(ServerPlayerEntity player, VillagerEntityMCA villager) {
        PlayerSaveData playerData = PlayerSaveData.get(player.getWorld(), player.getUuid());
        Memories memory = villager.getVillagerBrain().getMemoriesForPlayer(player);
        String response;
        boolean consume = false;

        if (villager.isBaby()) {
            response = "interaction.engage.fail.isbaby";
        } else if (Relationship.IS_PARENT.test(villager, player)) {
            response = "interaction.marry.fail.isparent";
        } else if (Relationship.IS_MARRIED.test(villager, player)) {
            response = "interaction.marry.fail.marriedtogiver";
        } else if (villager.getRelationships().isMarried()) {
            response = "interaction.marry.fail.marriedtoother";
        } else if (villager.getRelationships().isEngaged() && !Relationship.IS_ENGAGED.test(villager, player)) {
            response = "interaction.marry.fail.engaged";
        } else if (Relationship.IS_ENGAGED.test(villager, player)) {
            response = "interaction.engage.fail.engaged";
        } else if (playerData.isMarried()) {
            response = "interaction.marry.fail.playermarried";
        } else if (memory.getHearts() < getHeartsRequired()) {
            response = "interaction.marry.fail.lowhearts";
        } else {
            response = "interaction.engage.success";
            villager.getRelationships().getFamilyEntry().updateMarriage(player, MarriageState.ENGAGED);
            villager.getVillagerBrain().modifyMoodValue(10);
            consume = true;
        }

        villager.sendChatMessage(player, response);
        return consume;
    }
}

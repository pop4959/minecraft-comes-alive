package mca.item;

import mca.Config;
import mca.entity.VillagerEntityMCA;
import mca.entity.ai.Memories;
import mca.entity.ai.Relationship;
import mca.server.world.data.PlayerSaveData;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

public class WeddingRingItem extends TooltippedItem implements SpecialCaseGift {
    public WeddingRingItem(Item.Settings properties) {
        super(properties);
    }

    protected float getHeartsRequired() {
        return Config.getInstance().marriageHeartsRequirement;
    }

    @Override
    public boolean handle(ServerPlayerEntity player, VillagerEntityMCA villager) {
        PlayerSaveData playerData = PlayerSaveData.get(player);
        Memories memory = villager.getVillagerBrain().getMemoriesForPlayer(player);
        String response;
        boolean consume = false;

        if (villager.isBaby()) {
            response = "interaction.marry.fail.isbaby";
        } else if (Relationship.IS_PARENT.test(villager, player)) {
            response = "interaction.marry.fail.isparent";
        } else if (Relationship.IS_MARRIED.test(villager, player)) {
            response = "interaction.marry.fail.marriedtogiver";
        } else if (villager.getRelationships().isMarried()) {
            response = "interaction.marry.fail.marriedtoother";
        } else if (villager.getRelationships().isEngaged() && !Relationship.IS_ENGAGED.test(villager, player)) {
            response = "interaction.marry.fail.engaged";
        } else if (playerData.isMarried()) {
            response = "interaction.marry.fail.playermarried";
        } else if (memory.getHearts() < getHeartsRequired()) {
            response = "interaction.marry.fail.lowhearts";
        } else {
            response = "interaction.marry.success";
            playerData.marry(villager);
            villager.getRelationships().marry(player);
            villager.getVillagerBrain().modifyMoodValue(15);
            consume = true;
        }

        villager.sendChatMessage(player, response);
        return consume;
    }
}

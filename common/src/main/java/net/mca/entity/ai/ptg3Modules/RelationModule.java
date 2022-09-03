package net.mca.entity.ai.ptg3Modules;

import net.mca.entity.VillagerEntityMCA;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

public class RelationModule {
    public static void apply(List<String> input, VillagerEntityMCA villager, PlayerEntity player) {
        int hearts = villager.getVillagerBrain().getMemoriesForPlayer(player).getHearts();
        if (hearts < -25) {
            input.add("$villager hates $player. ");
        } else if (hearts < 0) {
            input.add("$villager dislikes $player. ");
        } else if (hearts < 33) {
            input.add("$villager knows $player well. ");
        } else if (hearts < 66) {
            input.add("$villager likes $player. ");
        } else {
            input.add("$villager likes $player really well. ");
        }

        if (villager.getRelationships().isMarriedTo(player.getUuid())) {
            input.add("$villager is married to $player. ");
        } else if (villager.getRelationships().isMarried()) {
            input.add("$villager is married. ");
        }
    }
}

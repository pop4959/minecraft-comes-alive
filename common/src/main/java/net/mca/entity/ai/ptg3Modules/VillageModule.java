package net.mca.entity.ai.ptg3Modules;

import net.mca.entity.VillagerEntityMCA;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

public class VillageModule {
    public static void apply(List<String> input, VillagerEntityMCA villager, PlayerEntity player) {
        input.add("$villager lives in a small, medieval village in a forest. ");
        input.add("The village has a library, and an armory. "); // todo village buildings
    }
}

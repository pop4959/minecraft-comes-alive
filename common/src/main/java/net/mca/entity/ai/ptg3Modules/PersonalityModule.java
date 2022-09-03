package net.mca.entity.ai.ptg3Modules;

import net.mca.entity.VillagerEntityMCA;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

import static net.mca.entity.ai.PTG3.translate;

public class PersonalityModule {
    public static void apply(List<String> input, VillagerEntityMCA villager, PlayerEntity player) {
        input.add("$villager is a " + translate(villager.getGenetics().getGender().name()) + ". ");
        input.add("$villager is " + translate(villager.getVillagerBrain().getMood().getName()) + ". ");
        input.add("$villager is " + translate(villager.getVillagerBrain().getPersonality().name()) + ". ");
    }
}

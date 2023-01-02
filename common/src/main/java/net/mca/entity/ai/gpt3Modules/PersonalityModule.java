package net.mca.entity.ai.gpt3Modules;

import net.mca.entity.VillagerEntityMCA;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.VillagerProfession;

import java.util.List;

import static net.mca.entity.ai.GPT3.translate;

public class PersonalityModule {
    public static void apply(List<String> input, VillagerEntityMCA villager, ServerPlayerEntity player) {
        input.add("This is a conversation with a " + translate(villager.getGenetics().getGender().name()) + " Minecraft villager named $villager and $player." + " ");
        input.add("$villager is " + translate(villager.getVillagerBrain().getPersonality().name()) + " and " + translate(villager.getVillagerBrain().getMood().getName()) + ". ");
        if (villager.getProfession() != VillagerProfession.NONE) {
            input.add("$villager is a " + translate(villager.getProfession().id()) + ". ");
        }
    }
}

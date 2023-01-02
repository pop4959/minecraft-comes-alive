package net.mca.entity.ai.gpt3Modules;

import net.mca.entity.VillagerEntityMCA;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class EnvironmentModule {

    public static void apply(List<String> input, VillagerEntityMCA villager, ServerPlayerEntity player) {
        if (player.world.isRaining()) {
            input.add("It is raining. ");
        }
        if (player.world.isThundering()) {
            input.add("It is thundering. ");
        }
        if (player.world.isNight()) {
            input.add("It is night. ");
        }
    }
}

package net.mca.entity.ai.ptg3Modules;

import net.mca.entity.VillagerEntityMCA;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

public class PlayerModule {
    public static void apply(List<String> input, VillagerEntityMCA villager, PlayerEntity player) {
        input.add("$player visited the nether. "); // todo player achievements
    }
}

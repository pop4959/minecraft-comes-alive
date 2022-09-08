package net.mca.entity.ai.ptg3Modules;

import net.mca.entity.VillagerEntityMCA;
import net.mca.server.world.data.Building;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VillageModule {
    private final static Map<String, String> nameExceptions = Map.of(
            "fishermansHut", "fisherman's hut",
            "weavingMill", "weaving mill.",
            "bigHouse", "big house.",
            "musicStore", "music store.",
            "townCenter", "town center"
    );

    public static void apply(List<String> input, VillagerEntityMCA villager, ServerPlayerEntity player) {
        input.add("$villager lives in a small, medieval village. "); // todo biome

        villager.getResidency().getHomeVillage().ifPresent(v -> {
            String buildings = v.getBuildings().values().stream()
                    .map(Building::getType)
                    .map(b -> nameExceptions.getOrDefault(b, b))
                    .distinct()
                    .collect(Collectors.joining(", "));
            input.add("The village has a " + buildings + ". ");
        });
    }
}

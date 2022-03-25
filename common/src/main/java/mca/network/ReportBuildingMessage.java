package mca.network;

import java.io.Serial;
import java.util.Locale;
import java.util.Optional;
import mca.cobalt.network.Message;
import mca.server.world.data.Building;
import mca.server.world.data.GraveyardManager;
import mca.server.world.data.Village;
import mca.server.world.data.VillageManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;

public class ReportBuildingMessage implements Message {
    @Serial
    private static final long serialVersionUID = 3510050513221709603L;

    private final Action action;
    private final String data;

    public ReportBuildingMessage(Action action, String data) {
        this.action = action;
        this.data = data;
    }

    public ReportBuildingMessage(Action action) {
        this(action, null);
    }

    @Override
    public void receive(PlayerEntity e) {
        VillageManager villages = VillageManager.get((ServerWorld)e.world);
        switch (action) {
            case ADD, ADD_ROOM -> {
                Building.validationResult result = villages.processBuilding(e.getBlockPos(), true, action == Action.ADD_ROOM);
                e.sendMessage(new TranslatableText("blueprint.scan." + result.name().toLowerCase(Locale.ENGLISH)), true);

                // also add tombstones
                GraveyardManager.get((ServerWorld)e.world).reportToVillageManager(e);
            }
            case AUTO_SCAN -> villages.findNearestVillage(e).ifPresent(Village::toggleAutoScan);
            case FULL_SCAN -> {
                villages.findNearestVillage(e).ifPresent(buildings ->
                        buildings.getBuildings().values().stream().toList().forEach(b ->
                                villages.processBuilding(b.getCenter(), true, false)
                        )
                );
            }
            case FORCE_TYPE, REMOVE -> {
                Optional<Village> village = villages.findNearestVillage(e);
                Optional<Building> building = village.flatMap(v -> v.getBuildings().values().stream().filter((b) ->
                        b.containsPos(e.getBlockPos())).findAny());
                if (building.isPresent()) {
                    if (action == Action.FORCE_TYPE) {
                        if (building.get().getType().equals(data)) {
                            building.get().determineType();
                        } else {
                            building.get().setForcedType(data);
                        }
                    } else {
                        village.get().removeBuilding(building.get().getId());
                        village.get().markDirty((ServerWorld)e.world);
                    }
                } else {
                    e.sendMessage(new TranslatableText("blueprint.noBuilding"), true);
                }
            }
        }
    }

    public enum Action {
        AUTO_SCAN,
        ADD_ROOM,
        ADD,
        REMOVE,
        FORCE_TYPE,
        FULL_SCAN
    }
}

package net.mca.entity.ai;

import net.mca.entity.VillagerEntityMCA;
import net.mca.server.world.data.GraveyardManager;
import net.mca.server.world.data.Village;
import net.mca.server.world.data.VillageManager;
import net.mca.util.network.datasync.CDataManager;
import net.mca.util.network.datasync.CDataParameter;
import net.mca.util.network.datasync.CParameter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Villagers need a place to live too.
 */
public class Residency {
    private static final CDataParameter<Integer> VILLAGE = CParameter.create("buildings", -1);

    public static <E extends Entity> CDataManager.Builder<E> createTrackedData(CDataManager.Builder<E> builder) {
        return builder.addAll(VILLAGE);
    }

    private final VillagerEntityMCA entity;

    public Residency(VillagerEntityMCA entity) {
        this.entity = entity;
    }

    public BlockPos getWorkplace() {
        return entity.getBrain()
                .getOptionalMemory(MemoryModuleType.JOB_SITE)
                .map(GlobalPos::getPos)
                .orElse(BlockPos.ORIGIN);
    }

    public void setWorkplace(PlayerEntity player) {
        entity.sendChatMessage(player, "interaction.setworkplace.success");
        entity.getBrain().forget(MemoryModuleType.JOB_SITE);
        entity.getBrain().forget(MemoryModuleType.POTENTIAL_JOB_SITE);
        //todo: Implement proper logic for this in 7.4.0
    }

    public Optional<Village> getHomeVillage() {
        VillageManager manager = VillageManager.get((ServerWorld)entity.world);
        return manager.getOrEmpty(entity.getTrackedValue(VILLAGE));
    }

    public void leaveHome() {
        Optional<Village> village = getHomeVillage();
        village.ifPresent(v -> {
            v.removeResident(entity);
            v.cleanReputation();
            v.markDirty((ServerWorld)entity.world);
        });
    }

    public void tick() {
        if (!entity.requiresHome()) {
            return;
        }

        if (entity.age % 600 == 0 && entity.doesProfessionRequireHome()) {
            if (getHomeVillage().filter(v -> !v.isAutoScan()).isEmpty()) {
                reportBuildings();
            }
        }

        //slowly inject village boni
        if (entity.age % 1200 == 0) {
            getHomeVillage().ifPresentOrElse(village -> {
                //fetch mood from the village storage
                int mood = village.popMood((ServerWorld)entity.world);
                if (mood != 0) {
                    entity.getVillagerBrain().modifyMoodValue(mood);
                }

                //fetch hearts
                entity.world.getPlayers().forEach(player -> {
                    int rep = village.popHearts(player);
                    if (rep != 0) {
                        entity.getVillagerBrain().getMemoriesForPlayer(player).modHearts(rep);
                    }
                });

                //update the reputation
                entity.world.getPlayers().forEach(player -> {
                    //currently, only hearts are considered, maybe additional factors can affect that too
                    int hearts = entity.getVillagerBrain().getMemoriesForPlayer(player).getHearts();
                    village.setReputation(player, entity, hearts);
                });
            }, this::setHomeLess);
        }
    }

    private void setHomeLess() {
        Optional<Village> village = getHomeVillage();
        village.ifPresent(buildings -> buildings.removeResident(this.entity));
        entity.getMCABrain().forget(MemoryModuleType.HOME);
    }

    //report potential buildings within this villagers reach
    private void reportBuildings() {
        VillageManager manager = VillageManager.get((ServerWorld)entity.world);

        //fetch all near POIs
        Stream<BlockPos> stream = ((ServerWorld)entity.world).getPointOfInterestStorage().getPositions(
                PointOfInterestType.ALWAYS_TRUE,
                (p) -> !manager.cache.contains(p),
                entity.getBlockPos(),
                48,
                PointOfInterestStorage.OccupationStatus.ANY);

        //check if it is a building
        stream.forEach(manager::reportBuilding);

        // also add tombstones
        GraveyardManager.get((ServerWorld)entity.world).reportToVillageManager(entity);
    }

    public void setHome(PlayerEntity player) {
        // also trigger a building refresh, because why not
        VillageManager manager = VillageManager.get((ServerWorld)player.world);
        manager.processBuilding(player.getBlockPos(), true, false);

        //check if a bed can be found
        //todo
        /*
        manager.findNearestVillage(player).ifPresentOrElse(village -> {
            village.getBuildingAt(player.getBlockPos()).ifPresentOrElse(building -> {
                if (!entity.doesProfessionRequireHome() || entity.getDespawnDelay() > 0) {
                    entity.sendChatMessage(player, "interaction.sethome.temporary");
                } else if (building.hasFreeSpace()) {
                    entity.sendChatMessage(player, "interaction.sethome.success");

                    //remove from old home
                    setHomeLess();

                    //add to residents
                    setBuilding(building, player.getBlockPos());
                    village.addResident(entity, building.getId());
                } else if (building.getBuildingType().noBeds()) {
                    entity.sendChatMessage(player, "interaction.sethome.bedfail." + building.getBuildingType().name());
                } else if (building.getBedCount() == 0) {
                    entity.sendChatMessage(player, "interaction.sethome.nobeds");
                } else {
                    entity.sendChatMessage(player, "interaction.sethome.bedfail");
                }
            }, () -> entity.sendChatMessage(player, "interaction.sethome.fail"));
        }, () -> {
            entity.sendChatMessage(player, "interaction.sethome.fail");
        });
        */
    }

    public Optional<GlobalPos> getHome() {
        return entity.getMCABrain().getOptionalMemory(MemoryModuleType.HOME);
    }

    public void goHome(PlayerEntity player) {
        getHome().filter(p -> p.getDimension() == entity.world.getRegistryKey()).ifPresentOrElse(home -> {
            entity.moveTowards(home.getPos());
            entity.sendChatMessage(player, "interaction.gohome.success");
        }, () -> entity.sendChatMessage(player, "interaction.gohome.fail.nohome"));
    }
}

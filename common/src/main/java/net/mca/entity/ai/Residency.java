package net.mca.entity.ai;

import net.mca.entity.VillagerEntityMCA;
import net.mca.server.world.data.Building;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Villagers need a place to live too.
 */
public class Residency {
    private static final CDataParameter<Integer> BUILDING = CParameter.create("buildings", -1);
    private static final CDataParameter<BlockPos> HANGOUT = CParameter.create("hangoutPos", BlockPos.ORIGIN);

    public static <E extends Entity> CDataManager.Builder<E> createTrackedData(CDataManager.Builder<E> builder) {
        return builder.addAll(BUILDING, HANGOUT);
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

    public BlockPos getHangout() {
        return entity.getTrackedValue(HANGOUT);
    }

    public void setHangout(PlayerEntity player) {
        entity.sendChatMessage(player, "interaction.sethangout.success");
        entity.setTrackedValue(HANGOUT, player.getBlockPos());
    }

    public void setBuildingId(int id) {
        entity.setTrackedValue(BUILDING, id);
    }

    public Optional<Village> getHomeVillage() {
        VillageManager manager = VillageManager.get((ServerWorld)entity.world);
        return manager.getOrEmpty(manager.mapBuildingToVillage(entity.getTrackedValue(BUILDING)));
    }

    public Optional<Building> getHomeBuilding() {
        Optional<Building> building = getHomeVillage().flatMap(v -> v.getBuilding(entity.getTrackedValue(BUILDING)));
        if (building.isEmpty()) {
            setHomeLess();
        }
        return building;
    }

    public void leaveHome() {
        VillageManager manager = VillageManager.get((ServerWorld)entity.world);
        Optional<Village> village = getHomeVillage();
        village.ifPresent(v -> {
            Optional<Building> building = v.getBuilding(entity.getTrackedValue(BUILDING));
            if (building.isPresent()) {
                building.get().getResidents().remove(entity.getUuid());
                manager.markDirty();
            }
            v.cleanReputation();
            v.markDirty((ServerWorld)entity.world);
        });
    }

    public Optional<GlobalPos> getHome() {
        return getHomeBuilding().map(building -> GlobalPos.create(entity.world.getRegistryKey(), building.getCenter()));
    }

    public void tick() {
        if (!entity.requiresHome()) {
            return;
        }

        if (entity.age % 600 == 0 && entity.doesProfessionRequireHome()) {
            if (getHomeVillage().filter(v -> !v.isAutoScan()).isEmpty()) {
                reportBuildings();
            }

            //poor villager has no home
            if (entity.getTrackedValue(BUILDING) == -1) {
                Village.findNearest(entity).ifPresent(this::seekNewHome);
            }
        }

        //check if his village and building still exists
        if (entity.age % 1200 == 0) {
            getHomeVillage().ifPresentOrElse(village -> {
                Optional<Building> building = village.getBuilding(entity.getTrackedValue(BUILDING));
                if (building.filter(b -> b.hasResident(entity.getUuid()) && !b.isCrowded()).isEmpty()) {
                    if (building.isPresent()) {
                        setHomeLess();
                    }
                } else {
                    //has a home location outside the building?
                    Optional<GlobalPos> globalPos = entity.getMCABrain().getOptionalMemory(MemoryModuleType.HOME);
                    if (globalPos.isPresent() && !building.get().containsPos(globalPos.get().getPos())) {
                        setHomeLess();
                        return;
                    }

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
                }
            }, this::setHomeLess);
        }
    }

    private void setHomeLess() {
        Optional<Village> village = getHomeVillage();
        village.ifPresent(buildings -> buildings.removeResident(this.entity));
        setBuildingId(-1);
        entity.getMCABrain().forget(MemoryModuleType.HOME);
    }

    private void setBuilding(Building b) {
        List<BlockPos> group = b.getBlocksOfGroup(new Identifier("minecraft:beds"));
        if (group.size() > 0) {
            setBuilding(b, group.get(b.getResidents().size() % group.size()));
        } else {
            setBuilding(b, b.getCenter());
        }
    }

    private void setBuilding(Building b, BlockPos p) {
        setBuildingId(b.getId());
        entity.getMCABrain().remember(MemoryModuleType.HOME, GlobalPos.create(entity.world.getRegistryKey(), p));
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

    private boolean seekNewHome(Village village) {
        //choose the first building available, shuffled
        List<Building> buildings = village.getBuildings().values().stream()
                .filter(Building::hasFreeSpace).toList();

        if (!buildings.isEmpty()) {
            Building b = buildings.get(entity.getRandom().nextInt(buildings.size()));

            //add to residents
            setBuilding(b);
            village.addResident(entity, b.getId());

            return true;
        }

        return false;
    }

    public void setHome(PlayerEntity player) {
        // make sure the building is up-to-date
        VillageManager manager = VillageManager.get((ServerWorld)player.world);
        manager.processBuilding(player.getBlockPos(), true, false);

        //check if a bed can be found
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
    }

    public void goHome(PlayerEntity player) {
        getHome().filter(p -> p.getDimension() == entity.world.getRegistryKey()).ifPresentOrElse(home -> {
            entity.moveTowards(home.getPos());
            entity.sendChatMessage(player, "interaction.gohome.success");
        }, () -> entity.sendChatMessage(player, "interaction.gohome.fail.nohome"));
    }
}

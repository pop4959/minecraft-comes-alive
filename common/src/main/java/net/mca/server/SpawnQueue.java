package net.mca.server;

import net.mca.Config;
import net.mca.ducks.IVillagerEntity;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.VillagerFactory;
import net.mca.entity.ZombieVillagerEntityMCA;
import net.mca.entity.ZombieVillagerFactory;
import net.mca.entity.ai.relationship.Gender;
import net.mca.server.world.data.Nationality;
import net.mca.util.WorldUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;

import java.util.LinkedList;
import java.util.Locale;

public class SpawnQueue {
    private static final SpawnQueue INSTANCE = new SpawnQueue();

    public static SpawnQueue getInstance() {
        return INSTANCE;
    }

    private final LinkedList<VillagerEntity> villagerSpawnQueue = new LinkedList<>();
    private final LinkedList<ZombieVillagerEntity> zombieVillagerSpawnQueue = new LinkedList<>();

    public void tick() {
        // lazy spawning of our villagers as they can't be spawned while loading
        if (!villagerSpawnQueue.isEmpty()) {
            VillagerEntity e = villagerSpawnQueue.remove();

            if (WorldUtils.isChunkLoaded(e.world, e.getBlockPos())) {
                e.discard();
                VillagerEntityMCA villager = VillagerFactory.newVillager(e.world)
                        .withName(e.hasCustomName() ? e.getName().getString() : null)
                        .withGender(Gender.getRandom())
                        .withAge(e.getBreedingAge())
                        .withPosition(e)
                        .withType(e.getVillagerData().getType())
                        .withProfession(e.getVillagerData().getProfession(), e.getVillagerData().getLevel(), e.getOffers())
                        .spawn(((IVillagerEntity)e).getSpawnReason());

                copyPastaIntensifies(villager, e);
            } else {
                villagerSpawnQueue.add(e);
            }
        }

        if (!zombieVillagerSpawnQueue.isEmpty()) {
            ZombieVillagerEntity e = zombieVillagerSpawnQueue.poll();

            if (WorldUtils.isChunkLoaded(e.world, e.getBlockPos())) {
                e.discard();
                ZombieVillagerEntityMCA villager = ZombieVillagerFactory.newVillager(e.world)
                        .withName(e.hasCustomName() ? e.getName().getString() : null)
                        .withGender(Gender.getRandom())
                        .withPosition(e)
                        .withType(e.getVillagerData().getType())
                        .withProfession(e.getVillagerData().getProfession(), e.getVillagerData().getLevel())
                        .spawn(((IVillagerEntity)e).getSpawnReason());

                copyPastaIntensifies(villager, e);
            } else {
                zombieVillagerSpawnQueue.add(e);
            }
        }
    }

    private void copyPastaIntensifies(PathAwareEntity villager, PathAwareEntity entity) {
        if (entity.isPersistent()) {
            villager.setPersistent();
        }
        if (entity.isInvulnerable()) {
            villager.setInvulnerable(true);
        }
        if (entity.isAiDisabled()) {
            villager.setAiDisabled(true);
        }

        for (String tag : entity.getCommandTags()) {
            villager.addCommandTag(tag);
        }
    }

    public static boolean shouldGetConverted(Entity entity) {
        if (Config.getInstance().fractionOfVanillaVillages <= 0) {
            return true;
        } else {
            int i = Nationality.get((ServerWorld)entity.getWorld()).getRegionId(entity.getBlockPos());
            return Math.floorMod(i, 100) >= Config.getInstance().fractionOfVanillaVillages * 100.0;
        }
    }

    public boolean addVillager(Entity entity) {
        if (entity instanceof IVillagerEntity villagerEntity && !handlesSpawnReason(villagerEntity.getSpawnReason())) {
            return false;
        }
        if (Config.getInstance().villagerDimensionBlacklist.contains(entity.getEntityWorld().getRegistryKey().getValue().toString())) {
            return false;
        }
        if (Config.getInstance().overwriteOriginalVillagers
                && (entity.getClass().equals(VillagerEntity.class) ||
                Config.getInstance().moddedVillagerWhitelist.contains(Registries.ENTITY_TYPE.getId(entity.getType()).toString()) && entity instanceof VillagerEntity)
                && shouldGetConverted(entity)
                && !villagerSpawnQueue.contains(entity)) {
            return villagerSpawnQueue.add((VillagerEntity)entity);
        }
        if (Config.getInstance().overwriteOriginalZombieVillagers
                && (entity.getClass().equals(ZombieVillagerEntity.class) ||
                Config.getInstance().moddedZombieVillagerWhitelist.contains(Registries.ENTITY_TYPE.getId(entity.getType()).toString()) && entity instanceof ZombieVillagerEntity)
                && !zombieVillagerSpawnQueue.contains(entity)) {
            return zombieVillagerSpawnQueue.add((ZombieVillagerEntity)entity);
        }
        return false;
    }

    private boolean handlesSpawnReason(SpawnReason reason) {
        return Config.getInstance().allowedSpawnReasons.contains(reason.name().toLowerCase(Locale.ROOT));
    }

    public void convert(VillagerEntity villager) {
        villagerSpawnQueue.add(villager);
    }
}

package net.mca.entity.ai.brain.tasks;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.FindPointOfInterestTask;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.Optional;
import java.util.function.Predicate;

public class LazyFindPointOfInterestTask extends FindPointOfInterestTask {
    // This task is performance intensive, let's slow it down a bit
    private static final int SLOWDOWN = 10;
    private int cooldown = SLOWDOWN;

    public LazyFindPointOfInterestTask(Predicate<RegistryEntry<PointOfInterestType>> poiTypePredicate, MemoryModuleType<GlobalPos> moduleType, MemoryModuleType<GlobalPos> targetMemoryModuleType, boolean onlyRunIfChild, Optional<Byte> entityStatus) {
        super(poiTypePredicate, moduleType, targetMemoryModuleType, onlyRunIfChild, entityStatus);
    }

    @Override
    protected boolean shouldRun(ServerWorld serverWorld, PathAwareEntity pathAwareEntity) {
        if (cooldown < 0) {
            cooldown = SLOWDOWN;
            return super.shouldRun(serverWorld, pathAwareEntity);
        } else {
            cooldown--;
            return false;
        }
    }
}

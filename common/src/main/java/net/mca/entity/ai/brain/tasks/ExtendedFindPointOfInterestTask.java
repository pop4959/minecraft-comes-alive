package net.mca.entity.ai.brain.tasks;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.FindPointOfInterestTask;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.Optional;
import java.util.function.Consumer;

public class ExtendedFindPointOfInterestTask extends FindPointOfInterestTask {
    private final Consumer<PathAwareEntity> consumer;

    public ExtendedFindPointOfInterestTask(PointOfInterestType poiType, MemoryModuleType<GlobalPos> moduleType, boolean onlyRunIfChild, Optional<Byte> entityStatus, Consumer<PathAwareEntity> consumer) {
        super(poiType, moduleType, onlyRunIfChild, entityStatus);
        this.consumer = consumer;
    }

    @Override
    protected void finishRunning(ServerWorld world, PathAwareEntity entity, long time) {
        super.finishRunning(world, entity, time);

        consumer.accept(entity);
    }
}

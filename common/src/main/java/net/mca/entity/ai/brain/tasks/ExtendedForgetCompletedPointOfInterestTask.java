package net.mca.entity.ai.brain.tasks;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.ForgetCompletedPointOfInterestTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.function.Consumer;

public class ExtendedForgetCompletedPointOfInterestTask extends ForgetCompletedPointOfInterestTask {
    private final Consumer<LivingEntity> onFinish;

    public ExtendedForgetCompletedPointOfInterestTask(PointOfInterestType poiType, MemoryModuleType<GlobalPos> memoryModule, Consumer<LivingEntity> onFinish) {
        super(poiType, memoryModule);

        this.onFinish = onFinish;
    }

    @Override
    protected void finishRunning(ServerWorld world, LivingEntity entity, long time) {
        super.finishRunning(world, entity, time);

        onFinish.accept(entity);
    }
}

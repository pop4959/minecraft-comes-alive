package net.mca.entity.ai.brain.tasks;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.ForgetCompletedPointOfInterestTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class ExtendedForgetCompletedPointOfInterestTask extends ForgetCompletedPointOfInterestTask {
    private final Consumer<LivingEntity> onFinish;
    private final MemoryModuleType<GlobalPos> memoryModule;

    public ExtendedForgetCompletedPointOfInterestTask(Predicate<RegistryEntry<PointOfInterestType>> poiType, MemoryModuleType<GlobalPos> memoryModule, Consumer<LivingEntity> onFinish) {
        super(poiType, memoryModule);

        this.memoryModule = memoryModule;

        this.onFinish = onFinish;
    }

    @Override
    protected void finishRunning(ServerWorld world, LivingEntity entity, long time) {
        super.finishRunning(world, entity, time);

        if (entity.getBrain().isMemoryInState(this.memoryModule, MemoryModuleState.VALUE_ABSENT)) {
            onFinish.accept(entity);
        }
    }
}

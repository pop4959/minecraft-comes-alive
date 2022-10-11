package net.mca.entity.ai.brain.tasks;

import com.google.common.collect.ImmutableMap;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.ActivityMCA;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

public class GrieveTask extends Task<VillagerEntityMCA> {
    public GrieveTask() {
        super(ImmutableMap.of());
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld serverWorld, VillagerEntityMCA villagerEntity, long l) {
        return villagerEntity.getVillagerBrain().shouldGrieve();
    }

    @Override
    protected void run(ServerWorld serverWorld, VillagerEntityMCA villager, long l) {
        Brain<VillagerEntity> brain = villager.getBrain();
        if (!brain.hasActivity(ActivityMCA.GRIEVE.get())) {
            brain.forget(MemoryModuleType.PATH);
            brain.forget(MemoryModuleType.WALK_TARGET);
            brain.forget(MemoryModuleType.LOOK_TARGET);
            brain.forget(MemoryModuleType.BREED_TARGET);
            brain.forget(MemoryModuleType.INTERACTION_TARGET);
        }
        villager.getMCABrain().doExclusively(ActivityMCA.GRIEVE.get());
    }
}
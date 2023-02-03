package net.mca.entity.ai.brain.tasks;

import com.google.common.collect.ImmutableMap;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.MemoryModuleTypeMCA;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ExtendedWalkTowardsTask extends Task<VillagerEntityMCA> {
    private final MemoryModuleType<GlobalPos> destination;
    private final float speed;
    private final int completionRange;
    private final int maxRange;
    private final int maxRunTime;

    private static final long GAVE_UP_COOLDOWN = 20 * 60;

    private final Predicate<VillagerEntityMCA> canGiveUp;
    private final Consumer<VillagerEntityMCA> onGiveUp;

    public ExtendedWalkTowardsTask(MemoryModuleType<GlobalPos> destination, float speed, int completionRange, int maxRange, int maxRunTime, Predicate<VillagerEntityMCA> canGiveUp, Consumer<VillagerEntityMCA> onGiveUp) {
        super(ImmutableMap.of(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleState.REGISTERED, MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT, destination, MemoryModuleState.VALUE_PRESENT));
        this.destination = destination;
        this.speed = speed;
        this.completionRange = completionRange;
        this.maxRange = maxRange;
        this.maxRunTime = maxRunTime;
        this.canGiveUp = canGiveUp;
        this.onGiveUp = onGiveUp;
    }

    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntityMCA villager) {
        boolean b = stillPissed(world, villager);
        return !b;
    }

    private void giveUp(VillagerEntityMCA villager, long time) {
        Brain<?> brain = villager.getBrain();
        if (canGiveUp.test(villager)) {
            villager.releaseTicketFor(this.destination);
            brain.forget(this.destination);
            brain.remember(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, time);
            onGiveUp.accept(villager);
        } else {
            // Not allowed to give up, instead avoid this task for a while
            brain.remember(MemoryModuleTypeMCA.LAST_CANT_FIND_HOME_PISSED_MOMENT.get(), time);
            brain.remember(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, time);
        }
    }

    protected void run(ServerWorld serverWorld, VillagerEntityMCA villager, long time) {
        Brain<?> brain = villager.getBrain();
        brain.getOptionalMemory(this.destination).ifPresent((pos) -> {
            if (!this.dimensionMismatches(serverWorld, pos) && !this.shouldGiveUp(serverWorld, villager)) {
                if (this.exceedsMaxRange(villager, pos)) {
                    Vec3d vec3d = null;
                    int attempt = 0;

                    while (attempt < 1000 && (vec3d == null || this.exceedsMaxRange(villager, GlobalPos.create(serverWorld.getRegistryKey(), new BlockPos(vec3d))))) {
                        vec3d = NoPenaltyTargeting.findTo(villager, 15, 7, Vec3d.ofBottomCenter(pos.getPos()), 1.5707963705062866);
                        attempt++;
                    }

                    if (attempt == 1000) {
                        this.giveUp(villager, time);
                        return;
                    }

                    assert vec3d != null;
                    brain.remember(MemoryModuleType.WALK_TARGET, new WalkTarget(vec3d, this.speed, this.completionRange));
                } else if (!this.reachedDestination(serverWorld, villager, pos)) {
                    brain.remember(MemoryModuleType.WALK_TARGET, new WalkTarget(pos.getPos(), this.speed, this.completionRange));
                }
            } else {
                this.giveUp(villager, time);
            }

        });
    }

    private boolean shouldGiveUp(ServerWorld world, VillagerEntityMCA villager) {
        Optional<Long> optional = villager.getBrain().getOptionalMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        return optional.filter(aLong -> world.getTime() - aLong > (long)this.maxRunTime).isPresent();
    }

    private boolean stillPissed(ServerWorld world, VillagerEntityMCA villager) {
        Optional<Long> optional = villager.getBrain().getOptionalMemory(MemoryModuleTypeMCA.LAST_CANT_FIND_HOME_PISSED_MOMENT.get());
        return optional.filter(aLong -> world.getTime() - aLong < GAVE_UP_COOLDOWN).isPresent();
    }

    private boolean exceedsMaxRange(VillagerEntityMCA villager, GlobalPos pos) {
        return pos.getPos().getManhattanDistance(villager.getBlockPos()) > this.maxRange;
    }

    private boolean dimensionMismatches(ServerWorld world, GlobalPos pos) {
        return pos.getDimension() != world.getRegistryKey();
    }

    private boolean reachedDestination(ServerWorld world, VillagerEntityMCA villager, GlobalPos pos) {
        return pos.getDimension() == world.getRegistryKey() && pos.getPos().getManhattanDistance(villager.getBlockPos()) <= this.completionRange;
    }
}
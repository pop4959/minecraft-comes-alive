package mca.entity.ai.brain.tasks;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import mca.entity.VillagerEntityMCA;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.brain.task.OpenDoorsTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.util.math.BlockPos;

public class ExtendedSleepTask extends Task<VillagerEntityMCA> {
    private long startTime;
    private long cooldown;
    private float speed;
    private BlockPos bed;

    public ExtendedSleepTask(float speed) {
        super(ImmutableMap.of(
                MemoryModuleType.HOME, MemoryModuleState.VALUE_PRESENT,
                MemoryModuleType.LAST_WOKEN, MemoryModuleState.REGISTERED,
                MemoryModuleType.WALK_TARGET, MemoryModuleState.REGISTERED));
        this.speed = speed;
    }

    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntityMCA entity) {
        if (entity.hasVehicle() && world.getTime() - cooldown > 40L) {
            return false;
        }

        cooldown = world.getTime();

        Brain<?> brain = entity.getBrain();
        GlobalPos globalPos = brain.getOptionalMemory(MemoryModuleType.HOME).get();
        if (world.getRegistryKey() != globalPos.getDimension()) {
            return false;
        }

        // wait a few seconds after wakeup
        Optional<Long> optional = brain.getOptionalMemory(MemoryModuleType.LAST_WOKEN);
        if (optional.isPresent()) {
            long l = world.getTime() - optional.get();
            if (l > 0 && l < 100) {
                return false;
            }
        }

        // look for nearest bed
        return entity.getResidency().getHomeBuilding()
                .flatMap(building -> building.findClosestEmptyBed(world, globalPos.getPos()))
                .map(bed -> {
                    this.bed = bed;
                    if (globalPos.getPos().isWithinDistance(entity.getPos(), 2)) {
                        return true;
                    }

                    brain.remember(MemoryModuleType.WALK_TARGET, new WalkTarget(globalPos.getPos(), speed, 1));
                    return false;
                }).orElse(false);
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntityMCA entity, long time) {
        if (bed != null) {
            boolean distance = bed.isWithinDistance(entity.getPos(), 1.14D);
            return entity.getBrain().hasActivity(Activity.REST) && distance;
        }

        return false;
    }

    @Override
    protected void run(ServerWorld world, VillagerEntityMCA entity, long time) {
        if (time > startTime) {
            OpenDoorsTask.pathToDoor(world, entity, null, null);
            entity.sleep(bed);
        }
    }

    @Override
    protected boolean isTimeLimitExceeded(long time) {
        return false;
    }

    @Override
    protected void finishRunning(ServerWorld world, VillagerEntityMCA entity, long time) {
        if (entity.isSleeping()) {
            entity.wakeUp();
            startTime = time + 40L;
        }
    }
}

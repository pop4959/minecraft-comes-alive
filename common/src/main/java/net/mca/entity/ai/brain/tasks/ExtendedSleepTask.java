package net.mca.entity.ai.brain.tasks;

import com.google.common.collect.ImmutableMap;
import net.mca.entity.VillagerEntityMCA;
import net.mca.server.world.data.Building;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.ai.brain.task.OpenDoorsTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ExtendedSleepTask extends MultiTickTask<VillagerEntityMCA> {
    private long startTime;
    private final float speed;
    private BlockPos bed;
    private long lastFail = 0;
    private static final long TICKS_BETWEEN_FAILS = 200;

    public ExtendedSleepTask(float speed) {
        super(ImmutableMap.of(
                MemoryModuleType.HOME, MemoryModuleState.VALUE_PRESENT,
                MemoryModuleType.LAST_WOKEN, MemoryModuleState.REGISTERED,
                MemoryModuleType.WALK_TARGET, MemoryModuleState.REGISTERED));
        this.speed = speed;
    }

    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntityMCA entity) {
        if (world.getTime() - lastFail < TICKS_BETWEEN_FAILS) {
            return false;
        }

        boolean b = shouldRunInner(world, entity);
        if (!b) {
            if (entity.isSleeping()) {
                entity.wakeUp();
            }
        }
        return b;
    }

    private boolean shouldRunInner(ServerWorld world, VillagerEntityMCA entity) {
        if (entity.hasVehicle()) {
            return false;
        }

        Brain<?> brain = entity.getBrain();
        GlobalPos globalPos = brain.getOptionalMemory(MemoryModuleType.HOME).get();
        if (world.getRegistryKey() != globalPos.getDimension()) {
            return false;
        } else {
            // wait a few seconds after wakeup
            Optional<Long> optional = brain.getOptionalMemory(MemoryModuleType.LAST_WOKEN);
            if (optional.isPresent()) {
                long l = world.getTime() - optional.get();
                if (l > 0L && l < 100L) {
                    return false;
                }
            }

            // look for nearest bed
            Optional<Building> building = entity.getResidency().getHomeBuilding();
            if (building.isPresent()) {
                Optional<BlockPos> bed = building.get().findClosestEmptyBed(world, globalPos.getPos());
                if (bed.isPresent()) {
                    this.bed = bed.get();
                    if (globalPos.getPos().isWithinDistance(entity.getPos(), 2.0D)) {
                        return true;
                    } else {
                        brain.remember(MemoryModuleType.WALK_TARGET, new WalkTarget(globalPos.getPos(), speed, 1));
                        return false;
                    }
                } else {
                    lastFail = world.getTime();
                }
            } else {
                lastFail = world.getTime();
            }
        }

        return false;
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntityMCA entity, long time) {
        if (bed != null) {
            return entity.getBrain().hasActivity(Activity.REST) && entity.getY() > (double)bed.getY() + 0.4D && bed.isWithinDistance(entity.getPos(), 1.14D);
        } else {
            return false;
        }
    }

    @Override
    protected void run(ServerWorld world, VillagerEntityMCA entity, long time) {
        if (time > startTime) {
            Brain<?> brain = entity.getBrain();
            if (brain.hasMemoryModule(MemoryModuleType.DOORS_TO_CLOSE)) {
                Set<GlobalPos> set = brain.getOptionalRegisteredMemory(MemoryModuleType.DOORS_TO_CLOSE).get();
                Optional<List<LivingEntity>> optional;
                if (brain.hasMemoryModule(MemoryModuleType.MOBS)) {
                    optional = brain.getOptionalRegisteredMemory(MemoryModuleType.MOBS);
                } else {
                    optional = Optional.empty();
                }

                OpenDoorsTask.pathToDoor(world, entity, null, null, set, optional);
            }
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

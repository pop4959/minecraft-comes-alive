package mca.entity.ai.brain.tasks;

import com.google.gson.JsonSyntaxException;
import mca.Config;
import mca.entity.ai.MemoryModuleTypeMCA;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.brain.task.WanderAroundTask;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.ServerTagManagerHolder;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

public class WanderOrTeleportToTargetTask extends WanderAroundTask {

    private static final double TELEPORT_LIMIT_SQ = Config.getInstance().villagerTeleportLimit;

    public WanderOrTeleportToTargetTask() {
    }

    public WanderOrTeleportToTargetTask(int minRunTime, int maxRunTime) {
        super(minRunTime, maxRunTime);
    }

    @Override
    protected boolean shouldRun(ServerWorld serverWorld, MobEntity mobEntity) {
        return super.shouldRun(serverWorld, mobEntity)
                && mobEntity.getBrain().isMemoryInState(MemoryModuleTypeMCA.STAYING, MemoryModuleState.VALUE_ABSENT);
    }

    @Override
    protected void keepRunning(ServerWorld world, MobEntity entity, long l) {
        if (Config.getInstance().allowVillagerTeleporting) {
            Brain<?> brain = entity.getBrain();
            WalkTarget walkTarget = brain.getOptionalMemory(MemoryModuleType.WALK_TARGET).get();

            BlockPos targetPos = walkTarget.getLookTarget().getBlockPos();

            // If the target is more than x blocks away, teleport to it immediately.
            if (targetPos.getSquaredDistance(entity.getBlockPos()) >= TELEPORT_LIMIT_SQ) {
                tryTeleport(world, entity, targetPos);
            }
        }

        super.keepRunning(world, entity, l);
    }

    private void tryTeleport(ServerWorld world, MobEntity entity, BlockPos targetPos) {
        for(int i = 0; i < 10; ++i) {
            int j = this.getRandomInt(entity, -3, 3);
            int k = this.getRandomInt(entity, -1, 1);
            int l = this.getRandomInt(entity, -3, 3);
            boolean bl = this.tryTeleportTo(world, entity, targetPos, targetPos.getX() + j, targetPos.getY() + k, targetPos.getZ() + l);
            if (bl) {
                return;
            }
        }
    }

    private boolean tryTeleportTo(ServerWorld world, MobEntity entity, BlockPos targetPos, int x, int y, int z) {
        if (Math.abs((double)x - targetPos.getX()) < 2.0D && Math.abs((double)z - targetPos.getZ()) < 2.0D) {
            return false;
        } else if (!this.canTeleportTo(world, entity, new BlockPos(x, y, z))) {
            return false;
        } else {
            entity.requestTeleport((double)x + 0.5D, (double)y, (double)z + 0.5D);
            return true;
        }
    }

    private boolean canTeleportTo(ServerWorld world, MobEntity entity, BlockPos pos) {
        PathNodeType pathNodeType = LandPathNodeMaker.getLandNodeType(world, pos.mutableCopy());
        if (pathNodeType != PathNodeType.WALKABLE) {
            return false;
        } else {
            if (!isAreaSafe(world, pos.down())) {
                return false;
            } else {
                BlockPos blockPos = pos.subtract(entity.getBlockPos());
                return world.isSpaceEmpty(entity, entity.getBoundingBox().offset(blockPos));
            }
        }
    }

    private int getRandomInt(MobEntity entity, int min, int max) {
        return entity.getRandom().nextInt(max - min + 1) + min;
    }

    private boolean isAreaSafe(ServerWorld world, Vec3d pos) {
        return isAreaSafe(world, new BlockPos(pos));
    }

    private boolean isAreaSafe(ServerWorld world, BlockPos pos) {
        // The following conditions define whether it is logically
        // safe for the entity to teleport to the specified pos within world
        final BlockState aboveState = world.getBlockState(pos);
        final Identifier aboveId = Registry.BLOCK.getId(aboveState.getBlock());
        for (String blockId : Config.getInstance().villagerPathfindingBlacklist) {
            if (blockId.equals(aboveId.toString())) {
                return false;
            } else if (blockId.charAt(0) == '#') {
                Identifier identifier = new Identifier(blockId.substring(1));
                Tag<Block> tag = ServerTagManagerHolder.getTagManager().getOrCreateTagGroup(Registry.BLOCK_KEY).getTag(identifier);
                if (tag != null) {
                    if (aboveState.isIn(tag)) {
                        return false;
                    }
                } else {
                    throw new JsonSyntaxException("Unknown block tag in villagerPathfindingBlacklist '" + identifier + "'");
                }
            }
        }
        return true;
    }
}

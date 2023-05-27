package net.mca.entity.ai.brain.tasks;

import com.google.common.collect.ImmutableMap;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.ConversationManager;
import net.mca.entity.ai.MemoryModuleTypeMCA;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class DeliverMessageTask extends MultiTickTask<VillagerEntityMCA> {
    private static final int MAX_COOLDOWN = 10;
    private static final int TALKING_TIME_MIN = 100;
    private static final int TALKING_TIME_MAX = 500;
    private static final long MIN_TIME_BETWEEN_SOUND = 20 * 60 * 5;

    private Optional<ConversationManager.Message> message = Optional.empty();

    private int cooldown;

    private int talked;

    private long lastInteraction = Long.MIN_VALUE;
    private Vec3d lastInteractionPos;

    public DeliverMessageTask() {
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryModuleState.REGISTERED,
                MemoryModuleType.LOOK_TARGET, MemoryModuleState.REGISTERED,
                MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.REGISTERED
        ), 600);
    }

    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntityMCA villager) {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        cooldown = MAX_COOLDOWN;
        message = getMessage(villager);
        return message.isPresent() && isWithinSeeRange(villager, message.get().getReceiver());
    }

    @Override
    protected void run(ServerWorld world, VillagerEntityMCA villager, long time) {
        message.ifPresent(m -> {
            talked = 0;
        });
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntityMCA villager, long time) {
        return talked < getMaxTalkingTime() && !villager.getVillagerBrain().isPanicking() && !villager.isSleeping();
    }

    private int getMaxTalkingTime() {
        if (message.isPresent() && lastInteractionPos != null) {
            Vec3d pos = message.get().getReceiver().getPos();
            if (lastInteractionPos.isInRange(pos, 1.0)) {
                return TALKING_TIME_MAX;
            }
        }
        return TALKING_TIME_MIN;
    }

    @Override
    protected void keepRunning(ServerWorld world, VillagerEntityMCA villager, long time) {
        message.ifPresent(m -> {
            if (m.getReceiver() instanceof LivingEntity e) {
                villager.getBrain().remember(MemoryModuleType.INTERACTION_TARGET, e);
                LookTargetUtil.lookAt(villager, e);
            }

            if (talked == 0) {
                if (isWithinRange(villager, m.getReceiver())) {
                    if (time - lastInteraction > MIN_TIME_BETWEEN_SOUND) {
                        villager.playWelcomeSound();
                    }
                    lastInteraction = time;
                    lastInteractionPos = m.getReceiver().getPos();
                    m.deliver();
                    talked = 1;
                } else {
                    LookTargetUtil.walkTowards(villager, m.getReceiver(), 0.65F, 2);
                }
            } else {
                LookTargetUtil.walkTowards(villager, m.getReceiver(), 0.45F, 2);
                talked++;
            }
        });
    }

    @Override
    protected void finishRunning(ServerWorld world, VillagerEntityMCA villager, long time) {
        message = Optional.empty();
        villager.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
        villager.getBrain().forget(MemoryModuleType.WALK_TARGET);
        villager.getBrain().forget(MemoryModuleType.LOOK_TARGET);
    }

    private static Optional<ConversationManager.Message> getMessage(VillagerEntityMCA villager) {
        return villager.conversationManager.getCurrentMessage();
    }

    private static boolean isWithinRange(VillagerEntityMCA villager, Entity player) {
        // Staying villagers can't reach you
        if (villager.getBrain().getOptionalMemory(MemoryModuleTypeMCA.STAYING.get()).isPresent()) {
            return true;
        }
        return villager.getBlockPos().isWithinDistance(player.getBlockPos(), 3);
    }

    private static boolean isWithinSeeRange(VillagerEntityMCA villager, Entity player) {
        return villager.getBlockPos().isWithinDistance(player.getBlockPos(), 64);
    }
}

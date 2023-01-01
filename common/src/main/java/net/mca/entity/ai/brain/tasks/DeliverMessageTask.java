package net.mca.entity.ai.brain.tasks;

import com.google.common.collect.ImmutableMap;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.ConversationManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;

public class DeliverMessageTask extends MultiTickTask<VillagerEntityMCA> {
    private static final int MAX_COOLDOWN = 10;
    private static final int TALKING_TIME = 200;

    private Optional<ConversationManager.Message> message = Optional.empty();

    private int cooldown;

    private int talked;

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
        return talked < TALKING_TIME && !villager.getVillagerBrain().isPanicking() && !villager.isSleeping();
    }

    @Override
    protected void keepRunning(ServerWorld world, VillagerEntityMCA villager, long time) {
        message.ifPresent(m -> {
            if (m.getReceiver() instanceof LivingEntity e) {
                villager.getBrain().remember(MemoryModuleType.INTERACTION_TARGET, e);
                LookTargetUtil.lookAt(villager, e);
            }

            if (talked == 0) {
                if (isWithinGreetingDistance(villager, m.getReceiver())) {
                    villager.playWelcomeSound();
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

    private static boolean isWithinGreetingDistance(VillagerEntityMCA villager, Entity player) {
        return villager.getBlockPos().isWithinDistance(player.getBlockPos(), 3);
    }

    private static boolean isWithinSeeRange(VillagerEntityMCA villager, Entity player) {
        return villager.getBlockPos().isWithinDistance(player.getBlockPos(), 64);
    }
}

package mca.entity.ai.goal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;

import java.util.Comparator;

public class GrimReaperTargetGoal extends Goal {
    private final TargetPredicate attackTargeting = TargetPredicate.createAttackable().setBaseMaxDistance(64.0D);

    private final PathAwareEntity mob;

    private int nextScanTick = 20;

    public GrimReaperTargetGoal(PathAwareEntity mob) {
        this.mob = mob;
    }

    @Override
    public boolean canStart() {
        if (nextScanTick > 0) {
            nextScanTick--;
            return false;
        }

        nextScanTick = 20;
        return mob.world.getPlayers(attackTargeting, mob, mob.getBoundingBox().expand(48, 64, 48))
                .stream()
                .sorted(Comparator.comparing(Entity::getY).reversed())
                .filter(player -> mob.isTarget(player, TargetPredicate.DEFAULT))
                .findFirst()
                .map(player -> {
                    mob.setTarget(player);
                    return true;
                }).orElse(false);
    }

    @Override
    public boolean shouldContinue() {
        return mob.getTarget() != null;
    }
}
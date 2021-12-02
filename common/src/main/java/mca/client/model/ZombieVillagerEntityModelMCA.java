package mca.client.model;

import mca.entity.VillagerLike;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.CrossbowPosing;
import net.minecraft.entity.mob.MobEntity;

public class ZombieVillagerEntityModelMCA<T extends MobEntity & VillagerLike<T>> extends VillagerEntityModelMCA<T> {

    public ZombieVillagerEntityModelMCA(ModelPart tree, boolean clothing) {
        super(tree, clothing);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        CrossbowPosing.meleeAttack(leftArm, rightArm, entity.isAttacking(), handSwingProgress, animationProgress);
        leftArmwear.copyTransform(leftArm);
        rightArmwear.copyTransform(rightArm);
    }
}

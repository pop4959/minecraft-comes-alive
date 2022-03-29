package mca.client.model;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.CrossbowPosing;
import net.minecraft.entity.LivingEntity;

public class ZombieVillagerEntityModelMCA<T extends LivingEntity> extends VillagerEntityModelMCA<T> {

    public ZombieVillagerEntityModelMCA(ModelPart tree) {
        super(tree);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        CrossbowPosing.meleeAttack(leftArm, rightArm, false, handSwingProgress, animationProgress);
        leftArmwear.copyTransform(leftArm);
        rightArmwear.copyTransform(rightArm);
    }
}

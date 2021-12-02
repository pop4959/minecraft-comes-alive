package mca.client.model;

import com.google.common.collect.ImmutableList;
import mca.entity.VillagerLike;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.mob.MobEntity;

public class VillagerEntityModelMCA<T extends MobEntity & VillagerLike<T>> extends VillagerEntityBaseModelMCA<T> {
    protected static final String BREASTPLATE = "breastplate";

    public final ModelPart breastsWear;
    public final ModelPart leftArmwear;
    public final ModelPart rightArmwear;
    public final ModelPart leftLegwear;
    public final ModelPart rightLegwear;
    public final ModelPart bodyWear;

    private boolean wearsHidden;

    public VillagerEntityModelMCA(ModelPart tree, boolean clothing) {
        super(tree, clothing);
        bodyWear = tree.getChild(EntityModelPartNames.JACKET);
        leftArmwear = tree.getChild("left_sleeve");
        rightArmwear = tree.getChild("right_sleeve");
        leftLegwear = tree.getChild("left_pants");
        rightLegwear = tree.getChild("right_pants");

        breastsWear = tree.getChild(BREASTPLATE);
    }

    //
    // body - 0 (body.body 0.0)
    // face - 0 (body.head 0.01)
    //  clothing - 1 (clothing.body 0.075)
    //   hair - 2 (hair.body 0.1) + (hair.hat 0.1 + 0.3 = 0.4)
    //    hood - 3 (clothing.hat 0.075 + 0.5 = 0.575)

    public static ModelData hairData(Dilation dilation) {
        ModelData modelData = clothingData(dilation);
        ModelPartData root = modelData.getRoot();
        root.addChild(EntityModelPartNames.HAT, ModelPartBuilder.create().uv(32, 0).cuboid(-4, -8, -4, 8, 8, 8, dilation.add(0.3F)), ModelTransform.NONE);
        return modelData;
    }

    public static ModelData clothingData(Dilation dilation) {
        ModelData modelData = PlayerEntityModel.getTexturedModelData(dilation, false);
        ModelPartData root = modelData.getRoot();
        root.addChild(BREASTS, newBreasts(dilation, true, 0), ModelTransform.NONE);
        root.addChild(BREASTPLATE, newBreasts(dilation.add(0.1F), true, 16), ModelTransform.NONE);
        return modelData;
    }

    public static ModelData bodyData(Dilation dilation) {
        ModelData modelData = PlayerEntityModel.getTexturedModelData(dilation, false);
        ModelPartData root = modelData.getRoot();
        root.addChild(BREASTS, newBreasts(dilation, false, 0), ModelTransform.NONE);
        root.addChild(BREASTPLATE, newBreasts(dilation.add(0.1F), false, 16), ModelTransform.NONE);
        return modelData;
    }

    @Override
    protected Iterable<ModelPart> getBodyParts() {
        return ImmutableList.of(body, rightArm, leftArm, rightLeg, leftLeg, bodyWear, leftLegwear, rightLegwear, leftArmwear, rightArmwear);
    }

    @Override
    protected Iterable<ModelPart> breastsParts() {
        return ImmutableList.of(breasts, breastsWear);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        leftLegwear.copyTransform(leftLeg);
        rightLegwear.copyTransform(rightLeg);
        leftArmwear.copyTransform(leftArm);
        rightArmwear.copyTransform(rightArm);
        bodyWear.copyTransform(body);
        breastsWear.copyTransform(body);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        leftArmwear.visible = !wearsHidden && visible;
        rightArmwear.visible = !wearsHidden && visible;
        leftLegwear.visible = !wearsHidden && visible;
        rightLegwear.visible = !wearsHidden && visible;
        bodyWear.visible = !wearsHidden && visible;
    }

    public VillagerEntityModelMCA<T> hideWears() {
        wearsHidden = true;
        breastsWear.visible = false;
        leftArmwear.visible = false;
        rightArmwear.visible = false;
        leftLegwear.visible = false;
        rightLegwear.visible = false;
        bodyWear.visible = false;
        return this;
    }

    @Override
    public void setAttributes(BipedEntityModel<T> target) {
        super.setAttributes(target);
        if (target instanceof VillagerEntityModelMCA) {
            copyAttributes((VillagerEntityModelMCA<T>)target);
        }
    }

    private void copyAttributes(VillagerEntityModelMCA<T> target) {
        target.leftLegwear.copyTransform(leftLegwear);
        target.rightLegwear.copyTransform(rightLegwear);
        target.leftArmwear.copyTransform(leftArmwear);
        target.rightArmwear.copyTransform(rightArmwear);
        target.bodyWear.copyTransform(bodyWear);
        target.breastsWear.copyTransform(breastsWear);
    }
}

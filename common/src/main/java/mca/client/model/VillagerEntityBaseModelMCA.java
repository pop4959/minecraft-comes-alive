package mca.client.model;

import com.google.common.collect.ImmutableList;
import mca.client.render.PlayerEntityMCARenderer;
import mca.entity.VillagerLike;
import mca.entity.ai.relationship.AgeState;
import mca.entity.ai.relationship.Gender;
import mca.entity.ai.relationship.VillagerDimensions;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

public class VillagerEntityBaseModelMCA<T extends LivingEntity> extends PlayerEntityModel<T> {

    protected static final String BREASTS = "breasts";

    public final ModelPart breasts;

    public float breastSize;
    public VillagerDimensions dimensions = AgeState.ADULT;

    public VillagerEntityBaseModelMCA(ModelPart root) {
        super(root, false);
        this.breasts = root.getChild(BREASTS);
    }

    public static ModelData getModelData(Dilation dilation) {
        ModelData modelData = PlayerEntityModel.getTexturedModelData(dilation, false);
        ModelPartData data = modelData.getRoot();

        data.addChild(BREASTS, newBreasts(dilation, 0), ModelTransform.NONE);

        return modelData;
    }

    protected static ModelPartBuilder newBreasts(Dilation dilation, int oy) {
        ModelPartBuilder builder = ModelPartBuilder.create();
        builder.uv(18, 21 + oy).cuboid(-3.25F, -1.25F, -1.5F, 6, 3, 3, dilation);
        return builder;
    }

    @Override
    protected Iterable<ModelPart> getHeadParts() {
        return ImmutableList.of(head, hat);
    }

    @Override
    protected Iterable<ModelPart> getBodyParts() {
        return ImmutableList.of(body, rightArm, leftArm, rightLeg, leftLeg);
    }

    protected Iterable<ModelPart> breastsParts() {
        return ImmutableList.of(breasts);
    }

    @Override
    public void animateModel(T entity, float limbAngle, float limbDistance, float tickDelta) {
        super.animateModel(entity, limbDistance, limbAngle, tickDelta);
        riding |= getVillager(entity).getAgeState() == AgeState.BABY;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        if (getVillager(entity).getAgeState() == AgeState.BABY && !entity.hasVehicle()) {
            limbDistance = (float)Math.sin(entity.age / 12F);
            limbAngle = (float)Math.cos(entity.age / 9F) * 3;
            headYaw += (float)Math.sin(entity.age / 2F);
        }

        //remove the boost for babies
        if (entity.isBaby()) {
            limbAngle /= 3.0f;
        }

        //and add our own
        limbAngle /= (0.2f + getVillager(entity).getRawScaleFactor());

        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);

        if (getVillager(entity).getVillagerBrain().isPanicking()) {
            float toRadiums = (float)Math.PI / 180;

            float armRaise = (((float)Math.sin(animationProgress / 5) * 30 - 180)
                    + ((float)Math.sin(animationProgress / 3) * 3))
                    * toRadiums;
            float waveSideways = ((float)Math.sin(animationProgress / 2) * 12 - 17) * toRadiums;

            this.leftArm.pitch = armRaise;
            this.leftArm.roll = -waveSideways;
            this.rightArm.pitch = -armRaise;
            this.rightArm.roll = waveSideways;
        }

        applyVillagerDimensions(getVillager(entity), entity.isSneaking());
    }

    @Override
    public void setAttributes(BipedEntityModel<T> target) {
        super.setAttributes(target);

        if (target instanceof VillagerEntityBaseModelMCA<T> m) {
            m.dimensions = dimensions;
            m.breastSize = breastSize;
            m.breasts.visible = breasts.visible;
            m.breasts.copyTransform(breasts);
        }
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        //head
        float headSize = dimensions.getHead();

        matrices.push();
        matrices.scale(headSize, headSize, headSize);
        this.getHeadParts().forEach(a -> a.render(matrices, vertices, light, overlay, red, green, blue, alpha));
        matrices.pop();

        //body
        this.getBodyParts().forEach(a -> a.render(matrices, vertices, light, overlay, red, green, blue, alpha));

        if (breasts.visible && body.visible) {
            float breastSize = this.breastSize * dimensions.getBreasts();

            if (breastSize > 0) {
                matrices.push();
                matrices.scale(breastSize * 0.2f + 1.05f, breastSize * 0.75f + 0.75f, breastSize * 0.75f + 0.75f);
                for (ModelPart part : breastsParts()) {
                    part.render(matrices, vertices, light, overlay, red, green, blue, alpha);
                }
                matrices.pop();
            }
        }
    }

    public static VillagerLike<?> getVillager(Entity villager) {
        if (villager instanceof VillagerLike<?> v) {
            return v;
        } else {
            return PlayerEntityMCARenderer.playerData.get(villager.getUuid());
        }
    }

    public void applyVillagerDimensions(VillagerLike<?> entity, boolean isSneaking) {
        dimensions = entity.getVillagerDimensions();
        breastSize = entity.getGenetics().getBreastSize();
        breasts.visible = entity.getGenetics().getGender() == Gender.FEMALE;

        for (ModelPart part : breastsParts()) {
            part.pitch = (float)Math.PI * 0.3f + body.pitch;
            float cy = 0.0f;
            float cz = 0.0f;
            if (isSneaking) {
                cy = 1.75f;
                cz = 0.75f;
            }

            part.setPivot(0.25f, (float)(5.0f - Math.pow(breastSize, 0.5) * 2.5f + cy), -1.5f + breastSize * 0.25f + cz);
        }
    }
}

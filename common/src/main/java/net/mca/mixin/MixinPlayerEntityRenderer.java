package net.mca.mixin;

import net.mca.Config;
import net.mca.MCAClient;
import net.mca.client.model.CommonVillagerModel;
import net.mca.client.model.PlayerEntityExtendedModel;
import net.mca.client.model.VillagerEntityModelMCA;
import net.mca.client.render.layer.*;
import net.mca.entity.ai.relationship.AgeState;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private PlayerEntityModel<AbstractClientPlayerEntity> villagerModel;
    private PlayerEntityModel<AbstractClientPlayerEntity> originalModel;

    @Shadow protected abstract void setModelPose(AbstractClientPlayerEntity player);

    SkinLayer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> skinLayer;
    ClothingLayer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> clothingLayer;

    public MixinPlayerEntityRenderer(EntityRendererFactory.Context ctx, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/render/entity/EntityRendererFactory$Context;Z)V", at = @At("TAIL"))
    private void init(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        if (Config.getInstance().enableVillagerPlayerModel) {
            villagerModel = createModel(VillagerEntityModelMCA.bodyData(new Dilation(0.0F), slim));
            originalModel = model;

            skinLayer = new SkinLayer<>(this, createModel(VillagerEntityModelMCA.bodyData(new Dilation(0.0F))));
            addFeature(skinLayer);
            addFeature(new FaceLayer<>(this, createModel(VillagerEntityModelMCA.bodyData(new Dilation(0.01F))), "normal"));

            clothingLayer = new ClothingLayer<>(this, createModel(VillagerEntityModelMCA.bodyData(new Dilation(0.0625F))), "normal");
            addFeature(clothingLayer);
            addFeature(new HairLayer<>(this, createModel(VillagerEntityModelMCA.hairData(new Dilation(0.125F)))));
        }
    }

    private static PlayerEntityExtendedModel<AbstractClientPlayerEntity> createModel(ModelData data) {
        return new PlayerEntityExtendedModel<>(TexturedModelData.of(data, 64, 64).createModel());
    }

    @Inject(method = "scale(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("TAIL"), cancellable = true)
    private void injectScale(AbstractClientPlayerEntity villager, MatrixStack matrices, float f, CallbackInfo ci) {
        if (MCAClient.useGeneticsRenderer(villager.getUuid())) {
            float height = CommonVillagerModel.getVillager(villager).getRawScaleFactor();
            float width = CommonVillagerModel.getVillager(villager).getHorizontalScaleFactor();
            matrices.scale(width, height, width);
            if (CommonVillagerModel.getVillager(villager).getAgeState() == AgeState.BABY && !villager.hasVehicle()) {
                matrices.translate(0, 0.6F, 0);
            }
            ci.cancel();
            model = villagerModel;
        } else {
            model = originalModel;
        }
    }

    @Inject(method = "renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;)V", at = @At("HEAD"), cancellable = true)
    public void injectRenderRightArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, CallbackInfo ci) {
        if (MCAClient.useVillagerRenderer(player.getUuid())) {
            renderCustomArm(matrices, vertexConsumers, light, player, skinLayer.model.rightArm, skinLayer.model.rightSleeve, skinLayer);
            renderCustomArm(matrices, vertexConsumers, light, player, clothingLayer.model.rightArm, clothingLayer.model.rightSleeve, clothingLayer);
            ci.cancel();
        }
    }

    @Inject(method = "renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;)V", at = @At("HEAD"), cancellable = true)
    public void injectRenderLeftArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, CallbackInfo ci) {
        if (MCAClient.useVillagerRenderer(player.getUuid())) {
            renderCustomArm(matrices, vertexConsumers, light, player, skinLayer.model.leftArm, skinLayer.model.leftSleeve, skinLayer);
            renderCustomArm(matrices, vertexConsumers, light, player, clothingLayer.model.leftArm, clothingLayer.model.leftSleeve, clothingLayer);
            ci.cancel();
        }
    }

    private void renderCustomArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, VillagerLayer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> layer) {
        PlayerEntityExtendedModel<AbstractClientPlayerEntity> model = (PlayerEntityExtendedModel<AbstractClientPlayerEntity>)layer.model;
        setModelPose(player);

        model.handSwingProgress = 0.0f;
        model.sneaking = false;
        model.leaningPitch = 0.0f;
        model.setAngles(player, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);

        model.setVisible(false);

        arm.visible = true;
        arm.pitch = 0.0f;
        arm.yaw = 0.0f;
        arm.roll = 0.0f;

        sleeve.visible = true;
        sleeve.pitch = 0.0f;
        sleeve.yaw = 0.0f;
        sleeve.roll = 0.0f;

        model.applyVillagerDimensions(CommonVillagerModel.getVillager(player), player.isInSneakingPose());

        layer.renderFinal(matrices, vertexConsumers, light, player, 0.0f);

        model.setVisible(true);
    }
}

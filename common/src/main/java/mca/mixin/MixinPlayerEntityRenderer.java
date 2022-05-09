package mca.mixin;

import mca.Config;
import mca.MCAClient;
import mca.client.model.PlayerEntityExtendedModel;
import mca.client.model.VillagerEntityModelMCA;
import mca.client.render.layer.*;
import mca.entity.ai.relationship.AgeState;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mca.client.model.CommonVillagerModel.getVillager;

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    @Shadow
    private static BipedEntityModel.ArmPose getArmPose(AbstractClientPlayerEntity player, Hand hand) {
        return null;
    }

    SkinLayer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> skinLayer;
    ClothingLayer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> clothingLayer;

    public MixinPlayerEntityRenderer(EntityRendererFactory.Context ctx, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/render/entity/EntityRendererFactory$Context;Z)V", at = @At("TAIL"))
    private void init(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        if (Config.getInstance().letPlayerCustomize) {
            model = createModel(VillagerEntityModelMCA.bodyData(new Dilation(0.0F), slim));

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
        if (MCAClient.useMCARenderer(villager.getUuid())) {
            float height = getVillager(villager).getRawScaleFactor();
            float width = getVillager(villager).getHorizontalScaleFactor();
            matrices.scale(width, height, width);
            if (getVillager(villager).getAgeState() == AgeState.BABY && !villager.hasVehicle()) {
                matrices.translate(0, 0.6F, 0);
            }
            ci.cancel();
        }
    }

    @Inject(method = "renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;)V", at = @At("HEAD"), cancellable = true)
    public void injectRenderRightArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, CallbackInfo ci) {
        if (MCAClient.useMCAModel(player.getUuid())) {
            renderCustomArm(matrices, vertexConsumers, light, player, skinLayer.model.rightArm, skinLayer.model.rightSleeve, skinLayer);
            renderCustomArm(matrices, vertexConsumers, light, player, clothingLayer.model.rightArm, clothingLayer.model.rightSleeve, clothingLayer);
            ci.cancel();
        }
    }

    @Inject(method = "renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;)V", at = @At("HEAD"), cancellable = true)
    public void injectRenderLeftArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, CallbackInfo ci) {
        if (MCAClient.useMCAModel(player.getUuid())) {
            renderCustomArm(matrices, vertexConsumers, light, player, skinLayer.model.leftArm, skinLayer.model.leftSleeve, skinLayer);
            renderCustomArm(matrices, vertexConsumers, light, player, clothingLayer.model.leftArm, clothingLayer.model.leftSleeve, clothingLayer);
            ci.cancel();
        }
    }

    private void renderCustomArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, VillagerLayer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> layer) {
        PlayerEntityExtendedModel<AbstractClientPlayerEntity> model = (PlayerEntityExtendedModel<AbstractClientPlayerEntity>)layer.model;
        setCustomModelPose(model, player);

        model.setVisible(false);

        arm.visible = true;
        arm.pitch = 0.0f;
        arm.yaw = 0.0f;
        arm.roll = 0.0f;

        sleeve.visible = true;
        sleeve.pitch = 0.0f;
        sleeve.yaw = 0.0f;
        sleeve.roll = 0.0f;

        model.applyVillagerDimensions(getVillager(player), player.isInSneakingPose());

        layer.renderFinal(matrices, vertexConsumers, light, player);

        model.setVisible(true);
    }

    private void setCustomModelPose(PlayerEntityModel<AbstractClientPlayerEntity> playerEntityModel, AbstractClientPlayerEntity player) {
        if (player.isSpectator()) {
            playerEntityModel.setVisible(false);
            playerEntityModel.head.visible = true;
            playerEntityModel.hat.visible = true;
        } else {
            playerEntityModel.setVisible(true);
            playerEntityModel.hat.visible = player.isPartVisible(PlayerModelPart.HAT);
            playerEntityModel.jacket.visible = player.isPartVisible(PlayerModelPart.JACKET);
            playerEntityModel.leftPants.visible = player.isPartVisible(PlayerModelPart.LEFT_PANTS_LEG);
            playerEntityModel.rightPants.visible = player.isPartVisible(PlayerModelPart.RIGHT_PANTS_LEG);
            playerEntityModel.leftSleeve.visible = player.isPartVisible(PlayerModelPart.LEFT_SLEEVE);
            playerEntityModel.rightSleeve.visible = player.isPartVisible(PlayerModelPart.RIGHT_SLEEVE);
            playerEntityModel.sneaking = player.isInSneakingPose();
            BipedEntityModel.ArmPose armPose = getArmPose(player, Hand.MAIN_HAND);
            BipedEntityModel.ArmPose armPose2 = getArmPose(player, Hand.OFF_HAND);
            if (player.getMainArm() == Arm.RIGHT) {
                playerEntityModel.rightArmPose = armPose;
                playerEntityModel.leftArmPose = armPose2;
            } else {
                playerEntityModel.rightArmPose = armPose2;
                playerEntityModel.leftArmPose = armPose;
            }
        }
    }
}

package mca.client.render;

import mca.client.model.VillagerEntityBaseModelMCA;
import mca.client.model.VillagerEntityModelMCA;
import mca.client.render.playerLayer.*;
import mca.entity.VillagerLike;
import mca.entity.ai.relationship.AgeState;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static mca.client.model.VillagerEntityBaseModelMCA.getVillager;

public class PlayerEntityMCARenderer extends PlayerEntityRenderer {
    public static EntityRenderer<?> entityRenderer;
    public static Map<UUID, VillagerLike<?>> playerData = new HashMap<>();
    public static Set<UUID> playerDataRequests = new HashSet<>();

    PlayerSkinLayer<AbstractClientPlayerEntity> skinLayer;
    PlayerClothingLayer<AbstractClientPlayerEntity> clothingLayer;

    public PlayerEntityMCARenderer(EntityRendererFactory.Context context) {
        super(context, false);

        skinLayer = new PlayerSkinLayer<>(this, createModel(VillagerEntityModelMCA.bodyData(new Dilation(0.0F))));
        addFeature(skinLayer);
        addFeature(new PlayerFaceLayer<>(this, createModel(VillagerEntityModelMCA.bodyData(new Dilation(0.01F)))));
        clothingLayer = new PlayerClothingLayer<>(this, createModel(VillagerEntityModelMCA.bodyData(new Dilation(0.0625F))));
        addFeature(clothingLayer);
        addFeature(new PlayerHairLayer<>(this, createModel(VillagerEntityModelMCA.hairData(new Dilation(0.125F)))));
    }

    private static VillagerEntityModelMCA<AbstractClientPlayerEntity> createModel(ModelData data) {
        return new VillagerEntityModelMCA<>(TexturedModelData.of(data, 64, 64).createModel());
    }

    @Override
    public boolean shouldRender(AbstractClientPlayerEntity entity, Frustum frustum, double x, double y, double z) {
        if (!entity.shouldRender(x, y, z)) {
            return false;
        }
        if (entity.ignoreCameraFrustum) {
            return true;
        }
        Box box = ((Entity)entity).getVisibilityBoundingBox().expand(0.5);
        if (box.isValid() || box.getAverageSideLength() == 0.0) {
            box = new Box(entity.getX() - 2.0, entity.getY() - 2.0, entity.getZ() - 2.0, entity.getX() + 2.0, entity.getY() + 2.0, entity.getZ() + 2.0);
        }
        return frustum.isVisible(box);
    }

    @Override
    protected void scale(AbstractClientPlayerEntity villager, MatrixStack matrices, float tickDelta) {
        float height = getVillager(villager).getRawScaleFactor();
        float width = getVillager(villager).getHorizontalScaleFactor();
        matrices.scale(width, height, width);
        if (getVillager(villager).getAgeState() == AgeState.BABY && !villager.hasVehicle()) {
            matrices.translate(0, 0.6F, 0);
        }
    }

    @Nullable
    @Override
    protected RenderLayer getRenderLayer(AbstractClientPlayerEntity entity, boolean showBody, boolean translucent, boolean showOutline) {
        return null;
    }

    @Override
    public void render(AbstractClientPlayerEntity entity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        features.forEach(feature -> {
            if (feature instanceof PlayerLayer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> playerFeature) {
                if (playerFeature.model instanceof VillagerEntityBaseModelMCA<AbstractClientPlayerEntity> model) {
                    model.applyVillagerDimensions(getVillager(entity));
                }
            }
        });

        super.render(entity, f, g, matrixStack, vertexConsumerProvider, i);
    }

    @Override
    public void renderRightArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player) {
        renderArm(matrices, vertexConsumers, light, player, skinLayer.model.rightArm, skinLayer.model.rightSleeve, skinLayer);
        renderArm(matrices, vertexConsumers, light, player, clothingLayer.model.rightArm, clothingLayer.model.rightSleeve, clothingLayer);
    }

    @Override
    public void renderLeftArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player) {
        renderArm(matrices, vertexConsumers, light, player, skinLayer.model.leftArm, skinLayer.model.leftSleeve, skinLayer);
        renderArm(matrices, vertexConsumers, light, player, clothingLayer.model.leftArm, clothingLayer.model.leftSleeve, clothingLayer);
    }

    private void renderArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, PlayerLayer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> layer) {
        VillagerEntityBaseModelMCA<AbstractClientPlayerEntity> model = (VillagerEntityBaseModelMCA<AbstractClientPlayerEntity>)layer.model;
        setModelPose(model, player);

        model.setVisible(false);

        arm.visible = true;
        arm.pitch = 0.0f;
        arm.yaw = 0.0f;
        arm.roll = 0.0f;

        sleeve.visible = true;
        sleeve.pitch = 0.0f;
        sleeve.yaw = 0.0f;
        sleeve.roll = 0.0f;

        model.applyVillagerDimensions(getVillager(player));

        layer.renderFinal(matrices, vertexConsumers, light, player);

        model.setVisible(true);
    }

    private void setModelPose(PlayerEntityModel<AbstractClientPlayerEntity> playerEntityModel, AbstractClientPlayerEntity player) {
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

    private static BipedEntityModel.ArmPose getArmPose(AbstractClientPlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isEmpty()) {
            return BipedEntityModel.ArmPose.EMPTY;
        }
        if (player.getActiveHand() == hand && player.getItemUseTimeLeft() > 0) {
            UseAction useAction = itemStack.getUseAction();
            if (useAction == UseAction.BLOCK) {
                return BipedEntityModel.ArmPose.BLOCK;
            }
            if (useAction == UseAction.BOW) {
                return BipedEntityModel.ArmPose.BOW_AND_ARROW;
            }
            if (useAction == UseAction.SPEAR) {
                return BipedEntityModel.ArmPose.THROW_SPEAR;
            }
            if (useAction == UseAction.CROSSBOW && hand == player.getActiveHand()) {
                return BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
            }
            if (useAction == UseAction.SPYGLASS) {
                return BipedEntityModel.ArmPose.SPYGLASS;
            }
        } else if (!player.handSwinging && itemStack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack)) {
            return BipedEntityModel.ArmPose.CROSSBOW_HOLD;
        }
        return BipedEntityModel.ArmPose.ITEM;
    }
}

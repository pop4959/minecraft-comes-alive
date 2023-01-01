package net.mca.mixin.client;

import net.mca.MCAClient;
import net.mca.client.model.PlayerEntityExtendedModel;
import net.mca.client.model.VillagerEntityModelMCA;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorFeatureRenderer.class)
public abstract class MixinArmorFeatureRenderer<T extends LivingEntity, A extends BipedEntityModel<T>> {
    @Shadow
    protected abstract boolean usesSecondLayer(EquipmentSlot slot);

    protected boolean mca$injectionActive;
    protected A mca$leggingsModel = createModel(VillagerEntityModelMCA.bodyData(new Dilation(0.3F)));
    protected A mca$bodyModel = createModel(VillagerEntityModelMCA.bodyData(new Dilation(0.55F)));

    private A createModel(ModelData data) {
        //noinspection unchecked
        return (A)new PlayerEntityExtendedModel<T>(TexturedModelData.of(data, 64, 32).createModel());
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"))
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
        mca$injectionActive = MCAClient.useGeneticsRenderer(livingEntity.getUuid());
    }

    @Inject(method = "getArmor(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/client/render/entity/model/BipedEntityModel;", at = @At("HEAD"), cancellable = true)
    private void getArmor(EquipmentSlot slot, CallbackInfoReturnable<A> cir) {
        if (mca$injectionActive) {
            cir.setReturnValue(this.usesSecondLayer(slot) ? mca$leggingsModel : mca$bodyModel);
        }
    }
}

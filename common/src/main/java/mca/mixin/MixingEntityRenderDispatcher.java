package mca.mixin;

import mca.client.render.MCAPlayerEntityRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixingEntityRenderDispatcher {
    @Inject(method = "getRenderer", at = @At("HEAD"), cancellable = true)
    public <T extends Entity> void getRenderer(T entity, CallbackInfoReturnable<EntityRenderer<? super T>> cir) {
        if (entity instanceof ClientPlayerEntity) {
            cir.setReturnValue((EntityRenderer<? super T>)MCAPlayerEntityRenderer.entityRenderer);
        }
    }
}

package mca.mixin;

import mca.Config;
import mca.client.render.PlayerEntityMCARenderer;
import mca.cobalt.network.NetworkHandler;
import mca.network.c2s.PlayerDataRequest;
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
        if (entity instanceof ClientPlayerEntity && Config.getInstance().letPlayerCustomize) {
            if (!PlayerEntityMCARenderer.playerDataRequests.contains(entity.getUuid())) {
                PlayerEntityMCARenderer.playerDataRequests.add(entity.getUuid());
                NetworkHandler.sendToServer(new PlayerDataRequest(entity.getUuid()));
            }
            if (PlayerEntityMCARenderer.playerData.containsKey(entity.getUuid())) {
                cir.setReturnValue((EntityRenderer<? super T>)PlayerEntityMCARenderer.entityRenderer);
            }
        }
    }
}

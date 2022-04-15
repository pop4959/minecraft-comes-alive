package mca.mixin;

import mca.client.render.PlayerEntityMCARenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.EntityRenderers;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(EntityRenderers.class)
public class MixingEntityRenderers {
    @Inject(method = "reloadPlayerRenderers(Lnet/minecraft/client/render/entity/EntityRendererFactory$Context;)Ljava/util/Map;", at = @At(value = "HEAD"))
    private static void reloadPlayerRenderers(EntityRendererFactory.Context ctx, CallbackInfoReturnable<Map<String, EntityRenderer<? extends PlayerEntity>>> cir) {
        PlayerEntityMCARenderer.entityRenderer = new PlayerEntityMCARenderer(ctx);
    }
}

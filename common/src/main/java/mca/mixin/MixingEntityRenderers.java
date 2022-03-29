package mca.mixin;

import com.google.common.collect.ImmutableMap;
import mca.client.render.MCAPlayerEntityRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.EntityRenderers;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Map;

@Mixin(EntityRenderers.class)
public class MixingEntityRenderers {
    private static final Map<String, EntityRendererFactory<AbstractClientPlayerEntity>> PLAYER_RENDERER_FACTORIES = ImmutableMap.of(
            "default", context -> new PlayerEntityRenderer(context, false),
            "slim", context -> new PlayerEntityRenderer(context, true));

    /**
     * @author Luke100000
     * @reason that's a nice place to yeet my RendererLoader into
     */
    @Overwrite
    public static Map<String, EntityRenderer<? extends PlayerEntity>> reloadPlayerRenderers(EntityRendererFactory.Context ctx) {
        ImmutableMap.Builder<String, EntityRenderer<? extends PlayerEntity>> builder = ImmutableMap.builder();
        PLAYER_RENDERER_FACTORIES.forEach((type, factory) -> {
            try {
                builder.put(type, factory.create(ctx));
                MCAPlayerEntityRenderer.entityRenderer = new MCAPlayerEntityRenderer(ctx);
            } catch (Exception exception) {
                throw new IllegalArgumentException("Failed to create player model for " + type, exception);
            }
        });
        return builder.build();
    }
}

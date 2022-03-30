package mca.client.render.playerLayer;

import com.google.common.collect.Maps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public abstract class PlayerLayer<T extends LivingEntity, M extends PlayerEntityModel<T>> extends FeatureRenderer<T, M> {

    private static final float[] DEFAULT_COLOR = new float[] {1, 1, 1};

    private static final Map<String, Identifier> TEXTURE_CACHE = Maps.newHashMap();

    public final M model;

    public PlayerLayer(FeatureRendererContext<T, M> renderer, M model) {
        super(renderer);
        this.model = model;
    }

    @Nullable
    protected Identifier getSkin(T villager) {
        return null;
    }

    @Nullable
    protected Identifier getOverlay(T villager) {
        return null;
    }

    protected float[] getColor(T villager) {
        return DEFAULT_COLOR;
    }

    protected boolean isTranslucent() {
        return false;
    }

    @Override
    public void render(MatrixStack transform, VertexConsumerProvider provider, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        //copy the animation to this layers model
        getContextModel().setAttributes(model);

        model.leftPants.copyTransform(model.leftLeg);
        model.rightPants.copyTransform(model.rightLeg);
        model.leftSleeve.copyTransform(model.leftArm);
        model.rightSleeve.copyTransform(model.rightArm);
        model.jacket.copyTransform(model.body);
        renderFinal(transform, provider, light, entity);
    }

    public void renderFinal(MatrixStack transform, VertexConsumerProvider provider, int light, T entity) {
        int tint = LivingEntityRenderer.getOverlay(entity, 0);

        Identifier skin = getSkin(entity);
        if (canUse(skin)) {
            float[] color = getColor(entity);
            renderModel(transform, provider, light, model, color[0], color[1], color[2], skin, tint);
        }

        Identifier overlay = getOverlay(entity);
        if (canUse(overlay)) {
            renderModel(transform, provider, light, model, 1, 1, 1, overlay, tint);
        }
    }

    private void renderModel(MatrixStack transform, VertexConsumerProvider provider, int light, M model, float r, float g, float b, Identifier texture, int overlay) {
        VertexConsumer buffer = provider.getBuffer(isTranslucent() ? RenderLayer.getEntityTranslucent(texture) : RenderLayer.getEntityCutoutNoCull(texture));
        model.render(transform, buffer, light, overlay, r, g, b, 1);
    }

    protected final boolean canUse(Identifier texture) {
        return texture != null && MinecraftClient.getInstance().getResourceManager().containsResource(texture);
    }

    @Nullable
    protected final Identifier cached(String name, Function<String, Identifier> supplier) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return TEXTURE_CACHE.computeIfAbsent(name, s -> {
            try {
                return supplier.apply(s);
            } catch (InvalidIdentifierException ignored) {
                return null;
            }
        });
    }
}

package mca.client.render.layer;

import mca.client.model.VillagerEntityModelMCA;
import mca.entity.VillagerLike;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

public class ClothingLayer<T extends MobEntity & VillagerLike<T>> extends VillagerLayer<T, VillagerEntityModelMCA<T>> {

    private final String variant;

    public ClothingLayer(FeatureRendererContext<T, VillagerEntityModelMCA<T>> renderer, VillagerEntityModelMCA<T> model, String variant) {
        super(renderer, model);
        this.variant = variant;
    }

    @Override
    protected Identifier getSkin(T villager) {
        return cached(villager.getClothes() + variant, clothes -> {
            Identifier id = new Identifier(villager.getClothes());

            // use it if it's already valid
            if (canUse(id)) {
                return id;
            }

            return new Identifier(id.getNamespace(), id.getPath().replace("normal", variant));
        });
    }
}

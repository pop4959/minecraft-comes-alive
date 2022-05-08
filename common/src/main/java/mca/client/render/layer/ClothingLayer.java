package mca.client.render.layer;

import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

import static mca.client.model.CommonVillagerModel.getVillager;

public class ClothingLayer<T extends LivingEntity, M extends BipedEntityModel<T>> extends VillagerLayer<T, M> {

    private final String variant;

    public ClothingLayer(FeatureRendererContext<T, M> renderer, M model, String variant) {
        super(renderer, model);
        this.variant = variant;
    }

    @Override
    protected Identifier getSkin(T villager) {
        return cached(getVillager(villager).getClothes() + variant, clothes -> {
            Identifier id = new Identifier(getVillager(villager).getClothes());

            // use it if it's already valid
            if (canUse(id)) {
                return id;
            }

            return new Identifier(id.getNamespace(), id.getPath().replace("normal", variant));
        });
    }
}

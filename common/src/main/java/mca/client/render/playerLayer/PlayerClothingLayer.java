package mca.client.render.playerLayer;

import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

import static mca.client.model.VillagerEntityBaseModelMCA.getVillager;

public class PlayerClothingLayer<T extends LivingEntity> extends PlayerLayer<T, PlayerEntityModel<T>> {

    private final String variant;

    public PlayerClothingLayer(FeatureRendererContext<T, PlayerEntityModel<T>> renderer, PlayerEntityModel<T> model) {
        super(renderer, model);
        this.variant = "normal";
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

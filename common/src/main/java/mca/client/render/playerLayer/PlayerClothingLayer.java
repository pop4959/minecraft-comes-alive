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

            String path = id.getPath();

            // use it if it's valid
            if (canUse(id)) {
                return id;
            }

            // old values require conversion
            if (path.startsWith("skins")) {
                Identifier converted = new Identifier(id.getNamespace(), path.replace("/clothing/", "/clothing/" + variant));
                if (canUse(converted)) {
                    return converted;
                }
            }

            return new Identifier(id.getNamespace(), String.format("skins/clothing/%s/%s", variant, path));
        });
    }
}

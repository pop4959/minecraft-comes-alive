package mca.client.render.playerLayer;

import mca.client.resources.ColorPalette;
import mca.entity.ai.Genetics;
import mca.entity.ai.Traits;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.Optional;

import static mca.client.model.VillagerEntityBaseModelMCA.getVillager;

public class PlayerHairLayer<T extends LivingEntity> extends PlayerLayer<T, PlayerEntityModel<T>> {
    public PlayerHairLayer(FeatureRendererContext<T, PlayerEntityModel<T>> renderer, PlayerEntityModel<T> model) {
        super(renderer, model);

        this.model.leftLeg.visible = false;
        this.model.rightLeg.visible = false;
    }

    @Override
    protected Identifier getSkin(T villager) {
        return cached(getVillager(villager).getHair(), Identifier::new);
    }

    @Override
    protected Identifier getOverlay(T villager) {
        return cached(getVillager(villager).getHair().replace(".png", "_overlay.png"), Identifier::new);
    }

    @Override
    protected float[] getColor(T villager) {
        Optional<DyeColor> hairDye = getVillager(villager).getHairDye();
        if (hairDye.isPresent()) {
            return hairDye.get().getColorComponents();
        }

        float albinism = getVillager(villager).getTraits().hasTrait(Traits.Trait.ALBINISM) ? 0.1f : 1.0f;

        return ColorPalette.HAIR.getColor(
                getVillager(villager).getGenetics().getGene(Genetics.EUMELANIN) * albinism,
                getVillager(villager).getGenetics().getGene(Genetics.PHEOMELANIN) * albinism,
                0
        );
    }
}

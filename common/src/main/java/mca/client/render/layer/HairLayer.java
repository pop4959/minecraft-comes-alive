package mca.client.render.layer;

import mca.client.resources.ColorPalette;
import mca.entity.ai.Genetics;
import mca.entity.ai.Traits;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

import static mca.client.model.CommonVillagerModel.getVillager;

public class HairLayer<T extends LivingEntity, M extends BipedEntityModel<T>> extends VillagerLayer<T, M> {
    public HairLayer(FeatureRendererContext<T, M> renderer, M model) {
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
        float[] hairDye = getVillager(villager).getHairDye();
        if (hairDye[0] >= 0.0f) {
            return hairDye;
        }

        float albinism = getVillager(villager).getTraits().hasTrait(Traits.Trait.ALBINISM) ? 0.1f : 1.0f;

        return ColorPalette.HAIR.getColor(
                getVillager(villager).getGenetics().getGene(Genetics.EUMELANIN) * albinism,
                getVillager(villager).getGenetics().getGene(Genetics.PHEOMELANIN) * albinism,
                0
        );
    }
}

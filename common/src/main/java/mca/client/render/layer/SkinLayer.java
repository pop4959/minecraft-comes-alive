package mca.client.render.layer;

import mca.MCA;
import mca.client.model.VillagerEntityModelMCA;
import mca.client.resources.ColorPalette;
import mca.entity.VillagerLike;
import mca.entity.ai.Genetics;
import mca.entity.ai.Traits;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

public class SkinLayer<T extends MobEntity & VillagerLike<T>> extends VillagerLayer<T, VillagerEntityModelMCA<T>> {
    public SkinLayer(FeatureRendererContext<T, VillagerEntityModelMCA<T>> renderer, VillagerEntityModelMCA<T> model) {
        super(renderer, model);
    }

    @Override
    protected Identifier getSkin(T villager) {
        Genetics genetics = villager.getGenetics();
        int skin = (int) Math.min(4, Math.max(0, genetics.getGene(Genetics.SKIN) * 5));
        return cached(String.format("%s:skins/skin/%s/%d.png", MCA.MOD_ID, genetics.getGender().getStrName(), skin), Identifier::new);
    }

    @Override
    protected float[] getColor(T villager) {
        float albinism = villager.getTraits().hasTrait(Traits.Trait.ALBINISM) ? 0.1f : 1.0f;

        return ColorPalette.SKIN.getColor(
                villager.getGenetics().getGene(Genetics.MELANIN) * albinism,
                villager.getGenetics().getGene(Genetics.HEMOGLOBIN) * albinism,
                villager.getInfectionProgress()
        );
    }
}

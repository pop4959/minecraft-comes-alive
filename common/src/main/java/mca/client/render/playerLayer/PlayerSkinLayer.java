package mca.client.render.playerLayer;

import mca.client.resources.ColorPalette;
import mca.entity.ai.Genetics;
import mca.entity.ai.Traits;
import mca.entity.ai.relationship.Gender;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

import static mca.client.model.VillagerEntityBaseModelMCA.getVillager;

public class PlayerSkinLayer<T extends LivingEntity> extends PlayerLayer<T, PlayerEntityModel<T>> {
    public PlayerSkinLayer(FeatureRendererContext<T, PlayerEntityModel<T>> renderer, PlayerEntityModel<T> model) {
        super(renderer, model);
    }

    @Override
    protected Identifier getSkin(T villager) {
        Gender gender = getVillager(villager).getGenetics().getGender();
        int skin = (int) Math.min(4, Math.max(0, getVillager(villager).getGenetics().getGene(Genetics.SKIN) * 5));
        return cached(String.format("mca:skins/skin/%s/%d.png", gender == Gender.FEMALE ? "female" : "male", skin), Identifier::new);
    }

    @Override
    protected float[] getColor(T villager) {
        float albinism = getVillager(villager).getTraits().hasTrait(Traits.Trait.ALBINISM) ? 0.1f : 1.0f;

        return ColorPalette.SKIN.getColor(
                getVillager(villager).getGenetics().getGene(Genetics.MELANIN) * albinism,
                getVillager(villager).getGenetics().getGene(Genetics.HEMOGLOBIN) * albinism,
                getVillager(villager).getInfectionProgress()
        );
    }
}

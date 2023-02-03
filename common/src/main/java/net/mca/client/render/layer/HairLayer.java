package net.mca.client.render.layer;

import net.mca.client.resources.ColorPalette;
import net.mca.entity.ai.Genetics;
import net.mca.entity.ai.Traits;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import static net.mca.client.model.CommonVillagerModel.getVillager;

public class HairLayer<T extends LivingEntity, M extends BipedEntityModel<T>> extends VillagerLayer<T, M> {
    public HairLayer(FeatureRendererContext<T, M> renderer, M model) {
        super(renderer, model);

        this.model.leftLeg.visible = false;
        this.model.rightLeg.visible = false;
    }

    @Override
    public Identifier getSkin(T villager) {
        return cached(getVillager(villager).getHair(), Identifier::new);
    }

    @Override
    protected Identifier getOverlay(T villager) {
        return cached(getVillager(villager).getHair().replace(".png", "_overlay.png"), Identifier::new);
    }

    private float[] getRainbow(LivingEntity entity, float tickDelta) {
        int n = Math.abs(entity.age) / 25 + entity.getId();
        int o = DyeColor.values().length;
        int p = n % o;
        int q = (n + 1) % o;
        float r = ((float)(Math.abs(entity.age) % 25) + tickDelta) / 25.0f;
        float[] fs = SheepEntity.getRgbColor(DyeColor.byId(p));
        float[] gs = SheepEntity.getRgbColor(DyeColor.byId(q));
        return new float[] {
                fs[0] * (1.0f - r) + gs[0] * r,
                fs[1] * (1.0f - r) + gs[1] * r,
                fs[2] * (1.0f - r) + gs[2] * r
        };
    }

    @Override
    protected float[] getColor(T villager, float tickDelta) {
        if (getVillager(villager).getTraits().hasTrait(Traits.Trait.RAINBOW)) {
            return getRainbow(villager, tickDelta);
        }

        float[] hairDye = getVillager(villager).getHairDye();
        if (hairDye[0] > 0.0f) {
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

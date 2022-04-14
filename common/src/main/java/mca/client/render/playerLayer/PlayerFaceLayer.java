package mca.client.render.playerLayer;

import mca.entity.ai.Genetics;
import mca.entity.ai.Traits;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

import static mca.client.model.VillagerEntityBaseModelMCA.getVillager;

public class PlayerFaceLayer<T extends LivingEntity> extends PlayerLayer<T, PlayerEntityModel<T>> {

    private final String variant;

    public PlayerFaceLayer(FeatureRendererContext<T, PlayerEntityModel<T>> renderer, PlayerEntityModel<T> model) {
        super(renderer, model);
        this.variant = "normal";

        model.setVisible(false);
        model.head.visible = true;
    }

    @Override
    protected boolean isTranslucent() {
        return true;
    }

    @Override
    protected Identifier getSkin(T villager) {
        int totalFaces = 11;
        int index = (int)Math.min(totalFaces - 1, Math.max(0, getVillager(villager).getGenetics().getGene(Genetics.FACE) * totalFaces));
        int time = villager.age / 2 + (int)(getVillager(villager).getGenetics().getGene(Genetics.HEMOGLOBIN) * 65536);
        boolean blink = time % 50 == 1 || time % 57 == 1 || villager.isSleeping() || villager.isDead();
        boolean hasHeterochromia = variant.equals("normal") && getVillager(villager).getTraits().hasTrait(Traits.Trait.HETEROCHROMIA);

        return cached(String.format("mca:skins/face/%s/%s/%d%s.png",
                variant,
                getVillager(villager).getGenetics().getGender().getStrName(),
                index,
                blink ? "_blink" : (hasHeterochromia ? "_hetero" : "")
        ), Identifier::new);
    }
}

package net.mca.client.render.layer;

import net.mca.client.model.CommonVillagerModel;
import net.mca.entity.ai.Genetics;
import net.mca.entity.ai.Traits;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

public class FaceLayer<T extends LivingEntity, M extends BipedEntityModel<T>> extends VillagerLayer<T, M> {
    private static final int FACE_COUNT = 22;

    private final String variant;

    public FaceLayer(FeatureRendererContext<T, M> renderer, M model, String variant) {
        super(renderer, model);
        this.variant = variant;

        model.setVisible(false);
        model.head.visible = true;
    }

    @Override
    protected boolean isTranslucent() {
        return true;
    }

    @Override
    protected Identifier getSkin(T villager) {
        int index = (int) Math.min(FACE_COUNT - 1, Math.max(0, CommonVillagerModel.getVillager(villager).getGenetics().getGene(Genetics.FACE) * FACE_COUNT));
        int time = villager.age / 2 + (int) (CommonVillagerModel.getVillager(villager).getGenetics().getGene(Genetics.HEMOGLOBIN) * 65536);
        boolean blink = time % 50 == 1 || time % 57 == 1 || villager.isSleeping() || villager.isDead();
        boolean hasHeterochromia = variant.equals("normal") && CommonVillagerModel.getVillager(villager).getTraits().hasTrait(Traits.Trait.HETEROCHROMIA);

        return cached(String.format("mca:skins/face/%s/%s/%d%s.png",
                variant,
                CommonVillagerModel.getVillager(villager).getGenetics().getGender().getStrName(),
                index,
                blink ? "_blink" : (hasHeterochromia ? "_hetero" : "")
        ), Identifier::new);
    }
}

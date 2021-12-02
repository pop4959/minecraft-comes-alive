package mca.client.render;

import mca.client.model.VillagerEntityModelMCA;
import mca.client.render.layer.ClothingLayer;
import mca.client.render.layer.FaceLayer;
import mca.client.render.layer.HairLayer;
import mca.client.render.layer.SkinLayer;
import mca.entity.VillagerEntityMCA;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.EntityRendererFactory;

public class VillagerEntityMCARenderer extends VillagerLikeEntityMCARenderer<VillagerEntityMCA> {
    public VillagerEntityMCARenderer(EntityRendererFactory.Context ctx) {
        super(ctx, createModel(VillagerEntityModelMCA.bodyData(Dilation.NONE), false).hideWears());

        addFeature(new SkinLayer<>(this, model));
        addFeature(new FaceLayer<>(this, createModel(VillagerEntityModelMCA.bodyData(new Dilation(0.01F)), false).hideWears(), "normal"));
        addFeature(new ClothingLayer<>(this, createModel(VillagerEntityModelMCA.clothingData(new Dilation(0.0625F)), true), "normal"));
        addFeature(new HairLayer<>(this, createModel(VillagerEntityModelMCA.hairData(new Dilation(0.125F)), true)));
    }

    private static VillagerEntityModelMCA<VillagerEntityMCA> createModel(ModelData data, boolean cloth) {
        return new VillagerEntityModelMCA<>(TexturedModelData.of(data, 64, 64).createModel(), cloth);
    }
}

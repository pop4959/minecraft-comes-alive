package mca.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import mca.MCA;
import mca.advancement.criterion.CriterionMCA;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public class MixinAbstractFurnaceBlockEntity {

    @Final
    @Shadow
    private Object2IntOpenHashMap<Identifier> recipesUsed;

    @Inject(method = "dropExperienceForRecipesUsed", at = @At("HEAD"))
    public void onDropExperience(ServerPlayerEntity player, CallbackInfo ci) {
        recipesUsed.forEach((identifier, count) -> {
            // Note: This could become a switch case possibly if this grows too big
            if (identifier.getNamespace().equals(MCA.MOD_ID)) {
                final String path = identifier.getPath();
                if (path.endsWith("baby_boy") || path.endsWith("baby_girl")) {
                    if (path.startsWith("sirben_")) {
                        CriterionMCA.BABY_SIRBEN_SMELTED_CRITERION.trigger(player, count);
                    }
                    CriterionMCA.BABY_SMELTED_CRITERION.trigger(player, count);
                }
            }
        });
    }
}

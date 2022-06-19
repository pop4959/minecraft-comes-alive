package mca.item;

import mca.entity.VillagerEntityMCA;
import mca.entity.ai.Traits;
import mca.entity.ai.relationship.Gender;
import mca.server.world.data.BabyTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class SirbenBabyItem extends BabyItem {
    public SirbenBabyItem(Gender gender, Settings properties) {
        super(gender, properties);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    protected VillagerEntityMCA birthChild(BabyTracker.ChildSaveState state, ServerWorld world, ServerPlayerEntity player) {
        VillagerEntityMCA child = super.birthChild(state, world, player);
        child.getTraits().addTrait(Traits.Trait.SIRBEN);
        return child;
    }
}

package mca.item;

import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public class NonPlayerItemUsageContext extends ItemUsageContext {
    public NonPlayerItemUsageContext(World world, Hand hand, ItemStack stack, BlockHitResult hit) {
        super(world, null, hand, stack, hit);
    }
}

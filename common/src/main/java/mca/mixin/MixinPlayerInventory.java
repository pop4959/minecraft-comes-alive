package mca.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mca.item.BabyItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Nameable;

@Mixin(PlayerInventory.class)
abstract class MixinPlayerInventory implements Inventory, Nameable {
    @Inject(method = "dropSelectedItem(Z)Lnet/minecraft/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true)
    public void onDropSelectedItem(boolean dropEntireStack, CallbackInfoReturnable<ItemStack> info) {
        ItemStack stack = ((PlayerInventory)(Object)this).getMainHandStack();
        if (stack.getItem() instanceof BabyItem && !((BabyItem)stack.getItem()).onDropped(stack, (PlayerEntity)(Object)this)) {
            info.setReturnValue(ItemStack.EMPTY);
        }
    }
}

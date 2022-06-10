package mca.item;

import mca.cobalt.network.NetworkHandler;
import mca.entity.VillagerLike;
import mca.network.s2c.OpenGuiRequest;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class NeedleAndThreadItem extends TooltippedItem {
    public NeedleAndThreadItem(Item.Settings properties) {
        super(properties);
    }

    @Override
    public final TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ItemStack stack = player.getStackInHand(hand);
            NetworkHandler.sendToPlayer(new OpenGuiRequest(OpenGuiRequest.Type.NEEDLE_AND_THREAD), serverPlayer);
            return TypedActionResult.success(stack);
        }
        return super.use(world, player, hand);
    }

    public ActionResult useOnEntity(ItemStack stack, PlayerEntity player, LivingEntity entity, Hand hand) {
        if (entity instanceof VillagerLike && !entity.world.isClient && player instanceof ServerPlayerEntity) {
            NetworkHandler.sendToPlayer(new OpenGuiRequest(OpenGuiRequest.Type.NEEDLE_AND_THREAD, entity), (ServerPlayerEntity)player);
            return ActionResult.SUCCESS;
        } else {
            return ActionResult.CONSUME;
        }
    }
}

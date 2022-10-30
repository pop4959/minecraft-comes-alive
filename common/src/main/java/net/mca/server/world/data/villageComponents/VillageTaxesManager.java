package net.mca.server.world.data.villageComponents;

import net.mca.Config;
import net.mca.resources.Rank;
import net.mca.resources.Tasks;
import net.mca.server.world.data.Village;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.Collection;

public class VillageTaxesManager {
    private static final int MAX_STORAGE_SIZE = 1024;

    private final Village village;

    public VillageTaxesManager(Village village) {
        this.village= village;
    }

    public void taxes(ServerWorld world) {
        int emeraldValue = 100;
        int taxes = (int)(Config.getInstance().taxesFactor * village.getPopulation() * village.getTaxes() + world.random.nextInt(emeraldValue));
        int moodImpact = 0;

        //response
        Text msg;
        float r = MathHelper.lerp(0.5f, village.getTaxes() / 100.0f, world.random.nextFloat());
        if (village.getTaxes() == 0.0f) {
            msg = new TranslatableText("gui.village.taxes.no", village.getName()).formatted(Formatting.GREEN);
            moodImpact = 5;
        } else if (r < 0.1) {
            msg = new TranslatableText("gui.village.taxes.more", village.getName()).formatted(Formatting.GREEN);
            taxes += village.getPopulation() * 0.25;
        } else if (r < 0.3) {
            msg = new TranslatableText("gui.village.taxes.happy", village.getName()).formatted(Formatting.DARK_GREEN);
            moodImpact = 5;
        } else if (r < 0.7) {
            msg = new TranslatableText("gui.village.taxes", village.getName());
        } else if (r < 0.8) {
            msg = new TranslatableText("gui.village.taxes.sad", village.getName()).formatted(Formatting.GOLD);
            moodImpact = -5;
        } else if (r < 0.9) {
            msg = new TranslatableText("gui.village.taxes.angry", village.getName()).formatted(Formatting.RED);
            moodImpact = -10;
        } else {
            msg = new TranslatableText("gui.village.taxes.riot", village.getName()).formatted(Formatting.DARK_RED);
            taxes = 0;
        }

        //send all player with rank merchant a notification
        world.getPlayers().stream()
                .filter(v -> Tasks.getRank(village, v).isAtLeast(Rank.MERCHANT))
                .forEach(player -> player.sendMessage(msg, true));

        if (village.hasBuilding("library")) {
            taxes *= 1.5;
        }

        int emeraldCount = taxes / emeraldValue;
        while (emeraldCount > 0 && village.storageBuffer.size() < MAX_STORAGE_SIZE) {
            village.storageBuffer.add(new ItemStack(Items.EMERALD, Math.min(emeraldCount, Items.EMERALD.getMaxCount())));
            emeraldCount -= Items.EMERALD.getMaxCount();
        }

        if (moodImpact != 0) {
            village.pushMood(world, moodImpact * village.getPopulation());
        }

        deliverTaxes(world);
    }

    public void deliverTaxes(ServerWorld world) {
        if (village.hasStoredResource()) {
            village.getBuildingsOfType("storage").forEach(building -> building.getBlocks().values().stream()
                    .flatMap(Collection::stream)
                    .forEach(p -> {
                        if (village.hasStoredResource()) {
                            tryToPutIntoInventory(world, p);
                        }
                    }));
        }
    }

    private void tryToPutIntoInventory(ServerWorld world, BlockPos p) {
        BlockState state = world.getBlockState(p);
        if (state.hasBlockEntity()) {
            BlockEntity blockEntity = world.getBlockEntity(p);
            if (blockEntity instanceof Inventory inventory) {
                Block block = state.getBlock();
                if (inventory instanceof ChestBlockEntity && block instanceof ChestBlock chest) {
                    inventory = ChestBlock.getInventory(chest, state, world, p, true);
                    if (inventory != null) {
                        putIntoInventory(inventory);
                    }
                }
            }
        }
    }

    private void putIntoInventory(Inventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            boolean changes = true;
            while (changes) {
                changes = false;
                ItemStack stack = inventory.getStack(i);
                ItemStack tax = village.storageBuffer.get(0);
                if (stack.getItem() == tax.getItem()) {
                    int diff = Math.min(tax.getCount(), stack.getMaxCount() - stack.getCount());
                    if (diff > 0) {
                        stack.increment(diff);
                        tax.decrement(diff);
                        if (tax.isEmpty()) {
                            village.storageBuffer.remove(0);
                            changes = true;
                        }
                        inventory.markDirty();
                    }
                } else if (stack.isEmpty()) {
                    inventory.setStack(i, tax);
                    inventory.markDirty();
                    village.storageBuffer.remove(0);
                    changes = true;
                }
                if (!village.hasStoredResource()) {
                    return;
                }
            }
        }
    }
}

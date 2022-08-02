package mca.entity.ai.brain.tasks.chore;

import com.google.common.collect.ImmutableMap;
import mca.TagsMCA;
import mca.entity.VillagerEntityMCA;
import mca.entity.ai.Chore;
import mca.entity.ai.TaskUtils;
import mca.util.InventoryUtils;
import net.minecraft.block.*;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class HarvestingTask extends AbstractChoreTask {
    /**
     * The minimum time to wait between actions.
     *
     * Actions include:
     * - switching items
     * - using bonemeal
     * - collecting/replanting crops
     * - planting new crops
     */
    private static final int TICKS_PER_TURN = 15;

    private static final int ITEM_READY = 0;
    private static final int ITEM_FOUND = 1;
    private static final int ITEM_MISSING = 2;

    private final List<BlockPos> harvestable = new ArrayList<>();

    private int lastCropScan = 0;

    public HarvestingTask() {
        super(ImmutableMap.of(
                MemoryModuleType.LOOK_TARGET, MemoryModuleState.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT
        ));
    }

    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntityMCA villager) {
        return villager.getVillagerBrain().getCurrentJob() == Chore.HARVEST && super.shouldRun(world, villager);
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntityMCA villager, long time) {
        return shouldRun(world, villager);
    }

    @Override
    protected void finishRunning(ServerWorld world, VillagerEntityMCA villager, long time) {
        ItemStack stack = villager.getStackInHand(villager.getDominantHand());
        if (!stack.isEmpty()) {
            villager.setStackInHand(villager.getDominantHand(), ItemStack.EMPTY);
        }
    }

    @Override
    protected void run(ServerWorld world, VillagerEntityMCA villager, long time) {
        super.run(world, villager, time);
        if (!villager.hasStackEquipped(villager.getDominantSlot())) {
            int i = InventoryUtils.getFirstSlotContainingItem(villager.getInventory(), stack -> stack.getItem() instanceof HoeItem);
            if (i == -1) {
                abandonJobWithMessage("chore.harvesting.nohoe");
            } else {
                ItemStack stack = villager.getInventory().getStack(i);
                villager.setStackInHand(villager.getDominantHand(), stack);
            }
        }
    }

    private BlockPos searchCrop(int rangeX, int rangeY, boolean harvestableOnly) {
        List<BlockPos> nearbyCrops = TaskUtils.getNearbyBlocks(villager.getBlockPos(), villager.world,
                blockState -> blockState.isIn(BlockTags.CROPS) || blockState.getBlock() instanceof GourdBlock, rangeX, rangeY);
        harvestable.clear();

        if (harvestableOnly) {
            harvestable.addAll(nearbyCrops.stream().filter(pos -> {
                BlockState state = villager.world.getBlockState(pos);
                return (state.getBlock() instanceof CropBlock crop && crop.isMature(state))
                        || state.getBlock() instanceof GourdBlock;
            }).toList());
        }

        return TaskUtils.getNearestPoint(villager.getBlockPos(), harvestable.isEmpty() ? nearbyCrops : harvestable);

    }

    private BlockPos searchUnusedFarmLand(int rangeX, int rangeY) {
        return TaskUtils.getNearestPoint(villager.getBlockPos(),
                TaskUtils.getNearbyBlocks(villager.getBlockPos(), villager.world,
                                blockState -> blockState.isOf(Blocks.FARMLAND), rangeX, rangeY)
                        .stream()
                        .filter(pos -> {
                            BlockState state = villager.world.getBlockState(pos);
                            return state.getBlock() instanceof FarmlandBlock
                                    && state.canPlaceAt(villager.world, pos)
                                    && villager.world.getBlockState(pos.up()).isAir();
                        })
                        .toList());
    }

    @Override
    protected void keepRunning(ServerWorld world, VillagerEntityMCA villager, long time) {
        if (this.villager == null) {
            this.villager = villager;
        }

        if (!InventoryUtils.contains(villager.getInventory(), HoeItem.class) && !villager.hasStackEquipped(villager.getDominantSlot())) {
            abandonJobWithMessage("chore.harvesting.nohoe");
        } else if (!villager.hasStackEquipped(villager.getDominantSlot())) {
            int i = InventoryUtils.getFirstSlotContainingItem(villager.getInventory(), stack -> stack.getItem() instanceof HoeItem);
            ItemStack stack = villager.getInventory().getStack(i);
            villager.setStackInHand(villager.getDominantHand(), stack);
        }

        BlockPos fertileFarmLand = searchUnusedFarmLand(16, 3);

        if (fertileFarmLand == null && villager.age - lastCropScan > 1200) {
            lastCropScan = villager.age;
            fertileFarmLand = searchUnusedFarmLand(32, 16);
        }

        if (fertileFarmLand != null && villager.hasSeedToPlant()) {
            villager.moveTowards(fertileFarmLand);

            if (villager.squaredDistanceTo(Vec3d.ofBottomCenter(fertileFarmLand)) <= 6 && tickAction()) {
                plantSeeds(world, villager, fertileFarmLand.up());
            }
        }

        BlockPos crops = searchCrop(16, 3, true);

        if (crops == null && villager.age - lastCropScan > 1200) {
            crops = searchCrop(32, 16, true);
        }

        if (crops == null) {
            failedTicks = FAILED_COOLDOWN;
            return;
        }

        if (harvestable.isEmpty()) {
            crops = searchCrop(16, 3, false);
        }

        // harvest if next to it, else try to reach it
        villager.moveTowards(crops);

        if (villager.squaredDistanceTo(Vec3d.ofBottomCenter(crops)) <= 4.5D && tickAction()) {
            BlockState state = world.getBlockState(crops);

            if (state.getBlock() instanceof CropBlock crop) {
                if (crop.isMature(state)) {
                    harvestCrops(world, crops);
                    plantSeeds(world, villager, crops);
                } else {
                    bonemealCrop(world, villager, crops);
                }
            } else if (state.getBlock() instanceof GourdBlock) {
                harvestCrops(world, crops);
            }
        }
    }

    /**
     * Checks whether an action can be performed on this tick.
     */
    private boolean tickAction() {
        return villager.world.getTime() % TICKS_PER_TURN == 0;
    }

    /**
     * Finds a matching item and places it in the villager's main hand slot.
     * Returns true when the item is ready to use.
     */
    private int swapItem(Predicate<ItemStack> find) {
        ItemStack stack = villager.getMainHandStack();
        if (find.test(stack)) {
            return ITEM_READY;
        }
        Inventory inventory = villager.getInventory();
        int slot = InventoryUtils.getFirstSlotContainingItem(inventory, find);
        if (slot < 0) {
            return ITEM_MISSING;
        }
        villager.setStackInHand(villager.getDominantHand(), inventory.getStack(slot));
        return ITEM_FOUND;
    }

    private boolean plantSeed(ItemStack itemStack, ServerWorld serverWorld, BlockPos currentTarget) {
        if (itemStack.isOf(Items.WHEAT_SEEDS)) {
            return serverWorld.setBlockState(currentTarget, Blocks.WHEAT.getDefaultState(), Block.NOTIFY_ALL);
        } else if (itemStack.isOf(Items.POTATO)) {
            return serverWorld.setBlockState(currentTarget, Blocks.POTATOES.getDefaultState(), Block.NOTIFY_ALL);
        } else if (itemStack.isOf(Items.CARROT)) {
            return serverWorld.setBlockState(currentTarget, Blocks.CARROTS.getDefaultState(), Block.NOTIFY_ALL);
        } else if (itemStack.isOf(Items.BEETROOT_SEEDS)) {
            return serverWorld.setBlockState(currentTarget, Blocks.BEETROOTS.getDefaultState(), Block.NOTIFY_ALL);
        }
        //todo add modded plants here
        return false;
    }

    private void plantSeeds(ServerWorld world, VillagerEntityMCA villager, BlockPos target) {
        BlockHitResult hitResult = new BlockHitResult(
                Vec3d.ofBottomCenter(target),
                Direction.DOWN,
                target,
                true
        );

        ActionResult result = InventoryUtils.stream(villager.getInventory())
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof BlockItem && stack.isIn(TagsMCA.Items.VILLAGER_PLANTABLE))
                .filter(stack -> {
                    if (plantSeed(stack, world, hitResult.getBlockPos())) {
                        stack.decrement(1);
                        return true;
                    } else {
                        return false;
                    }
                })
                .findFirst()
                .map(v -> ActionResult.SUCCESS)
                .orElse(ActionResult.FAIL);

        if (result.isAccepted()) {
            if (result.shouldSwingHand()) {
                villager.swingHand(villager.getDominantHand());
            }
        } else if (getAssigningPlayer().isPresent()) {
            villager.sendChatMessage(getAssigningPlayer().get(), "chore.harvesting.noseed");
        }
    }

    private void bonemealCrop(ServerWorld world, VillagerEntityMCA villager, BlockPos pos) {
        if (swapItem(stack -> stack.isOf(Items.BONE_MEAL)) == ITEM_READY && BoneMealItem.useOnFertilizable(villager.getMainHandStack(), world, pos)) {
            villager.swingHand(villager.getDominantHand());
        }
    }

    private void harvestCrops(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        LootContext.Builder lootcontext$builder = new LootContext.Builder(world)
                .parameter(LootContextParameters.ORIGIN, villager.getPos())
                .parameter(LootContextParameters.TOOL, ItemStack.EMPTY)
                .parameter(LootContextParameters.THIS_ENTITY, villager)
                .parameter(LootContextParameters.BLOCK_STATE, state)
                .random(villager.getRandom())
                .luck(0);

        List<ItemStack> drops = world.getServer().getLootManager().getTable(state.getBlock().getLootTableId()).generateLoot(lootcontext$builder.build(LootContextTypes.BLOCK));
        for (ItemStack stack : drops) {
            villager.getInventory().addStack(stack);
        }

        world.breakBlock(pos, false, villager);
    }
}

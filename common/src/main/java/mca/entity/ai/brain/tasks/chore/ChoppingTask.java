package mca.entity.ai.brain.tasks.chore;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonSyntaxException;
import mca.Config;
import mca.entity.VillagerEntityMCA;
import mca.entity.ai.Chore;
import mca.entity.ai.TaskUtils;
import mca.util.InventoryUtils;
import mca.util.RegistryHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChoppingTask extends AbstractChoreTask {
    private int chopTicks, targetTreeTicks;
    private BlockPos targetTree;

    public ChoppingTask() {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryModuleState.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT));
    }

    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntityMCA villager) {
        return villager.getVillagerBrain().getCurrentJob() == Chore.CHOP;
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntityMCA villager, long time) {
        return shouldRun(world, villager) && villager.getHealth() == villager.getMaxHealth();
    }

    @Override
    protected void finishRunning(ServerWorld world, VillagerEntityMCA villager, long time) {
        ItemStack stack = villager.getStackInHand(Hand.MAIN_HAND);
        if (!stack.isEmpty()) {
            villager.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        }
        villager.swingHand(Hand.MAIN_HAND);
    }

    @Override
    protected void run(ServerWorld world, VillagerEntityMCA villager, long time) {
        super.run(world, villager, time);

        if (!villager.hasStackEquipped(EquipmentSlot.MAINHAND)) {
            int i = InventoryUtils.getFirstSlotContainingItem(villager.getInventory(), stack -> stack.getItem() instanceof AxeItem);
            if (i == -1) {
                abandonJobWithMessage("chore.chopping.noaxe");
            } else {
                villager.setStackInHand(Hand.MAIN_HAND, villager.getInventory().getStack(i));
            }
        }
    }

    @Override
    protected void keepRunning(ServerWorld world, VillagerEntityMCA villager, long time) {
        if (this.villager == null) this.villager = villager;

        if (!InventoryUtils.contains(villager.getInventory(), AxeItem.class) && !villager.hasStackEquipped(EquipmentSlot.MAINHAND)) {
            abandonJobWithMessage("chore.chopping.noaxe");
        } else if (!villager.hasStackEquipped(EquipmentSlot.MAINHAND)) {
            int i = InventoryUtils.getFirstSlotContainingItem(villager.getInventory(), stack -> stack.getItem() instanceof AxeItem);
            ItemStack stack = villager.getInventory().getStack(i);
            villager.setStackInHand(Hand.MAIN_HAND, stack);
        }

        if (targetTree == null) {
            List<BlockPos> nearbyLogs = TaskUtils.getNearbyBlocks(villager.getBlockPos(), world, (blockState -> blockState.isIn(BlockTags.LOGS)), 15, 5);
            List<BlockPos> nearbyTrees = new ArrayList<>();

            // valid "trees" are logs on the ground with leaves around them
            nearbyLogs.stream()
                    .filter(log -> isTreeStartLog(world, log))
                    .forEach(nearbyTrees::add);
            targetTree = TaskUtils.getNearestPoint(villager.getBlockPos(), nearbyTrees);

            if (targetTree != null) {
                ItemStack stack = villager.getStackInHand(Hand.MAIN_HAND);
                BlockPos pos = targetTree;
                BlockState state;
                while ((state = world.getBlockState(pos)).isIn(BlockTags.LOGS)) {
                    targetTreeTicks += getTicksFor(state, 60) / stack.getMiningSpeedMultiplier(state);
                    pos = pos.add(0, 1, 0);
                }
            }
            return;
        }

        villager.moveTowards(targetTree);

        BlockState state = world.getBlockState(targetTree);
        if (state.isIn(BlockTags.LOGS)) {
            villager.swingHand(Hand.MAIN_HAND);
            chopTicks++;

            // cut down a tree every few seconds, dependent on config + the mining speed multiplier
            if (chopTicks >= targetTreeTicks) {
                chopTicks = 0;

                destroyTree(world, targetTree);
            }
        } else {
            targetTree = null;
            targetTreeTicks = 0;
        }
        super.keepRunning(world, villager, time);
    }

    /**
     * Returns trues if origin is bottom point of tree.
     */
    private boolean isTreeStartLog(ServerWorld world, BlockPos origin) {
        if (!world.getBlockState(origin).isIn(BlockTags.LOGS))
            return false;

        // ensure we're looking at a valid tree before continuing
        if (!isValidTree(world, origin.down()))
            return false;

        // check upside continues and valid leaves exist.
        BlockPos.Mutable pos_up = origin.mutableCopy(); // copy as mutable for reduce resources
        for (int y = 0; y < Config.getInstance().maxTreeHeight; y++) {
            BlockState up = world.getBlockState(pos_up.setY(pos_up.getY() + 1)); // use set directly instead of "pos_up.move(Direction.UP)" (set is faster)
            if (up.isIn(BlockTags.LOGS)) continue;
            else return up.isIn(BlockTags.LEAVES);
        }
        return false;
    }

    private void destroyTree(ServerWorld world, BlockPos origin) {
        ItemStack stack = villager.getStackInHand(Hand.MAIN_HAND);
        BlockPos pos = origin;
        BlockState state;

        while ((state = world.getBlockState(pos)).isIn(BlockTags.LOGS)) {
            world.breakBlock(pos, false, villager);
            pos = pos.add(0, 1, 0);
            villager.getInventory().addStack(new ItemStack(state.getBlock(), 1));
            stack.damage(1, villager, player -> player.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        }
    }

    private boolean isValidTree(ServerWorld world, Vec3d pos) {
        return isValidTree(world, new BlockPos(pos));
    }

    private boolean isValidTree(ServerWorld world, BlockPos pos) {
        // Similar logic to WanderOrTeleportToTargetTask#isAreaSafe
        final BlockState state = world.getBlockState(pos);
        final Identifier stateId = Registry.BLOCK.getId(state.getBlock());
        for (String blockId : Config.getInstance().validTreeSources) {
            if (blockId.equals(stateId.toString())) {
                return true;
            } else if (blockId.charAt(0) == '#') {
                Identifier identifier = new Identifier(blockId.substring(1));
                TagKey<Block> tag = TagKey.of(Registry.BLOCK_KEY, identifier);
                if (tag != null && !RegistryHelper.isTagEmpty(tag)) {
                    if (state.isIn(tag)) {
                        return true;
                    }
                } else {
                    throw new JsonSyntaxException("Unknown block tag in validTreeSources '" + identifier + "'");
                }
            }
        }
        return false;
    }

    private int getTicksFor(BlockState state, int fallback) {
        // Similar logic to WanderOrTeleportToTargetTask#isAreaSafe
        final Map<String, Integer> sources = Config.getInstance().maxTreeTicks;
        final Identifier stateId = Registry.BLOCK.getId(state.getBlock());
        for (String blockId : sources.keySet()) {
            if (blockId.equals(stateId.toString())) {
                return sources.get(blockId);
            } else if (blockId.charAt(0) == '#') {
                Identifier identifier = new Identifier(blockId.substring(1));
                TagKey<Block> tag = TagKey.of(Registry.BLOCK_KEY, identifier);
                if (tag != null && !RegistryHelper.isTagEmpty(tag)) {
                    if (state.isIn(tag)) {
                        return sources.get(blockId);
                    }
                } else {
                    throw new JsonSyntaxException("Unknown block tag in maxTreeTicks '" + identifier + "'");
                }
            }
        }
        return fallback;
    }
}

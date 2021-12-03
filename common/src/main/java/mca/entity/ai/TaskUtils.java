package mca.entity.ai;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface TaskUtils {
    /**
     * Finds a y position given an x,y,z coordinate that is assumed to be the world's "ground".
     *
     * @param world The world in which blocks will be tested
     * @param x     X coordinate
     * @param y     Y coordinate, used as the starting height for finding ground.
     * @param z     Z coordinate
     * @return Integer representing the air block above the first non-air block given the provided ordered triples.
     */
    static int getSpawnSafeTopLevel(World world, int x, int y, int z) {
        BlockPos.Mutable pos = new BlockPos.Mutable(x, Math.min(y, world.getTopY()), z);
        while (world.isAir(pos.move(Direction.DOWN)) && pos.getY() > world.getBottomY()) {}

        return pos.getY() + 1;
    }

    static List<BlockPos> getNearbyBlocks(BlockPos origin, World world, @Nullable Predicate<BlockState> filter, int xzDist, int yDist) {
        final List<BlockPos> pointsList = new ArrayList<>();
        for (int x = -xzDist; x <= xzDist; x++) {
            for (int y = -yDist; y <= yDist; y++) {
                for (int z = -xzDist; z <= xzDist; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        BlockPos pos = new BlockPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                        if (filter != null && filter.test(world.getBlockState(pos))) {
                            pointsList.add(pos);
                        } else if (filter == null) {
                            pointsList.add(pos);
                        }
                    }
                }
            }
        }
        return pointsList;
    }

    static BlockPos getNearestPoint(BlockPos origin, List<BlockPos> blocks) {
        double closest = 10000.0D;
        BlockPos returnPoint = null;
        for (BlockPos point : blocks) {
            double distance = origin.getSquaredDistance(point.getX(), point.getY(), point.getZ(), true);
            if (distance < closest) {
                closest = distance;
                returnPoint = point;
            }
        }

        return returnPoint;
    }
}

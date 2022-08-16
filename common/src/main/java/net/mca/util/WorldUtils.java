package net.mca.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList;
import net.minecraft.world.PersistentState;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface WorldUtils {
    static List<Entity> getCloseEntities(World world, Entity e) {
        return getCloseEntities(world, e, 256.0);
    }

    static List<Entity> getCloseEntities(World world, Entity e, double range) {
        Vec3d pos = e.getPos();
        return world.getOtherEntities(e, new Box(pos, pos).expand(range));
    }

    static <T extends Entity> List<T> getCloseEntities(World world, Entity e, Class<T> c) {
        return getCloseEntities(world, e, 256.0, c);
    }

    static <T extends Entity> List<T> getCloseEntities(World world, Entity e, double range, Class<T> c) {
        return getCloseEntities(world, e.getPos(), range, c);
    }

    static <T extends Entity> List<T> getCloseEntities(World world, Vec3d pos, double range, Class<T> c) {
        return world.getNonSpectatingEntities(c, new Box(pos, pos).expand(range));
    }

    static <T extends PersistentState> T loadData(ServerWorld world, Function<NbtCompound, T> loader, Function<ServerWorld, T> factory, String dataId) {
        return world.getPersistentStateManager().getOrCreate(loader, () -> factory.apply(world), dataId);
    }

    static void spawnEntity(World world, MobEntity entity, SpawnReason reason) {
        entity.initialize((ServerWorldAccess) world, world.getLocalDifficulty(entity.getBlockPos()), reason, null, null);
        world.spawnEntity(entity);
    }

    //a wrapper for the unnecessary complex query provided by minecraft
    static Optional<BlockPos> getClosestStructurePosition(ServerWorld world, BlockPos center, Identifier structure, int radius) {
        Registry<ConfiguredStructureFeature<?, ?>> registry = world.getRegistryManager().get(Registry.CONFIGURED_STRUCTURE_FEATURE_KEY);
        ConfiguredStructureFeature<?, ?> feature = registry.get(structure);
        Optional<RegistryEntry<ConfiguredStructureFeature<?, ?>>> entry = registry.getEntry(registry.getRawId(feature));
        if (entry.isPresent()) {
            RegistryEntryList.Direct<ConfiguredStructureFeature<?, ?>> of = RegistryEntryList.of(entry.get());
            Pair<BlockPos, RegistryEntry<ConfiguredStructureFeature<?, ?>>> pair = world.getChunkManager().getChunkGenerator().locateStructure(world, of, center, radius, false);
            return pair == null ? Optional.empty() : Optional.ofNullable(pair.getFirst());
        } else {
            return Optional.empty();
        }
    }
}

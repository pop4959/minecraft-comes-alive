package mca.entity;

import java.util.Optional;
import java.util.OptionalInt;

import mca.api.API;
import mca.core.MCA;
import mca.enums.Gender;
import mca.util.WorldUtils;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;

public class VillagerFactory {
    private final World world;

    private Optional<String> name = Optional.empty();
    private Optional<Gender> gender = Optional.empty();
    private Optional<VillagerProfession> profession = Optional.empty();
    private OptionalInt level = OptionalInt.empty();
    private OptionalInt age = OptionalInt.empty();
    private Optional<Vec3d> position = Optional.empty();

    private VillagerFactory(World world) {
        this.world = world;
    }

    public static VillagerFactory newVillager(World world) {
        return new VillagerFactory(world);
    }

    public VillagerFactory withGender(Gender gender) {
        this.gender = Optional.ofNullable(gender);
        return this;
    }

    public VillagerFactory withProfession(VillagerProfession prof) {
        this.profession = Optional.ofNullable(prof);
        return this;
    }

    public VillagerFactory withProfession(VillagerProfession prof, int level) {
        withProfession(prof);
        this.level = OptionalInt.of(level);
        return this;
    }

    public VillagerFactory withName(String name) {
        this.name = Optional.ofNullable(name);
        return this;
    }

    public VillagerFactory withPosition(double x, double y, double z) {
        return withPosition(new Vec3d(x, y, z));
    }

    public VillagerFactory withPosition(Entity entity) {
        return withPosition(entity.getX(), entity.getY(), entity.getZ());
    }

    public VillagerFactory withPosition(Vec3d pos) {
        position = Optional.of(pos);
        return this;
    }

    public VillagerFactory withPosition(BlockPos pos) {
        return withPosition(Vec3d.ofBottomCenter(pos.up()));
    }

    public VillagerFactory withAge(int age) {
        this.age = OptionalInt.of(age);
        return this;
    }

    public VillagerFactory spawn() {
        if (position.isEmpty()) {
            MCA.logger.info("Attempted to spawn villager without a position being set!");
        }

        WorldUtils.spawnEntity(world, build());
        return this;
    }

    public VillagerEntityMCA build() {
        VillagerEntityMCA villager = new VillagerEntityMCA(world);
        villager.getGenetics().setGender(gender.orElseGet(Gender::getRandom));
        villager.villagerName.set(name.orElseGet(() -> API.getRandomName(villager.getGenetics().getGender())));
        villager.setBreedingAge(age.orElseGet(() -> villager.getRandom().nextInt(24000 * 2) - 24000));
        position.ifPresent(pos -> villager.updatePosition(pos.getX(), pos.getY(), pos.getZ()));
        VillagerData data = villager.getVillagerData();
        villager.setVillagerData(new VillagerData(
                data.getType(),
                profession.orElseGet(API::randomProfession),
                level.orElseGet(data::getLevel)
            )
        );
        return villager;
    }
}
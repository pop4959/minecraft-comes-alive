package mca.entity.ai.relationship;

import mca.Config;
import mca.entity.EntitiesMCA;
import mca.entity.VillagerEntityMCA;
import mca.entity.ZombieVillagerEntityMCA;
import net.minecraft.entity.EntityType;

import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Gender {
    UNASSIGNED(0xFFFFFF),
    MALE(0x01A6EA),
    FEMALE(0xA649A4),
    NEUTRAL(0xFFFFFF);

    private static final Random RNG = new Random();
    private static final Gender[] VALUES = values();
    private static final Map<String, Gender> REGISTRY = Stream.of(VALUES).collect(Collectors.toMap(Gender::name, Function.identity()));

    private final int color;

    Gender(int color) {
        this.color = color;
    }

    public EntityType<VillagerEntityMCA> getVillagerType() {
        return this == FEMALE ? EntitiesMCA.FEMALE_VILLAGER.get() : EntitiesMCA.MALE_VILLAGER.get();
    }

    public EntityType<ZombieVillagerEntityMCA> getZombieType() {
        return this == FEMALE ? EntitiesMCA.FEMALE_ZOMBIE_VILLAGER.get() : EntitiesMCA.MALE_ZOMBIE_VILLAGER.get();
    }

    public int getColor() {
        return color;
    }

    public int getId() {
        return ordinal();
    }

    public String getStrName() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    public boolean isNonBinary() {
        return this == NEUTRAL || this == UNASSIGNED;
    }

    public Stream<Gender> getTransients() {
        return isNonBinary() ? Stream.of(MALE, FEMALE) : Stream.of(this);
    }

    public Gender binary() {
        return this == FEMALE ? FEMALE : MALE;
    }

    public Gender opposite() {
        return this == FEMALE ? MALE : FEMALE;
    }

    /**
     * Checks whether this gender is attracted to another.
     */
    public boolean isAttractedTo(Gender other) {
        return other == UNASSIGNED || this == NEUTRAL || other != this;
    }

    /**
     * Checks whether both genders are mutually attracted to each other.
     */
    public boolean isMutuallyAttracted(Gender other) {
        return isAttractedTo(other) && other.isAttractedTo(this);
    }

    public static Gender byId(int id) {
        if (id < 0 || id >= VALUES.length) {
            return UNASSIGNED;
        }
        return VALUES[id];
    }

    public static Gender getRandom() {
        return RNG.nextBoolean() ? MALE : FEMALE;
    }

    public static Gender byName(String name) {
        return REGISTRY.getOrDefault(name.toUpperCase(Locale.ENGLISH), UNASSIGNED);
    }

    public float getHorizontalScaleFactor() {
        return this == Gender.FEMALE ? Config.getInstance().femaleVillagerWidthFactor :
                this == Gender.MALE ? Config.getInstance().maleVillagerWidthFactor :
                        (Config.getInstance().femaleVillagerWidthFactor + Config.getInstance().maleVillagerWidthFactor) * 0.5f;
    }

    public float getScaleFactor() {
        return this == Gender.FEMALE ? Config.getInstance().femaleVillagerHeightFactor :
                this == Gender.MALE ? Config.getInstance().maleVillagerHeightFactor :
                        (Config.getInstance().femaleVillagerHeightFactor + Config.getInstance().maleVillagerHeightFactor) * 0.5f;
    }
}


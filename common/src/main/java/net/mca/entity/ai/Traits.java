package net.mca.entity.ai;

import net.mca.Config;
import net.mca.entity.VillagerLike;
import net.mca.util.network.datasync.CDataManager;
import net.mca.util.network.datasync.CDataParameter;
import net.mca.util.network.datasync.CParameter;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;

import java.util.*;
import java.util.stream.Collectors;

public class Traits {
    private static final CDataParameter<NbtCompound> TRAITS = CParameter.create("traits", new NbtCompound());

    public static final List<Trait> TRAIT_LIST = new ArrayList<>();

    public static Trait LEFT_HANDED = registerTrait("left_handed", 1.0F, 0.5F, false);
    public static Trait COLOR_BLIND = registerTrait("color_blind", 1.0F, 0.5F);
    public static Trait HETEROCHROMIA = registerTrait("heterochromia", 1.0F, 0.5F);
    public static Trait LACTOSE_INTOLERANCE = registerTrait("lactose_intolerance", 1.0F, 1.0F);
    public static Trait COELIAC_DISEASE = registerTrait("coeliac_disease", 1.0F, 1.0F, false); // TODO: Implement for 7.4
    public static Trait DIABETES = registerTrait("diabetes", 1.0F, 1.0F, false); // TODO: Implement for 7.4
    public static Trait DWARFISM = registerTrait("dwarfism", 1.0F, 1.0F);
    public static Trait ALBINISM = registerTrait("albinism", 1.0F ,1.0F);
    public static Trait VEGETARIAN = registerTrait("vegetarian", 1.0F, 1.0F, false); // TODO: Implement for 7.4
    public static Trait BISEXUAL = registerTrait("bisexual", 1.0F, 0.0F);
    public static Trait HOMOSEXUAL = registerTrait("homosexual", 1.0F, 0.0F);
    public static Trait ELECTRIFIED = registerTrait("electrified", 0.0F, 0.0F, false);
    public static Trait SIRBEN = registerTrait("sirben", 0.025F, 1.0F, true);
    public static Trait RAINBOW = registerTrait("rainbow", 0.05F, 0.0F);

    public static Trait registerTrait(String id, float chance, float inherit, boolean usableOnPlayer) {
        Trait trait = new Trait(id, chance, inherit, usableOnPlayer);
        TRAIT_LIST.add(trait);
        return trait;
    }
    public static Trait registerTrait(String id, float chance, float inherit) {
        Trait trait = new Trait(id, chance, inherit);
        TRAIT_LIST.add(trait);
        return trait;
    }

    public static class Trait {
        private final String id;
        private final float chance;
        private final float inherit;
        private final boolean usableOnPlayer;

        Trait(String id, float chance, float inherit, boolean usableOnPlayer) {
            this.id = id;
            this.chance = chance;
            this.inherit = inherit;
            this.usableOnPlayer = usableOnPlayer;
        }

        Trait(String id, float chance, float inherit) {
            this(id, chance, inherit, true);
        }

        public String name() {
            return this.id;
        }

        public static List<Trait> values() {
            return TRAIT_LIST;
        }

        public static Trait valueOf(String id) {
            for (Trait t : TRAIT_LIST) {
                if (t.name().equals(id)) return t;
            }
            return null;
        }

        public Text getName() {
            return Text.translatable("trait." + name().toLowerCase(Locale.ENGLISH));
        }

        public Text getDescription() {
            return Text.translatable("traitDescription." + name().toLowerCase(Locale.ENGLISH));
        }

        public boolean isUsableOnPlayer() {
            return usableOnPlayer;
        }

        public boolean isEnabled() {
            return Config.getServerConfig().enabledTraits.get(name());
        }
    }

    public static <E extends Entity> CDataManager.Builder<E> createTrackedData(CDataManager.Builder<E> builder) {
        return builder.addAll(TRAITS);
    }

    private Random random = Random.create();

    private final VillagerLike<?> entity;

    public Traits(VillagerLike<?> entity) {
        this.entity = entity;
    }

    public Set<Trait> getTraits() {
        return entity.getTrackedValue(TRAITS).getKeys().stream().map(Trait::valueOf).collect(Collectors.toSet());
    }

    public Set<Trait> getInheritedTraits() {
        return getTraits().stream().filter(t -> random.nextFloat() < t.inherit * Config.getInstance().traitInheritChance).collect(Collectors.toSet());
    }

    public boolean hasTrait(VillagerLike<?> target, Trait trait) {
        return target.getTrackedValue(TRAITS).contains(trait.name());
    }

    public boolean hasTrait(Trait trait) {
        return hasTrait(entity, trait);
    }

    public boolean hasTrait(String trait) {
        if (Trait.valueOf(trait) != null ) {
            return hasTrait(entity, Trait.valueOf(trait));
        }
        return false;
    }

    public boolean eitherHaveTrait(Trait trait, VillagerLike<?> other) {
        return hasTrait(entity, trait) || hasTrait(other, trait);
    }

    public boolean hasSameTrait(Trait trait, VillagerLike<?> other) {
        return hasTrait(entity, trait) && hasTrait(other, trait);
    }

    public void addTrait(Trait trait) {
        NbtCompound traits = entity.getTrackedValue(TRAITS).copy();
        traits.putBoolean(trait.name(), true);
        entity.setTrackedValue(TRAITS, traits);
    }

    public void removeTrait(Trait trait) {
        NbtCompound traits = entity.getTrackedValue(TRAITS).copy();
        traits.remove(trait.name());
        entity.setTrackedValue(TRAITS, traits);
    }

    //initializes the genes with random numbers
    public void randomize() {
        float total = (float)Trait.values().stream().mapToDouble(tr -> tr.chance).sum();
        for (Trait t : Trait.values()) {
            float chance = Config.getInstance().traitChance / total * t.chance;
            if (random.nextFloat() < chance && t.isEnabled()) {
                addTrait(t);
            }
        }
    }

    public void inherit(Traits from) {
        for (Trait t : from.getInheritedTraits()) {
            addTrait(t);
        }
    }

    public void inherit(Traits from, long seed) {
        Random old = random;
        random = Random.create(seed);
        inherit(from);
        random = old;
    }

    public float getVerticalScaleFactor() {
        return hasTrait(Traits.DWARFISM) ? 0.65f : 1.0f;
    }

    public float getHorizontalScaleFactor() {
        return hasTrait(Traits.DWARFISM) ? 0.85f : 1.0f;
    }
}

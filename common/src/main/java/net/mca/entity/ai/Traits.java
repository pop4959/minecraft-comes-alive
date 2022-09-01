package net.mca.entity.ai;

import net.mca.Config;
import net.mca.entity.VillagerLike;
import net.mca.util.network.datasync.CDataManager;
import net.mca.util.network.datasync.CDataParameter;
import net.mca.util.network.datasync.CParameter;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class Traits {
    private static final CDataParameter<NbtCompound> TRAITS = CParameter.create("traits", new NbtCompound());

    public enum Trait {
        LEFT_HANDED(1.0f, 0.5f, false),
        COLOR_BLIND(1.0f, 0.5f),
        HETEROCHROMIA(1.0f, 2.0f),
        LACTOSE_INTOLERANCE(1.0f, 1.0f),
        COELIAC_DISEASE(1.0f, 1.0f, false), // TODO: Implement for 7.3.21
        DIABETES(1.0f, 1.0f, false), // TODO: Implement for 7.3.21
        DWARFISM(1.0f, 1.0f),
        ALBINISM(1.0f, 1.0f),
        VEGETARIAN(1.0f, 0.0f, false), // TODO: Implement for 7.3.21
        BISEXUAL(1.0f, 0.0f),
        HOMOSEXUAL(1.0f, 0.0f),
        ELECTRIFIED(0.0f, 0.0f, false),
        SIRBEN(0.025f, 1.0f, false),
        RAINBOW(0.05f, 0.0f);

        private final float chance;
        private final float inherit;
        private final boolean usableOnPlayer;

        Trait(float chance, float inherit, boolean usableOnPlayer) {
            this.chance = chance;
            this.inherit = inherit;
            this.usableOnPlayer = usableOnPlayer;
        }

        Trait(float chance, float inherit) {
            this(chance, inherit, true);
        }

        public Text getName() {
            return new TranslatableText("trait." + name().toLowerCase(Locale.ENGLISH));
        }

        public Text getDescription() {
            return new TranslatableText("traitDescription." + name().toLowerCase(Locale.ENGLISH));
        }

        public boolean isUsableOnPlayer() {
            return usableOnPlayer;
        }
    }

    public static <E extends Entity> CDataManager.Builder<E> createTrackedData(CDataManager.Builder<E> builder) {
        return builder.addAll(TRAITS);
    }

    private Random random;

    private final VillagerLike<?> entity;

    public Traits(VillagerLike<?> entity) {
        this.entity = entity;
        random = new Random(entity.asEntity().world.random.nextLong());
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
        return hasTrait(entity, Trait.valueOf(trait.toUpperCase()));
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
        float total = (float)Arrays.stream(Trait.values()).mapToDouble(tr -> tr.chance).sum();
        for (Trait t : Trait.values()) {
            float chance = Config.getInstance().traitChance / total * t.chance;
            if (random.nextFloat() < chance) {
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
        random = new Random(seed);
        inherit(from);
        random = old;
    }

    public float getVerticalScaleFactor() {
        return hasTrait(Trait.DWARFISM) ? 0.65f : 1.0f;
    }

    public float getHorizontalScaleFactor() {
        return hasTrait(Trait.DWARFISM) ? 0.85f : 1.0f;
    }
}

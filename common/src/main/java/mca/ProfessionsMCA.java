package mca;

import com.google.common.collect.ImmutableSet;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mca.entity.ai.PointOfInterestTypeMCA;
import mca.mixin.MixinVillagerProfession;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public interface ProfessionsMCA {
    DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(MCA.MOD_ID, Registry.VILLAGER_PROFESSION_KEY);

    RegistrySupplier<VillagerProfession> OUTLAW = register("outlaw", false, true, PointOfInterestType.NONE, PointOfInterestType.NONE, SoundEvents.ENTITY_VILLAGER_WORK_FARMER);
    RegistrySupplier<VillagerProfession> GUARD = register("guard", false, true, PointOfInterestType.NONE, PointOfInterestType.NONE, SoundEvents.ENTITY_VILLAGER_WORK_ARMORER);
    RegistrySupplier<VillagerProfession> ARCHER = register("archer", true, true, PointOfInterestType.NONE, PointOfInterestType.NONE, SoundEvents.ENTITY_VILLAGER_WORK_FLETCHER);
    RegistrySupplier<VillagerProfession> ADVENTURER = register("adventurer", true, true, PointOfInterestType.NONE, PointOfInterestType.NONE, SoundEvents.ENTITY_VILLAGER_WORK_FLETCHER);
    // VillagerProfession JEWELER = register("jeweler", PointOfInterestTypeMCA.JEWELER, SoundEvents.ENTITY_VILLAGER_WORK_ARMORER);

    Set<VillagerProfession> canNotTrade = new HashSet<>();
    Set<VillagerProfession> isImportant = new HashSet<>();

    static void bootstrap() {
        PROFESSIONS.register();
        PointOfInterestTypeMCA.bootstrap();

        canNotTrade.add(VillagerProfession.NONE);
        canNotTrade.add(VillagerProfession.NITWIT);

        TradeOffersMCA.bootstrap();
    }

    private static RegistrySupplier<VillagerProfession> register(String name, boolean canTradeWith, boolean important, RegistryKey<PointOfInterestType> heldWorkstation, @Nullable SoundEvent workSound) {
        return register(name, canTradeWith, important, (entry) -> {
            return entry.matchesKey(heldWorkstation);
        }, (entry) -> {
            return entry.matchesKey(heldWorkstation);
        }, workSound);
    }

    static RegistrySupplier<VillagerProfession> register(String name, boolean canTradeWith, boolean important, Predicate<RegistryEntry<PointOfInterestType>> heldWorkstation, Predicate<RegistryEntry<PointOfInterestType>> acquirableWorkstation, @Nullable SoundEvent workSound) {
        return register(name, canTradeWith, important, heldWorkstation, acquirableWorkstation, ImmutableSet.of(), ImmutableSet.of(), workSound);
    }

    static RegistrySupplier<VillagerProfession> register(String name, boolean canTradeWith, boolean important, RegistryKey<PointOfInterestType> heldWorkstation, ImmutableSet<Item> gatherableItems, ImmutableSet<Block> secondaryJobSites, @Nullable SoundEvent workSound) {
        return register(name, canTradeWith, important, (entry) -> {
            return entry.matchesKey(heldWorkstation);
        }, (entry) -> {
            return entry.matchesKey(heldWorkstation);
        }, gatherableItems, secondaryJobSites, workSound);
    }

    static RegistrySupplier<VillagerProfession> register(String name, boolean canTradeWith, boolean important, Predicate<RegistryEntry<PointOfInterestType>> heldWorkstation, Predicate<RegistryEntry<PointOfInterestType>> acquirableWorkstation, ImmutableSet<Item> gatherableItems, ImmutableSet<Block> secondaryJobSites, @Nullable SoundEvent workSound) {
        Identifier id = new Identifier(MCA.MOD_ID, name);
        return PROFESSIONS.register(id, () -> {
            VillagerProfession result = MixinVillagerProfession.init(
                    id.toString().replace(':', '.'), heldWorkstation, acquirableWorkstation, gatherableItems, secondaryJobSites, workSound
            );
            if (!canTradeWith) {
                canNotTrade.add(result);
            }
            if (important) {
                isImportant.add(result);
            }
            return result;
        });
    }

    static String getFavoredBuilding(VillagerProfession profession) {
        if (VillagerProfession.CARTOGRAPHER == profession || VillagerProfession.LIBRARIAN == profession || VillagerProfession.CLERIC == profession) {
            return "library";
        } else if (GUARD.get() == profession || ARCHER.get() == profession) {
            return "inn";
        }
        return null;
    }
}

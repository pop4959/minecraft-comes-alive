package mca.resources;

import com.google.gson.JsonElement;
import mca.MCA;
import mca.entity.VillagerLike;
import mca.entity.ai.relationship.Gender;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClothingList extends JsonDataLoader {
    protected static final Identifier ID = MCA.locate("skins/clothing");

    private final WeightedPool.Mutable<Clothing> clothing = new WeightedPool.Mutable<>(new Clothing());

    private static ClothingList INSTANCE;

    public static ClothingList getInstance() {
        return INSTANCE;
    }

    public ClothingList() {
        super(Resources.GSON, "skins/clothing");
        INSTANCE = this;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> data, ResourceManager manager, Profiler profiler) {
        clothing.entries.clear();

        data.forEach((id, file) -> {
            Gender gender = Gender.byName(id.getPath().split("\\.")[0]);

            if (gender == Gender.UNASSIGNED) {
                MCA.LOGGER.warn("Invalid gender for clothing pool: {}", id);
                return;
            }

            // adds the skins to list
            JsonHelper.asObject(file, "root").getAsJsonObject().entrySet().forEach(entry -> {
                int count = JsonHelper.getInt(entry.getValue().getAsJsonObject(), "count");
                float chance = JsonHelper.getFloat(entry.getValue().getAsJsonObject(), "chance", 1.0f);
                if (count <= 0) {
                    return;
                }

                Identifier professionIdentifier = new Identifier(entry.getKey());
                VillagerProfession profession = Registry.VILLAGER_PROFESSION.get(professionIdentifier);

                for (int i = 0; i < count; i++) {
                    Identifier identifier = new Identifier(String.format("%s:%s/%s/%d.png", id.getNamespace(), gender.getStrName(), professionIdentifier.getPath(), i));
                    clothing.add(new Clothing(identifier, gender, profession), chance);
                }
            });
        });
    }

    /**
     * Gets a pool of clothing options valid for this entity's gender and profession.
     */
    public WeightedPool<Identifier> getPool(VillagerLike<?> villager) {
        Gender gender = villager.getGenetics().getGender();
        return switch (villager.getAgeState()) {
            case BABY, TODDLER -> getPool(gender, MCA.locate("baby"));
            case CHILD, TEEN -> getPool(gender, MCA.locate("child"));
            default -> getPool(gender, villager.getVillagerData().getProfession());
        };
    }

    public WeightedPool<Identifier> getPool(Gender gender, Identifier profession) {
        return getPool(gender, Registry.VILLAGER_PROFESSION.get(profession));
    }

    public WeightedPool<Identifier> getPool(Gender gender, @Nullable VillagerProfession profession) {
        return clothing.entries.stream()
                .filter(c -> c.getValue().gender == Gender.NEUTRAL || gender == Gender.NEUTRAL || c.getValue().gender == gender)
                .filter(c -> profession == null || c.getValue().profession == null || c.getValue().profession == profession)
                .collect(() -> new WeightedPool.Mutable<>(new Identifier("mca:missing")),
                        (list, entry) -> list.add(entry.getValue().identifier, entry.getWeight()),
                        (a, b) -> {
                            a.entries.addAll(b.entries);
                        });
    }

    public static class Clothing {
        Identifier identifier;
        Gender gender;

        @Nullable
        VillagerProfession profession;

        float temperatureMin;
        float temperatureMax;

        public Clothing() {
        }

        public Clothing(Identifier identifier, Gender gender, VillagerProfession profession) {
            this.identifier = identifier;
            this.gender = gender;
            this.profession = profession;
            this.temperatureMin = -100.0f;
            this.temperatureMax = 100.0f;
        }
    }
}

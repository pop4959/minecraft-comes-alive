package mca.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ClothingList extends JsonDataLoader {
    protected static final Identifier ID = MCA.locate("skins/clothing");

    public final HashMap<String, Clothing> clothing = new HashMap<>();

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
        clothing.clear();

        data.forEach((id, file) -> {
            Gender gender = Gender.byName(id.getPath().split("\\.")[0]);

            if (gender == Gender.UNASSIGNED) {
                MCA.LOGGER.warn("Invalid gender for clothing pool: {}", id);
                return;
            }

            for (String key : file.getAsJsonObject().keySet()) {
                JsonObject object = file.getAsJsonObject().get(key).getAsJsonObject();

                for (int i = 0; i < JsonHelper.getInt(object, "count", 1); i++) {
                    String identifier = String.format(key, i);

                    Clothing c = new Clothing(identifier);
                    c.gender = gender;
                    c.profession = JsonHelper.getString(object, "profession", null);
                    c.chance = JsonHelper.getFloat(object, "chance", 1.0f);
                    c.exclude = JsonHelper.getBoolean(object, "exclude", false);
                    c.temperature = JsonHelper.getInt(object, "temperature", 0);

                    if (!clothing.containsKey(identifier) || !object.has("count")) {
                        clothing.put(identifier, c);
                    }
                }
            }
        });
    }

    /**
     * Gets a pool of clothing options valid for this entity's gender and profession.
     */
    public WeightedPool<String> getPool(VillagerLike<?> villager) {
        Gender gender = villager.getGenetics().getGender();
        return switch (villager.getAgeState()) {
            case BABY, TODDLER -> getPool(gender, MCA.locate("baby").toString());
            case CHILD, TEEN -> getPool(gender, MCA.locate("child").toString());
            default -> {
                WeightedPool<String> pool = getPool(gender, villager.getVillagerData().getProfession());
                if (pool.entries.size() == 0) {
                    pool = getPool(gender, VillagerProfession.NONE);
                }
                yield pool;
            }
        };
    }

    public WeightedPool<String> getPool(Gender gender, @Nullable VillagerProfession profession) {
        return getPool(gender, profession == null ? "minecraft:none" : Registry.VILLAGER_PROFESSION.getId(profession).toString());
    }

    public WeightedPool<String> getPool(Gender gender, @Nullable String profession) {
        return clothing.values().stream()
                .filter(c -> c.gender == Gender.NEUTRAL || gender == Gender.NEUTRAL || c.gender == gender)
                .filter(c -> c.profession == null || profession == null && !c.exclude || c.profession.equals(profession))
                .collect(() -> new WeightedPool.Mutable<>("mca:missing"),
                        (list, entry) -> list.add(entry.identifier, entry.chance),
                        (a, b) -> {
                            a.entries.addAll(b.entries);
                        });
    }

    public static class Clothing extends ListEntry {
        @Nullable
        public String profession;
        public float temperature;
        public boolean exclude;

        public Clothing(String identifier) {
            super(identifier);
        }
    }

    public static class ListEntry implements Serializable {
        final String identifier;
        public Gender gender;
        public float chance;

        public ListEntry(String identifier) {
            this.identifier = identifier;
        }
    }
}

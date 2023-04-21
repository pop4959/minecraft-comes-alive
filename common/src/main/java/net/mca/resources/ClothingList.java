package net.mca.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.mca.Config;
import net.mca.MCA;
import net.mca.entity.VillagerLike;
import net.mca.entity.ai.relationship.Gender;
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

                    Clothing c = new Clothing(identifier, object);

                    c.gender = gender;

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
            case BABY -> getPool(gender, MCA.locate("baby").toString());
            case TODDLER -> getPool(gender, MCA.locate("toddler").toString());
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
        Map<String, String> map = Config.getInstance().professionConversionsMap;
        String currentValue = profession == null ? "minecraft:none" : Registry.VILLAGER_PROFESSION.getId(profession).toString();
        String identifier = map.getOrDefault(currentValue, map.getOrDefault("default", currentValue));
        return getPool(gender, identifier);
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
        public int temperature;
        public boolean exclude;

        public Clothing(String identifier) {
            super(identifier);
        }

        public Clothing(String identifier, JsonObject object) {
            super(identifier, object);

            this.profession = JsonHelper.getString(object, "profession", null);
            this.exclude = JsonHelper.getBoolean(object, "exclude", false);
            this.temperature = JsonHelper.getInt(object, "temperature", 0);
        }

        @Override
        public JsonObject toJson() {
            JsonObject j = super.toJson();
            j.addProperty("profession", profession);
            j.addProperty("exclude", exclude);
            j.addProperty("temperature", temperature);
            return j;
        }
    }

    public static abstract class ListEntry implements Serializable {
        public final String identifier;
        public Gender gender;
        public float chance;

        public ListEntry(String identifier) {
            this.identifier = identifier;
        }

        public ListEntry(String identifier, JsonObject object) {
            this.identifier = identifier;

            this.gender = Gender.byId(JsonHelper.getInt(object, "gender", 0));
            this.chance = JsonHelper.getFloat(object, "chance", 1.0f);
        }

        public String getPath() {
            return (new Identifier(this.identifier)).getPath();
        }

        public JsonObject toJson() {
            JsonObject j = new JsonObject();
            j.addProperty("gender", gender.getId());
            j.addProperty("chance", chance);
            return j;
        }
    }
}

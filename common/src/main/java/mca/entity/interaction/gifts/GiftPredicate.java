package mca.entity.interaction.gifts;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import mca.entity.VillagerEntityMCA;
import mca.entity.ai.Chore;
import mca.entity.ai.LongTermMemory;
import mca.entity.ai.MoodGroup;
import mca.entity.ai.Traits;
import mca.entity.ai.relationship.AgeState;
import mca.entity.ai.relationship.Gender;
import mca.entity.ai.relationship.Personality;
import mca.resources.Rank;
import mca.resources.Tasks;
import net.minecraft.advancement.Advancement;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;

public class GiftPredicate {
    public static final Map<String, Factory<JsonElement>> CONDITION_TYPES = new HashMap<>();

    public static float divideAndAdd(JsonObject json, long value) {
        return MathHelper.clamp(
                value
                        / (json.has("dividend") ? json.get("dividend").getAsFloat() : 1.0f)
                        + (json.has("add") ? json.get("add").getAsFloat() : 0.0f),
                0.0f,
                json.has("max") ? json.get("max").getAsInt() : 1.0f
        );
    }

    static {
        register("profession", (json, name) -> new Identifier(JsonHelper.asString(json, name)), profession -> {
            return (villager, stack, player) -> Registry.VILLAGER_PROFESSION.getId(villager.getProfession()).equals(profession) ? 1.0f : 0.0f;
        });
        register("age_group", (json, name) -> AgeState.valueOf(JsonHelper.asString(json, name).toUpperCase(Locale.ENGLISH)), group -> {
            return (villager, stack, player) -> villager.getAgeState() == group ? 1.0f : 0.0f;
        });
        register("gender", (json, name) -> Gender.valueOf(JsonHelper.asString(json, name).toUpperCase(Locale.ENGLISH)), gender -> {
            return (villager, stack, player) -> villager.getGenetics().getGender() == gender ? 1.0f : 0.0f;
        });
        register("has_item", (json, name) -> Ingredient.fromJson(json), item -> {
            return (villager, stack, player) -> {
                for (int i = 0; i < villager.getInventory().size(); i++) {
                    if (item.test(villager.getInventory().getStack(i))) {
                        return 1.0f;
                    }
                }
                return 0.0f;
            };
        });
        register("min_health", JsonHelper::asFloat, health -> {
            return (villager, stack, player) -> villager.getHealth() > health ? 1.0f : 0.0f;
        });
        register("is_married", JsonHelper::asBoolean, married -> {
            return (villager, stack, player) -> villager.getRelationships().isMarried() == married ? 1.0f : 0.0f;
        });
        register("has_home", JsonHelper::asBoolean, hasHome -> {
            return (villager, stack, player) -> villager.getResidency().getHome().isPresent() == hasHome ? 1.0f : 0.0f;
        });
        register("has_village", JsonHelper::asBoolean, hasVillage -> {
            return (villager, stack, player) -> villager.getResidency().getHomeVillage().isPresent() == hasVillage ? 1.0f : 0.0f;
        });
        register("min_infection_progress", JsonHelper::asFloat, progress -> {
            return (villager, stack, player) -> villager.getInfectionProgress() > progress ? 1.0f : 0.0f;
        });
        register("mood", (json, name) -> JsonHelper.asString(json, name).toLowerCase(Locale.ENGLISH), mood -> {
            return (villager, stack, player) -> villager.getVillagerBrain().getMood().getName().equals(mood) ? 1.0f : 0.0f;
        });
        register("mood_group", (json, name) -> MoodGroup.valueOf(JsonHelper.asString(json, name).toUpperCase(Locale.ENGLISH)), moodGroup -> {
            return (villager, stack, player) -> villager.getVillagerBrain().getPersonality().getMoodGroup() == moodGroup ? 1.0f : 0.0f;
        });
        register("personality", (json, name) -> Personality.valueOf(JsonHelper.asString(json, name).toUpperCase(Locale.ENGLISH)), personality -> {
            return (villager, stack, player) -> villager.getVillagerBrain().getPersonality() == personality ? 1.0f : 0.0f;
        });
        register("is_pregnant", JsonHelper::asBoolean, pregnant -> {
            return (villager, stack, player) -> villager.getRelationships().getPregnancy().isPregnant() == pregnant ? 1.0f : 0.0f;
        });
        register("min_pregnancy_progress", JsonHelper::asInt, progress -> {
            return (villager, stack, player) -> villager.getRelationships().getPregnancy().getBabyAge() > progress ? 1.0f : 0.0f;
        });
        register("pregnancy_child_gender", (json, name) -> Gender.valueOf(JsonHelper.asString(json, name).toUpperCase(Locale.ENGLISH)), gender -> {
            return (villager, stack, player) -> villager.getRelationships().getPregnancy().getGender() == gender ? 1.0f : 0.0f;
        });
        register("current_chore", (json, name) -> Chore.valueOf(JsonHelper.asString(json, name).toUpperCase(Locale.ENGLISH)), chore -> {
            return (villager, stack, player) -> villager.getVillagerBrain().getCurrentJob() == chore ? 1.0f : 0.0f;
        });
        register("item", (json, name) -> {
            Identifier id = new Identifier(JsonHelper.asString(json, name));
            Item item = Registry.ITEM.getOrEmpty(id).orElseThrow(() -> new JsonSyntaxException("Unknown item '" + id + "'"));
            return Ingredient.ofStacks(new ItemStack(item));
        }, (Ingredient ingredient) -> {
            return (villager, stack, player) -> ingredient.test(stack) ? 1.0f : 0.0f;
        });
        register("tag", (json, name) -> {
            Identifier id = new Identifier(JsonHelper.asString(json, name));
            TagKey<Item> tag = TagKey.of(Registry.ITEM_KEY, id);
            if (tag == null) {
                throw new JsonSyntaxException("Unknown item tag '" + id + "'");
            }

            return Ingredient.fromTag(tag);
        }, (Ingredient ingredient) -> {
            return (villager, stack, player) -> ingredient.test(stack) ? 1.0f : 0.0f;
        });
        register("trait", (json, name) -> Traits.Trait.valueOf(JsonHelper.asString(json, name).toUpperCase(Locale.ENGLISH)), trait -> {
            return (villager, stack, player) -> villager.getTraits().hasTrait(trait) ? 1.0f : 0.0f;
        });
        register("hearts_min", JsonHelper::asInt, hearts -> {
            return (villager, stack, player) -> {
                assert player != null;
                int h = villager.getVillagerBrain().getMemoriesForPlayer(player).getHearts();
                return h >= hearts ? 1.0f : 0.0f;
            };
        });
        register("hearts_max", JsonHelper::asInt, hearts -> {
            return (villager, stack, player) -> {
                assert player != null;
                int h = villager.getVillagerBrain().getMemoriesForPlayer(player).getHearts();
                return h <= hearts ? 1.0f : 0.0f;
            };
        });
        register("hearts", JsonHelper::asObject, json -> {
            return (villager, stack, player) -> {
                assert player != null;
                int h = villager.getVillagerBrain().getMemoriesForPlayer(player).getHearts();
                return divideAndAdd(json, h);
            };
        });
        register("memory", JsonHelper::asObject, json -> {
            return (villager, stack, player) -> {
                String id = LongTermMemory.parseId(json, player);
                long ticks = villager.getLongTermMemory().getMemory(id);
                return divideAndAdd(json, ticks);
            };
        });
        register("village_has_building", JsonHelper::asString, name -> {
            return (villager, stack, player) -> {
                return villager.getResidency().getHomeVillage().filter(v -> v.hasBuilding(name)).isPresent() ? 1.0f : 0.0f;
            };
        });
        register("rank", JsonHelper::asString, name -> {
            return (villager, stack, player) -> {
                return villager.getResidency().getHomeVillage().filter(v -> Tasks.getRank(v, player) == Rank.fromName(name)).isPresent() ? 1.0f : 0.0f;
            };
        });
        register("time_min", JsonHelper::asLong, time -> {
            return (villager, stack, player) -> {
                return villager.getWorld().getTimeOfDay() % 24000L >= time ? 1.0f : 0.0f;
            };
        });
        register("time_max", JsonHelper::asLong, time -> {
            return (villager, stack, player) -> {
                return villager.getWorld().getTimeOfDay() % 24000L <= time ? 1.0f : 0.0f;
            };
        });
        register("biome", (json, name) -> new Identifier(JsonHelper.asString(json, name)), biome -> {
            return (villager, stack, player) -> {
                return villager.getWorld().getBiome(villager.getBlockPos()).getKeyOrValue().left().filter(b -> b.getValue().equals(biome)).isPresent() ? 1.0f : 0.0f;
            };
        });
        register("advancement", (json, name) -> new Identifier(JsonHelper.asString(json, name)), id -> {
            return (villager, stack, player) -> {
                assert player != null;
                Advancement advancement = Objects.requireNonNull(player.getServer()).getAdvancementLoader().get(id);
                return player.getAdvancementTracker().getProgress(advancement).isDone() ? 1.0f : 0.0f;
            };
        });
    }

    public static <T> void register(String name, BiFunction<JsonElement, String, T> jsonParser, Factory<T> predicate) {
        CONDITION_TYPES.put(name, json -> predicate.parse(jsonParser.apply(json, name)));
    }

    public static GiftPredicate fromJson(JsonObject json) {
        int satisfaction = 0;
        @Nullable
        Condition condition = null;
        List<String> conditionKeys = new LinkedList<>();

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if ("satisfaction_boost".equals(entry.getKey())) {
                satisfaction = JsonHelper.asInt(entry.getValue(), entry.getKey());
            } else if (CONDITION_TYPES.containsKey(entry.getKey())) {
                Condition parsed = CONDITION_TYPES.get(entry.getKey()).parse(entry.getValue());
                conditionKeys.add(entry.getKey());
                if (condition == null) {
                    condition = parsed;
                } else {
                    condition = condition.and(parsed);
                }
            }
        }

        return new GiftPredicate(satisfaction, condition, conditionKeys);
    }

    private final int satisfactionBoost;

    @Nullable
    private final Condition condition;
    List<String> conditionKeys;

    public GiftPredicate(int satisfactionBoost, @Nullable Condition condition, List<String> conditionKeys) {
        this.satisfactionBoost = satisfactionBoost;
        this.condition = condition;
        this.conditionKeys = conditionKeys;
    }

    public float test(VillagerEntityMCA recipient, ItemStack stack, @Nullable ServerPlayerEntity player) {
        return condition != null ? condition.test(recipient, stack, player) : 0.0f;
    }

    public int getSatisfactionFor(VillagerEntityMCA recipient, ItemStack stack, @Nullable ServerPlayerEntity player) {
        return (int)(test(recipient, stack, player) * satisfactionBoost);
    }

    public interface Factory<T> {
        Condition parse(T value);
    }

    public interface Condition {
        float test(VillagerEntityMCA villager, ItemStack stack, @Nullable ServerPlayerEntity player);

        default Condition and(Condition b) {
            final Condition a = this;
            return (villager, stack, player) -> {
                return a.test(villager, stack, player) * b.test(villager, stack, player);
            };
        }
    }

    public List<String> getConditionKeys() {
        return conditionKeys;
    }
}

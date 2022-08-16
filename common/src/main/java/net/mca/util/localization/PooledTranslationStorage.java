package net.mca.util.localization;

import net.mca.resources.PoolUtil;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class PooledTranslationStorage {
    private static final Pattern TRAILING_NUMBERS_PATTERN = Pattern.compile("/[0-9]+$");
    private static final Predicate<String> TRAILING_NUMBERS_PREDICATE = TRAILING_NUMBERS_PATTERN.asPredicate();

    private final Map<String, List<String>> multiTranslations = new HashMap<>();

    private final Random rand = Random.create();

    public PooledTranslationStorage(Map<String, String> translations) {
        translations.forEach(this::addTranslation);
    }

    private void addTranslation(String key, String value) {
        if (TRAILING_NUMBERS_PREDICATE.test(key)) {
            multiTranslations
                .computeIfAbsent(TRAILING_NUMBERS_PATTERN.matcher(key).replaceAll(""), k -> new ArrayList<>())
                .add(value);
        }
    }

    @NotNull
    private List<String> getOptions(String key) {
        return multiTranslations.getOrDefault(key, Collections.emptyList());
    }

    @Nullable
    public String get(String key) {
        List<String> options = getOptions(key);
        if (!options.isEmpty()) {
            return TemplateSet.INSTANCE.replace(PoolUtil.pickOne(options, key, rand));
        }
        return null;
    }

    public boolean contains(String key) {
        return !getOptions(key).isEmpty();
    }
}

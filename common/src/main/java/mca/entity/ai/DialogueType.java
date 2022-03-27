package mca.entity.ai;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import mca.entity.ai.relationship.AgeState;
import net.minecraft.util.Language;

public enum DialogueType {
    ADULT(null),
    UNASSIGNED(ADULT),
    BABY(UNASSIGNED),
    CHILD(ADULT),
    CHILDP(CHILD),
    TODDLER(CHILD),
    TODDLERP(CHILDP),
    SPOUSE(ADULT),
    TEEN(ADULT),
    TEENP(TEEN);

    public final DialogueType fallback;

    private static final Random random = new Random();

    DialogueType(DialogueType fallback) {
        this.fallback = fallback;
    }

    private static final DialogueType[] VALUES = values();

    public static final Map<String, DialogueType> MAP = Arrays.stream(VALUES).collect(Collectors.toMap(DialogueType::name, Function.identity()));

    public DialogueType toChild() {
        return switch (this) {
            case TODDLER -> TODDLERP;
            case CHILD -> CHILDP;
            case TEEN -> TEENP;
            default -> UNASSIGNED;
        };
    }

    public static DialogueType fromAge(AgeState state) {
        for (DialogueType t : values()) {
            if (t.name().equals(state.name())) {
                return t;
            }
        }
        return UNASSIGNED;
    }

    public static DialogueType byId(int id) {
        if (id < 0 || id >= VALUES.length) {
            return UNASSIGNED;
        }
        return VALUES[id];
    }

    public static String applyFallback(String key) {
        if (!key.contains("#")) {
            return key;
        }

        //extract flags
        Map<String, String> flags = new HashMap<>();
        for (String s : key.split("\\.")) {
            if (s.startsWith("#")) {
                flags.put(s.substring(1, 2), s.substring(2));
                key = key.replace(s + ".", "");
            }
        }

        //check for type
        DialogueType type = null;
        if (flags.containsKey("T")) {
            type = DialogueType.MAP.get(flags.get("T"));
        }
        if (type == null) {
            return key;
        }

        //first try professions
        //children can't have profession, this is already checked in the Messenger
        if (flags.containsKey("P") && random.nextBoolean()) {
            DialogueType t = type;
            while (t != null) {
                String s = flags.get("P") + "." + t.name().toLowerCase(Locale.ENGLISH) + "." + key;
                if (Language.getInstance().hasTranslation(s)) {
                    return s;
                }
                t = t.fallback;
            }
            String s = flags.get("P") + "." + key;
            if (Language.getInstance().hasTranslation(s)) {
                return s;
            }
        }

        //try all types
        DialogueType t = type;
        while (t != null) {
            String s = t.name().toLowerCase(Locale.ENGLISH) + "." + key;
            if (Language.getInstance().hasTranslation(s)) {
                return s;
            }
            t = t.fallback;
        }

        return key;
    }
}

package net.mca;

import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class MCA {
    public static final String MOD_ID = "mca";
    public static final Logger LOGGER = LogManager.getLogger();

    public static Identifier locate(String id) {
        return new Identifier(MOD_ID, id);
    }

    public static boolean isBlankString(String string) {
        return string == null || string.trim().isEmpty();
    }
}

package net.mca.entity.ai;

import net.minecraft.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InworldAI {

    /** 30 min timeout for sessionIds. Set to 28 mins for now */
    private static final int SESSION_TIMEOUT_SECONDS = 28 * 60;

    // We don't need conversational memory, Inworld does that for us

    /** Manages NPC UUID -> character resource name mappings */
    final static Map<UUID, String> managedCharacters = new HashMap<>();
    /** Holds session IDs and last interaction time for open conversations */
    final static Map<UUID, Pair<String, Long>> openConversations = new HashMap<>();
    /*
    public static GPT3.Answer {

    }*/

    /**
     * Adds a new mapping to {@link #managedCharacters the managed characters map}
     */
    public static void addManagedCharacter(String name, String resourceName) {
        GPT3.inConversationWith()
    }
}

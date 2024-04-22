package net.mca.entity.ai.chatAI;

import net.mca.entity.VillagerEntityMCA;
import net.mca.util.WorldUtils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.mca.entity.VillagerLike.VILLAGER_NAME;

public class ChatAI {

    /** Max range to find a villager in */
    private final static int VILLAGER_SEARCH_RANGE = 32;

    /** Max time until a conversation is considered invalid */
    private static final int CONVERSATION_TIME = 20 * 60;

    /** Max distance until a conversation is considered invalid */
    private static final int CONVERSATION_DISTANCE = 16;

    /** Map of villager UUIDs to strategies (i.e. managed by InworldAI or GPT3) */
    private static final Map<UUID, ChatAIStrategy> strategies = new ConcurrentHashMap<>();

    /**
     * Current conversation of player. <p>
     * A player can max. have 1 conversation at all times.
     */
    private static final Map<UUID, OpenConversation> currentConversations = new ConcurrentHashMap<>();


    /**
     * Gets an answer for a specific message for a villager from a player with the villager-specific chat strategy
     * @param player ServerPlayerEntity of the player
     * @param villager VillagerEntityMCA of the villager
     * @param msg Message in question
     * @return {@code Optional.EMPTY} if answer couldn't be generated, Optional containing answer String otherwise.
     */
    public static Optional<String> answer(ServerPlayerEntity player, VillagerEntityMCA villager, String msg) {
        // Get villager-specific strategy
        ChatAIStrategy strategy = getOrAddDefaultStrategy(villager.getUuid());

        // Update the current conversation
        long time = villager.getWorld().getTime();
        currentConversations.put(player.getUuid(), new OpenConversation(villager.getUuid(), time));

        // Get answer
        return strategy.answer(player, villager, msg);
    }


    /**
     * Adds a specific ChatAIStrategy to a nearby villager with specified name
     * @param player ServerPlayerEntity object of the player
     * @param villagerName Name of the villager
     * @param strategy Strategy to be used
     * @return {@code true} if villager was found nearby, {@code false} otherwise
     */
    public static boolean addChatAIStrategyToVillager(ServerPlayerEntity player, String villagerName, ChatAIStrategy strategy) {
        Optional<VillagerEntityMCA> villager = findVillagerInArea(player, villagerName);
        if (villager.isEmpty()) {
            return false;
        }
        strategies.put(villager.get().getUuid(), strategy);
        return true;
    }

    /**
     * Removes any ChatAIStrategy from a nearby villager with a specified name
     * @param player ServerPlayerEntity object of the player
     * @param villagerName Name of the villager
     * @return {@code true} if villager was found nearby, {@code false} otherwise
     */
    public static boolean removeChatAIStrategyFromVillager(ServerPlayerEntity player, String villagerName) {
        Optional<VillagerEntityMCA> villager = findVillagerInArea(player, villagerName);
        if (villager.isEmpty()) {
            return false;
        }
        strategies.remove(villager.get().getUuid());
        return true;
    }

    /**
     * Gets a strategy for a specific villager or adds {@link GPT3} if none is found
     * @param villagerID UUID of villager
     * @return Object implementing the ChatAIStrategy interface
     */
    private static ChatAIStrategy getOrAddDefaultStrategy(UUID villagerID) {
        return strategies.computeIfAbsent(villagerID, (v) -> new GPT3());
    }

    /**
     * Checks if the message contains the name of any specific villagers and that villager is nearby. First match.
     * If not, checks if the player has a valid active conversation with a nearby villager.
     * @param player The player in the conversation
     * @param msg The message
     * @return {@code Optional.Empty} if no valid villager was found, Optional containing the VillagerEntityMCA object otherwise
     */
    public static Optional<VillagerEntityMCA> getVillagerForConversation(ServerPlayerEntity player, String msg) {
        UUID playerUUID = player.getUuid();
        // Get nearby villagers
        List<VillagerEntityMCA> nearbyVillagers = WorldUtils.getCloseEntities(player.getWorld(), player, VILLAGER_SEARCH_RANGE, VillagerEntityMCA.class);

        // Find name in message
        String normalizedMsg = normalizeString(msg);
        for (VillagerEntityMCA villager : nearbyVillagers) {
            String normalizedName = normalizeString(villager.getTrackedValue(VILLAGER_NAME));
            // Return first match
            if (normalizedMsg.contains(normalizedName)) {
                return Optional.of(villager);
            }
        }

        // Otherwise get current open conversation of player
        OpenConversation conv = currentConversations.getOrDefault(playerUUID, new OpenConversation(playerUUID, 0L));

        // Find first nearby villager matching the UUID of the conversation
        Optional<VillagerEntityMCA> optionalVillager = nearbyVillagers.stream().filter((v) -> conv.villagerUUID.equals(v.getUuid())).findFirst();
        // Return if found
        if (optionalVillager.isPresent() && isInConversationWith(player, optionalVillager.get())) {
            return optionalVillager;
        }

        return Optional.empty();
    }

    /**
     * Checks if a player is in a conversation with a villager
     * @param player ServerPlayerEntity of the player to be checked
     * @param villager VillagerEntityMCA entity of the villager to be checked
     * @return {@code true} if all the following conditions are met: <p>
     *  1. Villager is within {@value CONVERSATION_DISTANCE} blocks of the player<p>
     *  2. Last conversation interaction with this villager wasn't longer than {@value CONVERSATION_TIME} ago
     */
    private static boolean isInConversationWith(ServerPlayerEntity player, VillagerEntityMCA villager) {
        OpenConversation conversation = currentConversations.getOrDefault(player.getUuid(), new OpenConversation(villager.getUuid(), 0L));
        return villager.distanceTo(player) < CONVERSATION_DISTANCE
                && villager.getWorld().getTime() < conversation.lastInteractionTime + CONVERSATION_TIME;
    }

    /**
     * Scans the local area in a {@value #VILLAGER_SEARCH_RANGE} block range of the player for a villager with searchName.
     * searchName is normalized
     * @param player ServerPlayerEntity object of the reference player
     * @param searchName Name of the villager
     * @return Optional containing the VillagerEntityMCA of the first villager with the matching name, empty Optional otherwise
     */
    public static Optional<VillagerEntityMCA> findVillagerInArea(ServerPlayerEntity player, String searchName) {
        List<VillagerEntityMCA> entities = WorldUtils.getCloseEntities(player.getWorld(), player, VILLAGER_SEARCH_RANGE, VillagerEntityMCA.class);

        // Get specific villager
        String normalizedSearchName = normalizeString(searchName);

        // Go through list, look for first match for name
        for (VillagerEntityMCA villager : entities) {
            String villagerName = normalizeString(villager.getTrackedValue(VILLAGER_NAME));
            if (normalizedSearchName.equals(villagerName)) {
                return Optional.of(villager);
            }
        }
        return Optional.empty();
    }

    /**
     * Normalizes the String according to NFD and removes any accents, umlauts etc. <p>
     * Copy of net.mca.mixin.MixinServerPlayNetworkHandler.normalize
     * @param string The String to be normalized
     * @see <a href="https://unicode.org/reports/tr15/#Examples">Unicode Normalization Forms</a>
     */
    private static String normalizeString(String string) {
        return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Information needed to manage an open conversation.
     * @param villagerUUID UUID of the villager the conversation is with
     * @param lastInteractionTime Timestamp of the last interaction with the villager
     */
    private record OpenConversation(UUID villagerUUID, Long lastInteractionTime) {}

}

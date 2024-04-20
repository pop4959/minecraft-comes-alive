package net.mca.entity.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.mca.Config;
import net.mca.MCA;
import net.mca.entity.VillagerEntityMCA;
import net.mca.mixin.MixinServerPlayNetworkHandler;
import net.mca.util.WorldUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.jmx.Server;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.mca.entity.VillagerLike.VILLAGER_NAME;

/**
 * TODO: Further improvements.
 * @author CSCMe
 */
public class InworldAI {

    private static String INWORLD_BASE_URL = "https://api.inworld.ai/v1/";

    // We don't need conversational memory, Inworld does that for us
    // We also don't need to track session timeouts, the response will provide us with a new session_id if the current one is invalid

    /** Manages NPC UUID -> character resource name mappings */
    final static Map<UUID, String> managedCharacters = new HashMap<>();
    /** Holds session IDs for open conversations */
    final static Map<UUID, String> openConversations = new HashMap<>();

    /**
     * Record to hold response data.
     * @param sessionId name does not match API documentation name session_id. The actual response differs from docs
     */
    private record ResponseData(String name, String[] textList, String sessionId, RelationshipUpdate relationshipUpdate) {};
    private record RelationshipUpdate(int trust, int respect, int familiar, int flirtatious, int attraction) {};

    /**
     * Sends a simpleSendTextRequest to INWORLD_BASE_URL + resourceName
     * @param resourceName resourceName of character (from {@link #managedCharacters managedCharacters})
     * @param message Message sent by player
     * @param sessionID sessionID for the conversation
     * @param userName UserName of minecraft player
     * @param userID UUID of minecraft player
     * @return ResponseData, being the deserialized json of the response
     * @throws IOException if any part of the request fails (Opening connection, deserializing, etc.)
     */
    private static ResponseData simpleSendTextRequest(String resourceName, String message, String sessionID, String userName, UUID userID) throws IOException {

        // Create request
        String requestBody = """
                {\s
                        "character":"%s",
                        "text":"%s",
                        "session_id":"%s",
                        "endUserFullname":"%s",
                        "endUserID":"%s"
                    }
                   \s
                """.formatted(resourceName, message, sessionID, userName, userID);

        // Create endpoint
        URL endpoint = new URL(INWORLD_BASE_URL + resourceName + ":simpleSendText");
        // Log request
        MCA.LOGGER.debug("Sending %s to %s".formatted(requestBody, endpoint.toString()));
        // Create connection
        HttpsURLConnection con = (HttpsURLConnection) endpoint.openConnection();
        // Set connection properties
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("authorization", "Basic" + Config.getInstance().inworldAIToken);

        // Enable input and output streams
        con.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.write(requestBody.getBytes(StandardCharsets.UTF_8));
            wr.flush();
        }

        // Get response
        InputStream response = con.getInputStream();
        String responseString = new String(response.readAllBytes(), StandardCharsets.UTF_8);
        // Convert response to record
        ResponseData responseData = new Gson().fromJson(responseString, ResponseData.class);
        // Log response
        MCA.LOGGER.debug("Received %s".formatted(responseString));
        return responseData;
    }

    /**
     * Gets an answer for a specific message from player for villager.
     * Adds/Updates conversation context pair in {@link #openConversations openConversations}.
     * @param player The player requesting the answer
     * @param villager The villager responding
     * @param msg The message
     * @return
     */
    public static Optional<String> answer(ServerPlayerEntity player, VillagerEntityMCA villager, String msg) {
        // Variables needed for later
        UUID villagerID = villager.getUuid();

        ResponseData response;
        // Fetch response data. Abort if error
        try {
            // Get required variables
            UUID playerID = player.getUuid();
            String playerName = player.getName().getString();
            String sessionID = openConversations.getOrDefault(villagerID, "");
            String resourceName = managedCharacters.get(villagerID);

            assert resourceName != null;

            // Get response
            response = simpleSendTextRequest(resourceName, msg, sessionID, playerName, playerID);
        } catch (IOException e) {
            MCA.LOGGER.error(e);
            return Optional.empty();
        }
        assert response != null;

        // Update sessionId for future interactions
        openConversations.put(villagerID, response.sessionId);

        // TODO! Use response data to modify heart level

        // Build response String
        StringBuilder builder = new StringBuilder();
        for (String part : response.textList) {
            builder.append(part);
        }
        return Optional.of(builder.toString());
    }

    /**
     * Adds a new mapping to {@link #managedCharacters the managed characters map}
     * @param player Player entity
     * @param searchName Name of the villager
     * @param resourceName Resource name of the character
     */
    public static void addManagedCharacter(ServerPlayerEntity player, String searchName, String resourceName) {
        List<VillagerEntityMCA> entities = WorldUtils.getCloseEntities(player.getWorld(), player, 32, VillagerEntityMCA.class);

        // Get specific villager
        String normalizedSearchName = normalize(searchName);

        // Go through list, look for first match for name
        for (VillagerEntityMCA villager : entities) {
            String villagerName = normalize(villager.getTrackedValue(VILLAGER_NAME));
            if (normalizedSearchName.equals(villagerName)) {
                UUID villagerID = villager.getUuid();
                // Add first match to managedCharacters
                managedCharacters.put(villagerID, resourceName);
                break;
            }
        }
    }

    /**
     * Copy of net.mca.mixin.MixinServerPlayNetworkHandler.normalize
     */
    private static String normalize(String string) {
        return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}

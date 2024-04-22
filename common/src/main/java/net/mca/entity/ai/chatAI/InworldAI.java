package net.mca.entity.ai.chatAI;

import com.google.gson.Gson;
import net.mca.Config;
import net.mca.MCA;
import net.mca.entity.VillagerEntityMCA;
import net.mca.util.WorldUtils;
import net.minecraft.server.network.ServerPlayerEntity;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.mca.entity.VillagerLike.VILLAGER_NAME;

/**
 * TODO: Further improvements.
 * @author CSCMe
 */
public class InworldAI implements ChatAIStrategy {
    private final static String INWORLD_BASE_URL = "https://api.inworld.ai/v1/";
    private final String resourceName;
    private String sessionID;

    public InworldAI(String resourceName) {
        this.resourceName = resourceName;
        this.sessionID = "";
    }

    // We don't need conversational memory. Inworld does that for us. (TODO: Think: Is this true?)

    /**
     * Record to hold response data.
     * @param sessionId name does not match API documentation name session_id. The actual response differs from docs
     */
    private record ResponseData(String name, String[] textList, String sessionId, RelationshipUpdate relationshipUpdate) {}
    private record RelationshipUpdate(int trust, int respect, int familiar, int flirtatious, int attraction) {}

    /**
     * Sends a simpleSendTextRequest to INWORLD_BASE_URL + resourceName.
     * Updates sessionID if required
     * @param message Message sent by player
     * @param userName UserName of minecraft player
     * @param userID UUID of minecraft player
     * @return ResponseData object, being the deserialized json of the response
     * @throws IOException if any part of the request fails (Opening connection, deserializing, etc.)
     */
    private ResponseData simpleSendTextRequest(String message, String userName, UUID userID) throws IOException {

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
                """.formatted(this.resourceName, message, this.sessionID, userName, userID);

        // Create endpoint
        URL endpoint = new URL(INWORLD_BASE_URL + this.resourceName + ":simpleSendText");
        // Log request
        MCA.LOGGER.debug("InworldAI: Sending %s to %s".formatted(requestBody, endpoint.toString()));
        // Create connection
        HttpsURLConnection con = (HttpsURLConnection) endpoint.openConnection();
        // Set connection properties
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("authorization", "Basic " + Config.getInstance().inworldAIToken);

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
        MCA.LOGGER.debug("InworldAI: Received %s".formatted(responseString));
        // Update sessionID
        this.sessionID = responseData.sessionId;
        return responseData;
    }

    /**
     * Gets an answer for a specific message from player for villager.
     * @param player The player requesting the answer
     * @param villager The villager responding (currently unused, but could be used to make response influence emotions)
     * @param msg The message
     * @return {@code Optional.EMPTY} on error, Optional containing the answer to a message on success
     */
    public Optional<String> answer(ServerPlayerEntity player, VillagerEntityMCA villager, String msg) {
        ResponseData response;
        // Fetch response data. Abort if error
        try {
            // Get required variables
            UUID playerID = player.getUuid();
            String playerName = player.getName().getString();

            assert resourceName != null;

            // Get response
            response = simpleSendTextRequest(msg, playerName, playerID);
        } catch (IOException e) {
            MCA.LOGGER.error(e);
            return Optional.empty();
        }

        // TODO! Use response data to modify heart level and character emotion
        StringBuilder builder = new StringBuilder();
        for (String s : response.textList) {
            builder.append(s);
        }
        return Optional.of(builder.toString());
    }

}

package net.mca.entity.ai.chatAI.inworldAIModules;

import com.google.gson.Gson;

import net.mca.MCA;
import net.mca.entity.ai.chatAI.inworldAIModules.api.Interaction;
import net.mca.entity.ai.chatAI.inworldAIModules.api.Requests;
import net.mca.entity.ai.chatAI.inworldAIModules.api.Session;
import net.mca.server.world.data.PlayerSaveData;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Class to manage a session for Inworld Fully Managed Integration
 */
public class SessionModule {

    private final static String INWORLD_BASE_URL = "https://api.inworld.ai/v1/";
    private final static long SESSION_MAX_VALID_TIME = 28 * 60 * 1000;

    private final String characterResourceName;
    private final String workspaceID;
    private final Gson gson;

    /** Timestamp of last interaction. Needed for session management */
    private long lastInteraction;
    /** Contains latest response from openSessionRequest. Current session */
    private Session session = null;
    /** For SimpleSendText */
    @Deprecated
    private String sessionID = "";

    public SessionModule(String characterResourceName) {
        this.characterResourceName = characterResourceName;
        this.workspaceID = characterResourceName.split("/")[1];
        this.gson = new Gson();
    }

    public Optional<Interaction> getResponse(ServerPlayerEntity player, String msg) {
        long currentTime = System.currentTimeMillis();
        // Open session if needed
        if (session == null || currentTime - lastInteraction > SESSION_MAX_VALID_TIME) {
            Optional<Session> sessionOptional = openSessionRequest(player.getUuid().toString(),
                    player.getName().getString(),
                    PlayerSaveData.get(player).getGender().getDataName());

            if (sessionOptional.isEmpty()) {
                MCA.LOGGER.error("Failed to open Inworld session. Consult logs");
                return Optional.empty();
            }

            session = sessionOptional.get();
        }

        Optional<Interaction> interactionOptional = sendTextRequest(msg);
        if (interactionOptional.isPresent()) {
            this.lastInteraction = currentTime;
        }
        return interactionOptional;
    }

    /**
     * Makes a openSession request.
     * @param playerId Unique ID of player
     * @param playerName Name of player
     * @param playerGender Gender of player
     * @return {@code Optional.empty()} if error on request
     */
    private Optional<Session> openSessionRequest(String playerId, String playerName, String playerGender) {
        Requests.OpenSessionRequest.EndUserConfig config = new Requests.OpenSessionRequest.EndUserConfig(playerId, playerName, playerGender, null, null);
        Requests.OpenSessionRequest request = new Requests.OpenSessionRequest(this.characterResourceName, config);
        String requestBody = gson.toJson(request);
        // Make request
        String endpoint = INWORLD_BASE_URL + this.characterResourceName + ":openSession";
        Optional<String> response = Requests.makeRequest(endpoint, requestBody);
        if (response.isEmpty()) {
            return Optional.empty();
        }
        Session session = gson.fromJson(response.get(), Session.class);
        return Optional.of(session);
    }

    /**
     * Makes a sendText request to the Inworld API.
     * @param message Player message used in the request
     * @return {@code Optional.EMPTY} if request failed, else Optional containing Interaction object with response data
     */
    private Optional<Interaction> sendTextRequest(String message) {
        Requests.SendTextRequest request = new Requests.SendTextRequest(message);
        String requestBody = gson.toJson(request);
        // Create endpoint
        String endpoint = INWORLD_BASE_URL + getSessionCharacterResourceName() + ":sendText";
        // Make request
        Optional<String> response = Requests.makeRequest(endpoint, requestBody, session.name());
        if (response.isEmpty()) {
            return Optional.empty();
        }
        // Parse the response
        Interaction data = gson.fromJson(response.get(), Interaction.class);
        return Optional.of(data);
    }

    /**
     * Creates the session character endpoint needed for sendText and sendTrigger requests.
     * Uses workspaceID, session name and session character name
     * @return A string representing the resource name for the session character.
     */
    private String getSessionCharacterResourceName() {
        return "workspaces/%s/sessions/%s/sessionCharacters/%s".formatted(workspaceID, session.name(), session.sessionCharacters()[0].character());
    }

    /**
     * Sends a simpleSendTextRequest to INWORLD_BASE_URL + resourceName.
     * Updates sessionID if required
     * @param message Message sent by player
     * @param userName UserName of minecraft player
     * @param userID UUID of minecraft player
     * @return ResponseData object, being the deserialized json of the response
     */
    @Deprecated
    private Optional<Interaction> simpleSendTextRequest(String message, String userName, UUID userID) {
        // Create request body
        Requests.SimpleTextRequest request = new Requests.SimpleTextRequest(this.characterResourceName, message, this.sessionID, userName, userID.toString());
        String requestBody = gson.toJson(request);
        // Create endpoint
        String endpoint = INWORLD_BASE_URL + this.characterResourceName + ":simpleSendText";
        // Make request
        Optional<String> response = Requests.makeRequest(endpoint, requestBody);
        if (response.isEmpty()) {
            return Optional.empty();
        }
        // Parse the response
        Interaction data =  gson.fromJson(response.get(), Interaction.class);
        // Update sessionID
        this.sessionID = data.sessionId();
        return Optional.of(data);
    }

}

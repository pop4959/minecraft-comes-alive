package net.mca.entity.ai.chatAI.inworldAIModules;

import com.google.gson.Gson;
import net.mca.Config;
import net.mca.MCA;
import net.mca.entity.ai.chatAI.inworldAIModules.api.Interaction;
import net.mca.entity.ai.chatAI.inworldAIModules.api.Requests;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class SessionModule {

    private final static String INWORLD_BASE_URL = "https://api.inworld.ai/v1/";

    private final String resourceName;
    private final Gson gson;
    private String sessionID = "";

    public SessionModule(String resourceName) {
        this.resourceName = resourceName;
        this.gson = new Gson();
    }

    /**
     * Sets common properties of all Inworld API Requests
     * @param con Newly created HttpURLConnection
     * @throws ProtocolException See {@link HttpURLConnection#setRequestMethod}
     */
    private void setCommonProperties(HttpURLConnection con) throws ProtocolException {
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("authorization", "Basic " + Config.getInstance().inworldAIToken);
    }

    /**
     * Gets a response String from a HttpURLConnection
     * @param con HttpURLConnection with all properties set
     * @param body Body of the request
     * @return String with the response of the request
     * @throws IOException if any part of the request fails (Opening connection, deserializing, etc.)
     */
    private String getResponse(HttpURLConnection con, String body) throws IOException{
        // Enable input and output streams
        con.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.write(body.getBytes(StandardCharsets.UTF_8));
            wr.flush();
        }

        // Get response
        InputStream response = con.getInputStream();
        return new String(response.readAllBytes(), StandardCharsets.UTF_8);
    }


    /**
     * This is temporary and will only exist until Fully Managed Integration is added.
     * Sends a simpleSendTextRequest to INWORLD_BASE_URL + resourceName.
     * Updates sessionID if required
     * @param message Message sent by player
     * @param userName UserName of minecraft player
     * @param userID UUID of minecraft player
     * @return ResponseData object, being the deserialized json of the response
     */
    public Optional<Interaction> simpleSendTextRequest(String message, String userName, UUID userID) {

        // Create request body
        Requests.SimpleTextRequest request = new Requests.SimpleTextRequest(this.resourceName, message, this.sessionID, userName, userID.toString());
        String requestBody = gson.toJson(request);
        try {
            // Create endpoint
            URL endpoint = new URL(INWORLD_BASE_URL + this.resourceName + ":simpleSendText");

            // Log request
            MCA.LOGGER.info("InworldAI: Sending %s to %s".formatted(request, endpoint.toString()));

            // Create connection
            HttpsURLConnection con = (HttpsURLConnection) endpoint.openConnection();
            // Set connection properties
            setCommonProperties(con);
            // Make request
            String responseString = getResponse(con, requestBody);

            // Log response
            MCA.LOGGER.info("InworldAI: Received %s".formatted(responseString));

            // Parse the response
            Interaction data =  gson.fromJson(responseString, Interaction.class);
            // Update sessionID
            this.sessionID = data.sessionId();

            return Optional.of(data);

        } catch (IOException e) {
            MCA.LOGGER.error("InworldAISimpleSendTextRequestError:" + e);
            return Optional.empty();
        }
    }
}

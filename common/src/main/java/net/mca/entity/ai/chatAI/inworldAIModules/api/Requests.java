package net.mca.entity.ai.chatAI.inworldAIModules.api;

import net.mca.Config;
import net.mca.MCA;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Requests {
    public record SimpleTextRequest(String character, String text, String session_id, String endUserFullName, String endUserId) {}
    public record OpenSessionRequest(String name, EndUserConfig user) {
        public record EndUserConfig(String endUserId, String givenName, String gender, String role, Long age) {}
    }
    public record SendTextRequest(String text) {}
    public record SendTriggerRequest(TriggerEvent triggerEvent, String endUserId) {}


    public static Optional<String> makeRequest(String urlString, String body) {
        return makeRequest(urlString, body, "");
    }

    public static Optional<String> makeRequest(String urlString, String body, String sessionIDAuth) {
        String responseString = "No response";
        try {
            URL url = new URL(urlString);

            // Create connection
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("authorization", "Basic " + Config.getInstance().inworldAIToken);
            // Set second header if necessary
            if (!sessionIDAuth.isEmpty()) {
                con.setRequestProperty("Grpc-Metadata-session-id", sessionIDAuth);
            }

            // Enable input and output streams
            con.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(body.getBytes(StandardCharsets.UTF_8));
                wr.flush();
            }

            // Get response
            InputStream response = con.getInputStream();
            responseString = new String(response.readAllBytes(), StandardCharsets.UTF_8);


            return Optional.of(responseString);
        } catch (IOException e) {
            // Log request
            MCA.LOGGER.error("InworldAI: Sending %s to %s".formatted(body, urlString));
            MCA.LOGGER.error("InworldAI: Received %s".formatted(responseString));
            MCA.LOGGER.error(e);
            return Optional.empty();
        }
    }
}

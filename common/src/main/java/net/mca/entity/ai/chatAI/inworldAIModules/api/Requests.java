package net.mca.entity.ai.chatAI.inworldAIModules.api;

public class Requests {
    public record SimpleTextRequest(String character, String text, String session_id, String endUserFullName, String endUserId) {}
    public record OpenSessionRequest(String name, EndUserConfig user) {
        public record EndUserConfig(String endUserId, String givenName, String gender, String role, long age) {}
    }
    public record SendTextRequest(String session_character, String text) {}
    public record SendTriggerRequest(String session_character, TriggerEvent triggerEvent, String endUserId) {}
}

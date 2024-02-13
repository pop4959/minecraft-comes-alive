package net.mca.entity.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mca.Config;
import net.mca.MCA;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.gpt3Modules.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.apache.commons.io.IOUtils;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static net.minecraft.util.Util.NIL_UUID;

public class GPT3 {
    private static final int MIN_MEMORY = 100;
    private static final int MAX_MEMORY = 600;
    private static final int MAX_MEMORY_TIME = 20 * 60 * 45;
    private static final int CONVERSATION_TIME = 20 * 60;
    private static final int CONVERSATION_DISTANCE = 16;

    private static final Map<UUID, List<Pair<String, String>>> memory = new HashMap<>();
    private static final Map<UUID, Long> lastInteractions = new HashMap<>();
    private static final Map<UUID, UUID> lastInteraction = new HashMap<>();

    public static String translate(String phrase) {
        return phrase.replace("_", " ").toLowerCase(Locale.ROOT).replace("mca.", "");
    }

    public record Answer(String answer, String error) {
    }

    public static Answer post(String url, String requestBody, String token) {
        try {
            HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.toString());
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Authorization", "Bearer " + token);

            // Enable input and output streams
            con.setDoOutput(true);

            // Write the request body to the connection
            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(requestBody.getBytes(StandardCharsets.UTF_8));
                wr.flush();
            }

            InputStream response = con.getInputStream();
            String body = IOUtils.toString(response, StandardCharsets.UTF_8);

            // parse json
            JsonObject map = JsonParser.parseString(body).getAsJsonObject();
            String message = map.has("choices") ? map.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").getAsJsonPrimitive("content").getAsString() : null;
            String error = map.has("error") ? map.get("error").getAsString().trim().replace("\n", " ") : null;

            message = message == null ? null : cleanupAnswer(message);

            return new Answer(message, error);
        } catch (Exception e) {
            MCA.LOGGER.error(e);
            return new Answer(null, "unknown");
        }
    }

    public static Answer request(String encodedURL) {
        try {
            // receive
            HttpURLConnection con = (HttpURLConnection) (new URL(encodedURL)).openConnection();
            con.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.toString());
            InputStream response = con.getInputStream();
            String body = IOUtils.toString(response, StandardCharsets.UTF_8);

            // parse json
            JsonObject map = JsonParser.parseString(body).getAsJsonObject();
            String answer = map.has("answer") ? map.get("answer").getAsString().trim().replace("\n", " ") : "";
            String error = map.has("error") ? map.get("error").getAsString().trim().replace("\n", " ") : null;

            return new Answer(answer, error);
        } catch (Exception e) {
            MCA.LOGGER.error(e);
            return new Answer(null, "unknown");
        }
    }

    public static Optional<String> answer(ServerPlayerEntity player, VillagerEntityMCA villager, String msg) {
        try {
            String playerName = player.getName().getString();
            String villagerName = villager.getName().getString();

            // forgot about last conversation if it's too long ago
            long time = villager.world.getTime();
            if (time > lastInteractions.getOrDefault(villager.getUuid(), 0L) + MAX_MEMORY_TIME) {
                memory.remove(villager.getUuid());
            }
            lastInteractions.put(villager.getUuid(), time);
            lastInteraction.put(player.getUuid(), villager.getUuid());

            // remember phrase
            List<Pair<String, String>> pastDialogue = memory.computeIfAbsent(villager.getUuid(), key -> new LinkedList<>());
            int memory = MIN_MEMORY + Math.min(5, Config.getInstance().villagerChatAIIntelligence) * (MAX_MEMORY - MIN_MEMORY) / 5;
            while (pastDialogue.stream().mapToInt(v -> (v.getRight().length() / 4)).sum() > memory) {
                pastDialogue.remove(0);
            }

            // construct context
            List<String> input = new LinkedList<>();
            PersonalityModule.apply(input, villager, player);
            TraitsModule.apply(input, villager, player);
            RelationModule.apply(input, villager, player);
            VillageModule.apply(input, villager, player);
            EnvironmentModule.apply(input, villager, player);
            PlayerModule.apply(input, villager, player);

            // gather variables
            Map<String, String> variables = Map.of(
                    "player", playerName,
                    "villager", villagerName
            );

            // add variables
            StringBuilder sb = new StringBuilder();
            for (String s : input) {
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    s = s.replaceAll("\\$" + entry.getKey(), entry.getValue());
                }
                sb.append(s);
            }

            String system = sb.toString();

            // Construct body
            StringBuilder body = new StringBuilder();
            body.append("{");
            body.append("\"model\": \"").append(Config.getInstance().villagerChatAIModel).append("\",");
            body.append("\"messages\": [");
            body.append("{\"role\": \"system\", \"content\": ").append(jsonStringQuote(system)).append("},");
            for (Pair<String, String> pair : pastDialogue) {
                String role = pair.getLeft();
                String content = pair.getRight();
                body.append("{\"role\": \"").append(role).append("\", \"content\": ").append(jsonStringQuote(content)).append("},");
            }
            body.append("{\"role\": \"" + "user" + "\", \"content\": ").append(jsonStringQuote(msg)).append("}");
            body.append("]}");

            // get access token
            String token = Config.getInstance().villagerChatAIToken;
            if (token.isEmpty() || Config.getInstance().villagerChatAIEndpoint.contains("conczin.net")) {
                token = variables.get("player");
            }

            // encode and create url
            Answer message = post(Config.getInstance().villagerChatAIEndpoint, body.toString(), token);

            if (message.error == null) {
                // remember
                if (message.answer != null) {
                    pastDialogue.add(new Pair<>("user", msg));
                    pastDialogue.add(new Pair<>("assistant", message.answer));
                }
                return Optional.ofNullable(message.answer);
            } else if (message.error.equals("invalid_model")) {
                player.sendMessage(Text.literal("Invalid model!").formatted(Formatting.RED), false);
            } else if (message.error.equals("limit")) {
                MutableText styled = (Text.translatable("mca.limit.patreon")).styled(s -> s
                        .withColor(Formatting.GOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/Luke100000/minecraft-comes-alive/wiki/GPT3-based-conversations#increase-conversation-limit"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("mca.limit.patreon.hover"))));

                player.sendMessage(styled, false);
            } else if (message.error.equals("limit_premium")) {
                player.sendMessage(Text.translatable("mca.limit.premium").formatted(Formatting.RED), false);
            } else {
                MCA.LOGGER.error(message.error);
                player.sendMessage(Text.translatable("mca.ai_broken").formatted(Formatting.RED), false);
            }
        } catch (Exception e) {
            MCA.LOGGER.error(e);
            player.sendMessage(Text.translatable("mca.ai_broken").formatted(Formatting.RED), false);
        }

        return Optional.empty();
    }

    static String jsonStringQuote(String string) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : string.toCharArray())
            sb.append(switch (c) {
                case '\\', '"', '/' -> "\\" + c;
                case '\b' -> "\\b";
                case '\t' -> "\\t";
                case '\n' -> "\\n";
                case '\f' -> "\\f";
                case '\r' -> "\\r";
                default -> //noinspection MalformedFormatString
                        c < ' ' ? String.format("\\u%04x", c) : c;
            });
        return sb.append('"').toString();
    }

    static String cleanupAnswer(String answer) {
        answer = answer.replace("\"", "");
        answer = answer.replace("\n", " ");
        String[] parts = answer.split(":", 2);
        return parts[parts.length - 1].strip();
    }

    public static boolean inConversationWith(VillagerEntityMCA villager, ServerPlayerEntity player) {
        return villager.distanceTo(player) < CONVERSATION_DISTANCE
                && villager.world.getTime() < lastInteractions.getOrDefault(villager.getUuid(), 0L) + CONVERSATION_TIME
                && lastInteraction.getOrDefault(player.getUuid(), NIL_UUID).equals(villager.getUuid());
    }
}

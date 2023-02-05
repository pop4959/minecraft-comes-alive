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
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static net.minecraft.util.Util.NIL_UUID;

public class GPT3 {
    public static final String URL = "http://snoweagle.tk/";

    private static final int MIN_MEMORY = 100;
    private static final int MAX_MEMORY = 600;
    private static final int MAX_MEMORY_TIME = 20 * 60 * 45;
    private static final int CONVERSATION_TIME = 20 * 60;
    private static final int CONVERSATION_DISTANCE = 16;

    private static final Map<UUID, List<String>> memory = new HashMap<>();
    private static final Map<UUID, Long> lastInteractions = new HashMap<>();
    private static final Map<UUID, UUID> lastInteraction = new HashMap<>();

    public static String translate(String phrase) {
        return phrase.replaceAll("_", " ").toLowerCase(Locale.ROOT).replace("mca.", "");
    }

    public record Answer(String answer, String error) {
    }

    public static Answer request(String encodedURL) {
        try {
            // receive
            HttpURLConnection con = (HttpURLConnection)(new URL(encodedURL)).openConnection();
            con.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.toString());
            InputStream response = con.getInputStream();
            String body = IOUtils.toString(response, StandardCharsets.UTF_8);

            // parse json
            JsonObject map = JsonParser.parseString(body).getAsJsonObject();
            String answer = map.has("answer") ? map.get("answer").getAsString().trim().replace("\n", "") : "";
            String error = map.has("error") ? map.get("error").getAsString().trim().replace("\n", "") : null;

            return new Answer(answer, error);
        } catch (Exception e) {
            e.printStackTrace();
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
            List<String> pastDialogue = memory.computeIfAbsent(villager.getUuid(), (key) -> new LinkedList<>());
            int MEMORY = MIN_MEMORY + Math.min(5, Config.getInstance().villagerChatAIIntelligence) * (MAX_MEMORY - MIN_MEMORY) / 5;
            while (pastDialogue.stream().mapToInt(v -> (v.length() / 4)).sum() > MEMORY) {
                pastDialogue.remove(0);
            }
            pastDialogue.add("$player: " + msg);

            // construct context
            List<String> input = new LinkedList<>();
            PersonalityModule.apply(input, villager, player);
            TraitsModule.apply(input, villager, player);
            RelationModule.apply(input, villager, player);
            VillageModule.apply(input, villager, player);
            EnvironmentModule.apply(input, villager, player);
            PlayerModule.apply(input, villager, player);
            input.add("\n\n");

            // construct memory
            for (String s : pastDialogue) {
                input.add(s + "\n");
            }

            //prompt
            input.add("$villager:");

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

            // build http request
            Map<String, String> params = new HashMap<>();
            params.put("prompt", sb.toString());
            params.put("player", variables.get("player"));
            params.put("villager", variables.get("villager"));

            // encode and create url
            String encodedURL = params.keySet().stream()
                    .map(key -> key + "=" + URLEncoder.encode(params.get(key), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&", URL + "chat?", ""));

            // encode and create url
            Answer message = request(encodedURL);

            if (message.error == null) {
                // remember
                pastDialogue.add(villagerName + ": " + message.answer);
                return Optional.ofNullable(message.answer);
            } else if (message.error.equals("limit")) {
                MutableText styled = (Text.translatable("mca.limit.patreon")).styled(s -> s
                        .withColor(Formatting.GOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/Luke100000/minecraft-comes-alive/wiki/GPT3-based-conversations#increase-conversation-limit"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("mca.limit.patreon.hover"))));

                player.sendMessage(styled, false);
            } else if (message.error.equals("limit_premium")) {
                player.sendMessage(Text.translatable("mca.limit.premium").formatted(Formatting.RED), false);
            } else {
                player.sendMessage(Text.translatable("mca.ai_broken").formatted(Formatting.RED), false);
            }
        } catch (Exception e) {
            MCA.LOGGER.error(e);
            player.sendMessage(Text.translatable("mca.ai_broken").formatted(Formatting.RED), false);
        }

        return Optional.empty();
    }

    public static boolean inConversationWith(VillagerEntityMCA villager, ServerPlayerEntity player) {
        return villager.distanceTo(player) < CONVERSATION_DISTANCE
                && villager.world.getTime() < lastInteractions.getOrDefault(villager.getUuid(), 0L) + CONVERSATION_TIME
                && lastInteraction.getOrDefault(player.getUuid(), NIL_UUID).equals(villager.getUuid());
    }
}

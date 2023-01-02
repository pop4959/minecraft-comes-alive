package net.mca.entity.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.gpt3Modules.*;
import net.minecraft.server.network.ServerPlayerEntity;
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
    private static final String url = "http://snoweagle.tk/chat";

    private static final int MAX_MEMORY = 12;
    private static final int MAX_MEMORY_TIME = 20 * 60 * 30;
    private static final int CONVERSATION_TIME = 20 * 60;
    private static final int CONVERSATION_DISTANCE = 16;

    private static final Map<UUID, List<String>> memory = new HashMap<>();
    private static final Map<UUID, Long> lastInteractions = new HashMap<>();
    private static final Map<UUID, UUID> lastInteraction = new HashMap<>();

    public static String translate(String phrase) {
        return phrase.replaceAll("_", " ").toLowerCase(Locale.ROOT);
    }

    public static String answer(ServerPlayerEntity player, VillagerEntityMCA villager, String msg) {
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
        pastDialogue.add(playerName + ": " + msg);
        if (pastDialogue.size() > MAX_MEMORY) {
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
        input.add("\n\n");

        // construct memory
        for (String s : pastDialogue) {
            input.add(s + "\n");
        }
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
                .collect(Collectors.joining("&", url + "?", ""));

        try {
            // receive
            HttpURLConnection con = (HttpURLConnection)(new URL(encodedURL)).openConnection();
            con.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.toString());
            InputStream response = con.getInputStream();
            String body = IOUtils.toString(response, StandardCharsets.UTF_8);

            // parse json
            JsonObject map = JsonParser.parseString(body).getAsJsonObject();
            String message = map.get("answer").getAsString().trim().replace("\n", "");

            // remember
            pastDialogue.add(villagerName + ": " + message);

            return message;
        } catch (Exception e) {
            e.printStackTrace();
            return "(AI broke, please send latest.log to dev)";
        }
    }

    public static boolean inConversationWith(VillagerEntityMCA villager, ServerPlayerEntity player) {
        return villager.distanceTo(player) < CONVERSATION_DISTANCE
                && villager.world.getTime() < lastInteractions.getOrDefault(villager.getUuid(), 0L) + CONVERSATION_TIME
                && lastInteraction.getOrDefault(player.getUuid(), NIL_UUID).equals(villager.getUuid());
    }
}

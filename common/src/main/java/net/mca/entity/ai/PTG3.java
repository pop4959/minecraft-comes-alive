package net.mca.entity.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.ptg3Modules.*;
import net.minecraft.server.network.ServerPlayerEntity;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class PTG3 {
    static final OkHttpClient client = new OkHttpClient();

    private static final String url = "http://snoweagle.tk/chat";

    private static final int MAX_MEMORY = 10;
    private static final int MAX_MEMORY_TIME = 20 * 60 * 30;
    private static final int CONVERSATION_TIME = 20 * 60;
    private static final int CONVERSATION_DISTANCE = 16;

    private static final Map<UUID, List<String>> memory = new HashMap<>();
    private static final Map<UUID, Long> lastInteractions = new HashMap<>();
    private static final Map<UUID, UUID> lastInteraction = new HashMap<>();

    public static String translate(String phrase) {
        return phrase.replaceAll("_", " ").toLowerCase(Locale.ROOT);
    }

    public static void answer(ServerPlayerEntity player, VillagerEntityMCA villager, String msg, Consumer<String> consumer) {
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
        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        httpBuilder.addQueryParameter("prompt", sb.toString());
        httpBuilder.addQueryParameter("player", variables.get("player"));
        httpBuilder.addQueryParameter("villager", variables.get("villager"));

        Request request = new Request.Builder().url(httpBuilder.build()).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body != null) {
                    JsonObject map = JsonParser.parseString(body.string()).getAsJsonObject();
                    String message = map.get("answer").getAsString().trim().replace("\n", "");
                    pastDialogue.add(villagerName + ": " + message);
                    consumer.accept(message);
                }
            }
        });
    }

    public static boolean inConversationWith(VillagerEntityMCA villager, ServerPlayerEntity player) {
        return villager.distanceTo(player) < CONVERSATION_DISTANCE
                && villager.world.getTime() < lastInteractions.getOrDefault(villager.getUuid(), 0L) + CONVERSATION_TIME
                && lastInteraction.get(player.getUuid()).equals(villager.getUuid());
    }
}

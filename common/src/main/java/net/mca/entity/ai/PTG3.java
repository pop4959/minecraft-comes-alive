package net.mca.entity.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mca.entity.VillagerEntityMCA;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class PTG3 {
    static final OkHttpClient client = new OkHttpClient();

    private static final String url = "http://127.0.0.1:8000/chat";

    private static final int MAX_MEMORY = 10;
    private static final int MAX_MEMORY_TIME = 20 * 60 * 30;
    private static final int CONVERSATION_TIME = 20 * 60;
    private static final int CONVERSATION_DISTANCE = 16;

    private static final Map<UUID, List<String>> memory = new HashMap<>();
    private static final Map<UUID, Long> lastInteractions = new HashMap<>();

    private static String translate(String phrase) {
        return phrase.replaceAll("_", " ").toLowerCase(Locale.ROOT);
    }

    private static final Map<String, String> traitDescription = new HashMap<>() {{
        put("lactose_intolerance", "$villager is intolerant to lactose.");
        put("coeliac_disease", "$villager has coeliac disease.");
        put("diabetes", "$villager has diabetes.");
        put("sirben", "$villager has was unfortunately born as a Sirben.");
        put("dwarfism", "$villager has dwarfism.");
        put("albinism", "$villager is an albino.");
        put("heterochromia", "$villager has heterochromia.");
        put("color_blind", "$villager is color blind.");
        put("vegetarian", "$villager is a vegetarian.");
        put("bisexual", "$villager is bisexual.");
        put("homosexual", "$villager is homosexual.");
        put("left_handed", "$villager is left handed.");
        put("electrified", "$villager has been struck by lightning.");
        put("rainbow", "$villager has very colorful hair.");
    }};

    public static void answer(PlayerEntity player, VillagerEntityMCA villager, String msg, Consumer<String> consumer) {
        String playerName = player.getName().asString();
        String villagerName = villager.getName().asString();

        // forgot about last conversation if it's too long ago
        long time = villager.world.getTime();
        if (time > lastInteractions.getOrDefault(villager.getUuid(), 0L) + MAX_MEMORY_TIME) {
            memory.remove(villager.getUuid());
        }
        lastInteractions.put(villager.getUuid(), time);

        // remember phrase
        List<String> pastDialogue = memory.computeIfAbsent(villager.getUuid(), (key) -> new LinkedList<>());
        pastDialogue.add(playerName + ": " + msg);
        if (pastDialogue.size() > MAX_MEMORY) {
            pastDialogue.remove(0);
        }

        // construct context
        // todo split into modules
        List<String> input = new LinkedList<>();
        input.add("This is a conversation with a Minecraft villager named $villager and player." + " ");
        input.add("$villager lives in a small, medieval village in a forest. ");
        input.add("The village has a library, and an armory. "); // todo village buildings
        input.add("$villager is a " + translate(villager.getGenetics().getGender().name()) + ". ");
        input.add("$villager is " + translate(villager.getVillagerBrain().getMood().getName()) + ". ");
        input.add("$villager is " + translate(villager.getVillagerBrain().getPersonality().name()) + ". ");
        for (Traits.Trait trait : villager.getTraits().getTraits()) {
            input.add(traitDescription.getOrDefault(trait.name(), "$villager has " + translate(trait.name())));
        }
        int hearts = villager.getVillagerBrain().getMemoriesForPlayer(player).getHearts();
        if (hearts < -25) {
            input.add("$villager hates $player. ");
        } else if (hearts < 0) {
            input.add("$villager dislikes $player. ");
        } else if (hearts < 33) {
            input.add("$villager knows $player well. ");
        } else if (hearts < 66) {
            input.add("$villager likes $player. ");
        } else {
            input.add("$villager likes $player really well. ");
        }
        if (villager.getRelationships().isMarriedTo(player.getUuid())) {
            input.add("$villager is married to $player. ");
        } else if (villager.getRelationships().isMarried()) {
            input.add("$villager is married. ");
        }
        input.add("$player visited the nether. "); // todo player achievements
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
        return villager.distanceTo(player) < CONVERSATION_DISTANCE && villager.world.getTime() < lastInteractions.getOrDefault(villager.getUuid(), 0L) + CONVERSATION_TIME;
    }
}

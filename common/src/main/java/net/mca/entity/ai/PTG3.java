package net.mca.entity.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mca.entity.VillagerEntityMCA;
import net.minecraft.entity.player.PlayerEntity;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class PTG3 {
    static final OkHttpClient client = new OkHttpClient();

    private static final String url = "http://127.0.0.1:8000/chat";

    public static void answer(PlayerEntity player, VillagerEntityMCA villager, List<String> pastDialogue, Consumer<String> consumer) {
        List<String> input = new LinkedList<>();

        // construct context
        input.add("This is a conversation with a Minecraft villager named $villager and $villager." + " ");
        input.add("$villager lives in a small village in a forest. ");
        input.add("$villager is angry. ");
        input.add("$villager has diabetes. ");
        input.add("$villager dislikes you. ");
        input.add("\n");

        // construct memory
        for (int i = 0; i < pastDialogue.size(); i++) {
            input.add((i % 2 == 0 ? "$player: " : "$villager: ") + pastDialogue.get(i) + "\n");
        }
        input.add("$villager:");

        // gather variables
        Map<String, String> variables = Map.of(
                "player", player.getName().asString(),
                "villager", villager.getName().asString()
        );

        // add variables
        StringBuilder sb = new StringBuilder();
        for (String s : input) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                s = s.replaceAll("\\$" + entry.getKey(), entry.getValue());
            }
            sb.append(s);
        }
        String prompt = sb.toString();

        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        httpBuilder.addQueryParameter("prompt", prompt);
        httpBuilder.addQueryParameter("player", variables.get("player"));
        httpBuilder.addQueryParameter("villager", variables.get("villager"));

        Request request = new Request.Builder().url(httpBuilder.build()).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body != null) {
                    JsonObject map = JsonParser.parseString(body.string()).getAsJsonObject();
                    String message = map.get("answer").getAsString().trim().replace("\n", "");
                    consumer.accept(message);
                }
            }
        });
    }
}

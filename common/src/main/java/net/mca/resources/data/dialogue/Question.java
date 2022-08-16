package net.mca.resources.data.dialogue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.mca.client.gui.Constraint;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.interaction.InteractionPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Question {
    private final String id;
    private final String group;
    private final List<Answer> answers;
    private final boolean auto;
    private final boolean silent;

    public Question(String id, String group, List<Answer> answers, boolean auto, boolean silent) {
        this.id = id;
        this.group = group;
        this.answers = answers;
        this.auto = auto;
        this.silent = silent;
    }

    public static Question fromJson(String id, JsonObject json) {
        String group = json.has("group") ? json.get("group").getAsString() : null;
        boolean auto = json.has("auto") && json.get("auto").getAsBoolean();
        boolean silent = json.has("silent") && json.get("silent").getAsBoolean();

        List<Answer> answers = new LinkedList<>();
        for (JsonElement e : json.getAsJsonArray("answers")) {
            answers.add(Answer.fromJson(e.getAsJsonObject()));
        }

        //sometimes the conditions are the same for all results
        if (json.has("baseConditions")) {
            int r = 0;
            for (JsonElement conditions : json.getAsJsonArray("baseConditions")) {
                for (JsonElement e : conditions.getAsJsonArray()) {
                    InteractionPredicate predicate = InteractionPredicate.fromJson(e.getAsJsonObject());
                    int finalR = r;
                    answers.forEach(a -> a.getResults().get(finalR).getConditions().add(predicate));
                }
                r++;
            }
        }

        return new Question(id, group, answers, auto, silent);
    }

    public String getId() {
        return id;
    }

    public String getGroup() {
        return group;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public boolean isCloseScreen() {
        return answers == null;
    }

    public Answer getAnswer(String answer) {
        for (Answer a : answers) {
            if (a.getName().equals(answer)) {
                return a;
            }
        }
        return null;
    }

    public static String getTranslationKey(String question) {
        return "dialogue." + question;
    }

    public static String getTranslationKey(String question, String answer) {
        return "dialogue." + question + "." + answer;
    }

    public boolean isAuto() {
        return auto;
    }

    public List<String> getValidAnswers(ServerPlayerEntity player, VillagerEntityMCA villager) {
        Set<Constraint> constraints = Constraint.allMatching(villager, player);
        List<String> ans = new LinkedList<>();
        for (Answer a : answers) {
            if (a.isValidForConstraint(constraints)) {
                ans.add(a.getName());
            }
        }
        return ans;
    }

    public boolean isSilent() {
        return silent;
    }
}

package mca.resources;

import com.google.gson.JsonElement;
import mca.MCA;
import mca.cobalt.network.NetworkHandler;
import mca.entity.VillagerEntityMCA;
import mca.network.s2c.AnalysisResults;
import mca.resources.data.SerializablePair;
import mca.resources.data.analysis.ChanceAnalysis;
import mca.resources.data.analysis.IntAnalysis;
import mca.resources.data.dialogue.Actions;
import mca.resources.data.dialogue.Answer;
import mca.resources.data.dialogue.Question;
import mca.resources.data.dialogue.Result;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.*;

public class Dialogues extends JsonDataLoader {
    protected static final Identifier ID = new Identifier(MCA.MOD_ID, "dialogues");

    private final Random random = new Random();

    private static Dialogues INSTANCE;

    public static Dialogues getInstance() {
        return INSTANCE;
    }

    private final Map<String, Question> questions = new HashMap<>();
    private final Map<String, List<Question>> questionGroups = new HashMap<>();

    public Dialogues() {
        super(Resources.GSON, "dialogues");
        INSTANCE = this;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> data, ResourceManager manager, Profiler profiler) {
        questions.clear();
        data.forEach(this::loadDialogue);
    }

    private void loadDialogue(Identifier identifier, JsonElement element) {
        String id = identifier.getPath().substring(identifier.getPath().lastIndexOf('/') + 1);
        Question q = Question.fromJson(id, element.getAsJsonObject());

        this.questions.put(id, q);

        this.questionGroups.computeIfAbsent(q.getGroup(), x -> new LinkedList<>());
        this.questionGroups.get(q.getGroup()).add(q);
    }

    public Question getQuestion(String i) {
        return questions.get(i);
    }

    public Question getRandomQuestion(String i) {
        if (questions.containsKey(i)) {
            return questions.get(i);
        } else {
            List<Question> questions = this.questionGroups.get(i);
            if (questions == null) {
                return null;
            } else {
                return questions.get(random.nextInt(questions.size()));
            }
        }
    }

    //selects a specific answer while being in given question
    public void selectAnswer(VillagerEntityMCA villager, ServerPlayerEntity player, String questionId, String answerId) {
        Question question = getQuestion(questionId);
        Answer answer = question.getAnswer(answerId);

        //fetch chances for each result
        int total = 0;
        List<IntAnalysis> analysis = new LinkedList<>();
        for (Result r : answer.getResults()) {
            IntAnalysis a = r.getChances(villager, player);
            analysis.add(a);
            total += Math.max(0, a.getTotal());
        }

        //choose weighted random
        int chosen = -1;
        total = total == 0 ? 0 : villager.getRandom().nextInt(total);
        for (IntAnalysis a : analysis) {
            total -= Math.max(0, a.getTotal());
            chosen++;
            if (total < 0) {
                break;
            }
        }

        Actions chosenActions = answer.getResults().get(chosen).getActions();

        //send analysis (if there is a heart impact at all)
        if (chosenActions.isNegative() || chosenActions.isPositive()) {
            ChanceAnalysis finalAnalysis = new ChanceAnalysis();
            for (int i = 0; i < analysis.size(); i++) {
                boolean positive = answer.getResults().get(i).getActions().isPositive();
                boolean negative = answer.getResults().get(i).getActions().isNegative();
                for (SerializablePair<String, Integer> value : analysis.get(i).getSummands()) {
                    finalAnalysis.add(value.getLeft(), value.getRight() * (positive ? 1 : negative ? -1 : 0));
                }
            }
            NetworkHandler.sendToPlayer(new AnalysisResults(finalAnalysis), player);
        }

        //execute that results actions
        chosenActions.trigger(villager, player);
    }
}

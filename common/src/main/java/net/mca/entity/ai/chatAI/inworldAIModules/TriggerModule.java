package net.mca.entity.ai.chatAI.inworldAIModules;

import com.google.common.collect.ImmutableMap;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.MoveState;
import net.mca.entity.ai.chatAI.inworldAIModules.api.Interaction;
import net.mca.entity.ai.chatAI.inworldAIModules.api.TriggerEvent;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Class to manage command triggers (wear armor, follow me, etc.)
 */
public class TriggerModule {

    /** Map for trigger name => actions */
    private final static Map<String, BiConsumer<ServerPlayerEntity, VillagerEntityMCA>> triggerActions = ImmutableMap.of(
            "follow-trigger", (p, v) -> v.getVillagerBrain().setMoveState(MoveState.FOLLOW, p),
            "stay-trigger", (p, v) -> v.getVillagerBrain().setMoveState(MoveState.STAY, p),
            "move-trigger", (p, v) -> v.getVillagerBrain().setMoveState(MoveState.MOVE, p),
            "armor-trigger", (p, v) -> v.getVillagerBrain().setArmorWear(true),
            "no-armor-trigger", (p, v) -> v.getVillagerBrain().setArmorWear(false)
    );

    public TriggerModule(String characterResourceName) {
        // Could use studio api to set up character goals here
        // Seems like a lot of work to add tho
    }

    /**
     * Looks for outgoing triggers from the last interaction
     * and executes {@link #triggerActions specific actions} associated with different trigger names
     * @param interaction Interaction object from a SendText request
     * @param player Player in the conversation
     * @param villager Villager in the conversation
     */
    public void updateMoveState(Interaction interaction, ServerPlayerEntity player, VillagerEntityMCA villager) {
        // Get triggers sent from server
        TriggerEvent[] triggerEvents = interaction.outgoingTriggers();
        for (TriggerEvent event : triggerEvents) {
            // Get the action for the trigger
            BiConsumer<ServerPlayerEntity, VillagerEntityMCA> action = triggerActions.get(event.trigger());

            // Execute the action if it exists
            if (action != null) {
                action.accept(player, villager);
            }
        }
    }


    /** This is temporary and will be moved somewhere else */
    private final static String goals_yaml = """
            intents:
              - name: 'follow-player-intent'
                training_phrases:
                  - "Follow me"
                  - "Come with me"
                  - "Follow"
                        
            goals:
              - name: 'follow-player'
                activation:\s
                  intent: 'follow-player-intent'
                repeatable: true
                actions:
                  - instruction: "tell {player} you're going to follow them."
                    send_trigger: 'follow-player'
            """;
}

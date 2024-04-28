package net.mca.entity.ai.chatAI;

import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.chatAI.inworldAIModules.*;
import net.mca.entity.ai.chatAI.inworldAIModules.api.Interaction;
import net.mca.entity.ai.chatAI.inworldAIModules.api.TriggerEvent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class InworldAI implements ChatAIStrategy {
    private final SessionModule sessionModule;
    private final RelationshipModule relationshipModule;
    private final TriggerModule triggerModule;
    private final EmotionModule emotionModule;

    public InworldAI(String resourceName) {
        this.sessionModule = new SessionModule(resourceName);
        this.relationshipModule = new RelationshipModule();
        this.triggerModule = new TriggerModule();
        this.emotionModule = new EmotionModule();
    }

    // We don't need conversational memory. Inworld does that for us. (Within the same session, which is enough for us)

    /**
     * Informs the AI of the current relationship status and gets a reply to the player's message
     * @param player The player requesting the answer
     * @param villager The villager responding
     * @param msg The message
     * @return {@code Optional.EMPTY} on error, Optional containing the answer to a message on success
     */
    public Optional<String> answer(ServerPlayerEntity player, VillagerEntityMCA villager, String msg) {
        // Create character modifications for current relationship status
        TriggerEvent relationshipTrigger = relationshipModule.getRelationshipTrigger(player, villager);
        sessionModule.sendTrigger(player, relationshipTrigger);
        // Create character modification for intial mood (This will be sent every time, not ideal)
        TriggerEvent emotionTrigger = emotionModule.getEmotionTrigger(villager);
        sessionModule.sendTrigger(player, emotionTrigger);

        // Get response
        Optional<Interaction> optionalResponse = sessionModule.getResponse(player, msg);
        if (optionalResponse.isPresent()) {
            Interaction response = optionalResponse.get();
            // Use response data to update heart level
            relationshipModule.updateRelationship(response, player, villager);
            // Use response data to update move state
            triggerModule.processTriggers(response, player, villager);

            return Optional.of(String.join("", response.textList()));
        } else {
            player.sendMessage(Text.translatable("mca.ai_broken").formatted(Formatting.RED), false);
            return Optional.empty();
        }
    }

}

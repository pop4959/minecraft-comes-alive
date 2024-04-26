package net.mca.entity.ai.chatAI;

import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.chatAI.inworldAIModules.*;
import net.mca.entity.ai.chatAI.inworldAIModules.api.Interaction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class InworldAI implements ChatAIStrategy {
    private final SessionModule sessionModule;
    private final RelationshipModule relationshipModule;
    private final TriggerModule triggerModule;

    public InworldAI(String resourceName) {
        this.sessionModule = new SessionModule(resourceName);
        this.relationshipModule = new RelationshipModule();
        this.triggerModule = new TriggerModule(resourceName);
    }

    // We don't need conversational memory. Inworld does that for us. (Within the same session, which is enough for us)

    /**
     * Gets an answer for a specific message from player for villager.
     * @param player The player requesting the answer
     * @param villager The villager responding (currently unused, but could be used to make response influence emotions)
     * @param msg The message
     * @return {@code Optional.EMPTY} on error, Optional containing the answer to a message on success
     */
    public Optional<String> answer(ServerPlayerEntity player, VillagerEntityMCA villager, String msg) {
        // Get response
        Optional<Interaction> optionalResponse = sessionModule.getResponse(player, msg);
        if (optionalResponse.isPresent()) {
            Interaction response = optionalResponse.get();
            // Use response data to update heart level
            relationshipModule.updateRelationship(response, player, villager);
            // TODO: Use ResponseData to modify mood
            // Use response data to update move state
            triggerModule.updateMoveState(response, player, villager);

            return Optional.of(String.join("", response.textList()));
        } else {
            player.sendMessage(Text.translatable("mca.ai_broken").formatted(Formatting.RED), false);
            return Optional.empty();
        }
    }

}

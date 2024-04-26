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

    public InworldAI(String resourceName) {
        this.sessionModule = new SessionModule(resourceName);
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
        // Get required variables
        UUID playerID = player.getUuid();
        String playerName = player.getName().getString();

        // Get response
        Optional<Interaction> optionalResponse = sessionModule.getResponse(player, msg); //sessionModule.simpleSendTextRequest(msg, playerName, playerID);
        if (optionalResponse.isPresent()) {
            Interaction response = optionalResponse.get();
            // TODO: Use ResponseData to modify relationship etc.
            // TODO: Use ResponseData to modify mood
            // TODO: Use ResponseData to modify change state (follow, stay, etc.)
            // Optional

            return Optional.of(String.join("", response.textList()));
        } else {
            player.sendMessage(Text.translatable("mca.ai_broken").formatted(Formatting.RED), false);
            return Optional.empty();
        }
    }

}

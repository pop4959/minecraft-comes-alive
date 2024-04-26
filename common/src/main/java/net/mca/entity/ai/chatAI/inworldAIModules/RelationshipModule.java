package net.mca.entity.ai.chatAI.inworldAIModules;

import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.chatAI.inworldAIModules.api.Interaction;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Class to manage relationship updates supplied by Inworld responses
 */
public class RelationshipModule {

    private Interaction.RelationshipUpdate previousUpdate;

    public RelationshipModule() {
        previousUpdate = new Interaction.RelationshipUpdate(0,0,0,0,0);
    }

    /**
     * Updates the villager's heart level according to the difference in the relationshipUpdate of this and the last interaction
     * @param interaction The new interaction
     * @param player The player in this relationship
     * @param villager The villager whose heart level is getting updated
     */
    public void updateRelationship(Interaction interaction, ServerPlayerEntity player, VillagerEntityMCA villager) {
        Interaction.RelationshipUpdate update = interaction.relationshipUpdate();
        // Get difference to previous update, since relationshipUpdates are always an update on the current relationship state
        Interaction.RelationshipUpdate differences = new Interaction.RelationshipUpdate(
                update.trust() - previousUpdate.trust(),
                update.respect() - previousUpdate.respect(),
                update.familiar() - previousUpdate.familiar(),
                update.flirtatious() - previousUpdate.flirtatious(),
                update.attraction() - previousUpdate.attraction()
        );

        // Get total, with different weights applied to different relationship values
        // TODO: Some **serious*** balancing
        int weightedTotal = 1 * differences.trust()
                + 1 * differences.respect()
                + 1 * differences.familiar()
                + 1 * differences.flirtatious()
                + 1 * differences.attraction();

        int heartsUpdate = weightedTotal / 10;

        previousUpdate = update;

        villager.getVillagerBrain().getMemoriesForPlayer(player).modHearts(heartsUpdate);
    }
}

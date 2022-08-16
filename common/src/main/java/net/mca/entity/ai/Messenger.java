package net.mca.entity.ai;

import net.mca.Config;
import net.mca.entity.EntityWrapper;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.relationship.family.FamilyTree;
import net.mca.entity.ai.relationship.family.FamilyTreeNode;
import net.mca.resources.API;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public interface Messenger extends EntityWrapper {
    TargetPredicate CAN_RECEIVE = TargetPredicate.createNonAttackable();

    default boolean isSpeechImpaired() {
        return false;
    }

    default boolean isToYoungToSpeak() {
        return false;
    }

    default void playSpeechEffect() {

    }

    default DialogueType getDialogueType(PlayerEntity receiver) {
        return DialogueType.UNASSIGNED;
    }

    default MutableText getTranslatable(PlayerEntity target, String phraseId, Object... params) {
        String targetName;
        if (target.world instanceof ServerWorld world) {
            //todo won't work on a few client side use cases
            targetName = FamilyTree.get(world)
                    .getOrEmpty(target.getUuid())
                    .map(FamilyTreeNode::getName)
                    .filter(n -> !n.isEmpty())
                    .orElse(target.getName().getString());
        } else {
            targetName = target.getName().getString();
        }
        Object[] newParams = new Object[params.length + 1];
        System.arraycopy(params, 0, newParams, 1, params.length);
        newParams[0] = targetName;

        //also pass profession
        String professionString = "";
        if (!asEntity().isBaby() && asEntity() instanceof VillagerEntityMCA v) {
            professionString = "#P" + Registry.VILLAGER_PROFESSION.getId(v.getProfession()).getPath() + ".";
        }

        //and personality
        String personalityString = "";
        if (!asEntity().isBaby() && asEntity() instanceof VillagerEntityMCA v) {
            personalityString = "#E" + v.getVillagerBrain().getPersonality().name() + ".";
        }

        return Text.translatable(personalityString + professionString + "#T" + getDialogueType(target).name() + "." + phraseId, newParams);
    }

    default void sendChatToAllAround(String phrase, Object... params) {
        for (PlayerEntity player : asEntity().world.getPlayers(CAN_RECEIVE, asEntity(), asEntity().getBoundingBox().expand(20))) {
            float dist = player.distanceTo(asEntity());
            sendChatMessage(getTranslatable(player, phrase, params).formatted(dist < 10 ? Formatting.WHITE : Formatting.GRAY), player);
        }
    }

    default void sendChatMessage(PlayerEntity target, String phraseId, Object... params) {
        sendChatMessage(getTranslatable(target, phraseId, params), target);
    }

    default void sendChatMessage(MutableText message, Entity receiver) {
        // Infected villagers do not speak
        if (isSpeechImpaired()) {
            message = Text.translatable(API.getRandomSentence("zombie", message.getString()));
        } else if (isToYoungToSpeak()) {
            message = Text.translatable(API.getRandomSentence("baby", message.getString()));
        }

        MutableText textToSend = Text.literal(Config.getInstance().villagerChatPrefix).append(asEntity().getDisplayName()).append(": ").append(message);
        receiver.sendMessage(textToSend);

        playSpeechEffect();
    }

    default void sendEventMessage(Text message, PlayerEntity receiver) {
        receiver.sendMessage(message, true);
    }

    default void sendEventMessage(Text message) {
        if (!(this instanceof Entity)) {
            return; // Can't tell all
        }
        sendEventMessage(((Entity)this).world, message);
    }

    static void sendEventMessage(World world, Text message) {
        world.getPlayers().forEach(player -> player.sendMessage(message, true));
    }
}

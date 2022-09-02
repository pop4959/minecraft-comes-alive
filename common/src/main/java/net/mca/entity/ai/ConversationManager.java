package net.mca.entity.ai;

import net.mca.entity.VillagerEntityMCA;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class ConversationManager {
    private final VillagerEntityMCA entity;

    private final List<Message> pendingMessages = new LinkedList<>();

    public ConversationManager(VillagerEntityMCA entity) {
        this.entity = entity;
    }

    public void addMessage(Entity receiver, MutableText message) {
        addMessage(new TextMessage(receiver, message));
    }

    public void addMessage(Message message) {
        pendingMessages.add(message);
        message.entity = entity;
    }

    public Optional<Message> getCurrentMessage() {
        if (pendingMessages.size() > 0) {
            Message message = pendingMessages.get(0);
            if (message.stillValid()) {
                return Optional.of(message);
            } else {
                pendingMessages.remove(0);
            }
        }
        return Optional.empty();
    }

    public abstract static class Message {
        private final Entity receiver;
        VillagerEntityMCA entity;

        public final int validUntil;
        public static final int TIME_VALID = 20 * 60 * 5;

        private Message(Entity receiver) {
            this.receiver = receiver;
            this.validUntil = receiver.age + TIME_VALID;
        }

        public Entity getReceiver() {
            return receiver;
        }

        abstract public void deliver();

        public boolean stillValid() {
            return !receiver.isRemoved() && receiver.age < validUntil;
        }
    }

    public static class TextMessage extends Message {
        private final MutableText text;

        public TextMessage(Entity receiver, MutableText text) {
            super(receiver);
            this.text = text;
        }

        @Override
        public void deliver() {
            this.entity.sendChatMessage(text, getReceiver());
        }
    }

    public static class PhraseText extends Message {
        private final String text;

        public PhraseText(Entity receiver, String text) {
            super(receiver);
            this.text = text;
        }

        @Override
        public void deliver() {
            this.entity.sendChatMessage((PlayerEntity)getReceiver(), text);
        }
    }
}

package net.mca.client;

import net.mca.entity.CommonSpeechManager;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.Genetics;
import net.mca.util.LimitedLinkedHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.EntityTrackingSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

public class SpeechManager {
    public static final SpeechManager INSTANCE = new SpeechManager();

    private final LimitedLinkedHashMap<UUID, EntityTrackingSoundInstance> currentlyPlaying = new LimitedLinkedHashMap<>(10);

    private final Random threadSafeRandom = Random.createThreadSafe();

    public void onChatMessage(Text message, UUID sender) {
        if (CommonSpeechManager.INSTANCE.translations.containsKey(message.getContent())) {
            speak(CommonSpeechManager.INSTANCE.translations.get(message.getContent()), sender);
        } else {
            for (Text sibling : message.getSiblings()) {
                if (CommonSpeechManager.INSTANCE.translations.containsKey(sibling.getContent())) {
                    speak(CommonSpeechManager.INSTANCE.translations.get(sibling.getContent()), sender);
                }
            }
        }
    }

    private void speak(String phrase, UUID sender) {
        if (currentlyPlaying.containsKey(sender) && MinecraftClient.getInstance().getSoundManager().isPlaying(currentlyPlaying.get(sender))) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            VillagerEntityMCA villager = null;
            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof VillagerEntityMCA v && entity.getUuid().equals(sender)) {
                    villager = v;
                    break;
                }
            }

            if (villager != null) {
                if (villager.isSpeechImpaired()) return;
                if (villager.isToYoungToSpeak()) return;

                float pitch = villager.getSoundPitch();
                float gene = villager.getGenetics().getGene(Genetics.VOICE_TONE);
                int tone = Math.min(9, (int) Math.floor(gene * 10.0f));

                String gender = villager.getGenetics().getGender().binary().getDataName();
                Identifier sound = new Identifier("mca_voices", phrase.toLowerCase(Locale.ROOT) + "/" + gender + "_" + tone);

                if (client.world != null && client.player != null) {
                    Collection<Identifier> keys = client.getSoundManager().getKeys();
                    if (keys.contains(sound)) {
                        EntityTrackingSoundInstance instance = new EntityTrackingSoundInstance(SoundEvent.of(sound), SoundCategory.NEUTRAL, 1.0f, pitch, villager, threadSafeRandom.nextLong());
                        currentlyPlaying.put(sender, instance);
                        client.getSoundManager().play(instance);
                    }
                }
            }
        }
    }
}

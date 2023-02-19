package net.mca.entity;

import net.mca.client.SpeechManager;
import net.mca.util.LimitedLinkedHashMap;
import net.minecraft.client.sound.EntityTrackingSoundInstance;
import net.minecraft.text.Text;

import java.util.UUID;

public class CommonSpeechManager {
    public static final CommonSpeechManager INSTANCE = new CommonSpeechManager();

    public String lastResolvedKey;
    public LimitedLinkedHashMap<Text, String> translations = new LimitedLinkedHashMap<>(100);
}

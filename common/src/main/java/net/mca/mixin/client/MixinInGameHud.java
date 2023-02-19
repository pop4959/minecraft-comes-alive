package net.mca.mixin.client;

import net.mca.client.SpeechManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.ClientChatListener;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.network.MessageType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    @Shadow
    @Final
    private Map<MessageType, List<ClientChatListener>> listeners;

    @Inject(method = "<init>(Lnet/minecraft/client/MinecraftClient;)V", at = @At("TAIL"))
    public void mca$onInit(MinecraftClient client, CallbackInfo ci) {
        listeners.get(MessageType.CHAT).add(SpeechManager.INSTANCE);
        listeners.get(MessageType.SYSTEM).add(SpeechManager.INSTANCE);
    }
}

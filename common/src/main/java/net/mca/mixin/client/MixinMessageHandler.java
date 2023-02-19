package net.mca.mixin.client;

import net.mca.client.SpeechManager;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(MessageHandler.class)
public class MixinMessageHandler {
    @Inject(method = "onChatMessage(Lnet/minecraft/network/message/SignedMessage;Lnet/minecraft/network/message/MessageType$Parameters;)V", at = @At("TAIL"))
    public void mca$onInit(SignedMessage message, MessageType.Parameters params, CallbackInfo ci) {
        UUID sender = message.signedHeader().sender();
        SpeechManager.INSTANCE.onChatMessage(message.getContent(), sender);
    }
}

package net.mca.mixin.client;

import com.mojang.authlib.GameProfile;
import net.mca.client.SpeechManager;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MessageHandler.class)
public class MixinMessageHandler {
    @Inject(method = "onChatMessage(Lnet/minecraft/network/message/SignedMessage;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/network/message/MessageType$Parameters;)V", at = @At("TAIL"))
    public void mca$onInit(SignedMessage message, GameProfile sender, MessageType.Parameters params, CallbackInfo ci) {
        SpeechManager.INSTANCE.onChatMessage(message.getContent(), message.getSender());
    }
}

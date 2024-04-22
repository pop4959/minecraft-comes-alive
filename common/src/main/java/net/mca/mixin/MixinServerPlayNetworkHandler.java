package net.mca.mixin;

import net.mca.Config;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.chatAI.ChatAI;
import net.mca.entity.ai.chatAI.GPT3;
import net.mca.entity.ai.chatAI.InworldAI;
import net.mca.util.WorldUtils;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static net.mca.entity.VillagerLike.VILLAGER_NAME;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {
    @Shadow
    public ServerPlayerEntity player;

    private String normalize(String string) {
        return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    @Inject(method = "onChatMessage", at = @At("HEAD"))
    public void sendMessage(ChatMessageC2SPacket message, CallbackInfo ci) {
        if (Config.getInstance().enableVillagerChatAI) {
            String msg = StringUtils.normalizeSpace(message.chatMessage());
            if (!msg.startsWith("/")) {
                // Check if there's an eligible villager for the conversation
                Optional<VillagerEntityMCA> villager = ChatAI.getVillagerForConversation(player, msg);
                // Yes? => Talk to it
                villager.ifPresent(villagerEntityMCA -> runAsyncAnswerRequest(player, villagerEntityMCA, msg));
            }
        }
    }

    private void runAsyncAnswerRequest(ServerPlayerEntity player, VillagerEntityMCA villager, String msg) {
        CompletableFuture.runAsync(() -> {
            Optional<String> answer = ChatAI.answer(player, villager, msg);
            answer.ifPresent(a -> villager.conversationManager.addMessage(player, Text.literal(a)));
        });
    }
}


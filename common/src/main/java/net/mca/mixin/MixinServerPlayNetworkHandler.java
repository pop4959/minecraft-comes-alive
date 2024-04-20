package net.mca.mixin;

import net.mca.Config;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.GPT3;
import net.mca.entity.ai.InworldAI;
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
                List<VillagerEntityMCA> entities = WorldUtils.getCloseEntities(player.getWorld(), player, 32, VillagerEntityMCA.class);

                // talk to specific villager
                String search = normalize(msg);
                // Did we enter the first block and have this conversation already? Needed because Mixins can't return
                boolean talked = false;

                // Have a conversation with a specific villager. If the message contains the name of a villager, talk to that villager
                for (VillagerEntityMCA villager : entities) {
                    String name = normalize(villager.getTrackedValue(VILLAGER_NAME));
                    if (search.contains(name)) {
                        runAsyncAnswerRequest(villager, player, msg);
                        talked = true;
                        break;
                    }
                }

                // If the message was not directed at a specific villager, talk to the last villager the player talked to
                if (!talked) {
                    for (VillagerEntityMCA villager : entities) {
                        if (GPT3.inConversationWith(villager, player) || InworldAI.inConversationWith(villager, player)) {
                            runAsyncAnswerRequest(villager, player, msg);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void runAsyncAnswerRequest(VillagerEntityMCA villager, ServerPlayerEntity player, String msg) {
        CompletableFuture.runAsync(() -> {
            Optional<String> answer;
            // Check if villager is managed by InworldAI
            if (Config.getInstance().enableInworldAI && InworldAI.villagerManagedByInworld(villager.getUuid())) {
                answer = InworldAI.answer(player, villager, msg);
            } else {
                answer = GPT3.answer(player, villager, msg);
            }
            answer.ifPresent(a -> villager.conversationManager.addMessage(player, Text.literal(a)));
        });
    }
}


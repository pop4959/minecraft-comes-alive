package net.mca.mixin.client;

import net.mca.Config;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.PTG3;
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
                List<VillagerEntityMCA> entities = WorldUtils.getCloseEntities(player.world, player, 32, VillagerEntityMCA.class);

                // talk to specific villager
                String search = normalize(msg);
                boolean talked = false;
                for (VillagerEntityMCA villager : entities) {
                    String name = normalize(villager.getTrackedValue(VILLAGER_NAME));
                    if (search.contains(name)) {
                        CompletableFuture.runAsync(() -> {
                            villager.conversationManager.addMessage(player, Text.literal(PTG3.answer(player, villager, msg)));
                        });
                        talked = true;
                        break;
                    }
                }

                // continue convo
                if (!talked) {
                    for (VillagerEntityMCA villager : entities) {
                        if (PTG3.inConversationWith(villager, player)) {
                            CompletableFuture.runAsync(() -> {
                                villager.conversationManager.addMessage(player, Text.literal(PTG3.answer(player, villager, msg)));
                            });
                            break;
                        }
                    }
                }
            }
        }
    }
}

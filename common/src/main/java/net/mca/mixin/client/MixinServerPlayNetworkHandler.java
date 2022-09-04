package net.mca.mixin.client;

import net.mca.Config;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.PTG3;
import net.mca.util.WorldUtils;
import net.minecraft.server.filter.TextStream;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static net.mca.entity.VillagerLike.VILLAGER_NAME;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "handleMessage(Lnet/minecraft/server/filter/TextStream$Message;)V", at = @At("HEAD"))
    public void sendMessage(TextStream.Message message, CallbackInfo ci) {
        if (!Config.getInstance().enableVillagerChatAI) {
            return;
        }

        String msg = message.getRaw();
        if (!msg.startsWith("/")) {
            HashSet<String> search = new HashSet<>(Arrays.asList(msg.toLowerCase(Locale.ROOT).split("\\P{L}+")));
            List<VillagerEntityMCA> entities = WorldUtils.getCloseEntities(player.world, player, 32, VillagerEntityMCA.class);

            // talk to specific villager
            boolean talked = false;
            for (VillagerEntityMCA villager : entities) {
                if (search.contains(villager.getTrackedValue(VILLAGER_NAME).toLowerCase(Locale.ROOT))) {
                    PTG3.answer(player, villager, msg, (response) -> {
                        villager.conversationManager.addMessage(player, new LiteralText(response));
                    });
                    talked = true;
                    break;
                }
            }

            // continue convo
            if (!talked) {
                for (VillagerEntityMCA villager : entities) {
                    if (PTG3.inConversationWith(villager, player)) {
                        PTG3.answer(player, villager, msg, (response) -> {
                            villager.conversationManager.addMessage(player, new LiteralText(response));
                        });
                        break;
                    }
                }
            }
        }
    }
}

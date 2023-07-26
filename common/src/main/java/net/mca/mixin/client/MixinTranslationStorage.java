package net.mca.mixin.client;

import net.mca.entity.CommonSpeechManager;
import net.mca.entity.ai.DialogueType;
import net.mca.util.localization.PooledTranslationStorage;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.util.Language;
import net.minecraft.util.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = TranslationStorage.class, priority = 990)
abstract class MixinTranslationStorage extends Language {
    @Shadow
    private @Final Map<String, String> translations;

    private PooledTranslationStorage pool;

    private PooledTranslationStorage getPool() {
        if (pool == null) {
            pool = new PooledTranslationStorage(translations);
        }
        return pool;
    }

    @Inject(method = "get(Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void mca$onGet(String key, CallbackInfoReturnable<String> info) {
        key = DialogueType.applyFallback(key);

        Pair<String, String> unpooled = getPool().get(key);
        if (unpooled != null) {
            CommonSpeechManager.INSTANCE.lastResolvedKey = unpooled.getLeft();
            info.setReturnValue(unpooled.getRight());
        } else {
            CommonSpeechManager.INSTANCE.lastResolvedKey = null;
            if (translations.containsKey(key)) {
                info.setReturnValue(translations.get(key));
            }
        }
    }

    @Inject(method = "hasTranslation(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
    public void mca$onHasTranslation(String key, CallbackInfoReturnable<Boolean> info) {
        if (getPool().contains(key)) {
            info.setReturnValue(true);
        }
    }
}

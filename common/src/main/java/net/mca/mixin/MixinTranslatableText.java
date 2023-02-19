package net.mca.mixin;

import net.mca.entity.CommonSpeechManager;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TranslatableText.class)
public class MixinTranslatableText {
    @Inject(method = "updateTranslations()V", at = @At("TAIL"))
    private void updateTranslations(CallbackInfo ci) {
        if (CommonSpeechManager.INSTANCE.lastResolvedKey != null) {
            CommonSpeechManager.INSTANCE.translations.put((Text)this, CommonSpeechManager.INSTANCE.lastResolvedKey);
        }
    }
}

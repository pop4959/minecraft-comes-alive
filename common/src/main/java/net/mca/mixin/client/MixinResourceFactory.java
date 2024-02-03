package net.mca.mixin.client;

import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@Mixin(ResourceFactory.class)
public interface MixinResourceFactory {
    @Inject(method = "open(Lnet/minecraft/util/Identifier;)Ljava/io/InputStream;", at = @At("HEAD"), cancellable = true)
    default void immersiveChatter$injectOpen(Identifier resourceLocation, CallbackInfoReturnable<InputStream> cir) throws FileNotFoundException {
        if (resourceLocation.getPath().startsWith("sounds/tts_cache/")) {
            cir.setReturnValue(new FileInputStream(resourceLocation.getPath().replace("sounds/", "")));
        }
    }
}

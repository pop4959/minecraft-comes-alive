package mca.mixin;

import net.minecraft.entity.ai.brain.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Activity.class)
public interface MixinActivity {
    @Invoker("register")
    static Activity register(String id) {
        return null;
    }
}

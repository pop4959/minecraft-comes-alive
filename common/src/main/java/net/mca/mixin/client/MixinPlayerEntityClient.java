package net.mca.mixin.client;

import net.mca.Config;
import net.mca.MCAClient;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
abstract class MixinPlayerEntityClient extends LivingEntity {
    protected MixinPlayerEntityClient(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "getActiveEyeHeight(Lnet/minecraft/entity/EntityPose;Lnet/minecraft/entity/EntityDimensions;)F", at = @At("HEAD"), cancellable = true)
    public void mca$getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if (Config.getInstance().adjustPlayerEyesToHeight) {
            MCAClient.getPlayerData(getUuid()).ifPresent(data -> {
                switch (pose) {
                    case SWIMMING, FALL_FLYING, SPIN_ATTACK -> {
                        cir.setReturnValue(0.4f * data.getRawScaleFactor());
                    }
                    case CROUCHING -> {
                        cir.setReturnValue(1.27f * data.getRawScaleFactor());
                    }
                    default -> {
                        cir.setReturnValue(1.62f * data.getRawScaleFactor());
                    }
                }
            });
        }
    }
}

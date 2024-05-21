package net.mca.mixin;

import net.mca.server.SpawnQueue;
import net.mca.server.world.data.VillageManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.mca.server.ReaperSpawner.REAPER_TICKET;

@Mixin(ServerWorld.class)
abstract class MixinServerWorld extends World implements StructureWorldAccess {
    @Shadow @Final private static Logger LOGGER;

    MixinServerWorld() { super(null, null, null, null, null, true, false, 0, 0);}

    @Inject(method = "addEntity(Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddEntity(Entity entity, CallbackInfoReturnable<Boolean> info) {
        if (SpawnQueue.getInstance().addVillager(entity)) {
            info.setReturnValue(false);
        }
    }
    @Inject(method = "onBlockChanged(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;)V",
            at = @At("HEAD")
    )
    public void onOnBlockChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo info) {
        if (oldBlock.getBlock() != newBlock.getBlock()) {
            final ServerWorld self = (ServerWorld)(Object)this;
            final ServerChunkManager chunkManager = self.getChunkManager();
            final ChunkPos chunkPos = new ChunkPos(pos);
            LOGGER.info("On world changed tick at %d, %d: %d".formatted(chunkPos.x, chunkPos.z, self.getServer().getTicks()));
            chunkManager.addTicket(REAPER_TICKET, chunkPos, 1, pos);
            self.getServer().execute(() ->
                    VillageManager.get(self).getReaperSpawner().trySpawnReaper(self, newBlock, pos)
            );
        }
    }
}

@Mixin(ProtoChunk.class)
abstract class MixinProtoChunk extends Chunk {
    MixinProtoChunk() {super(null, null, null, null, 0, null, null);}

    @Inject(method = "addEntity(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void onAddEntity(Entity entity, CallbackInfo info) {
        if (SpawnQueue.getInstance().addVillager(entity)) {
            info.cancel();
        }
    }
}
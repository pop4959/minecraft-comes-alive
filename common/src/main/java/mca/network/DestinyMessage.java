package mca.network;

import mca.cobalt.network.Message;
import mca.util.WorldUtils;
import mca.util.compat.FuzzyPositionsCompat;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.Optional;

public class DestinyMessage implements Message {
    private final String location;

    public DestinyMessage(String location) {
        this.location = location;
    }

    @Override
    public void receive(PlayerEntity e) {
        Optional<BlockPos> position = WorldUtils.getClosestStructurePosition((ServerWorld)e.world, e.getBlockPos(), new Identifier(location), 256);
        position.ifPresentOrElse(pos -> {
            e.world.getWorldChunk(pos);
            pos = e.world.getTopPosition(Heightmap.Type.WORLD_SURFACE, pos);
            pos = FuzzyPositionsCompat.downWhile(pos, 1, p -> !e.world.getBlockState(p.down()).isFullCube(e.world, p));
            pos = FuzzyPositionsCompat.upWhile(pos, e.world.getHeight(), p -> e.world.getBlockState(p).shouldSuffocate(e.world, p));

            e.clearStatusEffects();
            e.teleport(pos.getX(), pos.getY(), pos.getZ());
        }, () -> {
            BlockPos pos = ((ServerWorld)e.world).getSpawnPos();
            e.teleport(pos.getX(), pos.getY(), pos.getZ());
        });

        //story
        e.sendMessage(new TranslatableText("destiny.story.reason"), false);
        e.sendMessage(new TranslatableText(location.equals("shipwreck_beached") ? "destiny.story.sailing" : "destiny.story.travelling"), false);
        e.sendMessage(new TranslatableText("destiny.story." + location), false);
    }
}

package mca.network.c2s;

import mca.Config;
import mca.cobalt.network.Message;
import mca.util.WorldUtils;
import mca.util.compat.FuzzyPositionsCompat;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;

import java.util.Optional;

public class DestinyMessage implements Message {
    private final String location;

    public DestinyMessage(String location) {
        this.location = location;
    }

    @Override
    public void receive(ServerPlayerEntity player) {
        if (Config.getInstance().allowDestinyTeleportation && location != null) {
            WorldUtils.getClosestStructurePosition(player.getWorld(), player.getBlockPos(), new Identifier(location), 256).ifPresent(pos -> {
                player.getWorld().getWorldChunk(pos);
                pos = player.getWorld().getTopPosition(Heightmap.Type.WORLD_SURFACE, pos);
                pos = FuzzyPositionsCompat.downWhile(pos, 1, p -> !player.getWorld().getBlockState(p.down()).isFullCube(player.getWorld(), p));
                pos = FuzzyPositionsCompat.upWhile(pos, player.getWorld().getHeight(), p -> player.getWorld().getBlockState(p).shouldSuffocate(player.getWorld(), p));
                player.teleport(pos.getX(), pos.getY(), pos.getZ());
            });
        }

        if (player.isSpectator()) {
            player.changeGameMode(GameMode.SURVIVAL);
        }
    }
}

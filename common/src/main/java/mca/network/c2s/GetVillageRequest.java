package mca.network.c2s;

import mca.cobalt.network.Message;
import mca.cobalt.network.NetworkHandler;
import mca.network.s2c.GetVillageFailedResponse;
import mca.network.s2c.GetVillageResponse;
import mca.resources.Rank;
import mca.resources.Tasks;
import mca.server.world.data.Village;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.Serial;
import java.util.Optional;
import java.util.Set;

public class GetVillageRequest implements Message {
    @Serial
    private static final long serialVersionUID = -1302412553466016247L;

    @Override
    public void receive(ServerPlayerEntity player) {
        Optional<Village> village = Village.findNearest(player);
        if (village.isPresent() && !village.get().getBuildings().isEmpty()) {
            int reputation = village.get().getReputation(player);
            Rank rank = Tasks.getRank(village.get(), player);
            Set<String> ids = Tasks.getCompletedIds(village.get(), player);
            NetworkHandler.sendToPlayer(new GetVillageResponse(village.get(), rank, reputation, ids), player);
        } else {
            NetworkHandler.sendToPlayer(new GetVillageFailedResponse(), player);
        }
    }
}

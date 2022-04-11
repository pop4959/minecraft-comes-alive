package mca.network.c2s;

import mca.ClientProxy;
import mca.network.NbtDataMessage;
import mca.resources.API;
import mca.resources.Rank;
import mca.resources.Tasks;
import mca.resources.data.BuildingType;
import mca.resources.data.tasks.Task;
import mca.server.world.data.Village;
import net.minecraft.entity.player.PlayerEntity;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GetVillageResponse extends NbtDataMessage {
    @Serial
    private static final long serialVersionUID = 4882425683460617550L;

    public final Rank rank;
    public final int reputation;
    public final Set<String> ids;
    public final Map<Rank, List<Task>> tasks;
    public final Map<String, BuildingType> buildingTypes;

    public GetVillageResponse(Village data, Rank rank, int reputation, Set<String> ids) {
        super(data.save());
        this.rank = rank;
        this.reputation = reputation;
        this.ids = ids;
        this.tasks = Tasks.getInstance().tasks;
        this.buildingTypes = API.getVillagePool().getBuildingTypes();
    }

    @Override
    public void receive() {
        ClientProxy.getNetworkHandler().handleVillageDataResponse(this);
    }
}

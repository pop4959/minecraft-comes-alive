package mca;

import mca.cobalt.network.NetworkHandler;
import mca.entity.VillagerEntityMCA;
import mca.entity.VillagerLike;
import mca.network.c2s.PlayerDataRequest;

import java.util.*;

import static mca.client.model.CommonVillagerModel.getVillager;

public class MCAClient {
    public static VillagerEntityMCA fallbackVillager;
    public static Map<UUID, VillagerLike<?>> playerData = new HashMap<>();
    public static Set<UUID> playerDataRequests = new HashSet<>();

    public static void onInitializeClient() {

    }

    public static boolean useMCARenderer(UUID uuid) {
        if (Config.getInstance().letPlayerCustomize) {
            if (!MCAClient.playerDataRequests.contains(uuid)) {
                MCAClient.playerDataRequests.add(uuid);
                NetworkHandler.sendToServer(new PlayerDataRequest(uuid));
            }
            return MCAClient.playerData.containsKey(uuid);
        }
        return false;
    }

    public static boolean useMCAModel(UUID uuid) {
        return useMCARenderer(uuid) && !MCAClient.playerData.get(uuid).usePlayerSkin();
    }
}

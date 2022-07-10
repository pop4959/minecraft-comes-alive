package mca;

import mca.cobalt.network.NetworkHandler;
import mca.entity.VillagerEntityMCA;
import mca.entity.VillagerLike;
import mca.network.c2s.PlayerDataRequest;
import net.minecraft.client.MinecraftClient;

import java.util.*;

public class MCAClient {
    public static VillagerEntityMCA fallbackVillager;
    public static Map<UUID, VillagerLike<?>> playerData = new HashMap<>();
    public static Set<UUID> playerDataRequests = new HashSet<>();

    private static final DestinyManager destinyManager = new DestinyManager();

    public static DestinyManager getDestinyManager() {
        return destinyManager;
    }

    public static void onInitializeClient() {

    }

    public static boolean useGeneticsRenderer(UUID uuid) {
        if (Config.getInstance().enableVillagerPlayerModel) {
            if (!MCAClient.playerDataRequests.contains(uuid)) {
                MCAClient.playerDataRequests.add(uuid);
                NetworkHandler.sendToServer(new PlayerDataRequest(uuid));
            }
            return MCAClient.playerData.containsKey(uuid) && MCAClient.playerData.get(uuid).getPlayerModel() != VillagerLike.PlayerModel.VANILLA;
        }
        return false;
    }

    public static boolean useVillagerRenderer(UUID uuid) {
        return useGeneticsRenderer(uuid) && MCAClient.playerData.get(uuid).getPlayerModel() == VillagerLike.PlayerModel.VILLAGER;
    }

    public static void tickClient(MinecraftClient client) {
        destinyManager.tick(client);
    }
}

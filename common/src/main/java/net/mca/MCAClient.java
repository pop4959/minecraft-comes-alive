package net.mca;

import net.mca.cobalt.network.NetworkHandler;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.VillagerLike;
import net.mca.network.c2s.PlayerDataRequest;
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

    public static void onLogin() {
        playerDataRequests.clear();
    }

    public static boolean useGeneticsRenderer(UUID uuid) {
        if (MCA.isPlayerRendererAllowed()) {
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

    public static boolean renderArms(UUID uuid, String key) {
        return useVillagerRenderer(uuid) &&
                Config.getInstance().playerRendererBlacklist.entrySet().stream()
                        .filter(entry -> entry.getValue().equals("arms") || entry.getValue().equals(key))
                        .noneMatch(entry -> MCA.doesModExist(entry.getKey()));
    }

    public static boolean renderArms(UUID uuid) {
        return renderArms(uuid, "arms");
    }

    public static void tickClient(MinecraftClient client) {
        destinyManager.tick(client);
    }
}

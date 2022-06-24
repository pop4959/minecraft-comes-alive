package mca;

import mca.client.gui.DestinyScreen;
import net.minecraft.client.MinecraftClient;

public class DestinyManager {
    private boolean openDestiny;
    private boolean allowTeleportation;
    private boolean allowPlayerModel;
    private boolean allowVillagerModel;

    public void tick(MinecraftClient client) {
        if (openDestiny && client.currentScreen == null) {
            openDestiny = false;

            assert client.player != null;
            client.setScreen(new DestinyScreen(client.player.getUuid(), allowTeleportation, allowPlayerModel, allowVillagerModel));
        }
    }

    public void requestOpen(boolean allowTeleportation, boolean allowPlayerModel, boolean allowVillagerModel) {
        this.openDestiny = true;
        this.allowTeleportation = allowTeleportation;
        this.allowPlayerModel = allowPlayerModel;
        this.allowVillagerModel = allowVillagerModel;
    }
}

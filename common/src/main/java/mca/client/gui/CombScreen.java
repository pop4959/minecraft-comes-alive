package mca.client.gui;

import mca.MCA;
import mca.cobalt.network.NetworkHandler;
import mca.network.c2s.DamageItemMessage;

import java.util.UUID;

public class CombScreen extends VillagerEditorScreen {
    public CombScreen(UUID playerUUID) {
        super(playerUUID, playerUUID);
    }

    public CombScreen(UUID villagerUUID, UUID playerUUID) {
        super(villagerUUID, playerUUID);
    }

    @Override
    protected boolean shouldShowPageSelection() {
        return false;
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    protected void eventCallback(String event) {
        if (event.equals("hair")) {
            NetworkHandler.sendToServer(new DamageItemMessage(MCA.locate("comb")));
        }
    }

    @Override
    protected void setPage(String page) {
        if (page.equals("loading")) {
            super.setPage("loading");
        } else if (page.equals("head")) {
            syncVillagerData();
            close();
        } else {
            super.setPage("hair");
        }
    }
}

package mca.client.gui;

import mca.MCA;
import mca.cobalt.network.NetworkHandler;
import mca.network.c2s.DamageItemMessage;
import mca.network.c2s.SkinListRequest;

import java.util.Objects;
import java.util.UUID;

public class NeedleScreen extends VillagerEditorScreen {
    public NeedleScreen(UUID playerUUID) {
        super(playerUUID, playerUUID);
    }

    public NeedleScreen(UUID villagerUUID, UUID playerUUID) {
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
        if (event.equals("clothing")) {
            NetworkHandler.sendToServer(new DamageItemMessage(MCA.locate("needle_and_thread")));
        }
    }

    @Override
    protected void setPage(String page) {
        if (page.equals("loading")) {
            super.setPage("loading");
        } else if (page.equals("body")) {
            syncVillagerData();
            close();
        } else {
            super.setPage("clothing");
        }
    }
}

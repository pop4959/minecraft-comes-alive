package mca.client.gui;

import java.util.UUID;

public class LimitedVillagerEditorScreen extends VillagerEditorScreen {
    public LimitedVillagerEditorScreen(UUID villagerUUID, UUID playerUUID) {
        super(villagerUUID, playerUUID);
    }

    @Override
    protected boolean shouldShowPageSelection() {
        return false;
    }

    @Override
    protected boolean shouldUsePlayerModel() {
        return villagerData.contains("usePlayerSkin");
    }

    @Override
    protected void setPage(String page) {
        this.page = page;

        if (page.equals("general")) {
            int y = height / 2 - 40;

            //name
            drawName(width / 2, y);
            y += 24;

            //gender
            drawGender(width / 2, y);
            y += 24;

            //which model to use
            if (villagerUUID.equals(playerUUID)) {
                drawModel(width / 2, y);
            }
        }
    }
}

package mca.network;

import mca.network.s2c.*;

public interface ClientInteractionManager {
    void handleGuiRequest(OpenGuiRequest message);

    void handleFamilyTreeResponse(GetFamilyTreeResponse message);

    void handleInteractDataResponse(GetInteractDataResponse message);

    void handleVillageDataResponse(GetVillageResponse message);

    void handleVillageDataFailedResponse(GetVillageFailedResponse message);

    void handleFamilyDataResponse(GetFamilyResponse message);

    void handleVillagerDataResponse(GetVillagerResponse message);

    void handleDialogueResponse(InteractionDialogueResponse message);

    void handleChildData(GetChildDataResponse message);

    void handleSkinListResponse(AnalysisResults message);

    void handleBabyNameResponse(BabyNameResponse message);

    void handleVillagerNameResponse(VillagerNameResponse message);

    void handleToastMessage(ShowToastRequest message);

    void handleFamilyTreeUUIDResponse(FamilyTreeUUIDResponse response);

    void handlePlayerDataMessage(PlayerDataMessage response);

    void handleSkinListResponse(SkinListResponse response);

    void handleDestinyGuiRequest(OpenDestinyGuiRequest request);
}

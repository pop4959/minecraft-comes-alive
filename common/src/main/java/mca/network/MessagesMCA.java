package mca.network;

import mca.cobalt.network.NetworkHandler;
import mca.network.s2c.*;
import mca.network.c2s.*;

public interface MessagesMCA {
    static void bootstrap() {
        NetworkHandler.registerMessage(InteractionVillagerMessage.class);
        NetworkHandler.registerMessage(BabyNamingVillagerMessage.class);
        NetworkHandler.registerMessage(GetFamilyRequest.class);
        NetworkHandler.registerMessage(GetFamilyResponse.class);
        NetworkHandler.registerMessage(GetVillagerResponse.class);
        NetworkHandler.registerMessage(CallToPlayerMessage.class);
        NetworkHandler.registerMessage(GetVillageRequest.class);
        NetworkHandler.registerMessage(GetVillageResponse.class);
        NetworkHandler.registerMessage(GetVillageFailedResponse.class);
        NetworkHandler.registerMessage(OpenGuiRequest.class);
        NetworkHandler.registerMessage(ReportBuildingMessage.class);
        NetworkHandler.registerMessage(SaveVillageMessage.class);
        NetworkHandler.registerMessage(GetFamilyTreeRequest.class);
        NetworkHandler.registerMessage(GetFamilyTreeResponse.class);
        NetworkHandler.registerMessage(GetInteractDataRequest.class);
        NetworkHandler.registerMessage(GetInteractDataResponse.class);
        NetworkHandler.registerMessage(InteractionDialogueMessage.class);
        NetworkHandler.registerMessage(InteractionDialogueResponse.class);
        NetworkHandler.registerMessage(InteractionDialogueInitMessage.class);
        NetworkHandler.registerMessage(GetChildDataRequest.class);
        NetworkHandler.registerMessage(GetChildDataResponse.class);
        NetworkHandler.registerMessage(GetVillagerRequest.class);
        NetworkHandler.registerMessage(VillagerEditorSyncRequest.class);
        NetworkHandler.registerMessage(AnalysisResults.class);
        NetworkHandler.registerMessage(InteractionCloseRequest.class);
        NetworkHandler.registerMessage(ShowToastRequest.class);
        NetworkHandler.registerMessage(BabyNameRequest.class);
        NetworkHandler.registerMessage(BabyNameResponse.class);
        NetworkHandler.registerMessage(VillagerNameRequest.class);
        NetworkHandler.registerMessage(VillagerNameResponse.class);
        NetworkHandler.registerMessage(RenameVillageMessage.class);
        NetworkHandler.registerMessage(FamilyTreeUUIDLookup.class);
        NetworkHandler.registerMessage(FamilyTreeUUIDResponse.class);
        NetworkHandler.registerMessage(DestinyMessage.class);
        NetworkHandler.registerMessage(PlayerDataMessage.class);
        NetworkHandler.registerMessage(PlayerDataRequest.class);
    }
}

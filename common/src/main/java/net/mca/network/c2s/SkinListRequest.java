package net.mca.network.c2s;

import net.mca.cobalt.network.Message;
import net.mca.cobalt.network.NetworkHandler;
import net.mca.network.s2c.SkinListResponse;
import net.mca.resources.ClothingList;
import net.mca.resources.HairList;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.Serial;

public class SkinListRequest implements Message {
    @Serial
    private static final long serialVersionUID = -6508206556519152120L;

    @Override
    public void receive(ServerPlayerEntity player) {
        NetworkHandler.sendToPlayer(new SkinListResponse(ClothingList.getInstance().clothing, HairList.getInstance().hair), player);
    }
}

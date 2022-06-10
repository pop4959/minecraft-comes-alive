package mca.network.s2c;

import mca.ClientProxy;
import mca.cobalt.network.Message;
import mca.resources.ClothingList;
import mca.resources.HairList;

import java.util.HashMap;

public class SkinListResponse implements Message {
    private final HashMap<String, ClothingList.Clothing> clothing;
    private final HashMap<String, HairList.Hair> hair;

    public SkinListResponse(HashMap<String, ClothingList.Clothing> clothing, HashMap<String, HairList.Hair> hair) {
        this.clothing = clothing;
        this.hair = hair;
    }

    @Override
    public void receive() {
        ClientProxy.getNetworkHandler().handleSkinListResponse(this);
    }

    public HashMap<String, ClothingList.Clothing> getClothing() {
        return clothing;
    }

    public HashMap<String, HairList.Hair> getHair() {
        return hair;
    }
}

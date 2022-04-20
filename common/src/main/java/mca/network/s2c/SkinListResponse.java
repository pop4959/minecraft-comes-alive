package mca.network.s2c;

import mca.ClientProxy;
import mca.cobalt.network.Message;

import java.util.List;

public class SkinListResponse implements Message {
    private final List<String> clothing;

    public SkinListResponse(List<String> clothing) {
        this.clothing = clothing;
    }

    @Override
    public void receive() {
        ClientProxy.getNetworkHandler().handleAnalysisResults(this);
    }

    public List<String> getClothing() {
        return clothing;
    }
}

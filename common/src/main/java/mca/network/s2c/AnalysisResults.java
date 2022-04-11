package mca.network.s2c;

import mca.ClientProxy;
import mca.cobalt.network.Message;
import mca.resources.data.analysis.Analysis;

import java.io.Serial;

public class AnalysisResults implements Message {
    @Serial
    private static final long serialVersionUID = 2451914344295985363L;

    public final Analysis<?> analysis;

    public AnalysisResults(Analysis<?> analysis) {
        this.analysis = analysis;
    }

    @Override
    public void receive() {
        ClientProxy.getNetworkHandler().handleAnalysisResults(this);
    }
}

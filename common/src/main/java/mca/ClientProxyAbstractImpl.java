package mca;

import mca.network.c2s.ClientInteractionManager;
import mca.network.c2s.ClientInteractionManagerImpl;

/**
 * Workaround for Forge's BS
 */
public abstract class ClientProxyAbstractImpl extends ClientProxy.Impl {

    private final ClientInteractionManager networkHandler = new ClientInteractionManagerImpl();

    @Override
    public final ClientInteractionManager getNetworkHandler() {
        return networkHandler;
    }
}

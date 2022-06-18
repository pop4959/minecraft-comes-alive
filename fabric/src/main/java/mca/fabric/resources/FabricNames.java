package mca.fabric.resources;

import mca.resources.Names;
import mca.resources.Tasks;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.util.Identifier;

public class FabricNames extends Names implements IdentifiableResourceReloadListener {
    @Override
    public Identifier getFabricId() {
        return ID;
    }
}

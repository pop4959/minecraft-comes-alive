package net.mca.item;

import dev.architectury.registry.CreativeTabRegistry;
import net.mca.MCA;
import net.minecraft.item.ItemGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public interface ItemGroupMCA {
    @SuppressWarnings("Convert2MethodRef")
    ItemGroup MCA_GROUP = CreativeTabRegistry.create(
            Text.of("mca.mca_tab"), // TODO: Verify (1.20)
            () -> ItemsMCA.ENGAGEMENT_RING.get().getDefaultStack()
    );
}

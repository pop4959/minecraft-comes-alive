package mca.advancement.criterion;

import mca.MCA;
import net.minecraft.util.Identifier;

public class BabySirbenSmeltedCriterion extends BabySmeltedCriterion {
    private static final Identifier ID = MCA.locate("baby_sirben_smelted");

    @Override
    public Identifier getId() {
        return ID;
    }
}

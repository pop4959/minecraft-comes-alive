package mca.block;

import java.util.function.BiFunction;

import mca.MCA;
import mca.cobalt.registration.Registration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

public interface BlockEntityTypesMCA {

    BlockEntityType<TombstoneBlock.Data> TOMBSTONE = register("tombstone", TombstoneBlock.Data::new, BlocksMCA.GRAVELLING_HEADSTONE, BlocksMCA.UPRIGHT_HEADSTONE, BlocksMCA.SLANTED_HEADSTONE, BlocksMCA.CROSS_HEADSTONE, BlocksMCA.WALL_HEADSTONE);

    static void bootstrap() {
    }

    static <T extends BlockEntity> BlockEntityType<T> register(String name, BiFunction<BlockPos, BlockState, T> factory, Block...blocks) {
        Identifier id = new Identifier(MCA.MOD_ID, name);
        return Registration.register(Registry.BLOCK_ENTITY_TYPE, id, Registration.ObjectBuilders.BlockEntityTypes.create(id, factory, blocks));
    }
}

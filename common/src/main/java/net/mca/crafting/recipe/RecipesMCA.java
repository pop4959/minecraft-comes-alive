package net.mca.crafting.recipe;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.mca.MCA;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public interface RecipesMCA {

    DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(MCA.MOD_ID, RegistryKeys.RECIPE_SERIALIZER);

   // TODO: See 55a267864e93b0a37a86506a85993953735b8979
   // - This 110% needs to be reimplemented, commit id is for the prior 1.17- version
   // RecipeType<PressingRecipe> PRESSING = type("pressing");
   // RegistrySupplier<RecipeSerializer<PressingRecipe>> PRESSING_SERIALIZER = serializer("pressing", PressingRecipe.Serializer::new);

    static void bootstrap() {
        RECIPE_SERIALIZERS.register();
    }

    // TODO: Migrate to Deferred when Mojang inevitably freezes this registry
//    static <T extends Recipe<?>> RecipeType<T> type(String name) {
//        Identifier id = new Identifier(MCA.MOD_ID, name);
//        return Registry.register(Registry.RECIPE_TYPE, id, new RecipeType<>() {
//            @Override
//            public String toString() {
//                return name;
//            }
//        });
//    }

    static <T extends RecipeSerializer<?>> RegistrySupplier<T> serializer(String name, Supplier<T> obj) {
        Identifier id = new Identifier(MCA.MOD_ID, name);
        return RECIPE_SERIALIZERS.register(id, obj);
    }
}

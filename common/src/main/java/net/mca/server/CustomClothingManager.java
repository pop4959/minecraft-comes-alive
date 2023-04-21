package net.mca.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.mca.resources.ClothingList;
import net.mca.resources.HairList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class CustomClothingManager {
    public static Storage<ClothingList.Clothing> getClothing(MinecraftServer server, String user) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate((nbt) -> new Storage<>(nbt, ClothingList.Clothing::new), Storage::new, "immersive_library_clothing_" + user);
    }

    public static Storage<HairList.Hair> getHair(MinecraftServer server, String user) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate((nbt) -> new Storage<>(nbt, HairList.Hair::new), Storage::new, "immersive_library_hair_" + user);
    }


    public static class Storage<T extends ClothingList.ListEntry> extends PersistentState {
        final Map<Identifier, T> entries = new HashMap<>();

        public Storage() {
        }

        public Storage(NbtCompound nbt, BiFunction<String, JsonObject, T> entryFromNbt) {
            Gson gson = new Gson();
            for (String identifier : nbt.getKeys()) {
                entries.put(new Identifier(identifier), entryFromNbt.apply(identifier, gson.fromJson(nbt.getString(identifier), JsonObject.class)));
            }
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt) {
            NbtCompound c = new NbtCompound();
            for (Map.Entry<Identifier, T> entry : entries.entrySet()) {
                c.putString(entry.getKey().toString(), entry.getValue().toJson().toString());
            }
            return c;
        }

        public Map<Identifier, T> getEntries() {
            return entries;
        }

        public void addEntry(Identifier id, T entry) {
            entries.put(id, entry);
        }

        public void removeEntry(Identifier id) {
            entries.remove(id);
        }
    }
}

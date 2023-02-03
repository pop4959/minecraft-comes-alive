package net.mca.resources;

import com.google.gson.JsonElement;
import net.mca.MCA;
import net.mca.resources.data.BuildingType;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BuildingTypes extends JsonDataLoader implements Iterable<BuildingType> {
    protected static final Identifier ID = new Identifier(MCA.MOD_ID, "building_types");

    private static BuildingTypes INSTANCE;

    private final Map<String, BuildingType> buildingTypes = new HashMap<>();

    public BuildingTypes() {
        super(Resources.GSON, ID.getPath());
        INSTANCE = this;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        for (Map.Entry<Identifier, JsonElement> pair : prepared.entrySet()) {
            String name = pair.getKey().getPath();
            buildingTypes.put(name, new BuildingType(name, pair.getValue().getAsJsonObject()));
        }
    }

    public Map<String, BuildingType> getBuildingTypes() {
        return buildingTypes;
    }

    public BuildingType getBuildingType(String type) {
        return buildingTypes.containsKey(type) ? buildingTypes.get(type) : new BuildingType();
    }

    public static BuildingTypes getInstance() {
        return INSTANCE;
    }

    @Override
    public Iterator<BuildingType> iterator() {
        return INSTANCE.buildingTypes.values().iterator();
    }
}

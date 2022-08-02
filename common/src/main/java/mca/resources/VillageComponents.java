package mca.resources;

import mca.resources.Resources.BrokenResourceException;
import mca.resources.data.BuildingType;
import mca.resources.data.NameSet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class VillageComponents implements Iterable<BuildingType> {
    private final Map<String, BuildingType> buildingTypes = new HashMap<>();

    private final Map<String, NameSet> namePool = new HashMap<>();

    private final Random rng;

    VillageComponents(Random rng) {
        this.rng = rng;
    }

    void load() throws BrokenResourceException {
        for (BuildingType bt : Resources.read("api/buildingTypes.json", BuildingType[].class)) {
            buildingTypes.put(bt.name(), bt);
        }

        namePool.put("village", Resources.read("api/names/village.json", NameSet.class));
    }

    //returns a random generated name for a given name set
    public String pickVillageName(String from) {
        return namePool.getOrDefault(from, NameSet.DEFAULT).toName(rng);
    }

    public Map<String, BuildingType> getBuildingTypes() {
        return buildingTypes;
    }

    public BuildingType getBuildingType(String type) {
        return buildingTypes.containsKey(type) ? buildingTypes.get(type) : new BuildingType();
    }

    @Override
    public Iterator<BuildingType> iterator() {
        return buildingTypes.values().iterator();
    }
}

package net.mca.resources.data.skin;

import com.google.gson.JsonObject;

public class Hair extends SkinListEntry {
    public Hair(String identifier) {
        super(identifier);
    }

    public Hair(String identifier, JsonObject object) {
        super(identifier, object);
    }
}

package net.mca.resources.data.skin;

import com.google.gson.JsonObject;
import net.mca.entity.ai.relationship.Gender;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.io.Serializable;

public abstract class SkinListEntry implements Serializable {
    public final String identifier;
    public Gender gender = Gender.NEUTRAL;
    public float chance;

    public SkinListEntry(String identifier) {
        this.identifier = identifier;
    }

    public SkinListEntry(String identifier, JsonObject object) {
        this.identifier = identifier;

        this.gender = Gender.byId(JsonHelper.getInt(object, "gender", 0));
        this.chance = JsonHelper.getFloat(object, "chance", 1.0f);
    }

    public String getPath() {
        return (new Identifier(this.identifier)).getPath();
    }

    public JsonObject toJson() {
        JsonObject j = new JsonObject();
        j.addProperty("gender", gender == null ? Gender.NEUTRAL.getId() : gender.getId());
        j.addProperty("chance", chance);
        return j;
    }
}

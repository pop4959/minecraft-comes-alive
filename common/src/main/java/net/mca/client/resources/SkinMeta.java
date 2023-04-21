package net.mca.client.resources;

import net.mca.entity.ai.relationship.Gender;

import javax.annotation.Nullable;

public class SkinMeta {
    @Nullable
    private final String profession;
    private final int temperature;
    private final Gender gender;
    private final float chance;

    public SkinMeta(@Nullable String profession, int temperature, Gender gender, float chance) {
        this.profession = profession;
        this.temperature = temperature;
        this.gender = gender;
        this.chance = chance;
    }

    @Nullable
    public String getProfession() {
        return profession;
    }

    public int getTemperature() {
        return temperature;
    }

    public Gender getGender() {
        return gender;
    }

    public float getChance() {
        return chance;
    }
}

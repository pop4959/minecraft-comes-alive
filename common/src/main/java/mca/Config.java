package mca;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.List;
import java.util.Map;

public final class Config implements Serializable {
    @Serial
    private static final long serialVersionUID = 956221997003825933L;

    private static final Config INSTANCE = loadOrCreate();

    public static Config getInstance() {
        return INSTANCE;
    }

    public static final int VERSION = 1;

    @SuppressWarnings("unused")
    public String README = "https://minecraft-comes-alive-reborn.fandom.com/wiki/Config";

    public int version = 0;

    //mod features
    public boolean overwriteOriginalVillagers = true;
    public boolean overwriteOriginalZombieVillagers = true;
    public boolean villagerTagsHacks = true;
    public boolean enableInfection = true;
    public int infectionChance = 5;
    public boolean allowGrimReaper = true;
    public String villagerChatPrefix = "";
    public boolean canHurtBabies = true;
    public boolean enterVillageNotification = true;
    public boolean villagerMarriageNotification = true;
    public boolean villagerBirthNotification = true;
    public int heartsToBeConsideredAsFriend = 40;

    //villager behavior
    public int chanceToHaveTwins = 2;
    public float marriageHeartsRequirement = 100;
    public int babyItemGrowUpTime = 24000;
    public int villagerMaxAgeTime = 192000;
    public int villagerMaxHealth = 20;
    public boolean allowVillagerTeleporting = false;
    public double villagerMinTeleportationDistance = 144.0D;
    public int childInitialHearts = 100;
    public int greetHeartsThreshold = 75;
    public int greetAfterDays = 1;
    public int immigrantChance = 20;
    public float traitChance = 0.25f;
    public float traitInheritChance = 0.5f;
    public int heartsForPardonHit = 30;
    public int pardonPlayerTicks = 1200;
    public boolean guardsTargetMonsters = false;
    public float maleVillagerHeight = 0.9f;
    public float femaleVillagerHeightFactor = 0.85f;
    public float maleVillagerWidthFactor = 1.0f;
    public float femaleVillagerWidthFactor = 0.95f;
    public boolean showNameTags = true;
    public boolean useVoices = false;
    public boolean useVanillaVoices = false;
    public float interactionChanceFatigue = 1.0f;
    public int interactionFatigueCooldown = 4800;
    public int villagerHealthBonusPerLevel = 5;
    public boolean useSquidwardModels = false;
    public boolean enableBoobs = true;

    //village behavior
    public int guardSpawnRate = 6;
    public float taxesFactor = 1.5f;
    public int taxSeason = 168000;
    public int marriageChance = 5;
    public int childrenChance = 5;
    public int bountyHunterInterval = 24000;
    public int bountyHunterThreshold = -5;

    //gifts
    public int giftDesaturationQueueLength = 16;
    public float giftDesaturationFactor = 0.5f;
    public double giftDesaturationExponent = 0.85;
    public double giftSatisfactionFactor = 0.33;
    public float giftMoodEffect = 0.5f;
    public float baseGiftMoodEffect = 2;
    public int giftDesaturationReset = 24000;

    //player interactions
    public boolean allowPlayerMarriage = true;

    //structure settings
    public int maxBuildingSize = 8192;
    public int maxBuildingRadius = 320;
    public int minPillarHeight = 2;
    public int maxTreeHeight = 8;
    public Map<String, Integer> maxTreeTicks = ImmutableMap.<String, Integer>builder()
            .put("#minecraft:logs", 60)
            .build();
    public List<String> validTreeSources = List.of(
            "minecraft:grass_block",
            "minecraft:dirt"
    );

    //player customization
    public boolean launchIntoDestiny = true;
    public boolean allowDestinyCommandOnce = true;
    public boolean allowDestinyCommandMoreThanOnce = false;
    public boolean allowDestinyTeleportation = true;
    public boolean enableVillagerPlayerModel = true;
    public boolean forceVillagerPlayerModel = false;
    public boolean allowPlayerEditor = true;

    public Map<String, Integer> guardsTargetEntities = ImmutableMap.<String, Integer>builder()
            .put("minecraft:creeper", -1)
            .put("minecraft:drowned", 2)
            .put("minecraft:evoker", 3)
            .put("minecraft:husk", 2)
            .put("minecraft:illusioner", 3)
            .put("minecraft:pillager", 3)
            .put("minecraft:ravager", 3)
            .put("minecraft:vex", 0)
            .put("minecraft:vindicator", 4)
            .put("minecraft:zoglin", 2)
            .put("minecraft:zombie", 4)
            .put("minecraft:zombie_villager", 3)
            .put("minecraft:spider", 0)
            .put("minecraft:skeleton", 0)
            .put("minecraft:slime", 0)
            .put(MCA.MOD_ID + "female_zombie_villager", 3)
            .put(MCA.MOD_ID + "male_zombie_villager", 3)
            .build();

    public List<String> villagerPathfindingBlacklist = List.of(
            "#minecraft:climbable",
            "#minecraft:fence_gates",
            "#minecraft:fences",
            "#minecraft:fire",
            "#minecraft:portals",
            "#minecraft:slabs",
            "#minecraft:stairs",
            "#minecraft:trapdoors",
            "#minecraft:walls"
    );

    public static File getConfigFile() {
        return new File("./config/mca.json");
    }

    public void save() {
        try (FileWriter writer = new FileWriter(getConfigFile())) {
            version = VERSION;
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Config loadOrCreate() {
        try (FileReader reader = new FileReader(getConfigFile())) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Config config = gson.fromJson(reader, Config.class);
            if (config.version != VERSION) {
                config = new Config();
            }
            config.save();
            return config;
        } catch (IOException e) {
            //e.printStackTrace();
        }
        Config config = new Config();
        config.save();
        return config;
    }
}

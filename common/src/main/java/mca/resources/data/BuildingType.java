package mca.resources.data;

import java.io.Serializable;
import java.util.*;

import mca.MCA;
import net.minecraft.block.Block;
import net.minecraft.tag.ServerTagManagerHolder;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

public final class BuildingType implements Serializable {
    private static final long serialVersionUID = 2215455350801127280L;

    private final String name;
    private final int size;
    private final String color;
    private final int priority;
    private final boolean visible;
    private final Map<String, Integer> blocks;
    private transient Map<Identifier, Identifier> blockToGroup;
    private transient Map<Identifier, Integer> groups;
    private final boolean icon;
    private final int iconU;
    private final int iconV;
    private final boolean grouped;
    private final int mergeRange;
    private final boolean noBeds;

    public BuildingType() {
        this("?", 0, "ffffffff", 0, true, false);
    }

    public BuildingType(String name, int size, String color, int priority, boolean visible, boolean noBeds) {
        this.name = name;
        this.size = size;
        this.color = color;
        this.priority = priority;
        this.visible = visible;
        this.noBeds = noBeds;
        this.blocks = Map.of("#minecraft:beds", 1000000000);
        this.blockToGroup = null;
        this.icon = false;
        this.iconU = 0;
        this.iconV = 0;
        this.grouped = false;
        this.mergeRange = 32;
    }

    public String name() {
        return name;
    }

    public int size() {
        return size;
    }

    public String color() {
        return color;
    }

    public int priority() {
        return priority;
    }

    public boolean visible() {
        return visible;
    }

    public int getColor() {
        return (int)Long.parseLong(color, 16);
    }

    /**
     * @return a mapping between block identifiers and groups (tags or individual blocks)
     */
    public Map<Identifier, Identifier> getBlockToGroup() {
        if (blockToGroup == null) {
            blockToGroup = new HashMap<>();
            groups = new HashMap<>();
            TagGroup<Block> blockTags = ServerTagManagerHolder.getTagManager().getOrCreateTagGroup(Registry.BLOCK_KEY);
            for (Map.Entry<String, Integer> requirement : blocks.entrySet()) {
                Identifier identifier;
                if (requirement.getKey().startsWith("#")) {
                    identifier = new Identifier(requirement.getKey().substring(1));
                    Tag<Block> tag = blockTags.getTag(identifier);
                    if (tag == null) {
                        MCA.LOGGER.error("Unknown building type tag " + identifier);
                    } else {
                        for (Block b : tag.values()) {
                            blockToGroup.putIfAbsent(Registry.BLOCK.getId(b), identifier);
                        }
                    }
                } else {
                    identifier = new Identifier(requirement.getKey());
                    blockToGroup.put(identifier, identifier);
                }
                groups.put(identifier, requirement.getValue());
            }
        }
        return blockToGroup;
    }

    public Map<Identifier, Integer> getGroups() {
        getBlockToGroup();
        return groups;
    }

    /**
     * @param blocks the map of block positions per block type of building
     *
     * @return a filtered and grouped map of block types relevant for this building type
     */
    public Map<Identifier, List<BlockPos>> getGroups(Map<Identifier, List<BlockPos>> blocks) {
        HashMap<Identifier, List<BlockPos>> available = new HashMap<>();
        for (Map.Entry<Identifier, List<BlockPos>> entry : blocks.entrySet()) {
            Optional.ofNullable(getBlockToGroup().get(entry.getKey())).ifPresent(v -> {
                available.computeIfAbsent(v, k -> new LinkedList<>()).addAll(entry.getValue());
            });
        }
        return available;
    }

    public boolean isIcon() {
        return icon;
    }

    public int iconU() {
        return iconU * 20;
    }

    public int iconV() {
        return iconV * 60;
    }

    public boolean grouped() {
        return grouped;
    }

    public int mergeRange() {
        return mergeRange;
    }

    public boolean noBeds() {
        return noBeds;
    }
}

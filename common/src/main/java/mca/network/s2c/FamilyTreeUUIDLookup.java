package mca.network.s2c;

import mca.cobalt.network.Message;
import mca.cobalt.network.NetworkHandler;
import mca.entity.ai.relationship.family.FamilyTree;
import mca.entity.ai.relationship.family.FamilyTreeNode;
import mca.network.c2s.FamilyTreeUUIDResponse;
import mca.resources.data.SerializablePair;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FamilyTreeUUIDLookup implements Message.ServerMessage {
    private final String search;

    public FamilyTreeUUIDLookup(String search) {
        this.search = search;
    }

    @Override
    public void receive(ServerPlayerEntity player) {
        FamilyTree tree = FamilyTree.get(player.getWorld());
        List<SerializablePair<UUID, SerializablePair<String, String>>> list = tree.getAllWithName(search)
                .map(entry -> new SerializablePair<>(entry.id(), new SerializablePair<>(
                        tree.getOrEmpty(entry.father()).map(FamilyTreeNode::getName).orElse(""),
                        tree.getOrEmpty(entry.mother()).map(FamilyTreeNode::getName).orElse(""))))
                .limit(100)
                .collect(Collectors.toList());
        NetworkHandler.sendToPlayer(new FamilyTreeUUIDResponse(list), player);
    }
}

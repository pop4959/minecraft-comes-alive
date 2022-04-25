package mca.network.c2s;

import mca.cobalt.network.Message;
import mca.cobalt.network.NetworkHandler;
import mca.entity.VillagerEntityMCA;
import mca.entity.VillagerLike;
import mca.entity.ai.relationship.Gender;
import mca.entity.ai.relationship.family.FamilyTree;
import mca.entity.ai.relationship.family.FamilyTreeNode;
import mca.network.NbtDataMessage;
import mca.network.s2c.PlayerDataMessage;
import mca.resources.ClothingList;
import mca.resources.HairList;
import mca.server.world.data.PlayerSaveData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;

import java.io.Serial;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class VillagerEditorSyncRequest extends NbtDataMessage implements Message {
    @Serial
    private static final long serialVersionUID = -5581564927127176555L;

    private final String command;
    private final UUID uuid;

    public VillagerEditorSyncRequest(String command, UUID uuid, NbtCompound data) {
        super(data);
        this.command = command;
        this.uuid = uuid;
    }

    @Override
    public void receive(ServerPlayerEntity player) {
        Entity entity = player.getWorld().getEntity(uuid);
        NbtCompound villagerData;
        switch (command) {
            case "hair":
                villagerData = GetVillagerRequest.getVillagerData(entity);
                if (villagerData != null) {
                    // fetch hair
                    String hair;
                    if (getData().contains("offset")) {
                        hair = HairList.getInstance().getPool(getGender(villagerData)).pickNext(villagerData.getString("hair"), getData().getInt("offset"));
                    } else {
                        hair = HairList.getInstance().getPool(getGender(villagerData)).pickOne();
                    }

                    // set
                    villagerData.putString("hair", hair);
                    saveEntity(player, entity, villagerData);
                }
                break;
            case "clothing":
                villagerData = GetVillagerRequest.getVillagerData(entity);
                if (villagerData != null) {
                    String clothes = "mca:missing";
                    if (entity instanceof PlayerEntity) {
                        if (getData().contains("offset")) {
                            clothes = ClothingList.getInstance().getPool(getGender(villagerData), VillagerProfession.NONE).pickNext(villagerData.getString("clothes"), getData().getInt("offset"));
                        } else {
                            clothes = ClothingList.getInstance().getPool(getGender(villagerData), VillagerProfession.NONE).pickOne();
                        }
                    } else if (entity instanceof VillagerLike<?> villager) {
                        if (getData().contains("offset")) {
                            clothes = ClothingList.getInstance().getPool(villager).pickNext(villager.getClothes(), getData().getInt("offset"));
                        } else {
                            clothes = ClothingList.getInstance().getPool(villager).pickOne();
                        }
                    }
                    villagerData.putString("clothes", clothes);
                    saveEntity(player, entity, villagerData);
                }
                break;
            case "sync":
                saveEntity(player, entity, getData());
                break;
            case "profession":
                if (entity instanceof VillagerEntityMCA villager) {
                    VillagerProfession profession = Registry.VILLAGER_PROFESSION.get(new Identifier(getData().getString("profession")));
                    villager.setProfession(profession);
                }
                break;
        }
        getData();
    }

    private void saveEntity(ServerPlayerEntity player, Entity entity, NbtCompound villagerData) {
        if (entity instanceof ServerPlayerEntity serverPlayer) {
            PlayerSaveData data = PlayerSaveData.get(serverPlayer.getWorld(), uuid);
            data.setEntityData(villagerData);
            data.setEntityDataSet(true);
            syncFamilyTree(player, entity, villagerData);

            //also update players
            serverPlayer.getWorld().getPlayers().forEach(p -> NetworkHandler.sendToPlayer(new PlayerDataMessage(player.getUuid(), villagerData), p));
        } else if (entity instanceof VillagerLike) {
            ((LivingEntity)entity).readCustomDataFromNbt(villagerData);
            entity.calculateDimensions();
            syncFamilyTree(player, entity, villagerData);
        }
    }

    private Gender getGender(NbtCompound villagerData) {
        return Gender.byId(villagerData.getInt("gender"));
    }

    private Optional<FamilyTreeNode> getFamilyNode(ServerPlayerEntity player, FamilyTree tree, String name, Gender gender) {
        try {
            UUID uuid = UUID.fromString(name);
            Optional<FamilyTreeNode> node = tree.getOrEmpty(uuid);
            if (node.isPresent()) {
                player.sendMessage(new TranslatableText("gui.villager_editor.uuid_known", name, node.get().getName()), true);
                return node;
            } else {
                player.sendMessage(new TranslatableText("gui.villager_editor.uuid_unknown", name).formatted(Formatting.RED), true);
                return Optional.empty();
            }
        } catch (IllegalArgumentException exception) {
            List<FamilyTreeNode> nodes = tree.getAllWithName(name).toList();
            if (nodes.isEmpty()) {
                //create a new entry
                player.sendMessage(new TranslatableText("gui.villager_editor.name_created", name).formatted(Formatting.YELLOW), true);
                return Optional.of(tree.getOrCreate(UUID.randomUUID(), name, gender));
            } else {
                if (nodes.size() > 1) {
                    player.sendMessage(new TranslatableText("gui.villager_editor.name_not_unique", name).formatted(Formatting.RED), true);

                    String uuids = nodes.stream().map(FamilyTreeNode::id).map(UUID::toString).collect(Collectors.joining(", "));
                    player.sendMessage(new TranslatableText("gui.villager_editor.list_of_ids", uuids), false);
                } else {
                    player.sendMessage(new TranslatableText("gui.villager_editor.name_unique", name), true);
                }

                return Optional.ofNullable(nodes.get(0));
            }
        }
    }

    private void syncFamilyTree(ServerPlayerEntity player, Entity entity, NbtCompound villagerData) {
        FamilyTree tree = FamilyTree.get((ServerWorld)entity.world);
        FamilyTreeNode entry = tree.getOrCreate(entity);
        entry.setGender(getGender(getData()));
        entry.setName(getData().getString("villagerName"));

        if (villagerData.contains("tree_father_new")) {
            String name = villagerData.getString("tree_father_new");
            if (name.isEmpty()) {
                entry.removeFather();
            } else {
                getFamilyNode(player, tree, name, Gender.MALE).ifPresent(entry::setFather);
            }
        }

        if (villagerData.contains("tree_mother_new")) {
            String name = villagerData.getString("tree_mother_new");
            if (name.isEmpty()) {
                entry.removeMother();
            } else {
                getFamilyNode(player, tree, name, Gender.FEMALE).ifPresent(entry::setMother);
            }
        }

        if (villagerData.contains("tree_spouse_new")) {
            String name = villagerData.getString("tree_spouse_new");
            if (name.isEmpty()) {
                Optional.of(entry.spouse()).flatMap(tree::getOrEmpty).ifPresent(node -> node.updateMarriage(null, null));
                entry.updateMarriage(null, null);
            } else {
                getFamilyNode(player, tree, name, entry.gender().opposite()).ifPresent(node -> {
                    entry.updateMarriage(node);
                    node.updateMarriage(entry);
                });
            }
        }
    }
}

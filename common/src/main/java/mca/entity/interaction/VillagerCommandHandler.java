package mca.entity.interaction;

import mca.ProfessionsMCA;
import mca.advancement.criterion.CriterionMCA;
import mca.entity.VillagerEntityMCA;
import mca.entity.ai.*;
import mca.entity.ai.relationship.RelationshipState;
import mca.entity.ai.relationship.family.FamilyTree;
import mca.entity.ai.relationship.family.FamilyTreeNode;
import mca.item.ItemsMCA;
import mca.server.world.data.BabyTracker;
import mca.server.world.data.PlayerSaveData;
import mca.util.WorldUtils;
import net.minecraft.entity.Saddleable;
import net.minecraft.entity.ai.FuzzyPositions;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;

import java.util.Comparator;
import java.util.Optional;

public class VillagerCommandHandler extends EntityCommandHandler<VillagerEntityMCA> {
    private static final String[] structures = new String[] {
            "igloo",
            "pyramid",
            "ruined_portal_desert",
            "ruined_portal_swamp",
            "ruined_portal",
            "ruined_portal_mountain",
            "mansion",
            "monument",
            "shipwreck",
            "shipwreck_beached",
            "village_desert",
            "village_taiga",
            "village_snowy",
            "village_plains",
            "village_savanna",
            "swamp_hut",
            "mineshaft",
            "jungle_pyramid",
            "pillager_outpost"
    };

    public VillagerCommandHandler(VillagerEntityMCA entity) {
        super(entity);
    }

    /**
     * Called on the server to respond to button events.
     */
    @Override
    public boolean handle(ServerPlayerEntity player, String command) {
        Memories memory = entity.getVillagerBrain().getMemoriesForPlayer(player);

        if (MoveState.byCommand(command).filter(state -> {
            entity.getVillagerBrain().setMoveState(state, player);
            return true;
        }).isPresent()) {
            return true;
        }

        if (Chore.byCommand(command).filter(chore -> {
            entity.getVillagerBrain().assignJob(chore, player);
            CriterionMCA.GENERIC_EVENT_CRITERION.trigger(player, "chores");
            return true;
        }).isPresent()) {
            return true;
        }

        //an optional argument is stored separated using a dot
        String arg = "";
        String[] split = command.split("\\.");
        if (split.length > 1) {
            command = split[0];
            arg = split[1];
        }

        switch (command) {
            case "pick_up" -> {
                if (player.getPassengerList().size() >= 3) {
                    player.getPassengerList().get(0).stopRiding();
                }
                if (entity.hasVehicle()) {
                    entity.stopRiding();
                } else {
                    entity.startRiding(player, true);
                }
                player.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(player));
                return false;
            }
            case "ridehorse" -> {
                if (entity.hasVehicle()) {
                    entity.stopRiding();
                } else {
                    entity.world.getOtherEntities(player, player.getBoundingBox()
                                    .expand(10), e -> e instanceof Saddleable && ((Saddleable)e).isSaddled())
                            .stream()
                            .filter(horse -> !horse.hasPassengers())
                            .min(Comparator.comparingDouble(a -> a.squaredDistanceTo(entity))).ifPresentOrElse(horse -> {
                                entity.startRiding(horse, false);
                                entity.sendChatMessage(player, "interaction.ridehorse.success");
                            }, () -> entity.sendChatMessage(player, "interaction.ridehorse.fail.notnearby"));
                }
                return true;
            }
            case "sethome" -> {
                entity.getResidency().setHome(player);
                return true;
            }
            case "gohome" -> {
                entity.getResidency().goHome(player);
                stopInteracting();
                return false;
            }
            case "setworkplace" -> {
                entity.getResidency().setWorkplace(player);
                return true;
            }
            case "sethangout" -> {
                entity.getResidency().setHangout(player);
                return true;
            }
            case "trade" -> {
                prepareOffersFor(player);
                return false;
            }
            case "inventory" -> {
                player.openHandledScreen(entity);
                return false;
            }
            case "gift" -> {
                entity.getRelationships().giveGift(player, memory);
                return true;
            }
            case "adopt" -> {
                entity.sendChatMessage(player, "interaction.adopt.success");
                FamilyTreeNode parentNode = FamilyTree.get(player.getWorld()).getOrCreate(player);
                entity.getRelationships().getFamilyEntry().assignParent(parentNode);
                Optional<FamilyTreeNode> parentSpouse = FamilyTree.get(player.getWorld()).getOrEmpty(parentNode.partner());
                parentSpouse.ifPresent(p -> entity.getRelationships().getFamilyEntry().assignParent(p));
            }
            case "procreate" -> {
                BabyTracker tracker = BabyTracker.get((ServerWorld)entity.world);
                if (tracker.hasActiveBaby(player.getUuid(), entity.getUuid())) {
                    BabyTracker.Pairing pairing = tracker.getPairing(player.getUuid(), entity.getUuid());

                    if (pairing.locateBaby(player).getRight().wasFound()) {
                        entity.sendChatMessage(player, "interaction.procreate.fail.hasbaby");
                    } else {
                        entity.sendChatMessage(player, "interaction.procreate.fail.lostbaby");
                        pairing.reconstructBaby(player);
                    }
                } else if (memory.getHearts() < 100) {
                    entity.sendChatMessage(player, "interaction.procreate.fail.lowhearts");
                } else {
                    entity.getRelationships().startProcreating();
                }
                return true;
            }
            case "divorcePapers" -> {
                player.getInventory().insertStack(new ItemStack(ItemsMCA.DIVORCE_PAPERS.get()));
                return true;
            }
            case "divorceConfirm" -> {
                ItemStack papers = ItemsMCA.DIVORCE_PAPERS.get().getDefaultStack();
                Memories memories = entity.getVillagerBrain().getMemoriesForPlayer(player);
                if (player.getInventory().contains(papers)) {
                    entity.sendChatMessage(player, "divorcePaper");
                    player.getInventory().removeOne(papers);
                    memories.modHearts(-20);
                } else {
                    entity.sendChatMessage(player, "divorce");
                    memories.modHearts(-200);
                }
                entity.getVillagerBrain().modifyMoodValue(-5);
                entity.getRelationships().endRelationShip(RelationshipState.SINGLE);
                PlayerSaveData playerData = PlayerSaveData.get(player);
                playerData.endRelationShip(RelationshipState.SINGLE);
                return true;
            }
            case "execute" -> {
                entity.setProfession(ProfessionsMCA.OUTLAW.get());
                return true;
            }
            case "pardon" -> {
                entity.setProfession(VillagerProfession.NONE);
                return true;
            }
            case "stay_in_village" -> {
                payEmeralds(player, 5);
                entity.setProfession(VillagerProfession.NONE);
                entity.setDespawnDelay(0);
                return true;
            }
            case "hire_short" -> {
                payEmeralds(player, 10);
                entity.makeMercenary(player);
                entity.setDespawnDelay(24000 * 3);
                return true;
            }
            case "hire_long" -> {
                entity.makeMercenary(player);
                entity.setDespawnDelay(24000 * 7);
                return true;
            }
            case "infected" -> {
                entity.setInfected(!entity.isInfected());
                return true;
            }
            case "stopworking" -> {
                entity.getVillagerBrain().abandonJob();
                return true;
            }
            case "armor" -> {
                entity.getVillagerBrain().setArmorWear(!entity.getVillagerBrain().getArmorWear());
                if (entity.getVillagerBrain().getArmorWear()) {
                    entity.sendChatMessage(player, "armor.enabled");
                } else {
                    entity.sendChatMessage(player, "armor.disabled");
                }
                return true;
            }
            case "profession" -> {
                switch (arg) {
                    case "none" -> {
                        entity.setProfession(VillagerProfession.NONE);
                        entity.sendChatMessage(player, "profession.set.none");
                    }
                    case "guard" -> {

                        entity.setProfession(ProfessionsMCA.GUARD.get());
                        entity.sendChatMessage(player, "profession.set.guard");
                    }
                    case "archer" -> {
                        entity.setProfession(ProfessionsMCA.ARCHER.get());
                        entity.sendChatMessage(player, "profession.set.archer");
                    }
                }
                return true;
            }
            case "apologize" -> {
                Vec3d pos = entity.getPos();
                entity.world.getNonSpectatingEntities(VillagerEntityMCA.class, new Box(pos, pos).expand(32)).forEach(v -> {
                    if (entity.squaredDistanceTo(v) <= (v.getTarget() == null ? 1024 : 64)) {
                        v.getBrain().forget(MemoryModuleTypeMCA.SMALL_BOUNTY.get());
                    }
                });
            }
            case "location" -> {
                //choose a random arg from the default pool
                if (arg.length() == 0) {
                    arg = structures[entity.getRandom().nextInt(structures.length)];
                }

                //slightly randomly the search center
                Identifier identifier = new Identifier(arg);
                BlockPos pos = FuzzyPositions.localFuzz(entity.getRandom(), 1024, 0).add(entity.getBlockPos());
                Optional<BlockPos> position = WorldUtils.getClosestStructurePosition((ServerWorld)entity.world, pos, identifier, 64);
                if (position.isPresent()) {
                    String posString = position.get().getX() + "," + position.get().getY() + "," + position.get().getZ();
                    entity.sendChatMessage(player, "dialogue.location." + identifier.getPath(), posString);
                } else {
                    entity.sendChatMessage(player, "dialogue.location.forgot");
                }
            }
            case "slap" -> player.damage(DamageSource.CRAMMING, 1.0f);
        }

        return super.handle(player, command);
    }

    private void payEmeralds(ServerPlayerEntity player, int emeralds) {
        PlayerInventory inventory = player.getInventory();
        for (int j = 0; j < inventory.size(); ++j) {
            ItemStack itemStack = inventory.getStack(j);
            if (itemStack.getItem().equals(Items.EMERALD)) {
                int c = Math.min(itemStack.getCount(), emeralds);
                itemStack.decrement(c);
                emeralds -= c;
                if (emeralds <= 0) {
                    return;
                }
            }
        }
    }

    public void prepareOffersFor(PlayerEntity player) {
        int i = entity.getReputation(player);
        if (i != 0) {
            for (TradeOffer tradeOffer : entity.getOffers()) {
                tradeOffer.increaseSpecialPrice(-MathHelper.floor(i * tradeOffer.getPriceMultiplier()));
            }
        }

        if (player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) {
            StatusEffectInstance effectInstance = player.getStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE);
            if (effectInstance != null) {
                int k = effectInstance.getAmplifier();

                for (TradeOffer merchantOffer : entity.getOffers()) {
                    double d0 = 0.3D + 0.0625D * k;
                    int j = (int)Math.floor(d0 * merchantOffer.getOriginalFirstBuyItem().getCount());
                    merchantOffer.increaseSpecialPrice(-Math.max(j, 1));
                }
            }
        }

        entity.setCustomer(player);
        entity.sendOffers(player, entity.getDisplayName(), entity.getVillagerData().getLevel());
    }
}

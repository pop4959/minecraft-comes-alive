package net.mca.entity.ai;

import net.mca.Config;
import net.mca.TagsMCA;
import net.mca.block.TombstoneBlock;
import net.mca.entity.Status;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.VillagerLike;
import net.mca.entity.ai.relationship.CompassionateEntity;
import net.mca.entity.ai.relationship.EntityRelationship;
import net.mca.entity.ai.relationship.Gender;
import net.mca.entity.ai.relationship.RelationshipType;
import net.mca.entity.ai.relationship.family.FamilyTreeNode;
import net.mca.entity.interaction.gifts.GiftSaturation;
import net.mca.server.world.data.GraveyardManager;
import net.mca.util.WorldUtils;
import net.mca.util.network.datasync.CDataManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * I know you, you know me, we're all a big happy family.
 */
public class Relationship<T extends MobEntity & VillagerLike<T>> implements EntityRelationship {
    public static final Predicate IS_MARRIED = (villager, player) -> villager.getRelationships().isMarriedTo(player);
    public static final Predicate IS_ENGAGED = (villager, player) -> villager.getRelationships().isEngagedWith(player);
    public static final Predicate IS_PROMISED = (villager, player) -> villager.getRelationships().isPromisedTo(player);
    public static final Predicate IS_RELATIVE = (villager, player) -> villager.getRelationships().getFamilyEntry().isRelative(player);
    public static final Predicate IS_FAMILY = IS_MARRIED.or(IS_RELATIVE);
    public static final Predicate IS_PARENT = (villager, player) -> villager.getRelationships().getFamilyEntry().isParent(player);
    public static final Predicate IS_ORPHAN = (villager, player) -> villager.getRelationships().getFamilyEntry().getParents().allMatch(FamilyTreeNode::isDeceased);

    public static <E extends Entity> CDataManager.Builder<E> createTrackedData(CDataManager.Builder<E> builder) {
        return builder.addAll();
    }

    protected final T entity;

    private final GiftSaturation giftSaturation = new GiftSaturation();

    public Relationship(T entity) {
        this.entity = entity;
    }

    @Override
    public Gender getGender() {
        return entity.getGenetics().getGender();
    }

    @Override
    public ServerWorld getWorld() {
        return (ServerWorld)entity.world;
    }

    @Override
    public UUID getUUID() {
        return entity.getUuid();
    }

    @NotNull
    @Override
    public FamilyTreeNode getFamilyEntry() {
        return getFamilyTree().getOrCreate(entity);
    }

    public void onDeath(DamageSource cause) {
        boolean beRemembered = getFamilyEntry().willBeRemembered();
        boolean beLoved = entity.getVillagerBrain().getMemories().values().stream().anyMatch(m -> m.getHearts() > Config.getInstance().heartsRequiredToAutoSpawnGravestone);

        if (beRemembered || beLoved || !entity.isHostile()) {
            getFamilyEntry().setDeceased(true);

            //look for a gravestone
            //todo place one for villagers being remembered or loved
            GraveyardManager.get((ServerWorld)entity.world)
                    .findNearest(entity.getBlockPos(), GraveyardManager.TombstoneState.EMPTY, 10)
                    .ifPresentOrElse(pos -> {
                        if (entity.world.getBlockState(pos).isIn(TagsMCA.Blocks.TOMBSTONES)) {
                            BlockEntity be = entity.world.getBlockEntity(pos);
                            if (be instanceof TombstoneBlock.Data) {
                                onTragedy(cause, pos);
                                ((TombstoneBlock.Data)be).setEntity(entity);
                            }
                        }
                        onTragedy(cause, null);
                    }, () -> {
                        onTragedy(cause, null);
                    });
        } else {
            onTragedy(cause, null);
        }

        // the family is too small to be remembered
        if (!beRemembered) {
            getFamilyEntry().streamParents().forEach(uuid -> {
                getFamilyTree().remove(uuid);
            });
            getFamilyTree().remove(entity.getUuid());
        }
    }

    public void onTragedy(DamageSource cause, @Nullable BlockPos burialSite) {
        // The death of a villager negatively modifies the mood of nearby strangers
        if (!entity.isHostile()) {
            WorldUtils
                    .getCloseEntities(entity.world, entity, 32, VillagerEntityMCA.class)
                    .forEach(villager -> villager.getRelationships().onTragedy(cause, burialSite, RelationshipType.STRANGER, entity));
        }

        onTragedy(cause, burialSite, RelationshipType.SELF, entity);
    }

    @Override
    public void onTragedy(DamageSource cause, @Nullable BlockPos burialSite, RelationshipType type, Entity with) {
        if (!cause.isOutOfWorld()) {
            int moodAffect = 5 * type.getProximityAmplifier();
            entity.world.sendEntityStatus(entity, Status.MCA_VILLAGER_TRAGEDY);
            entity.getVillagerBrain().modifyMoodValue(-moodAffect);

            // seen murder
            if (cause.getAttacker() instanceof PlayerEntity player) {
                entity.getVillagerBrain().getMemoriesForPlayer(player).modHearts(-20);
            }
        }

        if (burialSite != null && type != RelationshipType.STRANGER) {
            entity.getVillagerBrain().setGrieving();
            entity.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(burialSite, 1, 1));
            entity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new BlockPosLookTarget(burialSite));
        }

        EntityRelationship.super.onTragedy(cause, burialSite, type, with);
    }

    public GiftSaturation getGiftSaturation() {
        return giftSaturation;
    }

    public void readFromNbt(NbtCompound nbt) {
        giftSaturation.readFromNbt(nbt.getList("giftSaturationQueue", 8));
    }

    public void writeToNbt(NbtCompound nbt) {
        nbt.put("giftSaturationQueue", giftSaturation.toNbt());
    }

    public interface Predicate extends BiPredicate<CompassionateEntity<?>, Entity> {

        boolean test(CompassionateEntity<?> villager, UUID partner);

        @Override
        default boolean test(CompassionateEntity<?> villager, Entity partner) {
            return partner != null && test(villager, partner.getUuid());
        }

        default Predicate or(Predicate b) {
            return (villager, partner) -> test(villager, partner) || b.test(villager, partner);
        }

        @Override
        default Predicate negate() {
            return (villager, partner) -> !test(villager, partner);
        }

        default BiPredicate<VillagerLike<?>, ServerPlayerEntity> asConstraint() {
            return (villager, player) -> villager instanceof CompassionateEntity<?> && (test((CompassionateEntity<?>)villager, player));
        }
    }
}

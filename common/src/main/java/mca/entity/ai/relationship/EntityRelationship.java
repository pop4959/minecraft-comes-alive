package mca.entity.ai.relationship;

import mca.entity.ai.relationship.family.FamilyTree;
import mca.entity.ai.relationship.family.FamilyTreeNode;
import mca.server.world.data.PlayerSaveData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface EntityRelationship {

    default Gender getGender() {
        return Gender.MALE;
    }

    FamilyTree getFamilyTree();

    @NotNull
    FamilyTreeNode getFamilyEntry();

    Stream<Entity> getFamily(int parents, int children);

    Stream<Entity> getParents();

    Stream<Entity> getSiblings();

    Optional<Entity> getSpouse();

    default void onTragedy(DamageSource cause, @Nullable BlockPos burialSite, RelationshipType type, Entity victim) {
        if (type == RelationshipType.STRANGER) {
            return; // effects don't propagate from strangers
        }

        // notify family
        if (type == RelationshipType.SELF) {
            getParents().forEach(parent ->
                    EntityRelationship.of(parent).ifPresent(r ->
                            r.onTragedy(cause, burialSite, RelationshipType.CHILD, victim)
                    )
            );
            getSiblings().forEach(sibling ->
                    EntityRelationship.of(sibling).ifPresent(r ->
                            r.onTragedy(cause, burialSite, RelationshipType.SIBLING, victim)
                    )
            );
            getSpouse().flatMap(EntityRelationship::of).ifPresent(r ->
                    r.onTragedy(cause, burialSite, RelationshipType.SPOUSE, victim)
            );
        }

        // end the marriage for both the deceased one and the spouse
        if (type == RelationshipType.SPOUSE || type == RelationshipType.SELF) {
            if (getMarriageState().isMarried()) {
                endMarriage(MarriageState.WIDOW);
            }
        }
    }

    void marry(Entity spouse);

    void endMarriage(MarriageState newState);

    MarriageState getMarriageState();

    Optional<UUID> getSpouseUuid();

    Optional<Text> getSpouseName();

    default boolean isMarried() {
        return getMarriageState() == MarriageState.MARRIED_TO_PLAYER || getMarriageState() == MarriageState.MARRIED_TO_VILLAGER;
    }

    default boolean isEngaged() {
        return getMarriageState() == MarriageState.ENGAGED;
    }

    default boolean isPromised() {
        return getMarriageState() == MarriageState.PROMISED;
    }

    default boolean isMarriedTo(UUID uuid) {
        return getSpouseUuid().orElse(Util.NIL_UUID).equals(uuid) && isMarried();
    }

    default boolean isEngagedWith(UUID uuid) {
        return getSpouseUuid().orElse(Util.NIL_UUID).equals(uuid) && isEngaged();
    }

    static Optional<EntityRelationship> of(Entity entity) {
        if (entity instanceof ServerPlayerEntity player) {
            return Optional.ofNullable(PlayerSaveData.get(player.getWorld(), entity.getUuid()));
        }

        if (entity instanceof CompassionateEntity<?> compassionateEntity) {
            return Optional.ofNullable(compassionateEntity.getRelationships());
        }

        return Optional.empty();
    }
}

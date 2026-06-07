package com.knoxhack.echoashfallprotocol.entity.faction;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoFactionDefinition;
import com.knoxhack.echocore.api.EchoNpcRole;
import com.knoxhack.echoashfallprotocol.faction.AshfallBiomeFactions;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionMap;
import com.knoxhack.echoashfallprotocol.faction.FactionNpcDialogueService;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Neutral Ashfall faction contact backed by Echo Core faction definitions.
 */
public class FactionNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_FACTION_ID =
            SynchedEntityData.defineId(FactionNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ROLE_ID =
            SynchedEntityData.defineId(FactionNpcEntity.class, EntityDataSerializers.STRING);

    public FactionNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FACTION_ID, AshfallBiomeFactions.RADWARDEN_COMPACT.toString());
        builder.define(DATA_ROLE_ID, "quartermaster");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7D, 0.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    public void configure(Identifier factionId, String roleId) {
        this.entityData.set(DATA_FACTION_ID, safeFactionId(factionId).toString());
        this.entityData.set(DATA_ROLE_ID, roleId == null || roleId.isBlank() ? "contact" : roleId.trim());
        refreshNameplate();
    }

    public Identifier factionId() {
        try {
            return AshfallFactionMap.canonicalOrDefault(Identifier.parse(this.entityData.get(DATA_FACTION_ID)));
        } catch (RuntimeException ignored) {
            return AshfallBiomeFactions.RADWARDEN_COMPACT;
        }
    }

    public String roleId() {
        return this.entityData.get(DATA_ROLE_ID);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
            FactionNpcDialogueService.open(serverPlayer, this);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("FactionId", this.entityData.get(DATA_FACTION_ID));
        output.putString("RoleId", this.entityData.get(DATA_ROLE_ID));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        Identifier faction = AshfallFactionMap.resolveFactionId(input.getStringOr("FactionId", AshfallBiomeFactions.RADWARDEN_COMPACT.toString()));
        this.entityData.set(DATA_FACTION_ID, faction.toString());
        this.entityData.set(DATA_ROLE_ID, input.getStringOr("RoleId", "quartermaster"));
        refreshNameplate();
    }

    private void refreshNameplate() {
        EchoFactionDefinition definition = EchoCoreServices.factionDefinition(factionId()).orElse(null);
        String roleName = definition == null ? roleId() : definition.roles().stream()
                .filter(role -> role.id().equals(roleId()))
                .findFirst()
                .map(EchoNpcRole::displayName)
                .orElse(roleId());
        String faction = definition == null ? "Ashfall" : definition.shortName();
        setCustomName(Component.literal(faction + " " + roleName));
        setCustomNameVisible(true);
    }

    private static Identifier safeFactionId(Identifier factionId) {
        return AshfallFactionMap.canonicalOrDefault(factionId);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }
}

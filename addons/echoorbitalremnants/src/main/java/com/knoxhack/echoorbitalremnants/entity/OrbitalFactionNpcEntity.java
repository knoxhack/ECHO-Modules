package com.knoxhack.echoorbitalremnants.entity;

import com.knoxhack.echoorbitalremnants.faction.OrbitalFactionDialogueService;
import com.knoxhack.echoorbitalremnants.faction.OrbitalOutpostProfiles;
import com.knoxhack.echoorbitalremnants.item.FactionPledgeItem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

public class OrbitalFactionNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_FACTION =
            SynchedEntityData.defineId(OrbitalFactionNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ROLE =
            SynchedEntityData.defineId(OrbitalFactionNpcEntity.class, EntityDataSerializers.STRING);

    public OrbitalFactionNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FACTION, OrbitalOutpostProfiles.factionId(FactionPledgeItem.Faction.VOID_SALVAGERS));
        builder.define(DATA_ROLE, OrbitalOutpostProfiles.roleId(FactionPledgeItem.Faction.VOID_SALVAGERS));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.55D, 0.0F));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    public void configure(FactionPledgeItem.Faction faction, String roleId) {
        FactionPledgeItem.Faction resolved = faction == null ? FactionPledgeItem.Faction.VOID_SALVAGERS : faction;
        entityData.set(DATA_FACTION, OrbitalOutpostProfiles.factionId(resolved));
        entityData.set(DATA_ROLE, roleId == null || roleId.isBlank() ? OrbitalOutpostProfiles.roleId(resolved) : roleId.trim());
        refreshNameplate();
    }

    public FactionPledgeItem.Faction faction() {
        FactionPledgeItem.Faction faction = OrbitalOutpostProfiles.factionFromId(entityData.get(DATA_FACTION));
        return faction == null ? FactionPledgeItem.Faction.VOID_SALVAGERS : faction;
    }

    public String roleId() {
        return entityData.get(DATA_ROLE);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
            OrbitalFactionDialogueService.open(serverPlayer, this);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Faction", entityData.get(DATA_FACTION));
        output.putString("Role", entityData.get(DATA_ROLE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        FactionPledgeItem.Faction faction = OrbitalOutpostProfiles.factionFromId(input.getStringOr("Faction", ""));
        if (faction == null) {
            faction = FactionPledgeItem.Faction.VOID_SALVAGERS;
        }
        entityData.set(DATA_FACTION, OrbitalOutpostProfiles.factionId(faction));
        entityData.set(DATA_ROLE, input.getStringOr("Role", OrbitalOutpostProfiles.roleId(faction)));
        refreshNameplate();
    }

    private void refreshNameplate() {
        FactionPledgeItem.Faction faction = faction();
        setCustomName(Component.literal(OrbitalOutpostProfiles.shortName(faction) + " " + OrbitalOutpostProfiles.roleName(faction)));
        setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23);
    }
}

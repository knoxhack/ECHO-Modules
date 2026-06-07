package com.knoxhack.echoorbitalremnants.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EchoDefenseDroneEntity extends Vex {
    private static final EntityDataAccessor<String> DATA_DIALOG_STATUS =
            SynchedEntityData.defineId(EchoDefenseDroneEntity.class, EntityDataSerializers.STRING);

    public EchoDefenseDroneEntity(EntityType<? extends Vex> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vex.createAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DIALOG_STATUS, "Perimeter scan active.");
    }

    public String dialogCardStatus() {
        return cleanStatus(entityData.get(DATA_DIALOG_STATUS));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        Player targetPlayer = getTarget() instanceof Player player ? player : null;
        boolean scanLock = targetPlayer != null && distanceToSqr(targetPlayer) < 64.0D;
        if (tickCount % 20 == 0) {
            setDialogStatus(scanLock ? "Scan lock active." : "Perimeter scan active.");
        }
        if (tickCount % 100 == 0 && scanLock) {
            Player player = targetPlayer;
            player.sendSystemMessage(Component.literal("ECHO-7 // Defense drone scan lock detected."));
        }
    }

    private void setDialogStatus(String value) {
        String clean = cleanStatus(value);
        if (!clean.equals(entityData.get(DATA_DIALOG_STATUS))) {
            entityData.set(DATA_DIALOG_STATUS, clean);
        }
    }

    private static String cleanStatus(String value) {
        return value == null || value.isBlank() ? "Perimeter scan active." : value.trim();
    }
}

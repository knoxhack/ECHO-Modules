package com.knoxhack.echodeepreachprotocol.entity;

import com.knoxhack.echodeepreachprotocol.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.Supplier;

/**
 * Mid-game Deep Reach submersible vehicle. Faster and tougher than a default boat.
 */
public class RemoraSubmersibleEntity extends Boat {
    private static final String REMORA_MARKER_TAG = "Remora";
    private static final float MAX_DAMAGE = 80.0F;
    private static final float DAMAGE_RESISTANCE = 0.5F;

    public RemoraSubmersibleEntity(EntityType<? extends RemoraSubmersibleEntity> type, Level level) {
        super(type, level, createDropItemSupplier());
    }

    private static Supplier<Item> createDropItemSupplier() {
        return () -> ModItems.REMORA_SUBMERSIBLE.get();
    }

    @Override
    protected double rideHeight(EntityDimensions dimensions) {
        return dimensions.height() / 2.5F;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isRemoved()) {
            return true;
        }
        if (this.isInvulnerableToBase(source)) {
            return false;
        }

        float resisted = amount * (1.0F - DAMAGE_RESISTANCE);
        if (resisted < 1.0F && amount > 0.0F) {
            resisted = 1.0F;
        }

        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.markHurt();
        this.setDamage(this.getDamage() + resisted * 10.0F);
        this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());

        boolean creativePlayer = source.getEntity() instanceof Player player && player.getAbilities().instabuild;
        if ((creativePlayer || !(this.getDamage() > MAX_DAMAGE)) && !this.shouldSourceDestroy(source)) {
            if (creativePlayer) {
                this.discard();
            }
        } else {
            this.destroy(level, source);
        }
        return true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean(REMORA_MARKER_TAG, true);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // Marker distinguishes this from a vanilla boat when loading saved data.
        input.getBooleanOr(REMORA_MARKER_TAG, true);
    }
}

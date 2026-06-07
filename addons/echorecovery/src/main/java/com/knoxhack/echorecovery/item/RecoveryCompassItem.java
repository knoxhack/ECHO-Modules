package com.knoxhack.echorecovery.item;

import com.knoxhack.echorecovery.config.RecoveryConfig;
import com.knoxhack.echorecovery.api.RecoveryGraveSnapshot;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import com.knoxhack.echorecovery.data.RecoveryWorldData;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class RecoveryCompassItem extends Item {
    public RecoveryCompassItem(Properties properties) {
        super(properties);
    }

    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!RecoveryConfig.RECOVERY_COMPASS_ENABLED.get()) {
            return;
        }
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag existingTag = existing == null ? new CompoundTag() : existing.copyTag();
        RecoveryWorldData.GraveEntry target = selected(player, existingTag.getStringOr("SelectedGraveId", ""));
        if (target == null) {
            target = nearest(player);
        }
        CompoundTag tag = new CompoundTag();
        String selectedId = existingTag.getStringOr("SelectedGraveId", "");
        if (!selectedId.isBlank()) {
            tag.putString("SelectedGraveId", selectedId);
        }
        if (target == null) {
            tag.putString("Status", "No active graves.");
        } else {
            tag.putString("TargetId", target.graveId().toString());
            tag.putLong("TargetPos", target.pos().asLong());
            tag.putString("TargetDimension", target.dimension());
            tag.putBoolean("Recovered", target.recovered());
            tag.putBoolean("Expired", target.expired());
            boolean sameDimension = target.dimension().equals(level.dimension().identifier().toString());
            tag.putBoolean("SameDimension", sameDimension);
            if (target.recovered()) {
                tag.putString("Status", "Target already recovered.");
            } else if (target.expired()) {
                tag.putString("Status", "Target expired and locked.");
            } else if (sameDimension) {
                tag.putInt("Distance", (int)Math.round(Math.sqrt(player.blockPosition().distSqr(target.pos()))));
                tag.putString("Status", selectedId.isBlank() ? "Tracking nearest grave." : "Tracking selected grave.");
            } else if (RecoveryConfig.RECOVERY_COMPASS_WORKS_CROSS_DIMENSION.get()) {
                tag.putString("Status", "Cross-dimensional signal: " + target.dimension());
            } else {
                tag.putString("Status", "Signal blocked: grave is in " + target.dimension());
            }
            RecoveryIntegrations.signalStatus(player, snapshot(target)).ifPresent(status -> tag.putString("SignalStatus", status));
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        tooltip.accept(Component.translatable("tooltip.echorecovery.compass.status",
                tag.getStringOr("Status", "Points to your nearest grave.")));
        String target = tag.getStringOr("TargetId", "");
        if (!target.isBlank()) {
            tooltip.accept(Component.translatable("tooltip.echorecovery.compass.target",
                    target.substring(0, Math.min(8, target.length()))));
        }
        int distance = tag.getIntOr("Distance", -1);
        if (distance >= 0) {
            tooltip.accept(Component.translatable("tooltip.echorecovery.compass.distance", distance));
        }
        String dimension = tag.getStringOr("TargetDimension", "");
        if (!dimension.isBlank()) {
            tooltip.accept(Component.translatable("tooltip.echorecovery.compass.dimension", dimension));
        }
        if (RecoveryConfig.RECOVERY_COMPASS_WORKS_CROSS_DIMENSION.get()) {
            tooltip.accept(Component.translatable("tooltip.echorecovery.compass.cross_dimension"));
        }
        String signal = tag.getStringOr("SignalStatus", "");
        if (!signal.isBlank()) {
            tooltip.accept(Component.translatable("tooltip.echorecovery.compass.signal", signal));
        }
    }

    private static RecoveryWorldData.GraveEntry selected(ServerPlayer player, String selectedId) {
        if (selectedId == null || selectedId.isBlank()) {
            return null;
        }
        RecoveryWorldData.GraveLookup lookup = RecoveryWorldData.findLoaded(player, player.getUUID(), selectedId);
        return lookup.ambiguous() ? null : lookup.entry().orElse(null);
    }

    private static RecoveryWorldData.GraveEntry nearest(ServerPlayer player) {
        String currentDimension = player.level().dimension().identifier().toString();
        if (player.level().getServer() == null) {
            return null;
        }
        java.util.List<RecoveryWorldData.GraveEntry> graves = new java.util.ArrayList<>();
        for (net.minecraft.server.level.ServerLevel level : player.level().getServer().getAllLevels()) {
            graves.addAll(RecoveryWorldData.getOrCreate(level).getActiveGraves(player.getUUID()));
        }
        return graves.stream()
                .filter(grave -> RecoveryConfig.RECOVERY_COMPASS_WORKS_CROSS_DIMENSION.get()
                        || grave.dimension().equals(currentDimension))
                .min(java.util.Comparator.comparingDouble(grave -> grave.dimension().equals(currentDimension)
                        ? player.blockPosition().distSqr(grave.pos())
                        : Double.MAX_VALUE - grave.createdAt()))
                .orElse(null);
    }

    private static RecoveryGraveSnapshot snapshot(RecoveryWorldData.GraveEntry entry) {
        return new RecoveryGraveSnapshot(entry.graveId().toString(), entry.ownerId(), entry.ownerName(), entry.pos(),
                entry.dimension(), entry.graveTypeId(), entry.storedSlots().size(), entry.xpStored(), entry.createdAt(),
                entry.expiresAt(), entry.recovered(), entry.expired(), entry.contaminated(), entry.temporaryPlatform(),
                entry.hazardNotes());
    }
}

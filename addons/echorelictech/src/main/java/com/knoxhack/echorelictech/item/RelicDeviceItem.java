package com.knoxhack.echorelictech.item;

import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echorelictech.api.event.RelicTechEvents;
import com.knoxhack.echorelictech.api.relic.RelicCondition;
import com.knoxhack.echorelictech.api.relic.RelicInstanceData;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RelicDeviceItem extends Item {
    private static final int VAULT_SEARCH_CHUNK_RADIUS = 96;
    private final Device device;

    public RelicDeviceItem(Device device, Properties props) {
        super(props.stacksTo(1).durability(device.durability()));
        this.device = device;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        RelicInstanceData data = ensureRelicData(stack);
        if (!data.identified()) {
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.relic.unidentified"));
            return InteractionResult.FAIL;
        }
        if (data.cooldownRemaining() > 0) {
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech." + device.path() + ".cooldown"));
            return InteractionResult.FAIL;
        }
        int chargeCost = Math.max(0, RelicTechApi.nullChargeCost(stack));
        if (chargeCost > 0 && !RelicTechApi.consumeNullCharge(serverPlayer, chargeCost)) {
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.relic.no_null_charge"));
            return InteractionResult.FAIL;
        }

        boolean activated = switch (device) {
            case GRAVITY_CLAMP -> activateGravityClamp(serverPlayer, stack);
            case RIFT_LANTERN -> activateRiftLantern(serverPlayer, stack);
            case BLOOD_CIRCUIT -> activateBloodCircuit(serverPlayer, stack);
            case BROKEN_CLIMATE_KEY -> activateBrokenClimateKey(serverPlayer, stack);
            case SOUL_CAPACITOR -> activateSoulCapacitor(serverPlayer, stack);
            case VOID_COMPASS -> activateVoidCompass(serverPlayer, stack);
        };
        if (!activated) {
            return InteractionResult.FAIL;
        }
        RelicTechApi.addInstability(serverPlayer, RelicTechApi.instabilityCost(stack, device.instabilityFallback()), device.id());
        stack.set(ModDataComponents.RELIC_DATA.get(), stack.get(ModDataComponents.RELIC_DATA.get()).withCooldown(cooldown(stack)));
        RelicTechEvents.fireUse(serverPlayer, device.id(), stack);
        RelicTechApi.tryTriggerFailure(serverPlayer, stack,
                new com.knoxhack.echorelictech.api.relic.RelicUseContext(level, player, stack, player.blockPosition(), player.isShiftKeyDown()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.echorelictech." + device.path() + ".description"));
        tooltip.accept(Component.translatable("item.echorelictech." + device.path() + ".risk"));
        RelicInstanceData data = stack.get(ModDataComponents.RELIC_DATA.get());
        if (data != null && data.cooldownRemaining() > 0) {
            tooltip.accept(Component.translatable("item.echorelictech.relic.cooldown_ticks", data.cooldownRemaining()));
        }
    }

    private RelicInstanceData ensureRelicData(ItemStack stack) {
        RelicInstanceData data = stack.get(ModDataComponents.RELIC_DATA.get());
        if (data != null) {
            return data;
        }
        RelicInstanceData created = new RelicInstanceData(
                device.id(), RelicCondition.DAMAGED, 0, BlockPos.ZERO, "", 0,
                false, false, false, false, 0);
        stack.set(ModDataComponents.RELIC_DATA.get(), created);
        return created;
    }

    private boolean activateGravityClamp(ServerPlayer player, ItemStack stack) {
        int radius = radius(stack, 8);
        boolean push = player.isShiftKeyDown();
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(radius),
                entity -> entity != player && entity.isAlive());
        for (LivingEntity target : targets) {
            Vec3 delta = push
                    ? target.position().subtract(player.position())
                    : player.position().subtract(target.position());
            if (delta.lengthSqr() < 0.0001D) {
                continue;
            }
            Vec3 impulse = delta.normalize().scale(push ? 0.75D : 0.55D);
            target.push(impulse.x, 0.08D, impulse.z);
        }
        player.sendSystemMessage(Component.translatable(
                push ? "item.echorelictech.gravity_clamp.pushed" : "item.echorelictech.gravity_clamp.pulled",
                targets.size()));
        return true;
    }

    private boolean activateRiftLantern(ServerPlayer player, ItemStack stack) {
        int radius = radius(stack, 16);
        int duration = duration(stack, 600);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false));
        int marked = 0;
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, new AABB(player.blockPosition()).inflate(radius), Mob::isAlive)) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, Math.min(duration, 300), 0, false, false));
            marked++;
        }
        player.sendSystemMessage(Component.translatable("item.echorelictech.rift_lantern.activated", marked));
        return true;
    }

    private boolean activateBloodCircuit(ServerPlayer player, ItemStack stack) {
        if (player.getHealth() <= 5.0F) {
            player.sendSystemMessage(Component.translatable("item.echorelictech.blood_circuit.too_weak"));
            return false;
        }
        player.setHealth(Math.max(1.0F, player.getHealth() - 4.0F));
        int duration = duration(stack, 500);
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, false, true));
        player.sendSystemMessage(Component.translatable("item.echorelictech.blood_circuit.activated"));
        return true;
    }

    private boolean activateBrokenClimateKey(ServerPlayer player, ItemStack stack) {
        ServerLevel level = (ServerLevel) player.level();
        var weather = level.getWeatherData();
        int duration = duration(stack, 2400);
        if (player.isShiftKeyDown()) {
            weather.setClearWeatherTime(duration);
            weather.setRainTime(0);
            weather.setThunderTime(0);
            weather.setRaining(false);
            weather.setThundering(false);
            player.sendSystemMessage(Component.translatable("item.echorelictech.broken_climate_key.calmed"));
        } else {
            weather.setClearWeatherTime(0);
            weather.setRainTime(duration);
            weather.setThunderTime(duration);
            weather.setRaining(true);
            weather.setThundering(true);
            player.sendSystemMessage(Component.translatable("item.echorelictech.broken_climate_key.storm"));
        }
        return true;
    }

    private boolean activateSoulCapacitor(ServerPlayer player, ItemStack stack) {
        int duration = duration(stack, 400);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0, false, true));
        player.sendSystemMessage(Component.translatable("item.echorelictech.soul_capacitor.activated"));
        return true;
    }

    private boolean activateVoidCompass(ServerPlayer player, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        ResourceKey<Structure> structureKey = ResourceKey.create(Registries.STRUCTURE,
                Identifier.fromNamespaceAndPath("echorelictech", "pre_gridfall_research_vault"));
        var lookup = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var holder = lookup.get(structureKey);
        if (holder.isEmpty()) {
            player.sendSystemMessage(Component.translatable("item.echorelictech.void_compass.none"));
            return true;
        }
        Pair<BlockPos, Holder<Structure>> nearest = level.getChunkSource()
                .getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(holder.get()), player.blockPosition(), VAULT_SEARCH_CHUNK_RADIUS, false);
        if (nearest == null) {
            player.sendSystemMessage(Component.translatable("item.echorelictech.void_compass.none"));
            return true;
        }
        BlockPos pos = nearest.getFirst();
        Identifier vaultId = Identifier.fromNamespaceAndPath("echorelictech", "pre_gridfall_research_vault");
        RelicTechApi.recordVaultDiscovery(player, vaultId, pos);
        RelicTechEvents.fireVaultDiscover(player, vaultId, pos);
        player.sendSystemMessage(Component.translatable("item.echorelictech.void_compass.found", pos.toShortString()));
        return true;
    }

    private int cooldown(ItemStack stack) {
        return RelicTechApi.cooldownTicks(stack, device.cooldownFallback());
    }

    private int duration(ItemStack stack, int fallback) {
        int configured = RelicTechApi.activation(stack).durationTicks();
        return configured > 0 ? configured : fallback;
    }

    private int radius(ItemStack stack, int fallback) {
        int configured = RelicTechApi.activation(stack).radius();
        return configured > 0 ? configured : fallback;
    }

    public enum Device {
        GRAVITY_CLAMP("gravity_clamp", 1800, 8, 256),
        RIFT_LANTERN("rift_lantern", 1600, 6, 192),
        BLOOD_CIRCUIT("blood_circuit", 1200, 14, 128),
        BROKEN_CLIMATE_KEY("broken_climate_key", 6000, 16, 96),
        SOUL_CAPACITOR("soul_capacitor", 2400, 10, 192),
        VOID_COMPASS("void_compass", 2000, 9, 128);

        private final String path;
        private final int cooldownFallback;
        private final int instabilityFallback;
        private final int durability;

        Device(String path, int cooldownFallback, int instabilityFallback, int durability) {
            this.path = path;
            this.cooldownFallback = cooldownFallback;
            this.instabilityFallback = instabilityFallback;
            this.durability = durability;
        }

        public Identifier id() {
            return Identifier.fromNamespaceAndPath("echorelictech", path);
        }

        public String path() {
            return path;
        }

        public int cooldownFallback() {
            return cooldownFallback;
        }

        public int instabilityFallback() {
            return instabilityFallback;
        }

        public int durability() {
            return durability;
        }
    }
}

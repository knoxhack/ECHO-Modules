package com.knoxhack.echostationfall.event;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echostationfall.Config;
import com.knoxhack.echostationfall.EchoStationfall;
import com.knoxhack.echostationfall.progression.SignalPanicState;
import com.knoxhack.echostationfall.progression.StationPowerState;
import com.knoxhack.echostationfall.progression.StationfallCooldown;
import com.knoxhack.echostationfall.progression.StationfallRouteTracker;
import com.knoxhack.echostationfall.progression.StationSection;
import com.knoxhack.echostationfall.progression.StationfallProgress;
import com.knoxhack.echostationfall.registry.ModEntities;
import com.knoxhack.echostationfall.registry.ModItems;
import com.knoxhack.echostationfall.integration.StationfallSuitState;
import com.knoxhack.echostationfall.world.StationfallDimensions;
import com.knoxhack.echostationfall.world.StationfallStationState;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public class StationfallEvents {
    private static final int PRESSURE_MESSAGE_COOLDOWN_TICKS = 20 * 90;
    private static final int CUE_MESSAGE_COOLDOWN_TICKS = 20 * 60;

    public void onRegisterCommands(Object event) {
        CommandDispatcher<CommandSourceStack> dispatcher = commandDispatcher(event);
        if (dispatcher == null) {
            return;
        }
        dispatcher.register(
                Commands.literal("stationfall")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("debug")
                                .executes(ctx -> debug(ctx.getSource().getPlayerOrException())))
        );
    }

    public void onPlayerTick(Object event) {
        Player player = EchoBackendWorldEventBridge.postTickPlayer(event);
        if (player == null) {
            return;
        }
        if (player.level().isClientSide()
                || !StationfallDimensions.isStation(player.level())
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        StationfallStationState station = StationfallStationState.get(serverLevel);
        station.ensureSeeded(serverLevel);
        StationfallProgress progress = StationfallProgress.get(player);
        station.mergeFromProgress(progress);

        StationSection section = StationSection.fromPosition(player.blockPosition());
        StationPowerState power = station.powerState(section);
        StationfallSuitState suit = StationfallSuitState.get(player);
        SignalPanicState panic = SignalPanicState.get(player);
        boolean hasDampener = hasItem(player, ModItems.SIGNAL_PANIC_DAMPENER.get());

        panic.tick(player, power == StationPowerState.STABLE, hasDampener);

        if (player.tickCount % 80 == 0 && !player.hasInfiniteMaterials()) {
            int drain = power.oxygenDrain();
            if (drain > 0) {
                suit.drainOxygen(drain);
                if (power == StationPowerState.OVERLOADED || !station.breachRepaired(section)) {
                    suit.compromisePressure(Math.max(1, section.hazardIntensity()));
                }
                suit.save(player);
            }
        }

        if (suit.oxygen() <= 30 || suit.pressure() <= 30) {
            panic.gain(player, 2 + section.hazardIntensity());
        }
        if (power == StationPowerState.OFFLINE || power == StationPowerState.EMERGENCY) {
            panic.gain(player, 1);
        }

        pressure(serverLevel, player, section, power, suit, panic);
        cue(player, panic, section, power);
        if (player.tickCount % 240 == 0 && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSystemChatPacket(
                    Component.literal(StationfallRouteTracker.actionbarHint(player)),
                    true
            ));
        }
    }

    public void onClone(Object event) {
        Player player = EchoBackendWorldEventBridge.cloneNewPlayer(event);
        Player original = EchoBackendWorldEventBridge.cloneOriginalPlayer(event);
        if (player == null || original == null) {
            return;
        }
        player.getPersistentData().put(
                StationfallProgress.ROOT,
                original.getPersistentData().getCompoundOrEmpty(StationfallProgress.ROOT).copy()
        );
        player.getPersistentData().put(
                SignalPanicState.ROOT,
                original.getPersistentData().getCompoundOrEmpty(SignalPanicState.ROOT).copy()
        );
        StationfallCooldown.copy(original, player);
    }

    @SuppressWarnings("unchecked")
    private static CommandDispatcher<CommandSourceStack> commandDispatcher(Object event) {
        if (event == null) {
            return null;
        }
        try {
            Object dispatcher = event.getClass().getMethod("getDispatcher").invoke(event);
            return dispatcher instanceof CommandDispatcher<?> value ? (CommandDispatcher<CommandSourceStack>) value : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static void pressure(
            ServerLevel level,
            Player player,
            StationSection section,
            StationPowerState power,
            StationfallSuitState suit,
            SignalPanicState panic
    ) {
        int interval;
        try {
            interval = Config.PRESSURE_EVENT_TICKS.get();
        } catch (Exception ignored) {
            interval = 260;
        }
        if (player.tickCount % Math.max(20, interval) != 0) {
            return;
        }

        int chance = section.hazardIntensity() * 10
                + (power == StationPowerState.OVERLOADED ? 35 : power.hostile() ? 20 : 0);
        if (player.getRandom().nextInt(100) >= chance) {
            return;
        }

        suit.compromisePressure(4 + section.hazardIntensity() * 2);
        suit.drainOxygen(2 + section.hazardIntensity());
        suit.save(player);
        panic.gain(player, 8 + section.hazardIntensity());
        if (StationfallCooldown.ready(player, "message.pressure." + section.key(), PRESSURE_MESSAGE_COOLDOWN_TICKS)) {
            player.sendSystemMessage(Component.literal(
                    "ECHO-7 // Pressure event in " + section.displayName() + ". Seal, move, breathe."
            ));
        }

        if (power.hostile() && player.getRandom().nextBoolean()) {
            spawn(level, player, section);
        }
    }

    private static void cue(Player player, SignalPanicState panic, StationSection section, StationPowerState power) {
        if (!panic.cueReady(player)) {
            return;
        }

        boolean reduced;
        try {
            reduced = Config.REDUCED_HORROR.get();
        } catch (Exception ignored) {
            reduced = false;
        }

        String line = reduced
                ? "ECHO-7 // Signal Panic elevated. Verify route markers manually."
                : panic.critical()
                        ? "FALSE ECHO // Door label corrected: " + section.displayName() + " is HOME. Do not leave."
                        : power == StationPowerState.OVERLOADED
                                ? "ECHO-7 // The station is speaking with my voice. Do not answer."
                                : "FALSE ECHO // Mission marker updated. Safe corridor relocated behind you.";
        if (StationfallCooldown.ready(player, "message.cue." + cueKind(reduced, panic, power) + "." + section.key(), CUE_MESSAGE_COOLDOWN_TICKS)) {
            player.sendSystemMessage(Component.literal(line));
        }
        panic.markCue(player);
    }

    private static String cueKind(boolean reduced, SignalPanicState panic, StationPowerState power) {
        if (reduced) {
            return "reduced";
        }
        if (panic.critical()) {
            return "critical";
        }
        if (power == StationPowerState.OVERLOADED) {
            return "overloaded";
        }
        return "default";
    }

    private static int debug(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }

        StationSection section = StationSection.fromPosition(player.blockPosition());
        StationfallStationState station = StationfallStationState.get(level);
        StationPowerState power = station.powerState(section);
        SignalPanicState panic = SignalPanicState.get(player);
        StationfallSuitState suit = StationfallSuitState.get(player);
        long nearbyStationfallEntities = level.getEntities(
                        player,
                        AABB.ofSize(player.position(), 32.0D, 32.0D, 32.0D),
                        StationfallEvents::isStationfallEntity
                )
                .size();

        String dimension = player.level().dimension().identifier().toString();
        boolean stationDimension = StationfallDimensions.isStation(player.level());
        tell(player, "dimension=" + dimension + " station=" + stationDimension, ChatFormatting.AQUA);
        tell(player, "section=" + section.displayName()
                + " power=" + power.displayName()
                + " lighting_version=" + station.lightingVersion(), ChatFormatting.GRAY);
        tell(player, "panic=" + panic.value()
                + " oxygen=" + suit.oxygen()
                + " pressure=" + suit.pressure()
                + " nearby_entities=" + nearbyStationfallEntities, ChatFormatting.GRAY);
        tell(player, StationfallRouteTracker.status(player), ChatFormatting.YELLOW);
        EchoStationfall.LOGGER.info(
                "Stationfall debug: player={} dimension={} station={} section={} power={} lighting_version={} panic={} oxygen={} pressure={} nearby_entities={} status={}",
                player.getName().getString(),
                dimension,
                stationDimension,
                section.key(),
                power.name(),
                station.lightingVersion(),
                panic.value(),
                suit.oxygen(),
                suit.pressure(),
                nearbyStationfallEntities,
                StationfallRouteTracker.status(player)
        );
        return Command.SINGLE_SUCCESS;
    }

    private static boolean isStationfallEntity(Entity entity) {
        return EchoStationfall.MODID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace());
    }

    private static void tell(ServerPlayer player, String message, ChatFormatting color) {
        player.sendSystemMessage(Component.literal("[STATIONFALL DEBUG] " + message).withStyle(color));
    }

    private static void spawn(ServerLevel level, Player player, StationSection section) {
        EntityType<? extends Mob> type = switch (section) {
            case HYDROPONICS_BAY -> ModEntities.HYDROPONIC_GROWTH.get();
            case MEDICAL_WING -> ModEntities.MEDICAL_HUSK.get();
            case ENGINEERING_DECK, DATA_CORE -> ModEntities.MAINTENANCE_DRONE.get();
            case CONTAINMENT_WING -> ModEntities.SUIT_WITHOUT_BODY.get();
            case OBSERVATION_DECK -> ModEntities.EVA_STALKER.get();
            case COMMAND_MODULE -> ModEntities.SCREAMING_SIGNAL.get();
            default -> player.getRandom().nextInt(4) == 0
                    ? ModEntities.STATION_MIMIC.get()
                    : ModEntities.HOLLOW_CREWMAN.get();
        };
        Entity entity = type.create(level, EntitySpawnReason.EVENT);
        if (entity instanceof Mob mob) {
            mob.setPos(
                    player.getX() + player.getRandom().nextInt(7) - 3,
                    player.getY(),
                    player.getZ() + player.getRandom().nextInt(7) - 3
            );
            mob.setTarget(player);
            level.addFreshEntity(mob);
        }
    }

    private static boolean hasItem(Player player, net.minecraft.world.item.Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }
}

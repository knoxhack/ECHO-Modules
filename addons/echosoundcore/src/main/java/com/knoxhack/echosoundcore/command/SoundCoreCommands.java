package com.knoxhack.echosoundcore.command;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.SoundServiceDiagnostics;
import com.knoxhack.echosoundcore.EchoSoundCore;
import com.knoxhack.echosoundcore.SoundCoreCombatIntensity;
import com.knoxhack.echosoundcore.api.SoundCoreApi;
import com.knoxhack.echosoundcore.api.context.SoundCoreContext;
import com.knoxhack.echosoundcore.data.SoundCoreDataReloadListener;
import com.knoxhack.echosoundcore.registry.SoundCoreSounds;
import com.knoxhack.echosoundcore.util.SoundCoreCatalogValidator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Map;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;

public final class SoundCoreCommands {
    private SoundCoreCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("echosoundcore")
            .then(Commands.literal("play")
                .then(Commands.argument("soundId", StringArgumentType.string())
                    .executes(SoundCoreCommands::playSound)))
            .then(Commands.literal("stinger")
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(SoundCoreCommands::playStinger)))
            .then(Commands.literal("music")
                .then(Commands.argument("profileOrSoundId", StringArgumentType.string())
                    .executes(SoundCoreCommands::playMusic)))
            .then(Commands.literal("stop").executes(SoundCoreCommands::stop))
            .then(Commands.literal("context").executes(SoundCoreCommands::context))
            .then(Commands.literal("combat")
                .then(Commands.argument("level", StringArgumentType.word())
                    .executes(SoundCoreCommands::combat)))
            .then(Commands.literal("nexus")
                .then(Commands.argument("level", FloatArgumentType.floatArg(0.0f, 1.0f))
                    .executes(SoundCoreCommands::nexus)))
            .then(Commands.literal("debug").executes(SoundCoreCommands::debug))
            .then(Commands.literal("reload").executes(SoundCoreCommands::reload))
        );
    }

    private static int playSound(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Identifier id = Identifier.parse(StringArgumentType.getString(ctx, "soundId"));
        SoundEvent sound = findSoundById(id);
        if (sound == null) {
            source.sendFailure(Component.literal("Unknown SoundCore sound: " + id));
            return 0;
        }
        if (source.getPlayer() != null) {
            ServerPlayer player = source.getPlayer();
            EchoCoreServices.soundService().playEvent(player, id, 1.0f, 1.0f);
        }
        source.sendSuccess(() -> Component.literal("Playing SoundCore sound: " + id), false);
        return 1;
    }

    private static int playStinger(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        SoundEvent sound = findSoundByPath(id);
        if (sound == null) {
            source.sendFailure(Component.literal("Unknown stinger: " + id));
            return 0;
        }
        if (source.getPlayer() != null) {
            ServerPlayer player = source.getPlayer();
            EchoCoreServices.soundService().playEvent(player, sound.location(), 1.0f, 1.0f);
        }
        source.sendSuccess(() -> Component.literal("Playing stinger: " + id), false);
        return 1;
    }

    private static int playMusic(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String arg = StringArgumentType.getString(ctx, "profileOrSoundId");
        Identifier id = Identifier.parse(arg);
        if (source.getPlayer() != null && EchoCoreServices.soundService().playProfile(source.getPlayer(), id)) {
            source.sendSuccess(() -> Component.literal("Playing SoundCore music profile: " + id), false);
            return 1;
        }
        SoundEvent sound = findSoundById(id);
        if (sound != null && source.getPlayer() != null) {
            EchoCoreServices.soundService().playEvent(source.getPlayer(), id, 1.0f, 1.0f);
            source.sendSuccess(() -> Component.literal("Playing SoundCore music sound: " + id), false);
            return 1;
        }
        source.sendFailure(Component.literal("Unknown SoundCore music profile or sound: " + id));
        return 0;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (source.getPlayer() != null) {
            EchoCoreServices.soundService().stopControlledAudio(source.getPlayer());
        }
        source.sendSuccess(() -> Component.literal("Stopped SoundCore controlled audio."), false);
        return 1;
    }

    private static int context(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        SoundCoreContext current = com.knoxhack.echosoundcore.api.context.SoundCoreContextStack.current();
        source.sendSuccess(() -> Component.literal("ECHO SoundCore // Current Audio Context"), false);
        source.sendSuccess(() -> Component.literal("  Chapter: " + current.chapter()), false);
        source.sendSuccess(() -> Component.literal("  Combat: " + current.combatIntensity()), false);
        source.sendSuccess(() -> Component.literal("  Boss: " + current.bossId()), false);
        source.sendSuccess(() -> Component.literal("  Nexus Corruption: " + current.nexusCorruptionLevel()), false);
        source.sendSuccess(() -> Component.literal("  Terminal Open: " + current.terminalOpen()), false);
        return 1;
    }

    private static int combat(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String level = StringArgumentType.getString(ctx, "level");
        try {
            SoundCoreCombatIntensity intensity = SoundCoreCombatIntensity.valueOf(level.toUpperCase());
            SoundCoreApi.setCombatIntensity(intensity);
            source.sendSuccess(() -> Component.literal("Combat intensity set to: " + intensity), false);
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid intensity. Use none, light, heavy, elite, boss, or siege."));
            return 0;
        }
    }

    private static int nexus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        float level = FloatArgumentType.getFloat(ctx, "level");
        SoundCoreApi.setNexusCorruptionLevel(level);
        source.sendSuccess(() -> Component.literal("Nexus corruption level set to: " + level), false);
        return 1;
    }

    private static int debug(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("ECHO SoundCore // Debug Info"), false);
        source.sendSuccess(() -> Component.literal("  Music Profiles: " + SoundCoreDataReloadListener.getMusicProfiles().size()), false);
        source.sendSuccess(() -> Component.literal("  Ambience Profiles: " + SoundCoreDataReloadListener.getAmbienceProfiles().size()), false);
        SoundServiceDiagnostics diagnostics = EchoCoreServices.soundService().diagnostics();
        source.sendSuccess(() -> Component.literal("  Current Track: " + diagnostics.currentTrack()), false);
        source.sendSuccess(() -> Component.literal("  Selection: " + diagnostics.selectionReason()), false);
        source.sendSuccess(() -> Component.literal("  Active Events: " + diagnostics.activeEvents().size()), false);
        source.sendSuccess(() -> Component.literal("  Missing Assets: " + diagnostics.missingAssets().size()), false);
        source.sendSuccess(() -> Component.literal("  Last Failure: " + diagnostics.lastFailure()), false);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        SoundCoreCatalogValidator.clearCache();
        source.sendSuccess(() -> Component.literal("ECHO SoundCore // Use /reload to reload data-driven audio profiles."), false);
        return 1;
    }

    private static SoundEvent findSoundById(Identifier id) {
        for (var entry : SoundCoreSounds.getEntries()) {
            if (entry.getId().equals(id)) {
                return entry.get();
            }
        }
        return null;
    }

    private static SoundEvent findSoundByPath(String path) {
        Identifier id = EchoSoundCore.id(path);
        return findSoundById(id);
    }
}

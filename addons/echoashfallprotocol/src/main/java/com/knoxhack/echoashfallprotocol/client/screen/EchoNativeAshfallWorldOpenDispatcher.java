package com.knoxhack.echoashfallprotocol.client.screen;

import java.util.List;

import dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService;
import dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService.StartupAction;
import dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService.StartupPlan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

/**
 * Minecraft-facing dispatcher for the Native Loader Ashfall world startup plan.
 * Policy stays in {@link NativeLoaderAshfallWorldStartupService}; this class
 * only translates a safe startup plan into the current Minecraft open/create API.
 */
public final class EchoNativeAshfallWorldOpenDispatcher {
    private EchoNativeAshfallWorldOpenDispatcher() {
    }

    public static boolean openOrCreateProductWorldFromNativeLoader(Minecraft client, Screen parent) {
        if (gameplayAlreadyOpen(client)) {
            client.setScreen(null);
            return true;
        }
        StartupPlan plan = NativeLoaderAshfallWorldStartupService.prepare(client.gameDirectory.toPath());
        if (plan.action() == StartupAction.BLOCKED) {
            client.setScreen(EchoNativeMainMenuScreen.productStartupFailureScreen(parent, plan));
            return false;
        }
        if (plan.action() == StartupAction.OPEN_EXISTING) {
            try {
                if (!recordDispatchOrShowFailure(client, parent, plan, "Minecraft.createWorldOpenFlows.openWorld")) {
                    return false;
                }
                client.createWorldOpenFlows().openWorld(plan.folder(), () -> client.setScreen(null));
                return true;
            } catch (RuntimeException exception) {
                if (worldAlreadyOpening(exception)) {
                    client.setScreen(null);
                    return true;
                }
                client.setScreen(EchoNativeMainMenuScreen.productStartupFailureScreen(
                        parent,
                        "ASHFALL WORLD ACCESS BLOCKED",
                        List.of(
                                "Native Loader could not open the Ashfall product world.",
                                failureMessage(exception),
                                "World folder: " + plan.folder()
                        )));
                return false;
            }
        }
        try {
            if (!recordDispatchOrShowFailure(client, parent, plan, "Minecraft.createWorldOpenFlows.createFreshLevel")) {
                return false;
            }
            client.createWorldOpenFlows().createFreshLevel(
                    plan.folder(),
                    productLevelSettings(plan),
                    WorldOptions.defaultWithRandomSeed(),
                    lookup -> productWorldDimensions(plan, lookup),
                    parent);
            return true;
        } catch (RuntimeException exception) {
            client.setScreen(EchoNativeMainMenuScreen.productStartupFailureScreen(
                    parent,
                    "ASHFALL PRESET UNAVAILABLE",
                    List.of(
                            "Native Loader could not create the Ashfall product world.",
                            failureMessage(exception),
                            "The product path requires " + plan.worldPreset() + "."
                    )));
            return false;
        }
    }

    private static boolean gameplayAlreadyOpen(Minecraft client) {
        return client != null && (client.player != null || client.level != null);
    }

    private static boolean worldAlreadyOpening(RuntimeException exception) {
        String message = failureMessage(exception).toLowerCase(java.util.Locale.ROOT);
        return message.contains("locked")
                || message.contains("another process")
                || message.contains("cannot access the file");
    }

    private static boolean recordDispatchOrShowFailure(
            Minecraft client,
            Screen parent,
            StartupPlan plan,
            String dispatchKind) {
        boolean recorded = NativeLoaderAshfallWorldStartupService.recordProductWorldOpenDispatch(plan, dispatchKind);
        if (recorded) {
            return true;
        }
        client.setScreen(EchoNativeMainMenuScreen.productStartupFailureScreen(
                parent,
                "ASHFALL WORLD DISPATCH BLOCKED",
                List.of(
                        "Native Loader could not write product world dispatch evidence.",
                        "The product path will not fall back to vanilla world creation.",
                        "World folder: " + plan.folder()
                )));
        return false;
    }

    private static LevelSettings productLevelSettings(StartupPlan plan) {
        WorldDataConfiguration dataConfiguration = new WorldDataConfiguration(
                new DataPackConfig(List.of("vanilla", "file/" + plan.datapackFile()), List.of()),
                FeatureFlags.DEFAULT_FLAGS);
        return new LevelSettings(
                plan.worldName(),
                productGameType(plan.gameMode()),
                new LevelSettings.DifficultySettings(Difficulty.NORMAL, false, false),
                true,
                dataConfiguration);
    }

    private static WorldDimensions productWorldDimensions(StartupPlan plan, HolderLookup.Provider lookup) {
        Identifier presetId = Identifier.parse(plan.worldPreset());
        ResourceKey<WorldPreset> presetKey = ResourceKey.create(Registries.WORLD_PRESET, presetId);
        return lookup.lookupOrThrow(Registries.WORLD_PRESET)
                .get(presetKey)
                .map(holder -> holder.value().createWorldDimensions())
                .orElseThrow(() -> new IllegalStateException(
                        "Ashfall product world preset is not available: " + presetId));
    }

    private static GameType productGameType(String mode) {
        String value = mode == null ? "survival" : mode.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (value) {
            case "survival" -> GameType.SURVIVAL;
            case "adventure" -> GameType.ADVENTURE;
            case "spectator" -> GameType.SPECTATOR;
            default -> GameType.CREATIVE;
        };
    }

    private static String failureMessage(Throwable exception) {
        String message = exception == null ? "" : exception.getMessage();
        Throwable cause = exception == null ? null : exception.getCause();
        if ((message == null || message.isBlank()) && cause != null) {
            message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return message == null || message.isBlank()
                ? "Unknown native startup failure."
                : message;
    }
}

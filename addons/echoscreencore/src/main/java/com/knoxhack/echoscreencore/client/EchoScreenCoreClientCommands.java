package com.knoxhack.echoscreencore.client;

import com.knoxhack.echoscreencore.EchoScreenCoreMod;
import com.knoxhack.echoscreencore.api.EchoScreens;
import com.knoxhack.echoscreencore.api.theme.EchoAccessibilitySettings;
import com.knoxhack.echoscreencore.client.engine.EchoScreenEngine;
import com.knoxhack.echoscreencore.client.engine.EchoBindingResolver;
import com.knoxhack.echoscreencore.client.reference.ScreenCoreReferenceData;
import com.knoxhack.echoscreencore.client.screen.EchoScreen;
import com.knoxhack.echoscreencore.client.screen.EchoScreenWorkbench;
import com.knoxhack.echoscreencore.client.state.EchoPageStateStore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class EchoScreenCoreClientCommands {
    private EchoScreenCoreClientCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("echoscreencore")
                .then(Commands.literal("hub")
                    .executes(context -> open("echoscreencore:reference_feature_hub")))
                .then(Commands.literal("workbench")
                    .executes(context -> openWorkbench("echoscreencore:reference_list_detail"))
                    .then(Commands.argument("page", StringArgumentType.string())
                        .executes(context -> openWorkbench(StringArgumentType.getString(context, "page")))))
                .then(Commands.literal("open")
                    .executes(context -> open("echoscreencore:test_dashboard"))
                    .then(Commands.argument("page", StringArgumentType.greedyString())
                        .executes(context -> open(StringArgumentType.getString(context, "page")))))
                .then(Commands.literal("list")
                    .then(Commands.literal("pages")
                        .executes(context -> listResources("pages", EchoScreenEngine.availablePages())))
                    .then(Commands.literal("components")
                        .executes(context -> listResources("components", EchoScreenEngine.availableComponents())))
                    .then(Commands.literal("styles")
                        .executes(context -> listResources("styles", EchoScreenEngine.availableStyles()))))
                .then(Commands.literal("test")
                    .executes(context -> open("echoscreencore:test_dashboard")))
                .then(Commands.literal("reload")
                    .executes(context -> reload())
                    .then(Commands.literal("styles")
                        .executes(context -> reloadStyles()))
                    .then(Commands.literal("page")
                        .then(Commands.argument("page", StringArgumentType.greedyString())
                            .executes(context -> reloadPage(StringArgumentType.getString(context, "page"))))))
                .then(Commands.literal("data")
                    .then(Commands.literal("invalidate")
                        .executes(context -> invalidateData())))
                .then(Commands.literal("debug")
                    .then(Commands.literal("on")
                        .executes(context -> debug(true)))
                    .then(Commands.literal("off")
                        .executes(context -> debug(false)))
                    .then(Commands.literal("bindings")
                        .then(Commands.literal("on")
                            .executes(context -> debugBindings(true)))
                        .then(Commands.literal("off")
                            .executes(context -> debugBindings(false))))
                    .then(Commands.literal("overlays")
                        .then(Commands.literal("on")
                            .executes(context -> debug(true)))
                        .then(Commands.literal("off")
                            .executes(context -> debug(false))))
                    .then(Commands.literal("layout")
                        .then(Commands.literal("on")
                            .executes(context -> debug(true)))
                        .then(Commands.literal("off")
                            .executes(context -> debug(false)))))
                .then(Commands.literal("state")
                    .then(Commands.literal("clear")
                        .executes(context -> clearState(""))
                        .then(Commands.argument("page", StringArgumentType.greedyString())
                            .executes(context -> clearState(StringArgumentType.getString(context, "page"))))))
                .then(Commands.literal("inspect")
                    .then(Commands.literal("page")
                        .then(Commands.argument("page", StringArgumentType.string())
                            .executes(context -> inspectPage(StringArgumentType.getString(context, "page")))
                            .then(Commands.argument("width", IntegerArgumentType.integer(1))
                                .then(Commands.argument("height", IntegerArgumentType.integer(1))
                                    .executes(context -> inspectPage(
                                        StringArgumentType.getString(context, "page"),
                                        IntegerArgumentType.getInteger(context, "width"),
                                        IntegerArgumentType.getInteger(context, "height"))))))))
                .then(Commands.literal("validate")
                    .executes(context -> validate())
                    .then(Commands.literal("references")
                        .executes(context -> validateReferences()))
                    .then(Commands.literal("page")
                        .then(Commands.argument("page", StringArgumentType.string())
                            .executes(context -> inspectPage(StringArgumentType.getString(context, "page"))))))
                .then(Commands.literal("accessibility")
                    .then(Commands.literal("large_text")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                            .executes(context -> accessibility(EchoScreenCoreClient.accessibility()
                                .withLargeText(BoolArgumentType.getBool(context, "enabled"))))))
                    .then(Commands.literal("high_contrast")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                            .executes(context -> accessibility(EchoScreenCoreClient.accessibility()
                                .withHighContrast(BoolArgumentType.getBool(context, "enabled"))))))
                    .then(Commands.literal("reduced_clutter")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                            .executes(context -> accessibility(EchoScreenCoreClient.accessibility()
                                .withReducedClutter(BoolArgumentType.getBool(context, "enabled"))))))
                    .then(Commands.literal("density")
                        .then(Commands.literal("compact")
                            .executes(context -> accessibility(EchoScreenCoreClient.accessibility().withDensity("compact"))))
                        .then(Commands.literal("default")
                            .executes(context -> accessibility(EchoScreenCoreClient.accessibility().withDensity("default"))))
                        .then(Commands.literal("comfortable")
                            .executes(context -> accessibility(EchoScreenCoreClient.accessibility().withDensity("comfortable"))))))
        );
    }

    private static int open(String raw) {
        Identifier pageId = parsePage(raw);
        if (pageId == null) {
            message("Invalid ScreenCore page id: " + raw);
            return 0;
        }
        boolean opened = EchoScreens.open(pageId, EchoScreenCoreClient.testDashboardContext());
        message(opened ? "Opened ScreenCore page " + pageId + "." : "ScreenCore page opener is not available.");
        return opened ? 1 : 0;
    }

    private static int openWorkbench(String raw) {
        Identifier pageId = parsePage(raw);
        if (pageId == null) {
            message("Invalid ScreenCore page id: " + raw);
            return 0;
        }
        Minecraft.getInstance().setScreen(new EchoScreenWorkbench(pageId));
        message("Opened ScreenCore workbench for " + pageId + ".");
        return 1;
    }

    private static int listResources(String label, List<Identifier> resources) {
        if (resources.isEmpty()) {
            message("No ScreenCore " + label + " found.");
            return 0;
        }
        message("ScreenCore " + label + " (" + resources.size() + "): " + resources.stream().limit(16).map(Identifier::toString).toList());
        return resources.size();
    }

    private static int reload() {
        EchoScreenEngine.clearCaches();
        if (Minecraft.getInstance().screen instanceof EchoScreen screen) {
            screen.reloadPage();
        }
        Minecraft.getInstance().reloadResourcePacks();
        message("ScreenCore cache cleared and resource reload queued.");
        return 1;
    }

    private static int reloadStyles() {
        EchoScreenEngine.clearStyleCaches();
        if (Minecraft.getInstance().screen instanceof EchoScreen screen) {
            screen.reloadPage();
        }
        message("ScreenCore style cache cleared.");
        return 1;
    }

    private static int reloadPage(String raw) {
        Identifier pageId = parsePage(raw);
        if (pageId == null) {
            message("Invalid ScreenCore page id: " + raw);
            return 0;
        }
        EchoScreenEngine.clearPageCache(pageId);
        if (Minecraft.getInstance().screen instanceof EchoScreen screen && screen.pageId().equals(pageId)) {
            screen.reloadPage();
        }
        message("ScreenCore page cache cleared for " + pageId + ".");
        return 1;
    }

    private static int invalidateData() {
        EchoScreens.invalidateData();
        message("ScreenCore data invalidated.");
        return 1;
    }

    private static int debug(boolean enabled) {
        EchoScreenCoreClient.setDebugEnabled(enabled);
        refreshCurrentScreen();
        message("ScreenCore debug overlay " + (enabled ? "enabled." : "disabled."));
        return 1;
    }

    private static int debugBindings(boolean enabled) {
        EchoBindingResolver.setDebugPlaceholders(enabled);
        refreshCurrentScreen();
        message("ScreenCore binding debug placeholders " + (enabled ? "enabled." : "disabled."));
        return 1;
    }

    private static int clearState(String raw) {
        Identifier pageId = raw == null || raw.isBlank() ? null : parsePage(raw);
        EchoPageStateStore.clear(pageId);
        refreshCurrentScreen();
        message(pageId == null ? "ScreenCore state cleared." : "ScreenCore state cleared for " + pageId + ".");
        return 1;
    }

    private static int inspectPage(String raw) {
        Identifier pageId = parsePage(raw);
        if (pageId == null) {
            message("Invalid ScreenCore page id: " + raw);
            return 0;
        }
        for (String line : EchoScreenEngine.inspectPage(pageId).stream().limit(8).toList()) {
            message(line);
        }
        return 1;
    }

    private static int inspectPage(String raw, int width, int height) {
        Identifier pageId = parsePage(raw);
        if (pageId == null) {
            message("Invalid ScreenCore page id: " + raw);
            return 0;
        }
        for (String line : EchoScreenEngine.inspectPage(pageId, width, height).stream().limit(12).toList()) {
            message(line);
        }
        return 1;
    }

    private static int validate() {
        List<String> issues = EchoScreenEngine.validateManifests();
        for (String issue : issues.stream().limit(8).toList()) {
            message(issue);
        }
        return issues.size();
    }

    private static int validateReferences() {
        List<String> issues = EchoScreenEngine.validateReferencePages(ScreenCoreReferenceData.referencePageIds());
        for (String issue : issues.stream().limit(12).toList()) {
            message(issue);
        }
        return issues.size();
    }

    private static int accessibility(EchoAccessibilitySettings settings) {
        EchoScreenCoreClient.setAccessibility(settings);
        refreshCurrentScreen();
        message("ScreenCore accessibility updated.");
        return 1;
    }

    private static Identifier parsePage(String raw) {
        String value = raw == null || raw.isBlank() ? "test_dashboard" : raw.strip();
        try {
            return value.contains(":") ? Identifier.parse(value) : EchoScreenCoreMod.id(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static void refreshCurrentScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof EchoScreen screen) {
            minecraft.setScreen(new EchoScreen(screen.pageId(), screen.dataContext(), EchoScreenCoreClient.accessibility(),
                EchoScreenCoreClient.debugEnabled()));
        }
    }

    private static void message(String text) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(text));
        }
    }
}

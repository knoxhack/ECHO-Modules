package com.knoxhack.echo.npcore.test;

import com.knoxhack.echo.npcore.EchoNpcCore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoNpcCore.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NPC_SCREENCORE_RESOURCE_CONTRACT =
            TEST_FUNCTIONS.register("npc_screencore_resource_contract", () -> ModGameTests::npcScreenCoreResourceContract);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("npcore_screen_resources"));
        register(event, environment, "npc_screencore_resource_contract", NPC_SCREENCORE_RESOURCE_CONTRACT.getId());
    }

    private static void npcScreenCoreResourceContract(GameTestHelper helper) {
        Path root = workspaceRoot();
        try {
            String page = Files.readString(root.resolve("addons/echonpcore/src/main/resources/assets/echonpcore/eui/pages/npc_interaction.eui.xml"));
            String style = Files.readString(root.resolve("addons/echonpcore/src/main/resources/assets/echonpcore/eui/styles/npc_interaction.eui.css"));
            helper.assertTrue(page.contains("fit-mode=\"canvas\"")
                            && page.contains("design-width=\"1280\"")
                            && page.contains("design-height=\"720\""),
                    "NPCore ScreenCore page should use a 1280x720 canvas for GUI scale 3+ fitting.");
            helper.assertTrue(page.contains("class=\"npcore-layout\"")
                            && page.contains("stack-below=\"900\"")
                            && page.contains("compact-below=\"980\"")
                            && page.contains("dense-below=\"900\""),
                    "NPCore layout should advertise responsive metadata for ScreenCore diagnostics.");
            helper.assertTrue(count(page, "<copy-block") >= 10,
                    "NPCore screen should use copy-block for critical row and detail copy.");
            helper.assertTrue(page.contains("class=\"npcore-action-copy npcore-row-copy\"")
                            && page.contains("class=\"npcore-tab-copy npcore-copy-compact\"")
                            && page.contains("class=\"npcore-bridge-copy npcore-copy-compact\""),
                    "NPCore action, tab, and bridge rows should use copy-block copy areas.");
            helper.assertTrue(page.contains("<scroll class=\"npcore-bridge-scroll\""),
                    "NPCore bridge diagnostics should have their own scroll owner.");
            helper.assertTrue(style.contains(".npcore-row-copy")
                            && style.contains("title-line-height")
                            && style.contains("detail-line-height")
                            && style.contains("text-gap")
                            && style.contains("wrap: false"),
                    "NPCore copy-block styles should reserve bounded readable text space.");
            helper.succeed();
        } catch (IOException exception) {
            helper.assertTrue(false, "Failed to read NPCore ScreenCore resources: " + exception.getMessage());
        }
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String testName, Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 400, 0, true, Rotation.NONE, false, 1, 1,
                false, 16);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, path);
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return false;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoNpcCore.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static Path workspaceRoot() {
        Path current = Path.of("").toAbsolutePath();
        Path cursor = current;
        while (cursor != null) {
            if (Files.exists(cursor.resolve("settings.gradle")) || Files.exists(cursor.resolve("settings.gradle.kts"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return current;
    }

    private static int count(String value, String token) {
        int count = 0;
        int index = 0;
        while (value != null && (index = value.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }
}

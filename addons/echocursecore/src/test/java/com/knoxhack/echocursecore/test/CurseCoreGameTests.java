package com.knoxhack.echocursecore.test;

import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echocursecore.EchoCurseCore;
import com.knoxhack.echocursecore.api.CurseCoreApi;
import com.knoxhack.echocursecore.integration.arcana.CurseCoreArcanaIntegration;
import com.knoxhack.echocursecore.menu.CurseContractMenu;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = EchoCurseCore.MODID)
public final class CurseCoreGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoCurseCore.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CURSE_PROVIDER_REGISTERS =
            TEST_FUNCTIONS.register("curse_provider_registers", () -> CurseCoreGameTests::curseProviderRegisters);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CURSE_LIFECYCLE =
            TEST_FUNCTIONS.register("curse_lifecycle_applies_and_cleanses", () -> CurseCoreGameTests::curseLifecycle);

    private CurseCoreGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
        eventBus.addListener(CurseCoreGameTests::registerTests);
    }

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, CURSE_PROVIDER_REGISTERS.getId(),
                () -> CurseCoreGameTests::curseProviderRegisters);
        event.register(Registries.TEST_FUNCTION, CURSE_LIFECYCLE.getId(), () -> CurseCoreGameTests::curseLifecycle);
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("cursecore_first_slice"));
        register(event, environment, "curse_provider_registers", CURSE_PROVIDER_REGISTERS.getId());
        register(event, environment, "curse_lifecycle_applies_and_cleanses", CURSE_LIFECYCLE.getId());
    }

    private static void curseProviderRegisters(GameTestHelper helper) {
        CurseCoreArcanaIntegration.register();
        helper.assertTrue(ArcanaCoreServices.curses().stream().anyMatch(curse -> CurseCoreApi.ECHO_ROT.equals(curse.id())),
                "Echo Rot should register into Arcana Core");
        helper.assertTrue(ArcanaCoreServices.curses().stream().anyMatch(curse -> CurseCoreApi.GLASS_VEINS.equals(curse.id())),
                "Glass Veins should register into Arcana Core");
        helper.assertTrue(ArcanaCoreServices.curses().stream().anyMatch(curse -> CurseCoreApi.RIFT_HUNGER.equals(curse.id())),
                "Rift Hunger should register into Arcana Core");
        helper.assertTrue(ArcanaCoreServices.curses().stream().anyMatch(curse -> CurseCoreApi.VOID_MARK.equals(curse.id())),
                "Void Mark should register into Arcana Core");
        helper.succeed();
    }

    private static void curseLifecycle(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(CurseCoreApi.applyEchoRot(player, 2, "game_test"),
                "Echo Rot should apply to player persistent data");
        helper.assertTrue(CurseCoreApi.stage(player, CurseCoreApi.ECHO_ROT) == 2,
                "Echo Rot should persist stage");
        helper.assertTrue(CurseCoreApi.cleanseFirstMinorCurse(player),
                "Curse Cleansing bridge should reduce an active curse");
        helper.assertTrue(CurseCoreApi.stage(player, CurseCoreApi.ECHO_ROT) == 1,
                "Curse Cleansing should reduce Echo Rot by one stage");
        helper.assertTrue(CurseCoreApi.applyCurse(player, CurseCoreApi.VOID_MARK, 4, "game_test"),
                "Void Mark should apply as a broader HUD curse");
        helper.assertTrue(CurseCoreApi.activeCurses(player).containsKey(CurseCoreApi.VOID_MARK),
                "Active curse summary should include newly supported curse types");
        helper.assertTrue(CurseCoreApi.acceptContract(player, CurseCoreApi.BLOOD_DEBT, 2, "game_test"),
                "Blood Debt should support explicit curse contract binding");
        helper.assertTrue(CurseCoreApi.contractCount(player) == 1,
                "Curse contract count should be exposed for HUD treatment");
        CurseContractMenu menu = new CurseContractMenu(12, player.getInventory(), player);
        helper.assertTrue(menu.contractCount() == 1 && menu.bloodDebtStage() == 2,
                "Curse contract screen data should expose contract and stage state");
        helper.assertTrue(menu.contractDebt() >= 80 && menu.contractResistance() > 0,
                "Curse contract screen should expose debt and resistance diagnostics");
        helper.assertTrue(menu.cleansingReadiness() >= 0,
                "Curse contract screen should expose cleansing readiness diagnostics");
        helper.assertTrue(menu.cleansingPlanCode() == 2
                        && "pay_contract_debt".equals(CurseCoreApi.recommendedCleansingAction(player)),
                "Curse contract screen should recommend debt payment before severing bound contracts");
        helper.assertTrue(menu.cleansingPlanTargetCode() == 2
                        && CurseCoreApi.BLOOD_DEBT.equals(CurseCoreApi.recommendedCleansingTarget(player)),
                "Curse contract screen should expose the current recommended contract target");
        helper.assertTrue(!menu.clickMenuButton(player, CurseContractMenu.BUTTON_CLEANSE_BLOOD_DEBT),
                "Contract-bound curses should block direct cleanse buttons while debt remains");
        helper.assertTrue(menu.clickMenuButton(player, CurseContractMenu.BUTTON_PAY_DEBT),
                "Curse contract screen should provide a contract debt payment flow");
        helper.assertTrue(CurseCoreApi.totalContractDebt(player) < 80,
                "Contract payment should reduce stored debt before full severing");
        helper.assertTrue(menu.clickMenuButton(player, CurseContractMenu.BUTTON_ACCEPT_VOID_MARK),
                "Curse contract screen should bind a selected forbidden contract");
        helper.assertTrue(CurseCoreApi.contractBound(player, CurseCoreApi.VOID_MARK),
                "Curse contract screen button should create a contract-bound curse");
        CurseCoreApi.reduceCurse(player, CurseCoreApi.BLOOD_DEBT, 5, "ritual_cleansing");
        helper.assertTrue(CurseCoreApi.stage(player, CurseCoreApi.BLOOD_DEBT) == 1,
                "Contract-bound curse should resist ordinary final cleansing");
        while (CurseCoreApi.contractDebt(player, CurseCoreApi.BLOOD_DEBT) > 0) {
            CurseCoreApi.payContractDebt(player, CurseCoreApi.BLOOD_DEBT, 100, "game_test_clear");
        }
        helper.assertTrue(CurseCoreApi.severReadyCount(player) > 0
                        && CurseCoreApi.cleansingPlanCode(player) == 3,
                "Debt-cleared contracts should surface as sever-ready in the cleansing plan");
        helper.assertTrue(menu.clickMenuButton(player, CurseContractMenu.BUTTON_SEVER_CONTRACT),
                "Curse contract screen should expose a debt-gated sever and cleanse path");
        helper.assertTrue(!CurseCoreApi.contractBound(player, CurseCoreApi.BLOOD_DEBT)
                        && CurseCoreApi.stage(player, CurseCoreApi.BLOOD_DEBT) == 0,
                "Debt-gated severing should break the contract and fully cleanse the curse");
        helper.succeed();
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String testName, Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                400,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                2);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoCurseCore.MODID, path);
    }
}

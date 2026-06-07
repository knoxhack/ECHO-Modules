package com.knoxhack.echofamiliarcore.test;

import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echofamiliarcore.EchoFamiliarCore;
import com.knoxhack.echofamiliarcore.api.FamiliarCoreApi;
import com.knoxhack.echofamiliarcore.entity.ArcanaFamiliarEntity;
import com.knoxhack.echofamiliarcore.menu.FamiliarCommandMenu;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FamiliarCoreGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoFamiliarCore.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STARTER_BONDS =
            TEST_FUNCTIONS.register("starter_familiar_bonds_persist_and_tick", () -> FamiliarCoreGameTests::starterBonds);

    private FamiliarCoreGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("familiarcore_first_slice"));
        register(event, environment, "starter_familiar_bonds_persist_and_tick", STARTER_BONDS.getId());
    }

    private static void starterBonds(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos playerPos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.teleportTo(helper.getLevel(), playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D,
                java.util.Set.of(), 0.0F, 0.0F, false);
        helper.assertTrue(FamiliarCoreApi.bind(player, FamiliarCoreApi.AETHER_WISP, "game_test"),
                "Aether Wisp should bind to player data");
        helper.assertTrue(FamiliarCoreApi.AETHER_WISP.equals(FamiliarCoreApi.activeFamiliar(player)),
                "Aether Wisp should be the active familiar");
        helper.runAfterDelay(2L, () -> {
            ArcanaFamiliarEntity wisp = FamiliarCoreApi.activeEntity(player);
            helper.assertTrue(wisp != null && wisp.familiarKind() == ArcanaFamiliarEntity.KIND_AETHER_WISP,
                    "Aether Wisp bond should summon a persistent familiar entity; " + diagnostic(player));
            wisp.mobInteract(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(wisp.command() == ArcanaFamiliarEntity.COMMAND_STAY,
                    "Owner interaction should cycle familiar command state");
            FamiliarCommandMenu menu = new FamiliarCommandMenu(11, player.getInventory(), player);
            helper.assertTrue(menu.kind() == ArcanaFamiliarEntity.KIND_AETHER_WISP,
                    "Familiar command menu should expose active familiar status");
            helper.assertTrue(menu.clickMenuButton(player, FamiliarCommandMenu.BUTTON_DEFEND),
                    "Familiar command menu should set explicit command state");
            helper.assertTrue(wisp.command() == ArcanaFamiliarEntity.COMMAND_DEFEND,
                    "Explicit command UI should update the familiar entity");
            int xpBefore = FamiliarCoreApi.bondExperience(player);
            helper.assertTrue(menu.clickMenuButton(player, FamiliarCommandMenu.BUTTON_TRAIN),
                    "Familiar command menu should train the active bond");
            helper.assertTrue(FamiliarCoreApi.bondExperience(player) > xpBefore,
                    "Training should increase persistent bond experience");
            helper.assertTrue(FamiliarCoreApi.addBondExperience(player, 96, "game_test_evolution"),
                    "Focused training should advance familiar evolution state");
            helper.assertTrue(FamiliarCoreApi.evolutionTier(player) >= 1 && FamiliarCoreApi.upgradePoints(player) > 0,
                    "Bond levels should award evolution tier and upgrade points");
            int powerBefore = FamiliarCoreApi.evolutionPower(player);
            helper.assertTrue(menu.clickMenuButton(player, FamiliarCommandMenu.BUTTON_UPGRADE_ATTUNEMENT),
                    "Familiar command menu should spend upgrade points on an attunement upgrade");
            helper.assertTrue(FamiliarCoreApi.upgradeRank(player) > 0,
                    "Familiar attunement upgrade should persist in player bond data");
            helper.assertTrue(FamiliarCoreApi.evolutionPower(player) > powerBefore
                            && FamiliarCoreApi.evolutionForm(player).contains("attuned"),
                    "Familiar evolution should derive a form and power rating from branch upgrades");
            FamiliarCoreApi.addBondExperience(player, 260, "game_test_branch_upgrades");
            helper.assertTrue(menu.clickMenuButton(player, FamiliarCommandMenu.BUTTON_UPGRADE_WARDING)
                            && menu.clickMenuButton(player, FamiliarCommandMenu.BUTTON_UPGRADE_SCOUTING),
                    "Familiar command menu should support branch upgrades for warding and scouting");
            helper.assertTrue(FamiliarCoreApi.upgradeRank(player, FamiliarCoreApi.UPGRADE_WARDING) > 0
                            && FamiliarCoreApi.upgradeRank(player, FamiliarCoreApi.UPGRADE_SCOUTING) > 0,
                    "Familiar branch upgrades should persist independently");
            helper.assertTrue(FamiliarCoreApi.evolutionFormCode(player) >= 1
                            && menu.evolutionPower() > 0
                            && menu.evolutionAbilityCode() > 0,
                    "Familiar command menu should expose evolution form, ability, and power telemetry");
            helper.assertTrue(!FamiliarCoreApi.evolutionAbility(player).isBlank(),
                    "Familiar evolution should expose a branch-specific active ability label");
            double rawBefore = ArcanaCoreServices.aether().getAether(player, AetherSignalType.RAW_AETHER);
            FamiliarCoreApi.pulse(player);
            helper.assertTrue(ArcanaCoreServices.aether().getAether(player, AetherSignalType.RAW_AETHER) > rawBefore,
                    "Aether Wisp pulse should add raw aether");
            helper.assertTrue(FamiliarCoreApi.bind(player, FamiliarCoreApi.SPIRIT_DRONE, "game_test"),
                    "Spirit Drone should replace the active starter bond");
            helper.runAfterDelay(2L, () -> {
                ArcanaFamiliarEntity drone = FamiliarCoreApi.activeEntity(player);
                helper.assertTrue(drone != null && drone.familiarKind() == ArcanaFamiliarEntity.KIND_SPIRIT_DRONE,
                        "Spirit Drone bond should summon an actual commandable familiar entity");
                FamiliarCoreApi.pulse(player);
                helper.assertTrue(player.hasEffect(MobEffects.ABSORPTION),
                        "Spirit Drone tick should apply a shield pulse");
                helper.succeed();
            });
        });
    }

    private static String diagnostic(ServerPlayer player) {
        int familiars = 0;
        int total = 0;
        Iterable<Entity> entities = player.level() instanceof ServerLevel serverLevel
                ? serverLevel.getAllEntities()
                : java.util.List.of();
        for (Entity entity : entities) {
            total++;
            if (entity instanceof ArcanaFamiliarEntity) {
                familiars++;
            }
        }
        String uuid = player.getPersistentData()
                .getCompoundOrEmpty("echofamiliarcore_bond")
                .getStringOr("entity_uuid", "");
        return "active=" + FamiliarCoreApi.activeFamiliar(player)
                + ", entity_uuid=" + uuid
                + ", all_entities=" + total
                + ", familiars=" + familiars
                + ", player_pos=" + player.blockPosition();
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
        return Identifier.fromNamespaceAndPath(EchoFamiliarCore.MODID, path);
    }
}

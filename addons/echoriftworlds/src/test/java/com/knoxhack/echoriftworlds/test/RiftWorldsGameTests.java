package com.knoxhack.echoriftworlds.test;

import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echoriftworlds.EchoRiftWorlds;
import com.knoxhack.echoriftworlds.api.RiftWorldsApi;
import com.knoxhack.echoriftworlds.registry.ModBlocks;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RiftWorldsGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoRiftWorlds.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RIFT_CRACK =
            TEST_FUNCTIONS.register("rift_crack_and_pocket_encounter", () -> RiftWorldsGameTests::riftCrackAndPocketEncounter);

    private RiftWorldsGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("riftworlds_first_slice"));
        register(event, environment, "rift_crack_and_pocket_encounter", RIFT_CRACK.getId());
    }

    private static void riftCrackAndPocketEncounter(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos riftPos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.RIFT_CRACK.get().defaultBlockState());
        double before = ArcanaCoreServices.aether().getAether(player, AetherSignalType.RIFT_AETHER);
        RiftWorldsApi.scanRiftCrack(player, riftPos);
        helper.assertTrue(ArcanaCoreServices.aether().getAether(player, AetherSignalType.RIFT_AETHER) > before,
                "Rift Crack scan should add rift aether");
        RiftWorldsApi.triggerPocketEncounter(player, riftPos);
        helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING),
                "Pocket Rift encounter should apply a temporary traversal effect");
        helper.assertTrue(RiftWorldsApi.hasActivePocket(player),
                "Pocket Rift should store an active return anchor");
        helper.assertTrue(RiftWorldsApi.openedPocketInstances(player) == 1
                        && "active".equals(RiftWorldsApi.pocketInstanceState(player)),
                "Pocket Rift lifecycle should track opened instances and the active state");
        RiftWorldsApi.PocketRiftInstance instance = RiftWorldsApi.activePocketInstance(player);
        helper.assertTrue(instance != null && !instance.id().isBlank() && instance.expiresAt() > player.level().getGameTime(),
                "Pocket Rift should allocate a managed instance id, center, and expiry");
        helper.assertTrue(RiftWorldsApi.activePocketRemainingTicks(player) > 0L,
                "Pocket Rift lifecycle should expose remaining managed instance time");
        helper.assertTrue(player.level().getBlockState(player.blockPosition().below()).is(Blocks.AMETHYST_BLOCK),
                "Pocket Rift should generate a small encounter platform");
        helper.assertTrue(RiftWorldsApi.inPocketRiftLevel(player) || player.level() == helper.getLevel(),
                "Pocket Rift should prefer the dedicated pocket dimension and keep an overworld fallback");
        helper.assertTrue(RiftWorldsApi.returnFromPocket(player),
                "Pocket Rift return anchor should return the player");
        helper.assertTrue(RiftWorldsApi.completedPocketInstances(player) == 1,
                "Pocket Rift lifecycle should record completed instance returns");
        helper.assertTrue(player.level() == helper.getLevel(),
                "Pocket Rift return should restore the source level");
        RiftWorldsApi.triggerPocketEncounter(player, riftPos);
        RiftWorldsApi.triggerPocketEncounter(player, riftPos);
        helper.assertTrue(RiftWorldsApi.openedPocketInstances(player) == 3
                        && RiftWorldsApi.supersededPocketInstances(player) == 1,
                "Opening a replacement Pocket Rift should mark the prior active lane as superseded");
        helper.assertTrue(RiftWorldsApi.abandonPocketInstance(player)
                        && RiftWorldsApi.abandonedPocketInstances(player) == 1
                        && "abandoned".equals(RiftWorldsApi.pocketInstanceState(player)),
                "Pocket Rift lifecycle should expose explicit abandoned instance state");
        boolean cleaned = RiftWorldsApi.cleanupPocketChamber(player);
        helper.assertTrue(!RiftWorldsApi.inPocketRiftLevel(player) && (!cleaned || RiftWorldsApi.cleanedPocketInstances(player) == 1),
                "Pocket Rift lifecycle should expose safe cleanup when the dedicated pocket level is available");
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
        return Identifier.fromNamespaceAndPath(EchoRiftWorlds.MODID, path);
    }
}

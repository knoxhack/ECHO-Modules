package com.knoxhack.echoritualcore.test;

import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echoarcanacore.api.RitualFamily;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echoritualcore.EchoRitualCore;
import com.knoxhack.echoritualcore.api.RitualCoreApi;
import com.knoxhack.echoritualcore.block.entity.OfferingPedestalBlockEntity;
import com.knoxhack.echoritualcore.integration.arcana.RitualCoreArcanaIntegration;
import com.knoxhack.echoritualcore.integration.holomap.RitualCoreMapDataProvider;
import com.knoxhack.echoritualcore.registry.ModBlocks;
import com.knoxhack.echoritualcore.registry.ModItems;
import com.knoxhack.echoritualcore.ritual.RitualStructureReport;
import com.knoxhack.echoritualcore.ritual.RitualStructureValidator;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RitualCoreGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoRitualCore.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BASIC_ALTAR_PLACES =
            TEST_FUNCTIONS.register("basic_altar_places", () -> RitualCoreGameTests::basicAltarPlaces);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RITUAL_PROVIDER_REGISTERS =
            TEST_FUNCTIONS.register("ritual_provider_registers", () -> RitualCoreGameTests::ritualProviderRegisters);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RELICTECH_DATA_CONTRACT =
            TEST_FUNCTIONS.register("relictech_data_contract", () -> RitualCoreGameTests::relictechDataContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STRUCTURE_VALIDATOR_RECOGNIZES_ARRAY =
            TEST_FUNCTIONS.register("structure_validator_recognizes_array", () -> RitualCoreGameTests::structureValidatorRecognizesArray);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PEDESTAL_STORES_INPUT =
            TEST_FUNCTIONS.register("pedestal_stores_input", () -> RitualCoreGameTests::pedestalStoresInput);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NON_RELICTECH_RITUALS_COMPLETE =
            TEST_FUNCTIONS.register("non_relictech_rituals_complete", () -> RitualCoreGameTests::nonRelictechRitualsComplete);

    private RitualCoreGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("ritualcore_hardening"));
        register(event, environment, "basic_altar_places", BASIC_ALTAR_PLACES.getId());
        register(event, environment, "ritual_provider_registers", RITUAL_PROVIDER_REGISTERS.getId());
        register(event, environment, "relictech_data_contract", RELICTECH_DATA_CONTRACT.getId());
        register(event, environment, "structure_validator_recognizes_array", STRUCTURE_VALIDATOR_RECOGNIZES_ARRAY.getId());
        register(event, environment, "pedestal_stores_input", PEDESTAL_STORES_INPUT.getId());
        register(event, environment, "non_relictech_rituals_complete", NON_RELICTECH_RITUALS_COMPLETE.getId());
    }

    private static void basicAltarPlaces(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 0, 0);
        helper.setBlock(pos, ModBlocks.BASIC_ALTAR.get().defaultBlockState());
        helper.assertBlock(pos, b -> b == ModBlocks.BASIC_ALTAR.get(),
                b -> net.minecraft.network.chat.Component.literal("Basic Altar should place"));
        helper.succeed();
    }

    private static void ritualProviderRegisters(GameTestHelper helper) {
        RitualCoreArcanaIntegration.register();
        helper.assertTrue(ArcanaCoreServices.rituals().stream()
                .anyMatch(ritual -> RitualCoreApi.RELIC_STABILIZATION.equals(ritual.id())
                        && ritual.family() == RitualFamily.RELIC_AWAKENING),
                "Relic Stabilization should register as a relic awakening ritual");
        helper.assertTrue(ArcanaCoreServices.rituals().stream()
                .anyMatch(ritual -> RitualCoreApi.CURSE_CLEANSING_I.equals(ritual.id())
                        && ritual.family() == RitualFamily.CURSE_CLEANSING),
                "Curse Cleansing I should register as a curse cleansing ritual");
        helper.succeed();
    }

    private static void relictechDataContract(GameTestHelper helper) {
        if (!ModList.get().isLoaded("echorelictech")) {
            helper.succeed();
            return;
        }
        try {
            Class<?> apiClass = Class.forName("com.knoxhack.echorelictech.api.RelicTechApi");
            Class<?> componentClass = Class.forName("com.knoxhack.echorelictech.registry.ModDataComponents");
            ItemStack stack = (ItemStack) apiClass
                    .getMethod("createRelicStack", Identifier.class)
                    .invoke(null, Identifier.fromNamespaceAndPath("echorelictech", "phase_anchor"));
            boolean relic = (boolean) apiClass.getMethod("isRelic", ItemStack.class).invoke(null, stack);
            Object holder = componentClass.getField("RELIC_DATA").get(null);
            @SuppressWarnings("unchecked")
            DataComponentType<Object> component = (DataComponentType<Object>) holder.getClass().getMethod("get").invoke(holder);
            Object data = stack.get(component);
            boolean identified = data != null && (boolean) data.getClass().getMethod("identified").invoke(data);
            Object condition = data == null ? null : data.getClass().getMethod("condition").invoke(data);
            helper.assertTrue(relic, "RitualCore should see RelicTech relic stacks");
            helper.assertTrue(identified, "RelicTech ritual focus should expose identified lifecycle data");
            helper.assertTrue(condition != null && "DAMAGED".equals(condition.toString()),
                    "Starter RelicTech stack should begin damaged for stabilization rituals");
            helper.succeed();
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.fail("RelicTech data contract reflection failed: " + exception.getClass().getSimpleName());
        }
    }

    private static void structureValidatorRecognizesArray(GameTestHelper helper) {
        BlockPos origin = new BlockPos(4, 1, 4);
        buildBasicArray(helper, origin);
        RitualStructureReport report = RitualStructureValidator.validate(helper.getLevel(), helper.absolutePos(origin));
        helper.assertTrue(report.validBasicArray(), "RitualCore basic array should validate with four runes and a pedestal");
        helper.assertTrue(report.stabilityScore() >= 50, "RitualCore basic array should expose a useful stability score");
        helper.succeed();
    }

    private static void pedestalStoresInput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.OFFERING_PEDESTAL.get().defaultBlockState());
        if (helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof OfferingPedestalBlockEntity pedestal) {
            pedestal.setItem(OfferingPedestalBlockEntity.SLOT, new ItemStack(ModItems.AETHER_CHALK.get()));
            helper.assertTrue(pedestal.getItem(OfferingPedestalBlockEntity.SLOT).is(ModItems.AETHER_CHALK.get()),
                    "Offering Pedestal should persist one ritual input item");
            helper.succeed();
            return;
        }
        helper.fail("Offering Pedestal should create a block entity");
    }

    private static void nonRelictechRitualsComplete(GameTestHelper helper) {
        BlockPos origin = new BlockPos(4, 1, 4);
        buildBasicArray(helper, origin);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos altar = helper.absolutePos(origin);

        ItemStack chalk = new ItemStack(ModItems.AETHER_CHALK.get());
        helper.assertTrue(RitualCoreApi.tryAltarRitual(player, chalk, false, altar),
                "Aether Calibration should run from Aether Chalk");
        helper.assertTrue(count(player, ModItems.REFINED_AETHER_SAMPLE.get()) >= 1,
                "Aether Calibration should grant Refined Aether Sample");

        ItemStack focus = new ItemStack(ModItems.RITUAL_FOCUS.get());
        player.getInventory().add(new ItemStack(ModItems.REFINED_AETHER_SAMPLE.get()));
        helper.assertTrue(RitualCoreApi.tryAltarRitual(player, focus, false, altar),
                "Spell Core Awakening should run from Ritual Focus");
        helper.assertTrue(count(player, ModItems.AWAKENED_SPELL_CORE.get()) >= 1,
                "Spell Core Awakening should grant Awakened Spell Core");

        ItemStack sample = new ItemStack(ModItems.REFINED_AETHER_SAMPLE.get());
        player.getInventory().add(new ItemStack(ModItems.AETHER_CHALK.get()));
        helper.assertTrue(RitualCoreApi.tryAltarRitual(player, sample, false, altar),
                "Rift Crack Reveal should run from Refined Aether Sample");
        java.util.List<IMapMarker> markers = RitualCoreMapDataProvider.INSTANCE.markers(player);
        helper.assertTrue(markers.stream().anyMatch(marker -> marker.title().contains("Rift Crack")),
                "Rift Crack Reveal should generate a HoloMap marker");
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
        return Identifier.fromNamespaceAndPath(EchoRitualCore.MODID, path);
    }

    private static void buildBasicArray(GameTestHelper helper, BlockPos origin) {
        helper.setBlock(origin, ModBlocks.BASIC_ALTAR.get().defaultBlockState());
        helper.setBlock(origin.offset(2, 0, 0), ModBlocks.RUNE_CIRCLE.get().defaultBlockState());
        helper.setBlock(origin.offset(-2, 0, 0), ModBlocks.RUNE_CIRCLE.get().defaultBlockState());
        helper.setBlock(origin.offset(0, 0, 2), ModBlocks.RUNE_CIRCLE.get().defaultBlockState());
        helper.setBlock(origin.offset(0, 0, -2), ModBlocks.RUNE_CIRCLE.get().defaultBlockState());
        helper.setBlock(origin.offset(1, 0, 1), ModBlocks.OFFERING_PEDESTAL.get().defaultBlockState());
        helper.setBlock(origin.offset(-1, 0, -1), ModBlocks.STABILITY_PYLON.get().defaultBlockState());
    }

    private static int count(ServerPlayer player, Item item) {
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                found += stack.getCount();
            }
        }
        return found;
    }
}

package com.knoxhack.echo.equipmentcore.registry;

import com.knoxhack.echo.equipmentcore.api.EquipmentService;
import com.knoxhack.echo.equipmentcore.api.EquipmentSlot;
import com.knoxhack.echo.equipmentcore.api.EquipmentStats;
import com.knoxhack.echo.equipmentcore.integration.EquipmentHazardResistanceProvider;
import com.knoxhack.echo.equipmentcore.item.DivingSuitItem;
import com.knoxhack.echo.equipmentcore.item.UpgradeModuleItem;
import com.knoxhack.echo.hazardcore.api.HazardService;
import com.knoxhack.echo.hazardcore.api.HazardType;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, com.knoxhack.echo.equipmentcore.EchoEquipmentCore.MODID);
    private static final int TEST_PADDING = 24;

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SLOT_REGISTRATION =
            TEST_FUNCTIONS.register("slot_registration", () -> ModGameTests::slotRegistration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SUIT_STATS =
            TEST_FUNCTIONS.register("suit_stats", () -> ModGameTests::suitStats);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UPGRADE_INSTALL =
            TEST_FUNCTIONS.register("upgrade_install", () -> ModGameTests::upgradeInstall);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DURABILITY_DAMAGE =
            TEST_FUNCTIONS.register("durability_damage", () -> ModGameTests::durabilityDamage);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RESISTANCE_PROVIDER =
            TEST_FUNCTIONS.register("resistance_provider", () -> ModGameTests::resistanceProvider);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        register(event, "slot_registration", SLOT_REGISTRATION.getId());
        register(event, "suit_stats", SUIT_STATS.getId());
        register(event, "upgrade_install", UPGRADE_INSTALL.getId());
        register(event, "durability_damage", DURABILITY_DAMAGE.getId());
        register(event, "resistance_provider", RESISTANCE_PROVIDER.getId());
    }

    private static void slotRegistration(GameTestHelper helper) {
        helper.assertTrue(ModItems.SHOAL_SUIT.get() != Items.AIR, "Shoal Suit should be registered");
        helper.assertTrue(ModItems.HADAL_HARDSUIT.get() != Items.AIR, "Hadal Hardsuit should be registered");
        helper.assertTrue(ModItems.REBREATHER.get() != Items.AIR, "Rebreather should be registered");
        helper.assertTrue(ModDataComponents.INSTALLED_UPGRADES.get() != null, "Installed upgrades component should be registered");
        EquipmentService service = EquipmentService.find();
        helper.assertTrue(service.getSlot(EquipmentSlot.SUIT_FRAME.id()) != null, "Suit frame slot should be registered");
        helper.assertTrue(service.getSlot(EquipmentSlot.REBREATHER.id()) != null, "Rebreather slot should be registered");
        helper.assertTrue(service.getSlot(EquipmentSlot.LIGHT_SENSOR.id()) != null, "Light sensor slot should be registered");
        helper.assertTrue(service.getSlot(EquipmentSlot.TOOL_MOUNT.id()) != null, "Tool mount slot should be registered");
        helper.succeed();
    }

    private static void suitStats(GameTestHelper helper) {
        ItemStack suit = new ItemStack(ModItems.SHOAL_SUIT.get());
        EquipmentStats stats = ((DivingSuitItem) suit.getItem()).getStats(suit);
        helper.assertTrue(stats.pressureResistance() == 0.2F, "Shoal suit should expose 0.2 pressure resistance");
        helper.assertTrue(stats.durability() == 120, "Shoal suit should start with full durability");

        ItemStack hadal = new ItemStack(ModItems.HADAL_HARDSUIT.get());
        EquipmentStats hadalStats = ((DivingSuitItem) hadal.getItem()).getStats(hadal);
        helper.assertTrue(hadalStats.pressureResistance() == 1.0F, "Hadal hardsuit should expose 1.0 pressure resistance");
        helper.succeed();
    }

    private static void upgradeInstall(GameTestHelper helper) {
        ItemStack suit = new ItemStack(ModItems.DIVERS_RIG.get());
        ItemStack upgrade = new ItemStack(ModItems.REINFORCED_JOINTS.get());
        helper.assertTrue(DivingSuitItem.installUpgrade(suit, upgrade), "Reinforced Joints should install into Diver's Rig");
        helper.assertTrue(upgrade.isEmpty(), "Installing an upgrade should consume one upgrade item");
        EquipmentStats stats = ((DivingSuitItem) suit.getItem()).getStats(suit);
        helper.assertTrue(stats.pressureResistance() == 0.6F, "Reinforced Joints should add 0.1 pressure resistance");

        ItemStack duplicate = new ItemStack(ModItems.REINFORCED_JOINTS.get());
        helper.assertFalse(DivingSuitItem.installUpgrade(suit, duplicate), "Duplicate upgrade should be rejected");
        helper.assertTrue(duplicate.getCount() == 1, "Duplicate rejection must not consume the upgrade");
        helper.succeed();
    }

    private static void durabilityDamage(GameTestHelper helper) {
        ItemStack suit = new ItemStack(ModItems.ABYSSAL_EXOSUIT.get());
        int max = suit.getMaxDamage();
        helper.assertTrue(max > 0, "Abyssal exosuit should have durability");
        suit.setDamageValue(1);
        EquipmentStats stats = ((DivingSuitItem) suit.getItem()).getStats(suit);
        helper.assertTrue(stats.durability() == max - 1, "Damaged suit should report reduced durability");
        helper.succeed();
    }

    private static void resistanceProvider(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack suit = new ItemStack(ModItems.DIVERS_RIG.get());
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, suit);
        float pressure = EquipmentHazardResistanceProvider.INSTANCE.getResistance(player, HazardType.PRESSURE);
        helper.assertTrue(pressure == 0.5F, "Provider should map suit pressure resistance to hazard resistance");
        HazardService hazardService = HazardService.find();
        float serviceResistance = hazardService.getTotalResistance(player, HazardType.PRESSURE);
        helper.assertTrue(serviceResistance >= 0.5F, "HazardService should aggregate equipment provider resistance");
        helper.succeed();
    }

    private static void register(RegisterGameTestsEvent event, String testName, Identifier functionId) {
        net.minecraft.core.Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("equipmentcore_" + testName));
        TestData<net.minecraft.core.Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 400, 0, true, net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, TEST_PADDING
        );
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(com.knoxhack.echo.equipmentcore.EchoEquipmentCore.MODID, path);
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(com.knoxhack.echo.equipmentcore.EchoEquipmentCore.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }
}

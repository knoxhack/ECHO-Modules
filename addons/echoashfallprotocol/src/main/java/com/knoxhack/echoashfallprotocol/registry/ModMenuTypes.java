package com.knoxhack.echoashfallprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.menu.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {
    public static final Object MENU_TYPES =
            EchoBackendRegistryBridge.create(Registries.MENU, EchoAshfallProtocol.MODID);

    public static final EchoBackendRegistryEntry<MenuType<ResearchLabMenu>> RESEARCH_LAB =
            registerMenu("research_lab", ResearchLabMenu::new);

    public static final EchoBackendRegistryEntry<MenuType<HandRecyclerMenu>> HAND_RECYCLER =
            registerMenu("hand_recycler", HandRecyclerMenu::new);

    public static final EchoBackendRegistryEntry<MenuType<ThermalBurnerMenu>> THERMAL_BURNER =
            registerMenu("thermal_burner", ThermalBurnerMenu::new);

    public static final EchoBackendRegistryEntry<MenuType<WaterPurifierMenu>> WATER_PURIFIER =
            registerMenu("water_purifier", WaterPurifierMenu::new);

    public static final EchoBackendRegistryEntry<MenuType<MicroGeneratorMenu>> MICRO_GENERATOR =
            registerMenu("micro_generator", MicroGeneratorMenu::new);

    public static final EchoBackendRegistryEntry<MenuType<FilterWorkbenchMenu>> FILTER_WORKBENCH =
            registerMenu("filter_workbench", FilterWorkbenchMenu::new);

    public static final EchoBackendRegistryEntry<MenuType<ScrapPressMenu>> SCRAP_PRESS =
            registerMenu("scrap_press", ScrapPressMenu::new);

    public static final EchoBackendRegistryEntry<MenuType<MachineStatusMenu>> MACHINE_STATUS =
            registerMenu("machine_status", MachineStatusMenu::new);

    // === TIER 2.5 POWER GENERATION ===
    public static final EchoBackendRegistryEntry<MenuType<ThermalArrayMenu>> THERMAL_ARRAY =
            registerMenu("thermal_array", ThermalArrayMenu::new);

    // === GEO-EXTRACTOR MACHINES ===
    public static final EchoBackendRegistryEntry<MenuType<com.knoxhack.echoashfallprotocol.block.menu.OreGrinderMenu>> ORE_GRINDER =
            registerMenu("ore_grinder", com.knoxhack.echoashfallprotocol.block.menu.OreGrinderMenu::new);

    public static final EchoBackendRegistryEntry<MenuType<com.knoxhack.echoashfallprotocol.block.menu.IsotopeRefinerMenu>> ISOTOPE_REFINER =
            registerMenu("isotope_refiner", com.knoxhack.echoashfallprotocol.block.menu.IsotopeRefinerMenu::new);

    public static final EchoBackendRegistryEntry<MenuType<com.knoxhack.echoashfallprotocol.block.menu.CrystallineSynthesizerMenu>> CRYSTALLINE_SYNTHESIZER =
            registerMenu("crystalline_synthesizer",
                    com.knoxhack.echoashfallprotocol.block.menu.CrystallineSynthesizerMenu::new);

    // === ENDGAME MACHINES ===
    public static final EchoBackendRegistryEntry<MenuType<com.knoxhack.echoashfallprotocol.block.menu.DeepCoreMinerMenu>> DEEP_CORE_MINER =
            registerMenu("deep_core_miner", com.knoxhack.echoashfallprotocol.block.menu.DeepCoreMinerMenu::new);

    public static final EchoBackendRegistryEntry<MenuType<com.knoxhack.echoashfallprotocol.block.menu.RadiationCleanserMenu>> RADIATION_CLEANSER =
            registerMenu("radiation_cleanser", com.knoxhack.echoashfallprotocol.block.menu.RadiationCleanserMenu::new);

    // Companion and Scout Drone controls are intentionally routed through the ECHO terminal.

    private static <T extends net.minecraft.world.inventory.AbstractContainerMenu>
            EchoBackendRegistryEntry<MenuType<T>> registerMenu(String id,
                    com.knoxhack.echo.adaptercore.EchoMenuFactory<T> factory) {
        return EchoBackendRegistryBridge.register(MENU_TYPES, id, () -> EchoBackendMenuBridge.extendedMenuType(factory));
    }
}

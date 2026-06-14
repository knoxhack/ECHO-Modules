package com.knoxhack.echo.equipmentcore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.equipmentcore.api.EquipmentService;
import com.knoxhack.echo.equipmentcore.api.EquipmentSlot;
import com.knoxhack.echo.equipmentcore.event.EquipmentEvents;
import com.knoxhack.echo.equipmentcore.integration.EquipmentHazardResistanceProvider;
import com.knoxhack.echo.equipmentcore.registry.ModCreativeTabs;
import com.knoxhack.echo.equipmentcore.registry.ModDataComponents;
import com.knoxhack.echo.equipmentcore.registry.ModItems;
import com.knoxhack.echo.hazardcore.api.HazardService;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(EchoEquipmentCore.MODID)
public class EchoEquipmentCore {
    public static final String MODID = "echoequipmentcore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echoarmory",
            "echocombatcore",
            "echotoolcore",
            "echohazardcore"
        );
    public static final List<String> PROVIDES = List.of(
            "equipment.slots",
            "equipment.durability",
            "equipment.upgrades",
            "equipment.loadout_validation"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "gear_slot_contract",
            "durability_rules",
            "upgrade_modifiers",
            "loadout_validation"
        );

    private static final String PLAYER_TICK_POST_EVENT =
            "net.neoforged.neoforge.event.tick.PlayerTickEvent$Post";
    private static final String LIVING_DAMAGE_PRE_EVENT =
            "net.neoforged.neoforge.event.entity.living.LivingDamageEvent$Pre";

    private EchoEquipmentCore() {
        this(null);
    }

    public EchoEquipmentCore(IEventBus modEventBus) {
        bootstrap(modEventBus);
    }

    public void bootstrap(IEventBus modEventBus) {
        ModDataComponents.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        if (modEventBus != null) {
            modEventBus.addListener(FMLCommonSetupEvent.class, this::onCommonSetup);
            EchoBackendLifecycleBridge.registerGameEventHandler(PLAYER_TICK_POST_EVENT, EquipmentEvents::onPlayerTick);
            EchoBackendLifecycleBridge.registerGameEventHandler(LIVING_DAMAGE_PRE_EVENT, EquipmentEvents::onLivingDamage);
            EchoBackendLifecycleBridge.registerOptionalGameTests(modEventBus,
                    "com.knoxhack.echo.equipmentcore.registry.ModGameTests");
        } else {
            // Native/early bootstrap path: defer item access until registration is complete.
            onCommonSetup(null);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        EquipmentService service = EquipmentService.getInstance();
        service.registerDefaultItem(EquipmentSlot.SUIT_FRAME.id(), ModItems.SHOAL_SUIT.get());
        service.registerDefaultItem(EquipmentSlot.REBREATHER.id(), ModItems.REBREATHER.get());
        service.registerDefaultItem(EquipmentSlot.LIGHT_SENSOR.id(), ModItems.LIGHT_SENSOR.get());
        service.registerDefaultItem(EquipmentSlot.TOOL_MOUNT.id(), ModItems.DIVE_TOOL.get());

        HazardService.find().registerResistanceProvider(EquipmentHazardResistanceProvider.INSTANCE);
    }

    public String moduleId() {
        return MODID;
    }

    public List<String> provides() {
        return PROVIDES;
    }
}

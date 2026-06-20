package com.knoxhack.echoorbitalremnants;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echocore.client.model.EchoMobFamilyRenderer;
import com.knoxhack.echoorbitalremnants.entity.EchoDefenseDroneEntity;
import com.knoxhack.echoorbitalremnants.entity.EmergencyRocketEntity;
import com.knoxhack.echoorbitalremnants.suit.SuitEvents;
import com.knoxhack.echoorbitalremnants.suit.SuitState;
import com.knoxhack.echoorbitalremnants.client.EchoDefenseDroneRenderer;
import com.knoxhack.echoorbitalremnants.client.EchoTerminalScreen;
import com.knoxhack.echoorbitalremnants.client.EmergencyRocketModel;
import com.knoxhack.echoorbitalremnants.client.EmergencyRocketRenderer;
import com.knoxhack.echoorbitalremnants.client.OrbitalFactionDialogueScreen;
import com.knoxhack.echoorbitalremnants.client.OrbitalMachineScreen;
import com.knoxhack.echoorbitalremnants.integration.OrbitalTerminalIntegration;
import com.knoxhack.echoorbitalremnants.network.OpenEchoTerminalPayload;
import com.knoxhack.echoorbitalremnants.network.OrbitalEventVisualPayload;
import com.knoxhack.echoorbitalremnants.network.OrbitalFactionDialogueOpenPayload;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
import com.knoxhack.echoorbitalremnants.registry.ModMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

public class EchoOrbitalRemnantsClient {
    private static final Identifier ORBITAL_HUD = Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "orbital_hud");
    private static final String REGISTER_CLIENT_PAYLOAD_HANDLERS_EVENT =
            "net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent";
    private static final String BASE_VISUAL_CONTROLLER = "com.knoxhack.echoashfallprotocol.client.EnvironmentalVisualController";
    private static final String BASE_VISUAL_PULSE_METHOD = "triggerOrbitalPulse";
    private static final String BASE_CONFIG_CLASS = "com.knoxhack.echoashfallprotocol.Config";
    private static final String BASE_ORBITAL_VISUALS_FLAG = "ORBITAL_EVENT_VISUALS";
    private static int eventVisualTicks = 0;
    private static String eventVisualName = "";

    public EchoOrbitalRemnantsClient() {
        this(null);
    }

    public EchoOrbitalRemnantsClient(Object modEventBus) {
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            OrbitalTerminalIntegration.register();
        }
        if (EchoRuntimeModules.isLoaded("echorendercore")) {
            registerRenderCoreStaticSurfaces();
        }
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoOrbitalRemnantsClient::registerGuiLayers);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoOrbitalRemnantsClient::registerMenuScreens);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoOrbitalRemnantsClient::registerLayerDefinitions);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, REGISTER_CLIENT_PAYLOAD_HANDLERS_EVENT,
                EchoOrbitalRemnantsClient::registerClientPayloadHandlers);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoOrbitalRemnantsClient::registerEntityRenderers);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoOrbitalRemnantsClient::onClientTick);
    }

    private static void registerRenderCoreStaticSurfaces() {
        try {
            Class.forName("com.knoxhack.echoorbitalremnants.integration.OrbitalRenderCoreClientIntegration")
                    .getMethod("registerStaticSurfaces")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoOrbitalRemnants.LOGGER.warn("ECHO Orbital Remnants RenderCore static surface integration unavailable.", exception);
        }
    }

    static void registerGuiLayers(Object event) {
        EchoBackendClientBridge.registerGuiLayerAboveAir(event, ORBITAL_HUD, EchoOrbitalRemnantsClient::renderOrbitalHud);
    }

    static void registerMenuScreens(Object event) {
        EchoBackendClientBridge.registerMenuScreen(event, ModMenus.ORBITAL_MACHINE.get(), OrbitalMachineScreen.class);
    }

    static void registerLayerDefinitions(Object event) {
        EchoBackendClientBridge.registerLayerDefinition(event, EmergencyRocketModel.LAYER_LOCATION, EmergencyRocketModel::createBodyLayer);
    }

    static void registerClientPayloadHandlers(Object event) {
        EchoBackendClientBridge.registerClientPayloadHandler(event, OpenEchoTerminalPayload.TYPE, (payload, context) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof EchoTerminalScreen screen) {
                screen.updateSnapshot(payload.snapshot());
            } else {
                minecraft.setScreen(new EchoTerminalScreen(payload.snapshot()));
            }
        });
        EchoBackendClientBridge.registerClientPayloadHandler(event, OrbitalEventVisualPayload.TYPE, (payload, context) -> {
            eventVisualTicks = 120;
            eventVisualName = payload.eventName().replace('_', ' ');
            triggerBaseOrbitalPulse(payload.overlayColor(), payload.particleColor(), payload.intensity(), payload.seed());
        });
        EchoBackendClientBridge.registerClientPayloadHandler(event, OrbitalFactionDialogueOpenPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().setScreen(new OrbitalFactionDialogueScreen(payload)));
    }

    static void onClientTick(Object event) {
        if (eventVisualTicks <= 0) {
            return;
        }
        eventVisualTicks--;
    }

    static void registerEntityRenderers(Object event) {
        if (EchoRuntimeModules.isLoaded("echorendercore") && registerRenderCoreEntityRenderers(event)) {
            return;
        }
        EntityRendererProvider<EmergencyRocketEntity> rocketRenderer = EmergencyRocketRenderer::new;
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.EMERGENCY_ROCKET_VEHICLE.get(), rocketRenderer);
        registerFallbackEntityRenderers(event);
    }

    public static void registerFallbackEntityRenderers(Object event) {
        EntityRendererProvider<EchoDefenseDroneEntity> droneRenderer = EchoDefenseDroneRenderer::new;
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ECHO_DEFENSE_DRONE.get(), droneRenderer);
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.VACUUM_WRAITH.get(),
                renderer("vacuum_wraith", EchoMobFamily.WRAITH, 1.15F, 0.25F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.CORRUPTED_DOCKING_AI.get(),
                renderer("corrupted_docking_ai", EchoMobFamily.DRONE, 1.35F, 0.44F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.BROKEN_ASTRONAUT.get(),
                renderer("broken_astronaut", EchoMobFamily.STATION_SUIT, 1.0F, 0.52F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.NEXUS_HUSK.get(),
                renderer("nexus_husk", EchoMobFamily.HUMANOID, 1.05F, 0.56F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.LUNAR_NEXUS_HUSK.get(),
                renderer("lunar_nexus_husk", EchoMobFamily.STATION_SUIT, 1.22F, 0.68F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ABANDONED_CAPTAIN.get(),
                renderer("abandoned_captain", EchoMobFamily.STATION_SUIT, 1.18F, 0.72F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ECHO_ZERO.get(),
                renderer("echo_zero", EchoMobFamily.HEAVY_BOSS, 1.35F, 0.9F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.EUROPA_CRYO_WARDEN.get(),
                renderer("europa_cryo_warden", EchoMobFamily.DRONE, 1.45F, 0.58F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.SATURN_RELAY_SENTINEL.get(),
                renderer("saturn_relay_sentinel", EchoMobFamily.DRONE, 1.55F, 0.6F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.TITAN_METHANE_STALKER.get(),
                renderer("titan_methane_stalker", EchoMobFamily.HUMANOID, 1.18F, 0.66F));
        EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ORBITAL_FACTION_NPC.get(),
                renderer("orbital_faction_npc", EchoMobFamily.SURVIVOR_NPC, 1.0F, 0.5F));
    }

    private static boolean registerRenderCoreEntityRenderers(Object event) {
        try {
            Class.forName("com.knoxhack.echoorbitalremnants.integration.OrbitalRenderCoreClientIntegration")
                    .getMethod("registerEntityRenderers", Object.class)
                    .invoke(null, event);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoOrbitalRemnants.LOGGER.warn("ECHO Orbital Remnants RenderCore entity renderer integration unavailable; using generated family fallback renderers.", exception);
            return false;
        }
    }

    private static <T extends Mob> EntityRendererProvider<T> renderer(String entityName, EchoMobFamily family,
            float scale, float shadow) {
        return context -> new EchoMobFamilyRenderer<>(context, EchoOrbitalRemnants.MODID, entityName, family, scale, shadow);
    }

    private static void triggerBaseOrbitalPulse(int overlayColor, int particleColor, float intensity, long seed) {
        if (!baseOrbitalVisualsEnabled()) {
            return;
        }
        try {
            Class<?> controller = Class.forName(BASE_VISUAL_CONTROLLER);
            controller.getMethod(BASE_VISUAL_PULSE_METHOD, int.class, int.class, float.class, long.class)
                    .invoke(null, overlayColor, particleColor, intensity, seed);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // The base mod owns the cohesive overlay. If it is unavailable, keep the Orbital HUD label only.
        }
    }

    private static boolean baseOrbitalVisualsEnabled() {
        try {
            Object configValue = Class.forName(BASE_CONFIG_CLASS).getField(BASE_ORBITAL_VISUALS_FLAG).get(null);
            Object enabled = configValue.getClass().getMethod("get").invoke(configValue);
            return !(enabled instanceof Boolean value) || value;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return true;
        }
    }

    private static void renderOrbitalHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !SuitEvents.isOrbitalExposure(minecraft.player)) {
            return;
        }

        SuitState suit = SuitState.get(minecraft.player);
        Font font = minecraft.font;
        int x = 8;
        int y = 8;
        int width = 154;
        int height = 74;
        graphics.fill(x - 4, y - 4, x + width, y + height, 0xAA061014);
        graphics.fill(x - 4, y - 4, x + width, y - 2, 0xCC56D6FF);
        graphics.text(font, Component.literal("ECHO-7 ORBITAL STATUS"), x, y, 0x66E8FF, true);
        graphics.text(font, Component.literal("OXYGEN: " + suit.oxygen() + "%"), x, y + 11, colorFor(suit.oxygen()), true);
        graphics.text(font, Component.literal("PRESSURE: " + pressureLabel(suit)), x, y + 22, colorFor(suit.pressure()), true);
        graphics.text(font, Component.literal("RADIATION: " + radiationLabel(suit.radiation())), x, y + 33, radiationColor(suit.radiation()), true);
        graphics.text(font, Component.literal("GRAVITY: " + String.format(java.util.Locale.ROOT, "%.2fG", suit.gravity())), x, y + 44, 0xD9F7FF, true);
        graphics.text(font, Component.literal("STATION POWER: " + suit.stationPower() + "%"), x, y + 55, 0xD9F7FF, true);
        if (eventVisualTicks > 0) {
            graphics.text(font, Component.literal(eventVisualName), x, y + 66, 0xFFE09CFF, true);
        }
    }

    private static String pressureLabel(SuitState suit) {
        if (!suit.helmetSealSecure()) {
            return "COMPROMISED";
        }
        if (suit.suitLeak()) {
            return "LEAK";
        }
        return "STABLE";
    }

    private static String radiationLabel(int radiation) {
        if (radiation >= 75) {
            return "EXTREME";
        }
        if (radiation >= 45) {
            return "HIGH";
        }
        return "ELEVATED";
    }

    private static int colorFor(int value) {
        if (value <= 20) {
            return 0xFF6B6B;
        }
        if (value <= 45) {
            return 0xFFD166;
        }
        return 0xA8F7C5;
    }

    private static int radiationColor(int radiation) {
        if (radiation >= 75) {
            return 0xE099FF;
        }
        if (radiation >= 45) {
            return 0xFFD166;
        }
        return 0xD9F7FF;
    }
}

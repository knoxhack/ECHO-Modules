package com.knoxhack.echoashfallprotocol.integration;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.client.hud.HudState;
import com.knoxhack.echoashfallprotocol.echo.MissionRegistry;
import com.knoxhack.echoashfallprotocol.echo.MissionUxSummary;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventStatus;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventType;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class AshfallPresenceIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private AshfallPresenceIntegration() {
    }

    public static void registerClient() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        try {
            PresenceApi.registerProvider(new AshfallProvider());
            EchoAshfallProtocol.LOGGER.info("ECHO Ashfall registered Presence Link provider.");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            REGISTERED.set(false);
            EchoAshfallProtocol.LOGGER.debug("ECHO Presence Link API unavailable for Ashfall provider.", exception);
        }
    }

    private static final class AshfallProvider implements InvocationHandler {
        private static final Identifier ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "ashfall");

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "AshfallPresenceProvider[" + ID + "]";
                    case "hashCode" -> ID.hashCode();
                    case "equals" -> proxy == (args == null ? null : args[0]);
                    default -> null;
                };
            }
            return switch (method.getName()) {
                case "id" -> ID;
                case "snapshot" -> snapshot(args == null || args.length == 0 ? null : args[0]);
                case "order" -> 50;
                default -> null;
            };
        }

        private Object snapshot(Object context) {
            Player player = PresenceApi.player(context);
            if (player == null) {
                return null;
            }
            long start = PresenceApi.sessionStartEpochSeconds(context);

            HudState.BossTarget boss = HudState.getBossTarget();
            if (boss != null && boss.active() && boss.isLiveBoss()) {
                String title = clean(boss.title(), "Ashfall Warfront");
                String state = clean(boss.subtitle(), "Boss signal active")
                        + " | " + Math.round(Math.max(0.0F, boss.healthPercent()) * 100.0F) + "%";
                return PresenceApi.snapshot(ID, 110, title, state, bossAsset(boss), title,
                        "echo_ashfall", "Ashfall Protocol", start, List.of(), false);
            }

            if (HudState.isNexusCampaignAwakened() && !HudState.isNexusFinaleComplete()) {
                int resolved = Math.max(0, HudState.getNexusRelaysResolved());
                int total = Math.max(8, HudState.getNexusRelaysScanned());
                String instability = HudState.getNexusInstability() > 0
                        ? "Instability " + HudState.getNexusInstability() + "%"
                        : "Route unstable";
                return PresenceApi.snapshot(ID, 100, "Nexus Campaign",
                        "Guardian Nodes " + resolved + "/" + total + " | " + instability,
                        "nexus_core", "Nexus Campaign", "echo_ashfall", "Ashfall Protocol",
                        start, List.of(), false);
            }

            EnvironmentalEventStatus event = HudState.getEnvironmentalEventStatus(player.level().getGameTime());
            if (event.active()) {
                return PresenceApi.snapshot(ID, 90, "Ashfall Protocol | " + event.label(),
                        event.shortStatusText(), eventAsset(event.type()), event.label(),
                        "echo_ashfall", "Ashfall Protocol", start, List.of(), false);
            }

            SurvivalData survival = player.getData(ModAttachments.SURVIVAL_DATA.get());
            Object hazard = survivalSnapshot(survival, start);
            if (hazard != null) {
                return hazard;
            }

            QuestData quest = QuestData.get(player);
            MissionUxSummary summary = MissionUxSummary.current(player, quest);
            if (!summary.missionId().isBlank()) {
                String route = summary.routeHint().isBlank()
                        ? "P" + (quest.getCurrentPhase() + 1) + " " + phaseTitle(quest.getCurrentPhase())
                        : summary.routeHint().split("/", 2)[0].trim();
                return PresenceApi.snapshot(ID, 70, "Ashfall Protocol | " + route,
                        clean(summary.nextStep(), summary.shortTitle()), "echo_ashfall", "Ashfall Protocol",
                        "echo_terminal", summary.shortTitle(), start, List.of(), false);
            }

            if (!quest.isTerminalOnline() || quest.getTerminalHealth() <= 25 || quest.getDroneHealth() <= 25) {
                String terminal = quest.isTerminalOnline() ? "Terminal " + quest.getTerminalHealth() + "%" : "Terminal offline";
                String drone = quest.isDroneUnlocked() ? "Drone " + quest.getDroneHealth() + "%" : "Drone locked";
                return PresenceApi.snapshot(ID, 45, "Ashfall Protocol",
                        terminal + " | " + drone, "echo_terminal", "ECHO Terminal",
                        "echo_ashfall", "Ashfall Protocol", start, List.of(), false);
            }

            return null;
        }

        private static Object survivalSnapshot(SurvivalData survival, long start) {
            if (survival == null || survival.isSafeZone()) {
                return null;
            }
            if (survival.isRadiationStorm() || survival.isRadiationZone() || survival.getRadiationLevel() >= 40.0F) {
                return PresenceApi.snapshot(ID, 78, "Ashfall Protocol | Radiation Hazard",
                        "Radiation " + Math.round(survival.getRadiationLevel()) + "% | Filter "
                                + Math.round(survival.getFilterPercent() * 100.0F) + "%",
                        "hazard_radiation", "Radiation Hazard", "echo_ashfall", "Ashfall Protocol",
                        start, List.of(), false);
            }
            if (survival.isToxicAirActive() || "TOXIC".equalsIgnoreCase(survival.getPrimaryHazard())) {
                return PresenceApi.snapshot(ID, 76, "Ashfall Protocol | Toxic Air",
                        "Filters active | Hydration " + survival.getHydration() + "%",
                        "hazard_toxic", "Toxic Hazard", "echo_ashfall", "Ashfall Protocol",
                        start, List.of(), false);
            }
            if (survival.isCryoZone()) {
                return PresenceApi.snapshot(ID, 76, "Ashfall Protocol | Cryo Front",
                        "Thermal exposure rising", "hazard_cold", "Cold Hazard",
                        "echo_ashfall", "Ashfall Protocol", start, List.of(), false);
            }
            if (survival.isNexusAnomaly() || survival.isAcidContact()) {
                return PresenceApi.snapshot(ID, 75, "Ashfall Protocol | Field Mutation",
                        clean(survival.getHazardReason(), "Anomaly pressure rising"),
                        "hazard_mutation", "Mutation Hazard", "echo_ashfall", "Ashfall Protocol",
                        start, List.of(), false);
            }
            return null;
        }

        private static String eventAsset(EnvironmentalEventType type) {
            return switch (type == null ? EnvironmentalEventType.NONE : type) {
                case RADIATION_STORM -> "hazard_radiation";
                case TOXIC_STORM, ASH_STORM -> "hazard_toxic";
                case CRYO_FRONT -> "hazard_cold";
                case NEXUS_SURGE -> "nexus_core";
                case BLACKOUT -> "echo_terminal";
                default -> "echo_ashfall";
            };
        }

        private static String bossAsset(HudState.BossTarget boss) {
            String signal = (boss.bossId() + " " + boss.title() + " " + boss.category()).toLowerCase(Locale.ROOT);
            if (signal.contains("nexus") || signal.contains("scar")) {
                return "nexus_core";
            }
            if (signal.contains("toxic") || signal.contains("hive")) {
                return "hazard_toxic";
            }
            if (signal.contains("cryo")) {
                return "hazard_cold";
            }
            if (signal.contains("radiation")) {
                return "hazard_radiation";
            }
            return "echo_ashfall";
        }

        private static String phaseTitle(int phase) {
            try {
                return MissionRegistry.getPhaseTitle(phase);
            } catch (RuntimeException exception) {
                return "Signal Contact";
            }
        }

        private static String clean(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value.strip();
        }
    }

    private static final class PresenceApi {
        private static final String PROVIDER = "com.knoxhack.echopresencelink.api.EchoPresenceProvider";
        private static final String REGISTRY = "com.knoxhack.echopresencelink.api.EchoPresenceRegistry";
        private static final String SNAPSHOT = "com.knoxhack.echopresencelink.api.EchoPresenceSnapshot";

        private PresenceApi() {
        }

        private static void registerProvider(InvocationHandler handler) throws ReflectiveOperationException {
            Class<?> providerType = Class.forName(PROVIDER);
            Object provider = Proxy.newProxyInstance(
                    providerType.getClassLoader(),
                    new Class<?>[] { providerType },
                    handler);
            Class.forName(REGISTRY).getMethod("register", providerType).invoke(null, provider);
        }

        private static Player player(Object context) {
            if (context == null) {
                return null;
            }
            try {
                Object value = context.getClass().getMethod("player").invoke(context);
                return value instanceof Player player ? player : null;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                return null;
            }
        }

        private static long sessionStartEpochSeconds(Object context) {
            if (context == null) {
                return 0L;
            }
            try {
                Object value = context.getClass().getMethod("sessionStartEpochSeconds").invoke(context);
                return value instanceof Number number ? number.longValue() : 0L;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                return 0L;
            }
        }

        private static Object snapshot(
                Identifier id,
                int priority,
                String details,
                String state,
                String largeImageKey,
                String largeImageText,
                String smallImageKey,
                String smallImageText,
                long startTimestamp,
                List<?> buttons,
                boolean clear
        ) {
            try {
                Constructor<?> constructor = Class.forName(SNAPSHOT).getConstructor(
                        Identifier.class,
                        int.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        long.class,
                        List.class,
                        boolean.class);
                return constructor.newInstance(
                        id,
                        priority,
                        details,
                        state,
                        largeImageKey,
                        largeImageText,
                        smallImageKey,
                        smallImageText,
                        startTimestamp,
                        buttons,
                        clear);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                return null;
            }
        }
    }
}

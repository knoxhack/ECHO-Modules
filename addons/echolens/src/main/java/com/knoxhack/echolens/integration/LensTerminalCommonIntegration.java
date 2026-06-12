package com.knoxhack.echolens.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.LensProviderDiagnostic;
import com.knoxhack.echolens.config.LensConfig;
import com.knoxhack.echolens.network.ModNetwork;
import com.knoxhack.echolens.platform.LensModuleAccess;
import com.knoxhack.echolens.registry.LensProviderHealth;
import com.knoxhack.echolens.registry.LensProviderRegistry;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class LensTerminalCommonIntegration {
    private static boolean registered;

    private LensTerminalCommonIntegration() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        try {
            registerAddonInfo();
            registerArchiveEntry();
            registered = true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            registered = true;
            EchoLens.LOGGER.warn("ECHO: Lens terminal integration is unavailable; Core chapter metadata remains active.",
                    exception);
        }
    }

    private static void registerAddonInfo() throws ReflectiveOperationException {
        Class<?> providerClass = Class.forName("com.knoxhack.echoterminal.api.TerminalAddonInfoProvider");
        Class<?> registryClass = Class.forName("com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry");
        Object provider = Proxy.newProxyInstance(providerClass.getClassLoader(), new Class<?>[]{providerClass},
                new AddonInfoHandler());
        registryClass.getMethod("register", providerClass).invoke(null, provider);
    }

    private static void registerArchiveEntry() throws ReflectiveOperationException {
        Class<?> entryClass = Class.forName("com.knoxhack.echoterminal.api.TerminalArchiveEntry");
        Class<?> registryClass = Class.forName("com.knoxhack.echoterminal.api.TerminalArchiveRegistry");
        Constructor<?> constructor = entryClass.getConstructor(Identifier.class, String.class, String.class,
                String.class, List.class, boolean.class);
        Object entry = constructor.newInstance(
                EchoLens.id("archive/lens_overview"),
                "Systems",
                "ECHO: Lens",
                "ONLINE",
                List.of(
                        "Lens is a non-invasive scanner overlay for modern ECHO modpacks.",
                        "It favors compact, icon-led reads by default and deeper categorized diagnostics on demand.",
                        "Optional Terminal and Index hooks are soft integrations and no-op safely when unavailable."),
                false);
        registryClass.getMethod("register", entryClass).invoke(null, entry);
    }

    private static Object buildAddonInfo() throws ReflectiveOperationException {
        Class<?> infoClass = Class.forName("com.knoxhack.echoterminal.api.TerminalAddonInfo");
        Class<?> metricClass = Class.forName("com.knoxhack.echoterminal.api.TerminalAddonMetric");
        Class<?> sectionClass = Class.forName("com.knoxhack.echoterminal.api.TerminalAddonSection");
        Class<?> linkClass = Class.forName("com.knoxhack.echoterminal.api.TerminalAddonLink");
        Class<?> guideClass = Class.forName("com.knoxhack.echoterminal.api.TerminalAddonGuide");

        Constructor<?> metric = metricClass.getConstructor(String.class, String.class, String.class, int.class);
        Object providers = metric.newInstance("Providers", Integer.toString(LensProviderRegistry.count()),
                "Registered structured Lens providers", 0x66D9EF);
        long enabledProviders = LensProviderRegistry.diagnostics().stream()
                .filter(LensProviderDiagnostic::enabled)
                .count();
        Object enabled = metric.newInstance("Enabled", Long.toString(enabledProviders),
                "Providers whose categories are visible in current config", 0xA6E22E);
        Object privacy = metric.newInstance("Privacy", "Public first",
                "Inventory contents remain hidden by default", 0xA6E22E);
        Object serverScan = metric.newInstance("Server Scan",
                LensConfig.bool(LensConfig.SERVER_DEEP_SCAN_ENABLED, true) ? "Enabled" : "Disabled",
                LensProviderRegistry.serverProviders().size() + " server providers / packets "
                        + (ModNetwork.registered() ? "registered" : "offline"),
                LensConfig.bool(LensConfig.SERVER_DEEP_SCAN_ENABLED, true) ? 0x66D9EF : 0x888888);
        long failingProviders = LensProviderRegistry.health().stream()
                .filter(health -> health.failureCount() > 0)
                .count();
        Object providerHealth = metric.newInstance("Provider Health",
                failingProviders == 0 ? "Nominal" : failingProviders + " warning(s)",
                "Internal provider health tracking and category visibility are active",
                failingProviders == 0 ? 0xA6E22E : 0xFFD166);
        Object scanModes = metric.newInstance("Scan Modes",
                Integer.toString(EchoCoreServices.lensService().scanTypes().size()),
                "Compact, expanded, Deep Scan, and addon-declared scan types",
                0x66D9EF);
        boolean runtimeGuardLoaded = LensModuleAccess.isLoaded("echoruntimeguard");
        Object runtimeGuard = metric.newInstance("RuntimeGuard",
                runtimeGuardLoaded ? "Linked" : "Fallback",
                "Deep Scan budgets are enforced when RuntimeGuard is present",
                runtimeGuardLoaded ? 0xA6E22E : 0x888888);

        Constructor<?> section = sectionClass.getConstructor(String.class, List.class);
        Object usage = section.newInstance("How to use", List.of(
                "Look at a block, fluid, or entity to show the compact HUD.",
                "Hold Shift for expanded stats such as harvest, light, redstone, and health.",
                "Hold the Deep Scan key for server-verified public ECHO context."));
        Object safety = section.newInstance("Pack safety", List.of(
                "Compact and expanded scans stay local and instant.",
                "Deep Scan requests public server-verified rows through NetCore.",
                "Protected inventories report only safe public state unless config allows more."));
        Object diagnostics = section.newInstance("Provider diagnostics", providerDiagnosticLines());

        Constructor<?> link = linkClass.getConstructor(Identifier.class, String.class, String.class, int.class);
        Object archive = link.newInstance(Identifier.fromNamespaceAndPath("echoterminal", "archive"),
                "Open Archive", "Review Lens scanner guidance", 0x66D9EF);

        Object guide = guideClass.getMethod("optional", int.class, String.class, String.class, List.class)
                .invoke(null, 32, "Inspection",
                        "Use Lens when a block, machine, or entity state is unclear.",
                        List.of("Scan a stone block with the wrong tool.",
                                "Scan a hostile entity.",
                                "Deep Scan an ECHO machine if one is installed."));

        return infoClass.getConstructor(String.class, List.class, List.class, List.class, guideClass)
                .newInstance(
                        "Smart scanner HUD for contextual block, entity, fluid, machine, and progression diagnostics.",
                        List.of(providers, enabled, privacy, serverScan, providerHealth, scanModes, runtimeGuard),
                        List.of(usage, safety, diagnostics),
                        List.of(archive),
                        guide);
    }

    private static List<String> providerDiagnosticLines() {
        List<LensProviderDiagnostic> diagnostics = LensProviderRegistry.diagnostics();
        if (diagnostics.isEmpty()) {
            return List.of("No Lens providers are currently registered.");
        }
        return diagnostics.stream()
                .limit(8)
                .map(diagnostic -> {
                    LensProviderHealth health = LensProviderRegistry.health().stream()
                            .filter(candidate -> diagnostic.id().equals(candidate.id()))
                            .findFirst()
                            .orElse(new LensProviderHealth(diagnostic.id(), diagnostic.loaded(), diagnostic.enabled(),
                                    false, diagnostic.providerClass(), 0, ""));
                    return diagnostic.id()
                            + " | p" + diagnostic.priority()
                            + " | " + diagnostic.category()
                            + " | " + (health.serverSafe() ? "server-safe" : "local")
                            + " | " + (health.categoryEnabled() ? "visible" : "category hidden")
                            + " | failures " + health.failureCount();
                })
                .toList();
    }

    private static final class AddonInfoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "ECHO: Lens terminal addon info provider";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
                    default -> null;
                };
            }
            return switch (method.getName()) {
                case "chapterId" -> "lens";
                case "info" -> buildAddonInfo();
                default -> null;
            };
        }
    }
}

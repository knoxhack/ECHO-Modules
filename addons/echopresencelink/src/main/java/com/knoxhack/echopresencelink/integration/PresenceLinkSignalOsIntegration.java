package com.knoxhack.echopresencelink.integration;

import com.knoxhack.echopresencelink.EchoPresenceLink;
import com.knoxhack.echopresencelink.presence.PresenceLinkDiagnostics;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;

public final class PresenceLinkSignalOsIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private PresenceLinkSignalOsIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName("com.knoxhack.signalos.api.SignalOsApi");
            Class<?> providerInterface = Class.forName("com.knoxhack.signalos.api.TerminalDiagnosticProvider");
            Object provider = Proxy.newProxyInstance(
                    PresenceLinkSignalOsIntegration.class.getClassLoader(),
                    new Class<?>[] { providerInterface },
                    new SignalOsDiagnosticsHandler());
            apiClass.getMethod("registerDiagnostics", providerInterface).invoke(null, provider);
            EchoPresenceLink.LOGGER.info("ECHO Presence Link registered SignalOS diagnostics.");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            EchoPresenceLink.LOGGER.debug("SignalOS diagnostics are unavailable for Presence Link.", exception);
        }
    }

    private static final class SignalOsDiagnosticsHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "id" -> id("diagnostics");
                case "diagnostics" -> diagnostics();
                case "order" -> 30;
                case "providerStatus" -> providerStatus();
                case "toString" -> "ECHO Presence Link SignalOS diagnostics";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == (args == null ? null : args[0]);
                default -> method.getDefaultValue();
            };
        }

        private static List<Object> diagnostics() throws ReflectiveOperationException {
            PresenceLinkDiagnostics.Snapshot snapshot = PresenceLinkDiagnostics.snapshot();
            Object severity = severity(snapshot.lastFailure().isBlank() ? "INFO" : "WARNING");
            return List.of(
                    diagnostic("diagnostics/connection", "Presence IPC", snapshot.statusLine(), severity),
                    diagnostic("diagnostics/provider", "Current Provider",
                            snapshot.currentProviderId().isBlank() ? "No provider published yet." : snapshot.currentProviderId(),
                            severity("INFO")),
                    diagnostic("diagnostics/discord_response", "Last Discord Response",
                            snapshot.lastResponse().isBlank() ? "No Discord IPC response captured yet." : snapshot.lastResponse(),
                            severity("INFO")),
                    diagnostic("diagnostics/failure", "Last Failure",
                            snapshot.lastFailure().isBlank() ? "No recent Discord IPC failure." : snapshot.lastFailure(),
                            severity));
        }

        private static Object providerStatus() throws ReflectiveOperationException {
            PresenceLinkDiagnostics.Snapshot snapshot = PresenceLinkDiagnostics.snapshot();
            Class<?> statusClass = Class.forName("com.knoxhack.signalos.api.SignalOsProviderStatus");
            Class<?> severityClass = Class.forName("com.knoxhack.signalos.api.TerminalDiagnosticProvider$Severity");
            Constructor<?> constructor = statusClass.getConstructor(
                    Identifier.class, String.class, String.class, severityClass, String.class);
            return constructor.newInstance(id("diagnostics"), "Presence Link", snapshot.status(),
                    severity(snapshot.lastFailure().isBlank() ? "INFO" : "WARNING"), snapshot.detail());
        }

        private static Object diagnostic(String path, String title, String detail, Object severity)
                throws ReflectiveOperationException {
            Class<?> diagnosticClass = Class.forName("com.knoxhack.signalos.api.TerminalDiagnosticProvider$Diagnostic");
            Class<?> severityClass = Class.forName("com.knoxhack.signalos.api.TerminalDiagnosticProvider$Severity");
            Constructor<?> constructor = diagnosticClass.getConstructor(Identifier.class, String.class, String.class,
                    severityClass);
            return constructor.newInstance(id(path), title, detail, severity);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private static Object severity(String name) throws ClassNotFoundException {
            Class<? extends Enum> severityClass =
                    Class.forName("com.knoxhack.signalos.api.TerminalDiagnosticProvider$Severity")
                            .asSubclass(Enum.class);
            return Enum.valueOf(severityClass, name);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoPresenceLink.MODID, path);
    }
}

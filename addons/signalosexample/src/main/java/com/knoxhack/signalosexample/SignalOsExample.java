package com.knoxhack.signalosexample;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.signalos.api.SignalOsApi;
import com.knoxhack.signalos.api.SignalOsApp;
import com.knoxhack.signalos.api.SignalOsDataProvider;
import com.knoxhack.signalos.api.SignalOsDataRecord;
import com.knoxhack.signalos.api.SignalOsPeripheralProvider;
import com.knoxhack.signalos.api.SignalOsProviderStatus;
import com.knoxhack.signalos.api.TerminalArchiveRecord;
import com.knoxhack.signalos.api.TerminalChapter;
import com.knoxhack.signalos.api.TerminalDiagnosticProvider;
import com.knoxhack.signalos.api.TerminalMission;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class SignalOsExample {
    public static final String MODID = "signalosexample";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SignalOsExample(Object modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, SignalOsExample::registerSignalOsContent);
    }

    private static void registerSignalOsContent() {
        SignalOsApi.registerChapter(TerminalChapter.builder(id("java_ops"))
                .title("Java Ops")
                .section("system")
                .order(80)
                .accentColor(0x7AF7C1)
                .page("missions")
                .page("archives")
                .page("diagnostics")
                .build());

        SignalOsApi.registerMission(TerminalMission.builder(id("java_boot"))
                .chapter(id("java_ops").toString())
                .title("Boot the Java Link")
                .description("A mission registered entirely through the public SignalOS Java API.")
                .objective("Open a SignalOS terminal")
                .objective("Verify the diagnostics pane")
                .reward("minecraft:torch", 8)
                .build());

        SignalOsApi.registerArchive(TerminalArchiveRecord.builder(id("java_notes"))
                .chapter(id("java_ops").toString())
                .title("Java API Notes")
                .group("Developer")
                .status("OPEN")
                .line("SignalOS content can come from Java, datapack JSON, or the soft script bridge.")
                .line("The registry merge is reload-safe for JSON and script content.")
                .build());

        SignalOsApi.registerApp(SignalOsApp.builder(id("java_records"))
                .title("Example Records")
                .type("example_records")
                .summary("JSON-style record browser registered from Java.")
                .order(85)
                .accentColor(0x7AF7C1)
                .icon("minecraft:writable_book")
                .view("records")
                .recordSources(List.of("SignalOS Example"))
                .emptyText("NO EXAMPLE RECORDS")
                .build());

        SignalOsApi.registerDataProvider(new SignalOsDataProvider() {
            @Override
            public Identifier id() {
                return SignalOsExample.id("example_records");
            }

            @Override
            public List<SignalOsDataRecord> records(Player player) {
                return List.of(new SignalOsDataRecord(
                        SignalOsExample.id("records/java_provider"),
                        "Java Provider Record",
                        "record",
                        "SignalOS Example",
                        "This record is supplied by a Java SignalOsDataProvider and appears in Files, Data Vault, and custom record-view apps.",
                        10,
                        false));
            }

            @Override
            public SignalOsProviderStatus providerStatus(Player player) {
                return new SignalOsProviderStatus(id(), "Example Records", "ONLINE", TerminalDiagnosticProvider.Severity.INFO,
                        "1 Java record source");
            }

            @Override
            public int order() {
                return 60;
            }
        });

        SignalOsApi.registerComputerPeripheral(new SignalOsPeripheralProvider() {
            @Override
            public Identifier id() {
                return SignalOsExample.id("example_peripheral");
            }

            @Override
            public List<Peripheral> peripherals(Player player) {
                BlockPos pos = player == null ? BlockPos.ZERO : player.blockPosition();
                return List.of(new Peripheral(
                        SignalOsExample.id("peripherals/reference_beacon"),
                        "relay",
                        "Example Reference Beacon",
                        "ONLINE",
                        pos,
                        1));
            }

            @Override
            public SignalOsProviderStatus providerStatus(Player player) {
                return new SignalOsProviderStatus(id(), "Example Peripheral", "ONLINE",
                        TerminalDiagnosticProvider.Severity.INFO, "Adds one reference beacon row");
            }

            @Override
            public int order() {
                return 70;
            }
        });

        SignalOsApi.registerAppAction(id("java_records"), id("actions/ping"), (context, payload) ->
                LOGGER.info("SignalOS example app action on {} tier {} payload '{}'.",
                        context.networkId(), context.accessTier(), payload));

        SignalOsApi.registerDiagnostics(new TerminalDiagnosticProvider() {
            @Override
            public Identifier id() {
                return SignalOsExample.id("example_diagnostics");
            }

            @Override
            public List<Diagnostic> diagnostics(Player player) {
                return List.of(
                        new Diagnostic(SignalOsExample.id("example_diagnostics/java_api"), "Java API",
                                "Example provider online.", Severity.INFO),
                        new Diagnostic(SignalOsExample.id("example_diagnostics/player_link"), "Player Link",
                                player == null ? "No local player context." : "Operator context available.",
                                player == null ? Severity.WARNING : Severity.INFO));
            }

            @Override
            public int order() {
                return 50;
            }

            @Override
            public SignalOsProviderStatus providerStatus(Player player) {
                return new SignalOsProviderStatus(id(), "Example Diagnostics", "ONLINE", Severity.INFO,
                        "Java diagnostic provider registered");
            }
        });

        LOGGER.info("SignalOS example content registered.");
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}

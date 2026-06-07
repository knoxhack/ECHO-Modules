package com.knoxhack.signalos.api;

import com.knoxhack.signalos.content.SignalOsContentRegistry;
import com.knoxhack.signalos.service.SignalOsTerminalServices;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Public entry point for mods that want to publish content into SignalOS.
 */
public final class SignalOsApi {
    private SignalOsApi() {
    }

    public static void registerChapter(TerminalChapter chapter) {
        SignalOsContentRegistry.registerChapter(chapter);
    }

    public static void registerPage(TerminalPage page) {
        SignalOsContentRegistry.registerPage(page);
    }

    public static void registerMission(TerminalMission mission) {
        SignalOsContentRegistry.registerMission(mission);
    }

    public static void registerArchive(TerminalArchiveRecord record) {
        SignalOsContentRegistry.registerArchive(record);
    }

    public static void registerDiagnostics(TerminalDiagnosticProvider provider) {
        SignalOsContentRegistry.registerDiagnostics(provider);
    }

    public static void registerApp(SignalOsApp app) {
        SignalOsContentRegistry.registerApp(app);
    }

    public static void registerDataProvider(SignalOsDataProvider provider) {
        SignalOsContentRegistry.registerDataProvider(provider);
    }

    public static void registerComputerPeripheral(SignalOsPeripheralProvider provider) {
        SignalOsContentRegistry.registerPeripheralProvider(provider);
    }

    public static void registerNetProvider(SignalOsNetProvider provider) {
        SignalOsContentRegistry.registerNetProvider(provider);
    }

    public static void registerAppAction(Identifier appId, Identifier actionId, SignalOsAppActionHandler handler) {
        TerminalActionRegistry.registerAppAction(appId, actionId, handler);
    }

    public static void registerAppActionResult(Identifier appId, Identifier actionId,
            SignalOsAppActionResultHandler handler) {
        TerminalActionRegistry.registerAppActionResult(appId, actionId, handler);
    }

    public static SignalOsDriveData driveTemplate(Identifier templateId) {
        return SignalOsContentRegistry.driveTemplate(templateId);
    }

    public static Map<Identifier, SignalOsDriveData> driveTemplates() {
        return SignalOsContentRegistry.driveTemplateEntries();
    }

    public static List<SignalOsProviderStatus> providerStatuses(Player player) {
        return SignalOsContentRegistry.providerStatuses(player);
    }

    public static boolean hasActiveDrive(Player player) {
        return SignalOsTerminalServices.hasActiveDrive(player, true);
    }

    public static SignalOsDriveData activeDriveData(Player player) {
        return SignalOsTerminalServices.activeDriveData(player, true);
    }

    public static SignalOsDriveFileSystem activeDriveFileSystem(Player player) {
        return SignalOsTerminalServices.activeDriveFileSystem(player, true);
    }

    public static boolean updateActiveDrive(ServerPlayer player, UnaryOperator<SignalOsDriveData> updater) {
        return SignalOsTerminalServices.updateActiveDrive(player, updater);
    }

    public static SignalOsDriveWriteResult updateActiveDriveFileSystem(ServerPlayer player,
            Function<SignalOsDriveFileSystem, SignalOsDriveWriteResult> updater) {
        return SignalOsTerminalServices.updateActiveDriveFileSystem(player, updater);
    }

    public static Identifier id(String id) {
        return TerminalIds.parse(id, "SignalOS identifier");
    }
}

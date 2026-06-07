package com.knoxhack.signalos.service;

import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.api.SignalOsDataRecord;
import com.knoxhack.signalos.api.SignalOsDriveData;
import com.knoxhack.signalos.api.SignalOsDriveFileSystem;
import com.knoxhack.signalos.api.SignalOsDriveWriteResult;
import com.knoxhack.signalos.block.entity.SignalOsServerRackBlockEntity;
import com.knoxhack.signalos.content.SignalOsContentRegistry;
import com.knoxhack.signalos.integration.SignalOsMissionHooks;
import com.knoxhack.signalos.item.SignalOsDataDriveItem;
import com.knoxhack.signalos.menu.SignalOsServerRackMenu;
import com.knoxhack.signalos.network.SignalOsRackActionPacket;
import com.knoxhack.signalos.registry.ModBlocks;
import com.knoxhack.signalos.registry.ModDataComponents;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class SignalOsRackActions {
    public static final Identifier COPY_RECORD = id("copy_record");
    public static final Identifier REMOVE_RECORD = id("remove_record");
    public static final Identifier APPLY_TEMPLATE = id("apply_template");
    public static final Identifier CLEAR_DRIVE = id("clear_drive");
    public static final Identifier RENAME_DRIVE = id("rename_drive");
    private static final int MAX_LABEL = 80;
    private static final int MAX_PAYLOAD = 4096;

    private SignalOsRackActions() {
    }

    public static boolean handle(ServerPlayer player, SignalOsRackActionPacket packet) {
        if (player == null || packet == null || !(player.containerMenu instanceof SignalOsServerRackMenu menu)) {
            return false;
        }
        if (!menu.blockPos().equals(packet.pos()) || packet.slot() < 0
                || packet.slot() >= SignalOsServerRackBlockEntity.DRIVE_SLOTS || !menu.stillValid(player)) {
            status(player, "[SignalOS] Rack link expired.");
            return false;
        }
        SignalOsServerRackBlockEntity rack = menu.rack();
        if (rack == null) {
            return false;
        }
        ItemStack stack = rack.drives().getItem(packet.slot());
        if (stack.isEmpty() || !stack.is(ModBlocks.DATA_DRIVE.get())) {
            status(player, "[SignalOS] Select a data drive first.");
            return false;
        }
        SignalOsDriveData current = SignalOsDataDriveItem.data(stack);
        if (!current.isV2Supported()) {
            status(player, "[SignalOS] Legacy drives are read-only in SignalOS Drive API V2.");
            return false;
        }
        SignalOsDriveData next = current;
        Identifier action = packet.actionId();
        String payload = packet.payload() == null ? "" : packet.payload().strip();
        if (payload.length() > MAX_PAYLOAD) {
            status(player, "[SignalOS] Rack action payload is too large.");
            return false;
        }
        if (COPY_RECORD.equals(action)) {
            Identifier recordId = Identifier.tryParse(payload);
            if (recordId == null) {
                status(player, "[SignalOS] Network record id is invalid.");
                return false;
            }
            Optional<SignalOsDataRecord> record = SignalOsComputerNetworkService.networkRecords(player).stream()
                    .filter(candidate -> candidate.id().equals(recordId))
                    .findFirst();
            if (record.isEmpty()) {
                status(player, "[SignalOS] Network record unavailable.");
                return false;
            }
            SignalOsDriveWriteResult result = SignalOsDriveFileSystem.of(current).copyRecord(record.get(), "");
            if (!result.success()) {
                status(player, result.message());
                return false;
            }
            next = result.drive();
        } else if (REMOVE_RECORD.equals(action)) {
            Identifier recordId = Identifier.tryParse(payload);
            if (recordId == null) {
                status(player, "[SignalOS] Drive record id is invalid.");
                return false;
            }
            Optional<SignalOsDataRecord> driveRecord = current.records().stream()
                    .filter(candidate -> candidate.id().equals(recordId))
                    .findFirst();
            if (driveRecord.isEmpty()) {
                status(player, "[SignalOS] Drive record unavailable.");
                return false;
            }
            if (SignalOsDriveFileSystem.readOnly(driveRecord.get())) {
                status(player, "[SignalOS] Drive record is read-only.");
                return false;
            }
            next = current.withoutRecord(recordId);
        } else if (APPLY_TEMPLATE.equals(action)) {
            Identifier templateId = Identifier.tryParse(payload);
            if (templateId == null) {
                status(player, "[SignalOS] Drive template id is invalid.");
                return false;
            }
            SignalOsDriveData template = SignalOsContentRegistry.driveTemplate(templateId);
            if (template == null) {
                status(player, "[SignalOS] Drive template unavailable.");
                return false;
            }
            if (!template.isV2Supported()) {
                status(player, "[SignalOS] Drive template is not V2.");
                return false;
            }
            next = current.merge(template, SignalOsDriveData.MAX_PLAYER_RECORDS);
            if ("Blank Drive".equals(current.label()) && !"Blank Drive".equals(template.label())) {
                next = next.withLabel(template.label());
            }
        } else if (CLEAR_DRIVE.equals(action)) {
            next = current.clearRecords();
        } else if (RENAME_DRIVE.equals(action)) {
            next = current.withLabel(clamp(payload, MAX_LABEL));
        } else {
            return false;
        }
        stack.set(ModDataComponents.DRIVE_DATA.get(), next);
        rack.drives().setChanged();
        rack.setChanged();
        SignalOsComputerNetworkService.invalidateCache();
        if (COPY_RECORD.equals(action) || APPLY_TEMPLATE.equals(action)) {
            SignalOsMissionHooks.recordDriveRecordFlow(player, action.getPath());
        }
        status(player, "[SignalOS] Drive updated.");
        return true;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SignalOS.MODID, path);
    }

    private static String clamp(String value, int maxLength) {
        String safe = value == null || value.isBlank() ? "Data Drive" : value.strip();
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    private static void status(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message), true);
    }
}

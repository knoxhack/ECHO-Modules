package com.knoxhack.echobasegrid.client;

import com.knoxhack.echobasegrid.EchoBaseGrid;
import com.knoxhack.echobasegrid.api.ClaimPermission;
import com.knoxhack.echobasegrid.api.ClaimRole;
import com.knoxhack.echobasegrid.network.BaseGridSnapshotPacket;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoDataProvider;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BaseGridDataProviders {
    public static final EchoDataProvider PROVIDER = BaseGridDataProviders::resolve;
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private BaseGridDataProviders() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        EchoScreenRegistry.registerDataProvider("basegrid", PROVIDER);
        EchoScreenRegistry.registerDataProvider(EchoBaseGrid.id("basegrid"), PROVIDER);
        EchoScreenRegistry.registerStyleSheet(EchoBaseGrid.id("base_grid"));
    }

    private static Object resolve(EchoDataContext context, List<String> path) {
        maybeRequestInitialSnapshot();
        if (path == null || path.isEmpty()) {
            return summary();
        }
        String key = String.join(".", path);
        return switch (key) {
            case "summary", "status" -> summary();
            case "grid.cells", "cells" -> chunkRows();
            case "selected", "selected.claim" -> selected();
            case "members" -> memberRows();
            case "candidates", "onlinePlayers" -> candidateRows();
            case "roles" -> roleRows();
            case "permissions" -> permissionRows();
            case "status.message" -> snapshot().status();
            case "selected.key" -> BaseGridClientState.selectedKey();
            default -> resolveNested(key);
        };
    }

    private static void maybeRequestInitialSnapshot() {
        if (BaseGridClientState.needsInitialSnapshot()) {
            BaseGridActions.requestSnapshot();
        }
    }

    private static Object resolveNested(String key) {
        return nested(key, "summary.", summary())
                .orElseGet(() -> nested(key, "selected.", selected()).orElse(""));
    }

    private static java.util.Optional<Object> nested(String key, String prefix, Map<String, Object> map) {
        if (!key.startsWith(prefix)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(map.getOrDefault(key.substring(prefix.length()), ""));
    }

    private static BaseGridSnapshotPacket snapshot() {
        return BaseGridClientState.snapshot();
    }

    private static Map<String, Object> summary() {
        BaseGridSnapshotPacket packet = snapshot();
        int remaining = Math.max(0, packet.maxClaims() - packet.claimCount());
        String status = packet.status().isBlank() ? "Ready" : packet.status();
        return row(
                "title", "ECHO: Base Grid",
                "subtitle", "ScreenCore claim map and member permissions",
                "dimension", packet.dimension(),
                "center", packet.centerChunkX() + ", " + packet.centerChunkZ(),
                "selected", packet.selectedChunkX() + ", " + packet.selectedChunkZ(),
                "selectedKey", BaseGridClientState.selectedKey(),
                "claimCount", packet.claimCount(),
                "maxClaims", packet.maxClaims(),
                "remainingClaims", remaining,
                "claimBudget", packet.claimCount() + " / " + packet.maxClaims(),
                "gridRadius", packet.gridRadius(),
                "status", status,
                "statusKey", statusKey(packet.selectedState()),
                "selectedState", label(packet.selectedState()),
                "selectedOwner", packet.selectedOwner(),
                "selectedManageable", packet.selectedManageable(),
                "selectedReleaseAllowed", packet.selectedReleaseAllowed());
    }

    private static List<Map<String, Object>> chunkRows() {
        return snapshot().chunks().stream()
                .map(BaseGridDataProviders::chunkRow)
                .toList();
    }

    private static Map<String, Object> chunkRow(BaseGridSnapshotPacket.ChunkData chunk) {
        String state = chunk.state();
        String owner = chunk.ownerName().isBlank() ? "Unclaimed" : chunk.ownerName();
        return row(
                "key", chunk.key(),
                "dimension", chunk.dimension(),
                "chunkX", chunk.chunkX(),
                "chunkZ", chunk.chunkZ(),
                "dx", chunk.dx(),
                "dz", chunk.dz(),
                "selected", chunk.selected(),
                "current", chunk.current(),
                "state", state,
                "stateLabel", label(state),
                "status", statusKey(state),
                "owner", owner,
                "ownerName", owner,
                "label", chunk.label(),
                "coordinates", chunk.chunkX() + ", " + chunk.chunkZ(),
                "meta", (chunk.current() ? "current" : "offset " + signed(chunk.dx()) + ", " + signed(chunk.dz())));
    }

    private static Map<String, Object> selected() {
        BaseGridSnapshotPacket packet = snapshot();
        String owner = packet.selectedOwner().isBlank() ? "Unclaimed" : packet.selectedOwner();
        String state = packet.selectedState();
        boolean unclaimed = "unclaimed".equalsIgnoreCase(state);
        boolean claimDisabled = !unclaimed;
        boolean unclaimDisabled = !packet.selectedReleaseAllowed();
        return row(
                "key", BaseGridClientState.selectedKey(),
                "dimension", packet.dimension(),
                "chunkX", packet.selectedChunkX(),
                "chunkZ", packet.selectedChunkZ(),
                "coordinates", packet.selectedChunkX() + ", " + packet.selectedChunkZ(),
                "state", packet.selectedState(),
                "stateLabel", label(packet.selectedState()),
                "status", statusKey(packet.selectedState()),
                "owner", owner,
                "ownerName", owner,
                "ownedByPlayer", packet.selectedOwnedByPlayer(),
                "manageable", packet.selectedManageable(),
                "releaseAllowed", packet.selectedReleaseAllowed(),
                "manageLabel", manageLabel(packet),
                "manageStatus", manageStatus(packet),
                "releaseLabel", releaseLabel(packet),
                "releaseStatus", packet.selectedReleaseAllowed() ? "ready" : "locked",
                "summary", selectedSummary(packet),
                "memberCount", packet.members().size(),
                "candidateCount", packet.candidates().size(),
                "memberEmptyTitle", memberEmptyTitle(packet),
                "memberEmptyBody", memberEmptyBody(packet),
                "candidateEmptyTitle", candidateEmptyTitle(packet),
                "candidateEmptyBody", candidateEmptyBody(packet),
                "claimActionLabel", unclaimed ? "Claim" : "Claimed",
                "claimButtonDisabled", claimDisabled,
                "claimDisabledReason", claimDisabled ? "Only unclaimed chunks can be claimed." : "",
                "unclaimActionLabel", unclaimDisabled ? "Locked" : "Unclaim",
                "unclaimButtonDisabled", unclaimDisabled,
                "unclaimDisabledReason", unclaimDisabled ? unclaimDisabledReason(packet) : "");
    }

    private static List<Map<String, Object>> memberRows() {
        return snapshot().members().stream()
                .map(BaseGridDataProviders::memberRow)
                .toList();
    }

    private static Map<String, Object> memberRow(BaseGridSnapshotPacket.MemberData member) {
        return row(
                "uuid", member.uuid(),
                "name", member.name(),
                "role", member.role(),
                "roleLabel", member.roleLabel(),
                "build", member.build(),
                "interact", member.interact(),
                "containers", member.containers(),
                "manage", member.manage(),
                "manageable", member.manageable(),
                "actionDisabled", !member.manageable(),
                "removeDisabled", !member.manageable(),
                "disabledReason", member.manageable() ? "" : "Owner or manager access required.",
                "manageLabel", member.manageable() ? "Editable" : "Locked",
                "manageStatus", member.manageable() ? "ready" : "locked",
                "permissions", permissionsLabel(member),
                "status", member.manage() ? "ready" : member.build() ? "warning" : "info");
    }

    private static List<Map<String, Object>> candidateRows() {
        boolean canAdd = snapshot().selectedManageable();
        return snapshot().candidates().stream()
                .map(candidate -> row(
                        "uuid", candidate.uuid(),
                        "name", candidate.name(),
                        "status", "online",
                        "summary", canAdd ? "Add as trusted member" : "Management locked",
                        "actionLabel", canAdd ? "Add" : "Locked",
                        "addDisabled", !canAdd,
                        "disabledReason", canAdd ? "" : "Owner or manager access required."))
                .toList();
    }

    private static String manageLabel(BaseGridSnapshotPacket packet) {
        if (!BaseGridClientState.hasSnapshot()) {
            return "Awaiting Snapshot";
        }
        String state = packet.selectedState();
        if ("unclaimed".equalsIgnoreCase(state)) {
            return "Claim Available";
        }
        if (packet.selectedOwnedByPlayer()) {
            return "Owner Controls";
        }
        if (packet.selectedManageable()) {
            return "Manager Access";
        }
        if ("trusted".equalsIgnoreCase(state)) {
            return "Trusted Access";
        }
        return "Protected Claim";
    }

    private static String releaseLabel(BaseGridSnapshotPacket packet) {
        if ("unclaimed".equalsIgnoreCase(packet.selectedState())) {
            return "No Claim";
        }
        if (packet.selectedReleaseAllowed()) {
            return "Release Allowed";
        }
        return "Owner Release Only";
    }

    private static String manageStatus(BaseGridSnapshotPacket packet) {
        if (!BaseGridClientState.hasSnapshot()) {
            return "warning";
        }
        if (packet.selectedManageable() || packet.selectedOwnedByPlayer()) {
            return "ready";
        }
        String state = packet.selectedState();
        if ("unclaimed".equalsIgnoreCase(state)) {
            return "info";
        }
        if ("trusted".equalsIgnoreCase(state)) {
            return "warning";
        }
        return "danger";
    }

    private static String selectedSummary(BaseGridSnapshotPacket packet) {
        if (!BaseGridClientState.hasSnapshot()) {
            return "Waiting for the server to send local claim data.";
        }
        String state = packet.selectedState();
        if ("unclaimed".equalsIgnoreCase(state)) {
            return "No claim controls this chunk.";
        }
        if (packet.selectedOwnedByPlayer()) {
            return "You own this claim and can manage trusted members.";
        }
        if (packet.selectedManageable()) {
            return "You can manage trusted members and permissions here.";
        }
        if ("trusted".equalsIgnoreCase(state)) {
            return "You are trusted here with limited claim permissions.";
        }
        return "Protected by " + (packet.selectedOwner().isBlank() ? "another operator" : packet.selectedOwner()) + ".";
    }

    private static String memberEmptyTitle(BaseGridSnapshotPacket packet) {
        if (!BaseGridClientState.hasSnapshot()) {
            return "Snapshot Pending";
        }
        if ("unclaimed".equalsIgnoreCase(packet.selectedState())) {
            return "No Claim Members";
        }
        return "No Members";
    }

    private static String memberEmptyBody(BaseGridSnapshotPacket packet) {
        if (!BaseGridClientState.hasSnapshot()) {
            return "Refresh to request member data from the server.";
        }
        if ("unclaimed".equalsIgnoreCase(packet.selectedState())) {
            return "Claim this chunk before trusting players.";
        }
        if (packet.selectedManageable()) {
            return "Add online players from the list below.";
        }
        return "No trusted members are listed for this claim.";
    }

    private static String candidateEmptyTitle(BaseGridSnapshotPacket packet) {
        if (!BaseGridClientState.hasSnapshot()) {
            return "Snapshot Pending";
        }
        if ("unclaimed".equalsIgnoreCase(packet.selectedState())) {
            return "Claim Required";
        }
        if (!packet.selectedManageable()) {
            return "Management Locked";
        }
        return "No Candidates";
    }

    private static String candidateEmptyBody(BaseGridSnapshotPacket packet) {
        if (!BaseGridClientState.hasSnapshot()) {
            return "Refresh to request online player candidates.";
        }
        if ("unclaimed".equalsIgnoreCase(packet.selectedState())) {
            return "Claim this chunk before adding members.";
        }
        if (!packet.selectedManageable()) {
            return "Only owners and managers can trust players here.";
        }
        return "No online players are available to add to this claim.";
    }

    private static String unclaimDisabledReason(BaseGridSnapshotPacket packet) {
        if ("unclaimed".equalsIgnoreCase(packet.selectedState())) {
            return "This chunk is not claimed.";
        }
        return "Only the owner or an operator can release this claim.";
    }

    private static List<Map<String, Object>> roleRows() {
        return Arrays.stream(ClaimRole.values())
                .map(role -> row(
                        "id", role.name(),
                        "label", role.label(),
                        "summary", "Default: " + role.defaultPermissions().stream()
                                .map(ClaimPermission::label)
                                .toList()))
                .toList();
    }

    private static List<Map<String, Object>> permissionRows() {
        return Arrays.stream(ClaimPermission.values())
                .map(permission -> row(
                        "id", permission.name(),
                        "label", permission.label(),
                        "summary", switch (permission) {
                            case BUILD -> "Place and break blocks";
                            case INTERACT -> "Use doors, buttons, and machines";
                            case CONTAINERS -> "Open inventories and storage";
                            case MANAGE -> "Add members and tune permissions";
                        }))
                .toList();
    }

    private static String permissionsLabel(BaseGridSnapshotPacket.MemberData member) {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        if (member.build()) {
            labels.add("Build");
        }
        if (member.interact()) {
            labels.add("Interact");
        }
        if (member.containers()) {
            labels.add("Containers");
        }
        if (member.manage()) {
            labels.add("Manage");
        }
        return labels.isEmpty() ? "No permissions" : String.join(", ", labels);
    }

    private static String statusKey(String state) {
        return switch (state == null ? "" : state.toLowerCase(Locale.ROOT)) {
            case "mine", "trusted" -> "ready";
            case "occupied" -> "danger";
            case "unclaimed" -> "info";
            default -> "warning";
        };
    }

    private static String label(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "mine" -> "Owned";
            case "trusted" -> "Trusted";
            case "occupied" -> "Occupied";
            case "unclaimed" -> "Unclaimed";
            default -> Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ROOT);
        };
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    private static Map<String, Object> row(Object... pairs) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}

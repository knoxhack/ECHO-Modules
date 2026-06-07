package com.knoxhack.echobasegrid.client;

import com.knoxhack.echobasegrid.api.ClaimPermission;
import com.knoxhack.echobasegrid.api.ClaimRole;
import com.knoxhack.echobasegrid.network.BaseGridClaimActionPacket;
import com.knoxhack.echobasegrid.network.BaseGridSnapshotRequestPacket;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.EchoScreens;
import com.knoxhack.echoscreencore.api.action.EchoAction;
import com.knoxhack.echoscreencore.api.action.EchoActionContext;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BaseGridActions {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private BaseGridActions() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        register("basegrid.refresh", BaseGridActions::refresh);
        register("basegrid.select_chunk", BaseGridActions::selectChunk);
        register("basegrid.claim_selected", BaseGridActions::claimSelected);
        register("basegrid.unclaim_selected", BaseGridActions::unclaimSelected);
        register("basegrid.add_member", BaseGridActions::addMember);
        register("basegrid.remove_member", BaseGridActions::removeMember);
        register("basegrid.set_role", BaseGridActions::setRole);
        register("basegrid.toggle_permission", BaseGridActions::togglePermission);
    }

    public static boolean requestSnapshot() {
        boolean hasSnapshot = BaseGridClientState.hasSnapshot();
        BaseGridClientState.markSnapshotRequested();
        return EchoNetClientActions.trySendServerboundAction(new BaseGridSnapshotRequestPacket(
                hasSnapshot ? BaseGridClientState.selectedDimension() : "",
                hasSnapshot ? BaseGridClientState.selectedChunkX() : 0,
                hasSnapshot ? BaseGridClientState.selectedChunkZ() : 0));
    }

    private static void register(String id, EchoAction action) {
        EchoScreenRegistry.registerAction(id, action);
    }

    private static boolean refresh(EchoActionContext context) {
        return requestSnapshot();
    }

    private static boolean selectChunk(EchoActionContext context) {
        String key = context.actionValue();
        if (key == null || key.isBlank()) {
            key = context.param("chunk");
        }
        BaseGridClientState.select(key);
        requestSnapshot();
        return true;
    }

    private static boolean claimSelected(EchoActionContext context) {
        return send(BaseGridClaimActionPacket.Action.CLAIM, null, "", ClaimRole.MEMBER, ClaimPermission.BUILD);
    }

    private static boolean unclaimSelected(EchoActionContext context) {
        return send(BaseGridClaimActionPacket.Action.UNCLAIM, null, "", ClaimRole.MEMBER, ClaimPermission.BUILD);
    }

    private static boolean addMember(EchoActionContext context) {
        UUID target = uuid(context.actionValue());
        String name = context.param("name");
        return send(BaseGridClaimActionPacket.Action.ADD_MEMBER, target, name, ClaimRole.MEMBER, ClaimPermission.BUILD);
    }

    private static boolean removeMember(EchoActionContext context) {
        UUID target = uuid(context.actionValue());
        return send(BaseGridClaimActionPacket.Action.REMOVE_MEMBER, target, "", ClaimRole.MEMBER, ClaimPermission.BUILD);
    }

    private static boolean setRole(EchoActionContext context) {
        UUID target = uuid(context.actionValue());
        ClaimRole role = ClaimRole.fromId(paramOrValue(context, "role"));
        return send(BaseGridClaimActionPacket.Action.SET_ROLE, target, "", role, ClaimPermission.BUILD);
    }

    private static boolean togglePermission(EchoActionContext context) {
        UUID target = uuid(context.actionValue());
        ClaimPermission permission = ClaimPermission.fromId(paramOrValue(context, "permission"));
        return send(BaseGridClaimActionPacket.Action.TOGGLE_PERMISSION, target, "", ClaimRole.MEMBER, permission);
    }

    private static boolean send(BaseGridClaimActionPacket.Action action, UUID target, String targetName,
            ClaimRole role, ClaimPermission permission) {
        boolean sent = EchoNetClientActions.trySendServerboundAction(new BaseGridClaimActionPacket(
                action,
                BaseGridClientState.selectedDimension(),
                BaseGridClientState.selectedChunkX(),
                BaseGridClientState.selectedChunkZ(),
                target,
                targetName == null ? "" : targetName,
                role,
                permission));
        EchoScreens.invalidateData();
        return sent;
    }

    private static String paramOrValue(EchoActionContext context, String param) {
        String value = context.param(param);
        if (value == null || value.isBlank()) {
            value = context.actionValue();
        }
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }

    private static UUID uuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.strip());
        } catch (RuntimeException exception) {
            return null;
        }
    }
}

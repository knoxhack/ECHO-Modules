package com.knoxhack.echoterminal.client.hud;

import com.knoxhack.echoterminal.client.discovery.DiscoveryToastHud;
import com.knoxhack.echoterminal.client.mission.TerminalMissionHudController;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class TerminalHudNoticeSurface {
    private static String externalSurfaceOwner = "";
    private static final Map<String, Supplier<Optional<TerminalHudNotice>>> EXTERNAL_NOTICE_SUPPLIERS =
            new LinkedHashMap<>();

    private TerminalHudNoticeSurface() {
    }

    public static void claimExternalSurface(String owner) {
        externalSurfaceOwner = owner == null || owner.isBlank() ? "external" : owner.trim();
    }

    public static void releaseExternalSurface(String owner) {
        if (owner == null || owner.isBlank() || owner.equals(externalSurfaceOwner)) {
            externalSurfaceOwner = "";
        }
    }

    public static boolean externalSurfaceClaimed() {
        return !externalSurfaceOwner.isBlank();
    }

    public static String externalSurfaceOwner() {
        return externalSurfaceOwner;
    }

    public static boolean shouldRenderInternalCards() {
        return !externalSurfaceClaimed();
    }

    public static void registerExternalNoticeSupplier(String owner, Supplier<Optional<TerminalHudNotice>> supplier) {
        String key = cleanOwner(owner);
        if (supplier == null) {
            EXTERNAL_NOTICE_SUPPLIERS.remove(key);
            return;
        }
        EXTERNAL_NOTICE_SUPPLIERS.put(key, supplier);
    }

    public static void unregisterExternalNoticeSupplier(String owner) {
        EXTERNAL_NOTICE_SUPPLIERS.remove(cleanOwner(owner));
    }

    public static List<TerminalHudNotice> activeNotices() {
        ArrayList<TerminalHudNotice> notices = new ArrayList<>(4);
        TerminalMissionHudController.activeNoticeForHud().ifPresent(notices::add);
        DiscoveryToastHud.activeNoticeForHud().ifPresent(notices::add);
        for (Supplier<Optional<TerminalHudNotice>> supplier : EXTERNAL_NOTICE_SUPPLIERS.values()) {
            try {
                Optional<TerminalHudNotice> notice = supplier.get();
                if (notice != null) {
                    notice.ifPresent(notices::add);
                }
            } catch (LinkageError | RuntimeException ignored) {
                // Keep one optional HUD publisher from breaking the shared notice shelf.
            }
        }
        if (notices.size() <= 2) {
            return List.copyOf(notices);
        }
        return List.copyOf(notices.subList(0, 2));
    }

    public static void resetForTests() {
        externalSurfaceOwner = "";
        EXTERNAL_NOTICE_SUPPLIERS.clear();
    }

    private static String cleanOwner(String owner) {
        return owner == null || owner.isBlank() ? "external" : owner.trim();
    }
}

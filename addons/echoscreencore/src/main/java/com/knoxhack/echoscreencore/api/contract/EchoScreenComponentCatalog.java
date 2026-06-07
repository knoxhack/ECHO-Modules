package com.knoxhack.echoscreencore.api.contract;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoScreenComponentCatalog {
    private static final String NAMESPACE = "echoscreencore";

    private EchoScreenComponentCatalog() {
    }

    public static List<EchoScreenComponentContract> cyberglassDefaults() {
        return List.of(
            contract("sc_app_shell", EchoScreenComponentKind.SCREEN_SHELL, "Primary bounded application shell.", 360, 240, false, false, true),
            contract("sc_page_header", EchoScreenComponentKind.PAGE_HEADER, "Compact title, subtitle, and state header.", 220, 40, false, false, true),
            contract("sc_dense_list", EchoScreenComponentKind.SCROLL_PANE, "Single scroll owner for repeated rows.", 180, 120, false, true, true),
            contract("sc_list_detail_shell", EchoScreenComponentKind.DETAIL_PANE, "List and inspector split surface.", 420, 220, false, false, true),
            contract("sc_feature_card", EchoScreenComponentKind.CARD, "Reusable feature card with bounded copy.", 120, 72, true, false, true),
            contract("sc_action_strip", EchoScreenComponentKind.COMMAND_BAR, "Icon or text command row with stable button heights.", 220, 34, true, false, true),
            contract("search_filter_bar", EchoScreenComponentKind.SEARCH_BAR, "Search field plus compact filters.", 220, 34, true, false, true),
            contract("status_chip_row", EchoScreenComponentKind.STATUS_CHIP, "Semantic status chip grouping.", 120, 22, false, false, true),
            contract("confirm_dialog", EchoScreenComponentKind.MODAL, "Confirmation surface with explicit actions.", 240, 120, true, false, true),
            contract("empty_state", EchoScreenComponentKind.EMPTY_STATE, "Graceful degraded state for empty providers.", 160, 70, false, false, true)
        );
    }

    public static Optional<EchoScreenComponentContract> findCyberglassDefault(EchoScreenComponentKind kind) {
        return cyberglassDefaults().stream()
            .filter(contract -> contract.kind() == kind)
            .findFirst();
    }

    private static EchoScreenComponentContract contract(String path, EchoScreenComponentKind kind, String purpose,
            int minWidth, int minHeight, boolean focusable, boolean scrollOwner, boolean controllerReady) {
        return new EchoScreenComponentContract(
            Identifier.fromNamespaceAndPath(NAMESPACE, path),
            kind,
            purpose,
            minWidth,
            minHeight,
            focusable,
            scrollOwner,
            controllerReady,
            true,
            "Render a bounded empty state or plain list row if theme tokens or optional providers are unavailable."
        );
    }
}

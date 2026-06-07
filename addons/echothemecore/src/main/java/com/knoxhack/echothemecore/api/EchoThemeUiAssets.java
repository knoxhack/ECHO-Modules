package com.knoxhack.echothemecore.api;

import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoThemeUiAssets(
    Identifier backgroundTexture,
    Identifier panelTexture,
    Identifier panelAltTexture,
    Identifier buttonTexture,
    Identifier buttonHoverTexture,
    Identifier tabTexture,
    Identifier tabActiveTexture,
    Identifier missionCardTexture,
    Identifier missionCardSelectedTexture,
    Identifier statusChipTexture,
    Identifier progressBarTexture,
    Identifier scrollbarTexture,
    Identifier iconPack,
    Identifier hologramOverlay,
    Identifier energyOverlay,
    Identifier edgeGlow,
    Identifier particleGlints,
    Identifier lockedOverlay
) {
    public Optional<Identifier> texture(EchoThemeTextureKey key) {
        return Optional.ofNullable(switch (key) {
            case BACKGROUND -> backgroundTexture;
            case PANEL -> panelTexture;
            case PANEL_ALT -> panelAltTexture;
            case BUTTON -> buttonTexture;
            case BUTTON_HOVER -> buttonHoverTexture;
            case TAB -> tabTexture;
            case TAB_ACTIVE -> tabActiveTexture;
            case MISSION_CARD -> missionCardTexture;
            case MISSION_CARD_SELECTED -> missionCardSelectedTexture;
            case STATUS_CHIP -> statusChipTexture;
            case PROGRESS_BAR -> progressBarTexture;
            case SCROLLBAR -> scrollbarTexture;
            case ICON_PACK -> iconPack;
            case HOLOGRAM_OVERLAY -> hologramOverlay;
            case ENERGY_OVERLAY -> energyOverlay;
            case EDGE_GLOW -> edgeGlow;
            case PARTICLE_GLINTS -> particleGlints;
            case LOCKED_OVERLAY -> lockedOverlay;
            default -> null;
        });
    }
}

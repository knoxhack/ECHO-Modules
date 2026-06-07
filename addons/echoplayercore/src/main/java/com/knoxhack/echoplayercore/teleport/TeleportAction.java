package com.knoxhack.echoplayercore.teleport;

public enum TeleportAction {
    HOME("home"),
    SET_HOME("sethome"),
    DELETE_HOME("delhome"),
    LIST_HOMES("homes"),
    RTP("rtp"),
    BACK("back"),
    SPAWN("spawn"),
    SET_SPAWN("setspawn"),
    WARP("warp"),
    TPA("tpa"),
    TPA_HERE("tpahere");

    private final String cooldownKey;

    TeleportAction(String cooldownKey) {
        this.cooldownKey = cooldownKey;
    }

    public String cooldownKey() {
        return cooldownKey;
    }
}

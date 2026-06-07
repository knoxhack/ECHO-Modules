package com.knoxhack.echo.socialcore;

public enum EchoDialogueConsequenceKind {
    OPEN_NODE("open_node"),
    CLOSE_DIALOGUE("close_dialogue"),
    START_MISSION("start_mission"),
    COMPLETE_OBJECTIVE("complete_objective"),
    ADJUST_REPUTATION("adjust_reputation"),
    GRANT_UNLOCK("grant_unlock"),
    REVEAL_CONTENT("reveal_content"),
    TRIGGER_FEATURE("trigger_feature"),
    OPEN_TRADE_REFERENCE("open_trade_reference"),
    PLAY_VOICE("play_voice"),
    TRIGGER_CINEMATIC_REFERENCE("trigger_cinematic_reference"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoDialogueConsequenceKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}

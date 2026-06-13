package com.knoxhack.echo.skillcore;

import java.util.List;

public final class EchoSkillCore {
    public static final String MODID = "echoskillcore";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echoprogressioncore",
            "echoplayercore",
            "echomissioncore"
        );
    public static final List<String> PROVIDES = List.of(
            "skill.tracks",
            "skill.mastery",
            "skill.passive_unlocks",
            "skill.progression_gates"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "skill_track_contract",
            "mastery_contract",
            "passive_unlocks",
            "progression_gates"
        );

    public EchoSkillCore() {
        bootstrap();
    }

    public void bootstrap() {
    }

    public String moduleId() {
        return MODID;
    }

    public List<String> provides() {
        return PROVIDES;
    }
}

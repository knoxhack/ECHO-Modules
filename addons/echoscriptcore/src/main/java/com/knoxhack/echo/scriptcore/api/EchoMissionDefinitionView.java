package com.knoxhack.echo.scriptcore.api;

import com.google.gson.JsonObject;
import com.knoxhack.echo.scriptcore.model.EchoObjective;
import com.knoxhack.echo.scriptcore.model.EchoReward;
import java.util.List;

public interface EchoMissionDefinitionView extends EchoScriptDefinitionView {
    String route();

    String phase();

    String role();

    String briefing();

    List<EchoObjective> objectives();

    List<EchoReward> rewards();

    List<EchoCondition> prerequisites();

    List<EchoAction> onStart();

    List<EchoAction> onComplete();

    List<EchoAction> onFail();

    JsonObject terminal();

    JsonObject lens();

    JsonObject holomap();
}

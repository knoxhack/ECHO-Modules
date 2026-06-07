package com.knoxhack.echo.economycore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.progressioncore.EchoObjectiveId;
import com.knoxhack.echo.progressioncore.EchoProgressionId;

import java.util.List;
import java.util.Map;

public record EchoMissionRewardValue(
        String rewardId,
        EchoProgressionId progressionId,
        EchoObjectiveId objectiveId,
        List<EchoCurrencyAmount> currencyRewards,
        List<EchoContentReference> itemRewards,
        boolean claimOnce,
        Map<String, String> attributes
) {
    public EchoMissionRewardValue {
        rewardId = EconomyContractGuards.requireText(rewardId, "mission reward id");
        currencyRewards = EconomyContractGuards.immutableList(currencyRewards);
        itemRewards = EconomyContractGuards.immutableList(itemRewards);
        attributes = EconomyContractGuards.immutableMap(attributes);
    }
}

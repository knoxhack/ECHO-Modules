package com.knoxhack.echo.lootcore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.progressioncore.EchoObjectiveId;
import com.knoxhack.echo.progressioncore.EchoProgressionId;

import java.util.List;
import java.util.Map;

public record EchoMissionRewardPool(
        String rewardPoolId,
        EchoProgressionId progressionId,
        EchoObjectiveId objectiveId,
        List<EchoLootPoolId> lootPoolIds,
        List<EchoContentReference> directRewards,
        EchoDuplicationPolicy duplicationPolicy,
        boolean claimOnce,
        Map<String, String> attributes
) {
    public EchoMissionRewardPool {
        rewardPoolId = LootContractGuards.requireText(rewardPoolId, "mission reward pool id");
        lootPoolIds = LootContractGuards.immutableList(lootPoolIds);
        directRewards = LootContractGuards.immutableList(directRewards);
        duplicationPolicy = duplicationPolicy == null ? EchoDuplicationPolicy.UNKNOWN : duplicationPolicy;
        attributes = LootContractGuards.immutableMap(attributes);
    }
}

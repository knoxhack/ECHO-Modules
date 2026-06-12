package com.echoplatform.echocore.api.prime;

import java.util.List;
import net.minecraft.resources.Identifier;

public interface PrimeMissionRegistry {
    boolean registerMissionChain(PrimeMissionChain chain);

    List<PrimeMissionChain> missionChains();

    record PrimeMissionChain(
            Identifier id,
            String title,
            String summary,
            Identifier routeId,
            List<Identifier> missionIds,
            int order) {
        public PrimeMissionChain {
            title = title == null ? "" : title;
            summary = summary == null ? "" : summary;
            missionIds = missionIds == null ? List.of() : List.copyOf(missionIds);
        }
    }
}

package com.echoplatform.echocore.api;

public record EchoFactionPoiAffinity(String profileId, String route, int weight, boolean landmark) {
    public EchoFactionPoiAffinity {
        profileId = profileId == null ? "" : profileId;
        route = route == null ? "" : route;
    }
}

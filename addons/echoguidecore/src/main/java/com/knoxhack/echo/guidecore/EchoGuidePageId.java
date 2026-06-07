package com.knoxhack.echo.guidecore;

public record EchoGuidePageId(String value) {
    public EchoGuidePageId {
        value = GuideContractGuards.id(value, "guide page id");
    }

    public static EchoGuidePageId of(String value) {
        return new EchoGuidePageId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

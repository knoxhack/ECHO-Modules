package com.knoxhack.echo.guidecore;

public record EchoGuideCategoryId(String value) {
    public EchoGuideCategoryId {
        value = GuideContractGuards.id(value, "guide category id");
    }

    public static EchoGuideCategoryId of(String value) {
        return new EchoGuideCategoryId(value);
    }
}

package com.knoxhack.echoscreencore.api.layout;

public record EchoBounds(EchoRect outer, EchoRect content) {
    public static final EchoBounds ZERO = new EchoBounds(EchoRect.ZERO, EchoRect.ZERO);
}

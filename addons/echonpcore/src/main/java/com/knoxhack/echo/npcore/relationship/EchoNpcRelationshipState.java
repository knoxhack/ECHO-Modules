package com.knoxhack.echo.npcore.relationship;

public record EchoNpcRelationshipState(String label, int reputation) {
    public static final EchoNpcRelationshipState NEUTRAL = new EchoNpcRelationshipState("Neutral", 0);

    public EchoNpcRelationshipState {
        label = label == null || label.isBlank() ? "Neutral" : label.trim();
    }
}

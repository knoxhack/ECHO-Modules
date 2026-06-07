package com.knoxhack.echoindex.service;

public enum IndexRecipeActionState {
    READY("Ready"),
    MISSING("Missing"),
    PLAN_ONLY("Plan Only");

    private final String label;

    IndexRecipeActionState(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

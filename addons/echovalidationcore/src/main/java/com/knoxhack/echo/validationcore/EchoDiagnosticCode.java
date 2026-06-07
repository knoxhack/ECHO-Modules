package com.knoxhack.echo.validationcore;

public record EchoDiagnosticCode(String value) {
    public EchoDiagnosticCode {
        value = ValidationContractGuards.requireText(value, "diagnostic code");
    }

    public static EchoDiagnosticCode of(String value) {
        return new EchoDiagnosticCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

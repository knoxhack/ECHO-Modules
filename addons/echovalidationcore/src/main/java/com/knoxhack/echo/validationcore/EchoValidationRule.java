package com.knoxhack.echo.validationcore;

public interface EchoValidationRule {
    String id();

    EchoValidationCategory category();

    EchoValidationScope scope();

    default boolean supports(EchoValidationTarget target) {
        boolean categoryMatches = category() == EchoValidationCategory.UNKNOWN || category() == target.category();
        boolean scopeMatches = scope() == EchoValidationScope.UNKNOWN || scope() == target.scope();
        return categoryMatches && scopeMatches;
    }

    EchoValidationResult validate(EchoValidationTarget target, EchoDiagnosticContext context);
}

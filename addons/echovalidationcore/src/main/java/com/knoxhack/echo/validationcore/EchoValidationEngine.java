package com.knoxhack.echo.validationcore;

import java.util.ArrayList;
import java.util.List;

public final class EchoValidationEngine {
    private final List<EchoValidationRule> rules;

    public EchoValidationEngine(List<EchoValidationRule> rules) {
        this.rules = ValidationContractGuards.immutableList(rules);
    }

    public static EchoValidationEngine empty() {
        return new EchoValidationEngine(List.of());
    }

    public static EchoValidationEngine of(List<EchoValidationRule> rules) {
        return new EchoValidationEngine(rules);
    }

    public List<EchoValidationRule> rules() {
        return rules;
    }

    public EchoValidationResult validate(EchoValidationTarget target, EchoDiagnosticContext context) {
        List<EchoDiagnostic> diagnostics = new ArrayList<>();
        for (EchoValidationRule rule : rules) {
            if (rule.supports(target)) {
                diagnostics.addAll(rule.validate(target, context).diagnostics());
            }
        }
        return EchoValidationResult.of(target, diagnostics);
    }

    public EchoDiagnosticReport validateAll(String title, List<EchoValidationTarget> targets, EchoDiagnosticContext context) {
        List<EchoDiagnostic> diagnostics = new ArrayList<>();
        for (EchoValidationTarget target : ValidationContractGuards.immutableList(targets)) {
            diagnostics.addAll(validate(target, context).diagnostics());
        }
        return EchoDiagnosticReport.of(title, context, diagnostics);
    }
}

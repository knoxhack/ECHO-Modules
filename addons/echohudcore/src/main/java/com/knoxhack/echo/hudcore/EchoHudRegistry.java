package com.knoxhack.echo.hudcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoHudRegistry(
        Map<EchoHudWidgetId, EchoHudWidget> widgets,
        List<EchoMissionTrackerEntry> missionTrackerEntries,
        List<EchoCompassIndicator> compassIndicators,
        List<EchoHazardMeter> hazardMeters,
        List<EchoNotificationAnchor> notificationAnchors,
        List<EchoScreenSafeArea> safeAreas,
        List<EchoHudScaleRule> scaleRules,
        List<EchoDiagnostic> diagnostics
) {
    public EchoHudRegistry {
        widgets = HudContractGuards.immutableMap(widgets);
        missionTrackerEntries = HudContractGuards.immutableList(missionTrackerEntries);
        compassIndicators = HudContractGuards.immutableList(compassIndicators);
        hazardMeters = HudContractGuards.immutableList(hazardMeters);
        notificationAnchors = HudContractGuards.immutableList(notificationAnchors);
        safeAreas = HudContractGuards.immutableList(safeAreas);
        scaleRules = HudContractGuards.immutableList(scaleRules);
        diagnostics = HudContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || widgets.values().stream().anyMatch(EchoHudWidget::blocking)
                || missionTrackerEntries.stream().anyMatch(EchoMissionTrackerEntry::blocking)
                || compassIndicators.stream().anyMatch(EchoCompassIndicator::blocking);
    }
}

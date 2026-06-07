package com.knoxhack.echoscreencore.client.layout;

public record EchoResponsiveRules(
    boolean hidden,
    boolean stacked,
    boolean compact,
    boolean dense,
    boolean collapsed,
    boolean sidebarCollapsed,
    boolean detailCollapsed,
    EchoBreakpoint activeBreakpoint
) {
}

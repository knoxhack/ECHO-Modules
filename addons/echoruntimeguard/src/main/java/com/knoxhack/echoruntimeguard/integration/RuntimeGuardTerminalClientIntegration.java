package com.knoxhack.echoruntimeguard.integration;

import com.knoxhack.echoruntimeguard.EchoRuntimeGuard;
import com.knoxhack.echoruntimeguard.api.ClientMetricsSnapshot;
import com.knoxhack.echoruntimeguard.api.ParticleBudgetSnapshot;
import com.knoxhack.echoruntimeguard.api.RuntimeMetricsSnapshot;
import com.knoxhack.echoruntimeguard.client.ClientFpsMonitor;
import com.knoxhack.echoruntimeguard.runtime.IntegrationThrottleService;
import com.knoxhack.echoruntimeguard.runtime.MultiblockValidationScheduler;
import com.knoxhack.echoruntimeguard.runtime.ParticleBudgetService;
import com.knoxhack.echoruntimeguard.runtime.RuntimeProfilerService;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class RuntimeGuardTerminalClientIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final Identifier TAB_ID = EchoRuntimeGuard.id("terminal/runtime");
    private static final int ACCENT = 0xFF66E8FF;

    private RuntimeGuardTerminalClientIntegration() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            TerminalTabRegistry.register(new RuntimeGuardTab());
        }
    }

    private static final class RuntimeGuardTab implements ClientTerminalTab {
        private final TerminalTabDescriptor descriptor = new TerminalTabDescriptor(TAB_ID, "RUNTIME", 240, ACCENT);
        private final TerminalTabChrome chrome = TerminalTabChrome.of("Signal Health",
                TerminalTabChrome.GROUP_SYSTEMS, "RG", "RuntimeGuard performance status", 240);

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                float partialTick) {
            RuntimeMetricsSnapshot metrics = RuntimeProfilerService.INSTANCE.lastSnapshot();
            ClientMetricsSnapshot client = ClientFpsMonitor.snapshot();
            ParticleBudgetSnapshot particles = ParticleBudgetService.INSTANCE.getSnapshot();
            int x = context.contentX() + 12;
            int y = context.contentY() + 10 - context.scrollY();
            int w = context.contentWidth() - 24;
            y = TerminalUi.sectionHeader(context, graphics, "SIGNAL HEALTH", "RUNTIME", x, y, w, ACCENT);
            TerminalUi.flatHudPanel(context, graphics, x, y, w, 136, ACCENT);
            int lineY = y + 12;
            lineY = metric(context, graphics, x + 12, lineY, w - 24, "Mode", metrics.mode().displayName());
            lineY = metric(context, graphics, x + 12, lineY, w - 24, "Emergency", metrics.emergency() ? "ON" : "OFF");
            lineY = metric(context, graphics, x + 12, lineY, w - 24, "TPS", String.format(java.util.Locale.ROOT, "%.1f", metrics.averageTps()));
            lineY = metric(context, graphics, x + 12, lineY, w - 24, "MSPT", Math.round(metrics.averageMspt()) + "ms");
            lineY = metric(context, graphics, x + 12, lineY, w - 24, "FPS", client.currentFps() < 0 ? "unavailable" : String.valueOf(client.currentFps()));
            lineY = metric(context, graphics, x + 12, lineY, w - 24, "Particles", particles.used() + "/" + particles.budget());
            lineY = metric(context, graphics, x + 12, lineY, w - 24, "Multiblocks",
                    MultiblockValidationScheduler.INSTANCE.getSnapshot().queued() + " queued");
            metric(context, graphics, x + 12, lineY, w - 24, "Guards", IntegrationThrottleService.INSTANCE.statusLine());
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return 190;
        }

        private int metric(TerminalRenderContext context, GuiGraphicsExtractor graphics, int x, int y, int w,
                String label, String value) {
            TerminalUi.line(context, graphics, label, x, y, 92, TerminalUi.muted(context));
            TerminalUi.line(context, graphics, value, x + 98, y, Math.max(60, w - 98), TerminalUi.text(context));
            return y + 14;
        }
    }
}

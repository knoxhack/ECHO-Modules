package com.knoxhack.echopowergrid.integration.terminal;

import com.knoxhack.echopowergrid.client.PowerGridClientState;
import com.knoxhack.echopowergrid.network.PowerGridNetworkSummaryPacket;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoProvider;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonLink;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class PowerGridTerminalClientIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int ACCENT = 0xFF55DDEF;

    private PowerGridTerminalClientIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        TerminalTab tab = new PowerGridTab();
        TerminalTabRegistry.register(tab);
        TerminalNavigationProfiles.register(tab.descriptor().id(),
                TerminalNavigationProfile.chapter("power_grid", "Optional: Power Grid", "PG", 62));
        TerminalAddonInfoRegistry.register(new PowerGridAddonInfoProvider());
    }

    private static final class PowerGridAddonInfoProvider implements TerminalAddonInfoProvider {
        @Override
        public String chapterId() {
            return "power_grid";
        }

        @Override
        public TerminalAddonInfo info(net.minecraft.world.entity.player.Player player) {
            PowerGridNetworkSummaryPacket packet = PowerGridClientState.snapshot();
            long generation = packet.networks().stream().mapToLong(PowerGridNetworkSummaryPacket.Entry::totalGeneration).sum();
            long demand = packet.networks().stream().mapToLong(PowerGridNetworkSummaryPacket.Entry::totalDemand).sum();
            long stored = packet.networks().stream().mapToLong(PowerGridNetworkSummaryPacket.Entry::totalStored).sum();
            long stressed = packet.networks().stream()
                    .filter(entry -> "BROWNOUT".equals(entry.state()) || "OVERLOADED".equals(entry.state()))
                    .count();
            return new TerminalAddonInfo(
                    "Shared EP generation, storage, network health, and facility power routing.",
                    List.of(
                            new TerminalAddonMetric("Networks", String.valueOf(packet.networks().size()),
                                    stressed + " stressed", stressed > 0 ? TerminalUi.AMBER : ACCENT),
                            new TerminalAddonMetric("Generation", generation + " EP/t", "loaded chunks", TerminalUi.GREEN),
                            new TerminalAddonMetric("Demand", demand + " EP/t", "loaded consumers", demand > generation ? TerminalUi.AMBER : ACCENT),
                            new TerminalAddonMetric("Stored", stored + " EP", "battery reserve", TerminalUi.CYAN)),
                    List.of(
                            new TerminalAddonSection("Grid Feed", List.of(packet.statusLine())),
                            new TerminalAddonSection("Next Step", List.of(nextStep(packet)))),
                    List.of(new TerminalAddonLink(PowerGridTerminalIds.TAB, "Power Grid", "Network dashboard", ACCENT)),
                    TerminalAddonGuide.optional(420, "Infrastructure route",
                            "Build generators, batteries, substations, and cables before higher-tier automation leans on EP.",
                            List.of(
                                    "Place a generator and battery on a connected PowerGrid network.",
                                    "Use substations and meters to verify state, quality, and reserve.",
                                    "Feed MultiblockCore capability costs through echo:power_input.")));
        }

        private static String nextStep(PowerGridNetworkSummaryPacket packet) {
            if (packet.networks().isEmpty()) {
                return "Build a generator, Field Battery Bank, and cable run, then refresh this tab.";
            }
            long stressed = packet.networks().stream()
                    .filter(entry -> "BROWNOUT".equals(entry.state()) || "OVERLOADED".equals(entry.state()))
                    .count();
            if (stressed > 0) {
                return "Stabilize stressed grids, then scan the fault with Lens and check HoloMap alerts.";
            }
            boolean hasHv = packet.networks().stream().anyMatch(entry -> entry.transferLimit() >= 1200L);
            if (!hasHv) {
                return "Commission high-voltage cable and mark the trunk before adding heavy processors.";
            }
            boolean hasReserve = packet.networks().stream().anyMatch(entry -> entry.totalCapacity() >= 160000L);
            if (!hasReserve) {
                return "Add a Field Battery Bank so the factory has a visible reserve buffer.";
            }
            return "Open Index: Terminal Next Steps and finish the grid commissioning mission.";
        }
    }

    private static final class PowerGridTab implements ClientTerminalTab {
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(PowerGridTerminalIds.TAB, "POWER GRID", 62, ACCENT);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Power Grid", TerminalTabChrome.GROUP_SYSTEMS, "PG", "EP network telemetry", 62);
        private final List<Hit> hits = new ArrayList<>();
        private int selectedIndex;

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void onSelected(TerminalRenderContext context) {
            if (!TerminalNavigationProfiles.referenceOnlyChapterContext(context)
                    && PowerGridClientState.shouldRequest(1000L)) {
                context.sendAction(PowerGridTerminalIds.TAB, PowerGridTerminalIds.STATUS_ACTION, "");
            }
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            hits.clear();
            PowerGridNetworkSummaryPacket packet = PowerGridClientState.snapshot();
            List<PowerGridNetworkSummaryPacket.Entry> entries = packet.networks();
            selectedIndex = entries.isEmpty() ? 0 : Math.max(0, Math.min(selectedIndex, entries.size() - 1));
            PowerGridNetworkSummaryPacket.Entry selected = entries.isEmpty() ? null : entries.get(selectedIndex);
            Font font = context.minecraft().font;
            int x = context.contentX();
            int y = context.contentY();
            int w = context.contentWidth();
            int leftW = Math.min(226, Math.max(176, w / 2));
            int rightX = x + leftW + 12;
            int rightW = Math.max(176, w - leftW - 12);

            graphics.text(font, Component.literal("POWER GRID // " + packet.statusLine()), x, y, ACCENT, false);
            drawAction(context, graphics, x + w - 76, y - 2, 76, 18, "REFRESH", mouseX, mouseY);

            long generation = entries.stream().mapToLong(PowerGridNetworkSummaryPacket.Entry::totalGeneration).sum();
            long demand = entries.stream().mapToLong(PowerGridNetworkSummaryPacket.Entry::totalDemand).sum();
            long available = entries.stream().mapToLong(PowerGridNetworkSummaryPacket.Entry::availablePower).sum();
            long stored = entries.stream().mapToLong(PowerGridNetworkSummaryPacket.Entry::totalStored).sum();
            drawMetric(graphics, font, x, y + 22, "NET", entries.size(), ACCENT);
            drawMetric(graphics, font, x + 82, y + 22, "GEN", generation, TerminalUi.GREEN);
            drawMetric(graphics, font, x + 164, y + 22, "LOAD", demand, demand > generation ? TerminalUi.AMBER : ACCENT);
            drawMetric(graphics, font, x + 246, y + 22, "DRAW", available, available > 0 ? TerminalUi.GREEN : TerminalUi.MUTED);
            drawMetric(graphics, font, x + 328, y + 22, "STORED", stored, stored > 0 ? TerminalUi.CYAN : TerminalUi.MUTED);
            graphics.text(font, Component.literal("NEXT // " + nextStep(packet)), x, y + 58,
                    nextStepColor(packet), false);

            drawList(context, graphics, entries, x, y + 78, leftW, mouseX, mouseY);
            drawDetail(context, graphics, selected, rightX, y + 78, rightW);
        }

        @Override
        public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }
            for (Hit hit : hits) {
                if (!inside(mouseX, mouseY, hit.x(), hit.y(), hit.w(), hit.h())) {
                    continue;
                }
                if (hit.selectIndex() >= 0) {
                    selectedIndex = hit.selectIndex();
                    context.playCommandSound();
                } else {
                    if (TerminalNavigationProfiles.referenceOnlyChapterContext(context)) {
                        context.playRejectedSound();
                        return true;
                    }
                    context.sendAction(PowerGridTerminalIds.TAB, PowerGridTerminalIds.STATUS_ACTION, "");
                }
                return true;
            }
            return false;
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return 402;
        }

        private void drawList(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<PowerGridNetworkSummaryPacket.Entry> entries, int x, int y, int w, int mouseX, int mouseY) {
            Font font = context.minecraft().font;
            graphics.text(font, Component.literal("LOADED NETWORKS"), x, y, ACCENT, false);
            if (entries.isEmpty()) {
                frame(graphics, x, y + 18, w, 46, TerminalUi.MUTED);
                graphics.text(font, Component.literal("No loaded networks synced."), x + 8, y + 29, TerminalUi.MUTED, false);
                return;
            }
            int rowY = y + 18;
            for (int i = 0; i < Math.min(entries.size(), 9); i++) {
                PowerGridNetworkSummaryPacket.Entry entry = entries.get(i);
                boolean selected = i == selectedIndex;
                boolean hovered = inside(mouseX, mouseY, x, rowY, w, 28);
                int color = stateColor(entry.state());
                graphics.fill(x, rowY, x + w, rowY + 28,
                        selected ? 0xCC123241 : hovered ? 0x8812242F : 0x6610242F);
                graphics.outline(x, rowY, w, 28, selected ? color : 0x4438DFF4);
                graphics.fill(x, rowY, x + 3, rowY + 28, color);
                graphics.text(font, Component.literal(entry.networkId().toString().substring(0, 8).toUpperCase()),
                        x + 8, rowY + 5, color, false);
                graphics.text(font, Component.literal(entry.state() + " / " + entry.nodeCount() + " nodes"),
                        x + 8, rowY + 17, TerminalUi.MUTED, false);
                hits.add(new Hit(x, rowY, w, 28, i));
                rowY += 32;
            }
        }

        private void drawDetail(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                PowerGridNetworkSummaryPacket.Entry entry, int x, int y, int w) {
            Font font = context.minecraft().font;
            frame(graphics, x, y, w, 214, entry == null ? TerminalUi.MUTED : stateColor(entry.state()));
            graphics.text(font, Component.literal("NETWORK DETAIL"), x + 10, y + 8, ACCENT, false);
            if (entry == null) {
                graphics.text(font, Component.literal("Select or load a PowerGrid network."), x + 10, y + 28, TerminalUi.MUTED, false);
                return;
            }
            int lineY = y + 28;
            line(graphics, font, x, lineY, "State", entry.state(), stateColor(entry.state())); lineY += 14;
            line(graphics, font, x, lineY, "Quality", entry.quality(), qualityColor(entry.quality())); lineY += 14;
            line(graphics, font, x, lineY, "Dimension", entry.dimension(), TerminalUi.CYAN); lineY += 14;
            line(graphics, font, x, lineY, "Anchor", pos(entry.anchorPos()), TerminalUi.TEXT); lineY += 14;
            line(graphics, font, x, lineY, "Generation", entry.totalGeneration() + " EP/t", TerminalUi.GREEN); lineY += 14;
            line(graphics, font, x, lineY, "Demand", entry.totalDemand() + " EP/t",
                    entry.totalDemand() > entry.totalGeneration() ? TerminalUi.AMBER : TerminalUi.TEXT); lineY += 14;
            line(graphics, font, x, lineY, "Available Draw", entry.availablePower() + " EP", TerminalUi.GREEN); lineY += 14;
            line(graphics, font, x, lineY, "Stored", entry.totalStored() + "/" + entry.totalCapacity() + " EP", TerminalUi.CYAN); lineY += 14;
            line(graphics, font, x, lineY, "Transfer", transfer(entry.transferLimit()), TerminalUi.TEXT); lineY += 14;
            if ("BROWNOUT".equals(entry.state()) || "OVERLOADED".equals(entry.state())) {
                graphics.text(font, Component.literal("ATTENTION: balance generation, reserve, or cable transfer."),
                        x + 10, lineY + 8, TerminalUi.AMBER, false);
            }
        }

        private void line(GuiGraphicsExtractor graphics, Font font, int x, int y, String label, String value, int color) {
            graphics.text(font, Component.literal(label), x + 10, y, TerminalUi.MUTED, false);
            graphics.text(font, Component.literal(value), x + 94, y, color, false);
        }

        private void drawMetric(GuiGraphicsExtractor graphics, Font font, int x, int y, String label, long value, int color) {
            frame(graphics, x, y, 72, 34, color);
            graphics.text(font, Component.literal(label), x + 6, y + 5, TerminalUi.MUTED, false);
            graphics.text(font, Component.literal(String.valueOf(value)), x + 6, y + 18, color, false);
        }

        private void drawAction(TerminalRenderContext context, GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                String label, int mouseX, int mouseY) {
            boolean enabled = !TerminalNavigationProfiles.referenceOnlyChapterContext(context);
            boolean hovered = enabled && inside(mouseX, mouseY, x, y, w, h);
            graphics.fill(x, y, x + w, y + h, enabled ? hovered ? 0xCC123241 : 0x8810242F : 0x66101417);
            graphics.outline(x, y, w, h, enabled ? hovered ? ACCENT : 0x5538DFF4 : 0xFF273136);
            graphics.centeredText(context.minecraft().font, label, x + w / 2, y + 5,
                    enabled ? hovered ? TerminalUi.TEXT : ACCENT : TerminalUi.MUTED);
            hits.add(new Hit(x, y, w, h, -1));
        }

        private void frame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
            graphics.fill(x, y, x + w, y + h, 0x8810242F);
            graphics.outline(x, y, w, h, 0x5538DFF4);
            graphics.fill(x, y, x + Math.max(18, Math.min(w, w / 4)), y + 2, color);
        }

        private static int stateColor(String state) {
            return switch (state) {
                case "STABLE", "CHARGING" -> TerminalUi.GREEN;
                case "DISCHARGING" -> TerminalUi.CYAN;
                case "BROWNOUT", "OVERLOADED" -> TerminalUi.AMBER;
                case "TRIPPED", "EMERGENCY", "OFFLINE" -> TerminalUi.RED;
                default -> ACCENT;
            };
        }

        private static int qualityColor(String quality) {
            return "DIRTY".equals(quality) ? TerminalUi.AMBER : TerminalUi.GREEN;
        }

        private static String nextStep(PowerGridNetworkSummaryPacket packet) {
            if (packet.networks().isEmpty()) {
                return "Build generator + battery, then refresh.";
            }
            long stressed = packet.networks().stream()
                    .filter(entry -> "BROWNOUT".equals(entry.state()) || "OVERLOADED".equals(entry.state()))
                    .count();
            if (stressed > 0) {
                return "Fix stressed grid, then Lens scan the alert.";
            }
            boolean hasHv = packet.networks().stream().anyMatch(entry -> entry.transferLimit() >= 1200L);
            if (!hasHv) {
                return "Route High Voltage Cable to a Factory Substation.";
            }
            boolean hasReserve = packet.networks().stream().anyMatch(entry -> entry.totalCapacity() >= 160000L);
            if (!hasReserve) {
                return "Add a Field Battery Bank reserve.";
            }
            return "Finish grid commissioning and mark factory rooms.";
        }

        private static int nextStepColor(PowerGridNetworkSummaryPacket packet) {
            if (packet.networks().isEmpty()) {
                return TerminalUi.MUTED;
            }
            return packet.networks().stream()
                    .anyMatch(entry -> "BROWNOUT".equals(entry.state()) || "OVERLOADED".equals(entry.state()))
                    ? TerminalUi.AMBER
                    : TerminalUi.CYAN;
        }

        private static String pos(net.minecraft.core.BlockPos pos) {
            return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        }

        private static String transfer(long transferLimit) {
            return transferLimit >= Long.MAX_VALUE / 4L ? "unlimited" : transferLimit + " EP/t";
        }

        private static boolean inside(double mx, double my, int x, int y, int w, int h) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }
    }

    private record Hit(int x, int y, int w, int h, int selectIndex) {
    }
}

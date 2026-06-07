package com.knoxhack.echonexusprotocol.integration;

import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoProvider;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonLink;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echoterminal.client.mission.TerminalMissionBrowser;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.entity.player.Player;

public final class NexusTerminalIntegration {
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
   private static final int ACCENT = 0xFF7DE7FF;

   private NexusTerminalIntegration() {
   }

   public static void register() {
      if (!REGISTERED.compareAndSet(false, true)) {
         return;
      }
      TerminalTabRegistry.register(new NexusTab());
      TerminalTabRegistry.register(new NexusFieldTab());
      TerminalTabRegistry.register(new NexusFieldMapTab());
      TerminalNavigationProfiles.register(NexusTerminalIds.RESEARCH_TAB,
         TerminalNavigationProfile.chapter("nexus", "Chapter 4: Nexus Protocol", "C4", 400));
      TerminalNavigationProfiles.register(NexusTerminalIds.FIELD_TAB,
         TerminalNavigationProfile.chapter("nexus", "Chapter 4: Nexus Protocol", "C4", 401));
      TerminalNavigationProfiles.register(NexusTerminalIds.FIELD_MAP_TAB,
         TerminalNavigationProfile.chapter("nexus", "Chapter 4: Nexus Protocol", "C4", 402));
      TerminalAddonInfoRegistry.register(new NexusAddonInfoProvider());
   }

   private static final class NexusAddonInfoProvider implements TerminalAddonInfoProvider {
      @Override
      public String chapterId() {
         return NexusTerminalIds.CHAPTER_ID.getPath();
      }

      @Override
      public TerminalAddonInfo info(Player player) {
         if (player == null) {
            return new TerminalAddonInfo(
               "Chapter IV research, field stability, local map, and ending readiness.",
               List.of(new TerminalAddonMetric("Signal", "OFFLINE", "waiting for player telemetry", ACCENT)),
               List.of(new TerminalAddonSection("Research Feed",
                  List.of("Open Nexus Protocol after player telemetry is available."))),
               links(),
               guide());
         }
         NexusPlayerData data = NexusPlayerData.get(player);
         NexusWorldData.FieldState state = NexusWorldData.FieldState.fromValue(data.telemetryFieldValue());
         return new TerminalAddonInfo(
            "Chapter IV research, field stability, local map, and ending readiness.",
            List.of(
               new TerminalAddonMetric("Research", data.researchUnlocks().size() + "/6", "unlocked protocols", ACCENT),
               new TerminalAddonMetric("Field", state.name(), data.telemetryFieldValue() + "% local stability", stateColor(state)),
               new TerminalAddonMetric("Corruption", data.telemetryCorruptionPressure() + "%",
                  data.telemetryActiveStorm() ? "storm active" : "local pressure", data.telemetryCorruptionPressure() >= 45 ? TerminalUi.RED : TerminalUi.AMBER),
               new TerminalAddonMetric("Ending", endingLabel(data.telemetryWorldEndingState()),
                  data.telemetryWorldGuardianDefeated() ? "guardian defeated" : "guardian online",
                  data.telemetryWorldEndingState().isBlank() ? TerminalUi.MUTED : TerminalUi.AMBER)),
            List.of(new TerminalAddonSection("Field Feed", List.of(
               "Scans: " + data.scanCount(),
               "Blackbox fragments: " + data.blackboxFragments(),
               "Monolith: " + (data.telemetryWorldMonolithActivated() ? "activated" : "sealed"),
               "Reality tears: " + data.telemetryRealityTears()))),
            links(),
            guide());
      }

      private static TerminalAddonGuide guide() {
         return TerminalAddonGuide.mainline(4, 40, "Late story",
            "Start Nexus Protocol after Stationfall has exposed the blackbox handoff and the world can support field instability.",
            List.of(
               "Open Nexus Protocol to review research chain readiness.",
               "Watch Nexus Field before running risky charge systems.",
               "Use Field Map to avoid collapsed or critical chunks."));
      }

      private static List<TerminalAddonLink> links() {
         return List.of(
            new TerminalAddonLink(NexusTerminalIds.RESEARCH_TAB, "Nexus Protocol",
               "Research chain and mission records", ACCENT),
            new TerminalAddonLink(NexusTerminalIds.FIELD_TAB, "Nexus Field",
               "Live chunk stability and corruption telemetry", ACCENT),
            new TerminalAddonLink(NexusTerminalIds.FIELD_MAP_TAB, "Field Map",
               "Nearby field-state grid", ACCENT));
      }

      private static int stateColor(NexusWorldData.FieldState state) {
         return switch (state) {
            case STABLE -> TerminalUi.GREEN;
            case UNSTABLE -> TerminalUi.CYAN;
            case FRACTURED -> TerminalUi.AMBER;
            case CRITICAL, COLLAPSED -> TerminalUi.RED;
         };
      }

      private static String endingLabel(String ending) {
         return ending == null || ending.isBlank() ? "UNCOMMITTED" : ending.toUpperCase(java.util.Locale.ROOT);
      }
   }

   private static final class NexusTab implements ClientTerminalTab {
      private final TerminalTabDescriptor descriptor =
         new TerminalTabDescriptor(NexusTerminalIds.RESEARCH_TAB, "NEXUS PROTOCOL", 400, ACCENT);
      private final TerminalTabChrome chrome =
         TerminalTabChrome.of("Nexus Protocol", TerminalTabChrome.GROUP_NEXUS, "NX",
            "Chapter IV research chain", 400);
      private final TerminalMissionBrowser browser =
         new TerminalMissionBrowser(NexusTerminalMissionProvider.INSTANCE, descriptor.id(), true,
            TerminalMissionBrowser.ActionMode.TRACKING_ONLY,
            TerminalMissionBrowser.InitialTreeFocus.ALIGN_SELECTED_TOP);

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
         browser.onSelected(context);
      }

      @Override
      public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int mouseX, int mouseY, float partialTick) {
         browser.render(context, graphics, mouseX, mouseY, partialTick);
      }

      @Override
      public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
         return browser.mouseClicked(context, mouseX, mouseY, button);
      }

      @Override
      public boolean mouseDragged(TerminalRenderContext context, double mouseX, double mouseY, int button,
            double dragX, double dragY) {
         return browser.mouseDragged(context, mouseX, mouseY, button, dragX, dragY);
      }

      @Override
      public boolean mouseReleased(TerminalRenderContext context, double mouseX, double mouseY, int button) {
         return browser.mouseReleased(context, mouseX, mouseY, button);
      }

      @Override
      public boolean mouseScrolled(TerminalRenderContext context, double mouseX, double mouseY, double delta) {
         return browser.mouseScrolled(context, mouseX, mouseY, delta);
      }

      @Override
      public boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
         return browser.keyPressed(context, event);
      }

      @Override
      public boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
         return browser.charTyped(context, event);
      }

      @Override
      public int contentHeight(TerminalRenderContext context) {
         return browser.contentHeight(context);
      }
   }

   private static final class NexusFieldTab implements ClientTerminalTab {
      private final TerminalTabDescriptor descriptor =
         new TerminalTabDescriptor(NexusTerminalIds.FIELD_TAB, "NEXUS FIELD", 401, ACCENT);
      private final TerminalTabChrome chrome =
         TerminalTabChrome.of("Nexus Field", TerminalTabChrome.GROUP_NEXUS, "FLD",
            "Live chunk stability and corruption telemetry", 401);

      @Override
      public TerminalTabDescriptor descriptor() {
         return descriptor;
      }

      @Override
      public TerminalTabChrome chrome() {
         return chrome;
      }

      @Override
      public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int mouseX, int mouseY, float partialTick) {
         NexusPlayerData data = NexusPlayerData.get(context.player());
         int x = context.contentX() + 10;
         int y = context.contentY() - context.scrollY() + 8;
         int width = Math.max(220, context.contentWidth() - 20);
         int field = data.telemetryFieldValue();
         NexusWorldData.FieldState state = NexusWorldData.FieldState.fromValue(field);
         int color = stateColor(state);
         String stateLabel = state.name();

         int cy = TerminalUi.flatDataPanel(context, graphics, x, y, width, 118,
            "NEXUS FIELD SCAN", stateLabel + " // CHUNK LOCAL", color);
         TerminalUi.wrap(context, graphics,
            "ECHO-7 // The current chunk is being sampled from synced Nexus telemetry. Treat these values as the base safety readout before running dirty charge, reactors, or Protocol Seals.",
            x + 14, cy, width - 28, TerminalUi.MUTED);
         TerminalUi.meter(context, graphics, x + 14, y + 68, width - 28, "FIELD", field, color);
         TerminalUi.meter(context, graphics, x + 14, y + 88, width - 28, "CORRUPTION", data.telemetryCorruptionPressure(),
            data.telemetryCorruptionPressure() >= 45 ? TerminalUi.RED : TerminalUi.AMBER);

         int rowY = y + 132;
         int leftW = Math.max(150, (width - 10) / 2);
         int rightX = x + leftW + 10;
         int rightW = Math.max(120, width - leftW - 10);
         rowY = TerminalUi.sectionHeader(context, graphics, "LIVE FLAGS", "local chunk", x, rowY, width, color);
         int leftY = rowY;
         leftY = TerminalUi.statusLineRow(context, graphics, x, leftY, leftW, null, "Storm",
            data.telemetryActiveStorm() ? "ACTIVE" : "CLEAR", data.telemetryActiveStorm() ? TerminalUi.RED : TerminalUi.GREEN);
         leftY = TerminalUi.statusLineRow(context, graphics, x, leftY, leftW, null, "Reality tears",
            Integer.toString(data.telemetryRealityTears()), data.telemetryRealityTears() > 0 ? TerminalUi.RED : TerminalUi.GREEN);
         leftY = TerminalUi.statusLineRow(context, graphics, x, leftY, leftW, null, "Quarantine",
            data.telemetryQuarantineTicks() > 0 ? seconds(data.telemetryQuarantineTicks()) + "s" : "NONE",
            data.telemetryQuarantineTicks() > 0 ? TerminalUi.GREEN : TerminalUi.MUTED);

         int rightY = rowY;
         rightY = TerminalUi.statusLineRow(context, graphics, rightX, rightY, rightW, null, "Monolith",
            data.telemetryWorldMonolithActivated() ? "ACTIVATED" : "SEALED",
            data.telemetryWorldMonolithActivated() ? TerminalUi.AMBER : TerminalUi.MUTED);
         rightY = TerminalUi.statusLineRow(context, graphics, rightX, rightY, rightW, null, "Warden",
            data.telemetryWorldWardenDefeated() ? "DEFEATED" : "CONTAINED",
            data.telemetryWorldWardenDefeated() ? TerminalUi.GREEN : TerminalUi.MUTED);
         rightY = TerminalUi.statusLineRow(context, graphics, rightX, rightY, rightW, null, "Guardian",
            data.telemetryWorldGuardianDefeated() ? "DEFEATED" : "ONLINE",
            data.telemetryWorldGuardianDefeated() ? TerminalUi.GREEN : TerminalUi.AMBER);
         TerminalUi.statusLineRow(context, graphics, rightX, rightY, rightW, null, "Ending",
            endingLabel(data.telemetryWorldEndingState()), data.telemetryWorldEndingState().isBlank() ? TerminalUi.MUTED : TerminalUi.AMBER);

         int guidanceY = Math.max(leftY, rightY) + 14;
         guidanceY = TerminalUi.sectionHeader(context, graphics, "FIELD GUIDANCE", stateLabel, x, guidanceY, width, color);
         String[] guidance = guidance(state, data.telemetryCorruptionPressure(), data.telemetryActiveStorm(), data.telemetryRealityTears());
         for (int i = 0; i < guidance.length; i += 2) {
            guidanceY = TerminalUi.objectiveRow(context, graphics, x, guidanceY, width,
               guidance[i], guidance[i + 1], state == NexusWorldData.FieldState.STABLE && i == 0, color);
         }
      }

      @Override
      public int contentHeight(TerminalRenderContext context) {
         return 390;
      }

      private static int stateColor(NexusWorldData.FieldState state) {
         return switch (state) {
            case STABLE -> TerminalUi.GREEN;
            case UNSTABLE -> TerminalUi.CYAN;
            case FRACTURED -> TerminalUi.AMBER;
            case CRITICAL, COLLAPSED -> TerminalUi.RED;
         };
      }

      private static String[] guidance(NexusWorldData.FieldState state, int corruption, boolean storm, int tears) {
         return switch (state) {
            case STABLE -> new String[]{
               "Maintain charge storage", "Keep tanks buffered and continue the active Nexus mission.",
               "Watch dirty inputs", corruption > 0 ? "Run a Corruption Filter before pressure climbs." : "Field is clean enough for normal processing."
            };
            case UNSTABLE -> new String[]{
               "Run a Field Stabilizer", "Raise the local field before starting long machine chains.",
               "Filter contamination", "Deploy a Corruption Filter near Recycler or Reactor output."
            };
            case FRACTURED -> new String[]{
               "Deploy Quarantine/Purify seals", "Stop spread first, then clean corrupted blocks.",
               "Reduce dirty processing", "Pause Static Fluid, Reactor fuel, and Collapse seals until pressure falls."
            };
            case CRITICAL -> new String[]{
               "Use Purity Charges", storm ? "Anomaly storm active. Clean the chunk before fighting or crafting." : "Prevent the next anomaly storm by restoring field value.",
               "Move vital machines", "Relocate storage and critical processing until stability is above 40."
            };
            case COLLAPSED -> new String[]{
               "Evacuate the chunk", tears > 0 ? "Reality tears detected. Leave before continuing Core work." : "Collapse threshold reached. Leave and return with purity tools.",
               "Disable forbidden systems", "Stop Corruption Reactor output and Collapse seals, then purify from the edge inward."
            };
         };
      }

      private static int seconds(int ticks) {
         return Math.max(1, ticks / 20);
      }

      private static String endingLabel(String ending) {
         return ending == null || ending.isBlank() ? "UNCOMMITTED" : ending.toUpperCase(java.util.Locale.ROOT);
      }
   }

   private static final class NexusFieldMapTab implements ClientTerminalTab {
      private final TerminalTabDescriptor descriptor =
         new TerminalTabDescriptor(NexusTerminalIds.FIELD_MAP_TAB, "NEXUS FIELD MAP", 402, ACCENT);
      private final TerminalTabChrome chrome =
         TerminalTabChrome.of("Nexus Field Map", TerminalTabChrome.GROUP_NEXUS, "MAP",
            "Nearby chunk field-state grid", 402);

      @Override
      public TerminalTabDescriptor descriptor() {
         return descriptor;
      }

      @Override
      public TerminalTabChrome chrome() {
         return chrome;
      }

      @Override
      public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int mouseX, int mouseY, float partialTick) {
         NexusPlayerData data = NexusPlayerData.get(context.player());
         NexusFieldMapPlanner.Analysis analysis = NexusFieldMapPlanner.analyze(data);
         int x = context.contentX() + 10;
         int y = context.contentY() - context.scrollY() + 8;
         int width = Math.max(250, context.contentWidth() - 20);
         int cy = TerminalUi.flatDataPanel(context, graphics, x, y, width, 84,
            "LOCAL FIELD MAP", "5x5 CHUNK TELEMETRY", ACCENT);
         TerminalUi.wrap(context, graphics,
            "ECHO-7 // Center cell is your current chunk. The map now prioritizes a safe work chunk and a recovery target before you run dirty charge, reactors, or Core work.",
            x + 14, cy, width - 28, TerminalUi.MUTED);

         int gridY = y + 102;
         int cell = Math.max(46, Math.min(52, (width - 18) / NexusPlayerData.FIELD_MAP_DIAMETER));
         int gridW = cell * NexusPlayerData.FIELD_MAP_DIAMETER;
         int gridX = x + Math.max(0, (width - gridW) / 2);
         var font = Minecraft.getInstance().font;
         int index = 0;
         for (int row = 0; row < NexusPlayerData.FIELD_MAP_DIAMETER; row++) {
            for (int col = 0; col < NexusPlayerData.FIELD_MAP_DIAMETER; col++) {
               NexusFieldMapPlanner.CellRisk cellRisk = analysis.cell(index);
               int field = cellRisk.field();
               NexusWorldData.FieldState state = cellRisk.state();
               int fill = cellFill(state);
               int border = cellBorder(analysis, cellRisk, state);
               int cx = gridX + col * cell;
               int cz = gridY + row * cell;
               graphics.fill(cx, cz, cx + cell - 2, cz + cell - 2, fill);
               graphics.fill(cx, cz, cx + cell - 2, cz + 1, border);
               graphics.fill(cx, cz + cell - 3, cx + cell - 2, cz + cell - 2, border);
               graphics.fill(cx, cz, cx + 1, cz + cell - 2, border);
               graphics.fill(cx + cell - 3, cz, cx + cell - 2, cz + cell - 2, border);
               if (cellRisk.corruption() > 0) {
                  int barWidth = Math.max(2, (cell - 7) * cellRisk.corruption() / 100);
                  graphics.fill(cx + 3, cz + cell - 7, cx + 3 + barWidth, cz + cell - 5,
                     cellRisk.corruption() >= 45 ? TerminalUi.RED : TerminalUi.AMBER);
               }
               String label = abbreviation(state) + (cellRisk.isCenter() ? "*" : "");
               graphics.text(font, label, cx + 5, cz + 5, 0xFFFFFFFF, false);
               graphics.text(font, field + "%", cx + 5, cz + 17, 0xFFBFEFFF, false);
               String flags = flags(cellRisk);
               graphics.text(font, flags.isBlank() ? "R" + cellRisk.risk() : flags, cx + 5, cz + 29,
                  flags.isBlank() ? 0xFFBFEFFF : 0xFFFFA7D8, false);
               index++;
            }
         }

         int legendY = gridY + gridW + 16;
         legendY = TerminalUi.sectionHeader(context, graphics, "RECOVERY READOUT", "map guidance", x, legendY, width, ACCENT);
         legendY = TerminalUi.objectiveRow(context, graphics, x, legendY, width,
            "Safest adjacent", analysis.safestAdjacentGuidance(), false, TerminalUi.GREEN);
         legendY = TerminalUi.objectiveRow(context, graphics, x, legendY, width,
            "Priority recovery", analysis.priorityRecoveryGuidance(), false, recoveryColor(analysis));
         TerminalUi.objectiveRow(context, graphics, x, legendY, width,
            "Hazard summary", analysis.hazardSummary(), !analysis.hasHazards(), analysis.hasHazards() ? TerminalUi.RED : TerminalUi.GREEN);
      }

      @Override
      public int contentHeight(TerminalRenderContext context) {
         return 540;
      }

      private static int cellFill(NexusWorldData.FieldState state) {
         return switch (state) {
            case STABLE -> 0x5529D67A;
            case UNSTABLE -> 0x553AB7FF;
            case FRACTURED -> 0x66FFB84A;
            case CRITICAL -> 0x66FF5C7A;
            case COLLAPSED -> 0x88340B44;
         };
      }

      private static int stateColor(NexusWorldData.FieldState state) {
         return switch (state) {
            case STABLE -> TerminalUi.GREEN;
            case UNSTABLE -> 0xFF66E8FF;
            case FRACTURED -> TerminalUi.AMBER;
            case CRITICAL, COLLAPSED -> TerminalUi.RED;
         };
      }

      private static String abbreviation(NexusWorldData.FieldState state) {
         return switch (state) {
            case STABLE -> "STB";
            case UNSTABLE -> "UNS";
            case FRACTURED -> "FRC";
            case CRITICAL -> "CRT";
            case COLLAPSED -> "COL";
         };
      }

      private static int cellBorder(NexusFieldMapPlanner.Analysis analysis, NexusFieldMapPlanner.CellRisk cell, NexusWorldData.FieldState state) {
         if (cell.isCenter()) {
            return 0xFFFFFFFF;
         }
         if (analysis.highestRisk() != null && cell.index() == analysis.highestRisk().index()) {
            return TerminalUi.RED;
         }
         if (analysis.safestAdjacent() != null && cell.index() == analysis.safestAdjacent().index()) {
            return TerminalUi.GREEN;
         }
         return stateColor(state);
      }

      private static String flags(NexusFieldMapPlanner.CellRisk cell) {
         StringBuilder builder = new StringBuilder();
         if (cell.storm()) {
            builder.append("STM");
         }
         if (cell.tears() > 0) {
            if (!builder.isEmpty()) {
               builder.append(' ');
            }
            builder.append('T').append(cell.tears());
         }
         return builder.toString();
      }

      private static int recoveryColor(NexusFieldMapPlanner.Analysis analysis) {
         NexusFieldMapPlanner.CellRisk cell = analysis.highestRisk();
         return cell != null && cell.risk() >= 100 ? TerminalUi.RED : TerminalUi.AMBER;
      }
   }
}

package com.knoxhack.echorendercore.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echorendercore.EchoRenderCore;
import com.knoxhack.echorendercore.profile.CreatorExportIndex;
import com.knoxhack.echorendercore.profile.CreatorPackArtifact;
import com.knoxhack.echorendercore.profile.CreatorProfileAudit;
import com.knoxhack.echorendercore.profile.CreatorProfileCard;
import com.knoxhack.echorendercore.profile.CreatorProfileDraft;
import com.knoxhack.echorendercore.profile.ProfileScreenshotCaptureResult;
import com.knoxhack.echorendercore.profile.RenderCoreCreatorPackExporter;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import com.knoxhack.echorendercore.profile.RenderCoreVector;
import com.knoxhack.echorendercore.profile.VisualProfile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class RenderCoreCreatorWorkbenchScreen extends Screen {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final int BG = 0xF2050710;
   private static final int PANEL = 0xF00A1020;
   private static final int ROW = 0xAA102136;
   private static final int CYAN = 0xFF36E8FF;
   private static final int MAGENTA = 0xFFFF45F6;
   private static final int TEXT = 0xFFE9FBFF;
   private static final int MUTED = 0xFF8CA7B5;
   private static final int WARN = 0xFFFFD166;
   private static final int ERROR = 0xFFFF6B9A;

   private final CreatorExportIndex export;
   private final List<Hitbox> hitboxes = new ArrayList<>();
   private final Map<Identifier, CreatorProfileDraft> drafts = new HashMap<>();
   private String search = "";
   private String statusLine = "Draft edits save to generated output only.";
   private int panelX;
   private int panelY;
   private int panelW;
   private int panelH;
   private int selected;
   private int firstVisible;
   private EditField activeField = EditField.SEARCH;

   public RenderCoreCreatorWorkbenchScreen(CreatorExportIndex export) {
      super(Component.literal("RenderCore Creator Workbench"));
      this.export = export == null ? RenderCoreCreatorPackExporter.export(RenderCoreProfiles.loaded()) : export;
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      hitboxes.clear();
      layout();
      Font font = Minecraft.getInstance().font;
      List<CreatorProfileCard> cards = filteredCards();
      selected = clamp(selected, 0, Math.max(0, cards.size() - 1));
      firstVisible = clamp(firstVisible, 0, Math.max(0, cards.size() - visibleRows()));
      if (selected < firstVisible) {
         firstVisible = selected;
      } else if (selected >= firstVisible + visibleRows()) {
         firstVisible = Math.max(0, selected - visibleRows() + 1);
      }

      graphics.fill(0, 0, width, height, 0xDD01040A);
      EchoCyberGlassUi.panel(graphics, panelX, panelY, panelW, panelH, BG, CYAN);
      graphics.fill(panelX, panelY, panelX + panelW, panelY + 3, CYAN);
      graphics.fill(panelX + panelW / 2, panelY, panelX + panelW, panelY + 3, MAGENTA);
      graphics.text(font, "ECHO RENDER CORE // CREATOR WORKBENCH", panelX + 14, panelY + 12, CYAN, true);
      graphics.text(font, "ESC", panelX + panelW - 36, panelY + panelH - 18, MUTED, false);

      drawSearch(graphics, font);
      drawSummary(graphics, font);
      drawList(graphics, font, cards, mouseX, mouseY);
      drawDetails(graphics, font, cards.isEmpty() ? null : cards.get(selected));
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      for (Hitbox hitbox : List.copyOf(hitboxes)) {
         if (inside(event.x(), event.y(), hitbox.x(), hitbox.y(), hitbox.w(), hitbox.h())) {
            hitbox.action().run();
            return true;
         }
      }
      return super.mouseClicked(event, doubleClick);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      List<CreatorProfileCard> cards = filteredCards();
      int maxFirst = Math.max(0, cards.size() - visibleRows());
      if (scrollY < 0 && firstVisible < maxFirst) {
         firstVisible++;
         selected = clamp(selected, firstVisible, Math.max(firstVisible, firstVisible + visibleRows() - 1));
         return true;
      }
      if (scrollY > 0 && firstVisible > 0) {
         firstVisible--;
         selected = clamp(selected, firstVisible, Math.max(firstVisible, firstVisible + visibleRows() - 1));
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      List<CreatorProfileCard> cards = filteredCards();
      if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
         return backspace();
      }
      if (event.key() == GLFW.GLFW_KEY_UP && selected > 0) {
         selected--;
         return true;
      }
      if (event.key() == GLFW.GLFW_KEY_DOWN && selected + 1 < cards.size()) {
         selected++;
         return true;
      }
      if (event.key() == GLFW.GLFW_KEY_PAGE_UP) {
         selected = Math.max(0, selected - visibleRows());
         firstVisible = Math.max(0, firstVisible - visibleRows());
         return true;
      }
      if (event.key() == GLFW.GLFW_KEY_PAGE_DOWN) {
         selected = Math.min(Math.max(0, cards.size() - 1), selected + visibleRows());
         firstVisible = Math.min(Math.max(0, cards.size() - visibleRows()), firstVisible + visibleRows());
         return true;
      }
      if (event.key() == GLFW.GLFW_KEY_ENTER && !cards.isEmpty()) {
         ensureDraft(cards.get(selected));
         activeField = EditField.TITLE;
         return true;
      }
      return super.keyPressed(event);
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      if (event == null || !event.isAllowedChatCharacter() || search.length() >= 40) {
         return false;
      }
      String typed = event.codepointAsString();
      if (typed == null || (typed.isBlank() && activeField == EditField.SEARCH)) {
         return false;
      }
      return insertText(typed);
   }

   private void drawSearch(GuiGraphicsExtractor graphics, Font font) {
      int x = panelX + 14;
      int y = panelY + 34;
      int w = Math.min(260, panelW / 2 - 24);
      graphics.fill(x, y, x + w, y + 18, 0xDD061321);
      graphics.outline(x, y, w, 18, activeField == EditField.SEARCH ? CYAN : search.isEmpty() ? 0x5536E8FF : 0xAA36E8FF);
      graphics.text(font, search.isEmpty() ? "search id / namespace / effect" : search, x + 6, y + 5, search.isEmpty() ? MUTED : TEXT, false);
      hitboxes.add(new Hitbox(x, y, w, 18, () -> activeField = EditField.SEARCH));
      if (!search.isEmpty()) {
         int clearX = x + w - 36;
         graphics.fill(clearX, y + 2, clearX + 30, y + 16, 0xAA1D1730);
         graphics.centeredText(font, "clear", clearX + 15, y + 5, MAGENTA);
         hitboxes.add(new Hitbox(clearX, y + 2, 30, 14, () -> {
            search = "";
            selected = 0;
            firstVisible = 0;
         }));
      }
   }

   private void drawSummary(GuiGraphicsExtractor graphics, Font font) {
      String line = "profiles " + export.cards().size()
         + " / migration " + export.cards().stream().filter(CreatorProfileCard::migrationRequired).count()
         + " / screenshots " + export.cards().stream().filter(CreatorProfileCard::screenshotAvailable).count()
         + " / qa blockers " + export.visualQa().totalBlockerCount()
         + " / screen qa " + export.visualQa().screenChromeEvidenceCount() + "/" + export.visualQa().screenChromeEvidence().size()
         + " / schema " + export.manifest().schemaVersion()
         + " / addons " + export.addonIntegrations().size();
      graphics.text(font, trim(font, line, panelW / 2 - 22), panelX + panelW / 2 + 8, panelY + 39, MUTED, false);
   }

   private void drawList(GuiGraphicsExtractor graphics, Font font, List<CreatorProfileCard> cards, int mouseX, int mouseY) {
      int x = panelX + 14;
      int y = panelY + 62;
      int w = listWidth();
      int rowH = 30;
      graphics.enableScissor(x, y, x + w, panelY + panelH - 28);
      if (cards.isEmpty()) {
         graphics.fill(x, y, x + w, y + 28, ROW);
         graphics.text(font, "No profiles match the current filter.", x + 8, y + 10, MUTED, false);
      }
      int visible = Math.min(cards.size() - firstVisible, visibleRows());
      for (int i = 0; i < visible; i++) {
         int index = firstVisible + i;
         CreatorProfileCard card = cards.get(index);
         int rowY = y + i * rowH;
         boolean active = index == selected;
         boolean hover = inside(mouseX, mouseY, x, rowY, w, rowH - 3);
         graphics.fill(x, rowY, x + w, rowY + rowH - 3, active ? 0xEE123241 : hover ? 0xCC102630 : ROW);
         EchoCyberGlassUi.frame(graphics, x, rowY, w, rowH - 3, active ? CYAN : 0x3336E8FF);
         int status = card.migrationRequired() || card.validationErrorCount() > 0 ? ERROR
            : card.validationWarningCount() > 0 || card.performanceWarningCount() > 0 ? WARN : CYAN;
         graphics.text(font, trim(font, card.profileId().toString(), w - 16), x + 7, rowY + 5, status, false);
         graphics.text(font, trim(font, "fx " + card.effectPresets() + " / layers " + card.layerCount(), w - 16),
            x + 7, rowY + 17, MUTED, false);
         hitboxes.add(new Hitbox(x, rowY, w, rowH - 3, () -> selected = index));
      }
      graphics.disableScissor();
   }

   private void drawDetails(GuiGraphicsExtractor graphics, Font font, CreatorProfileCard card) {
      int x = panelX + 28 + listWidth();
      int y = panelY + 62;
      int w = panelW - listWidth() - 42;
      int h = panelH - 94;
      EchoCyberGlassUi.panel(graphics, x, y, w, h, PANEL, 0x665A7DFF);
      if (card == null) {
         graphics.text(font, "No profile selected.", x + 12, y + 14, MUTED, false);
         return;
      }
      graphics.text(font, trim(font, card.title(), w - 24), x + 12, y + 12, TEXT, true);
      graphics.text(font, card.profileId().toString(), x + 12, y + 26, MUTED, false);
      button(graphics, font, "draft", x + w - 156, y + 10, 42, () -> ensureDraft(card));
      button(graphics, font, "save", x + w - 110, y + 10, 36, () -> saveDraft(card));
      button(graphics, font, "shot", x + w - 70, y + 10, 36, () -> captureThumbnail(card));

      int thumbY = y + 44;
      graphics.fill(x + 12, thumbY, x + w - 12, thumbY + 58, 0xAA040A14);
      graphics.outline(x + 12, thumbY, w - 24, 58, card.screenshotAvailable() ? CYAN : 0x555A7DFF);
      CreatorProfileDraft draft = drafts.get(card.profileId());
      String screenshotLabel = draft != null && !draft.screenshotPath().isBlank()
         ? "draft screenshot: " + draft.screenshotPath()
         : card.screenshotAvailable() ? "screenshot: " + card.screenshotProvider() : "metadata preview card";
      graphics.centeredText(font, trim(font, screenshotLabel, w - 36),
         x + w / 2, thumbY + 13, card.screenshotAvailable() ? CYAN : MUTED);
      graphics.centeredText(font, "schema " + card.schemaVersion() + " -> " + export.manifest().schemaVersion()
         + " / " + (card.migrationRequired() ? "migration required" : "runtime ready"),
         x + w / 2, thumbY + 30, card.migrationRequired() ? ERROR : TEXT);

      int lineY = thumbY + 72;
      lineY = detailLine(graphics, font, x + 12, lineY, w - 24, "Layers", card.layerCount() + "  Materials " + card.materialCount());
      lineY = detailLine(graphics, font, x + 12, lineY, w - 24, "Effects", String.join(", ", card.effectPresets()));
      lineY = detailLine(graphics, font, x + 12, lineY, w - 24, "Diagnostics",
         card.validationWarningCount() + " warning(s), " + card.validationErrorCount() + " error(s), perf " + card.performanceWarningCount());
      lineY = detailLine(graphics, font, x + 12, lineY, w - 24, "Certification",
         export.certification().status().id() + " / visual QA blockers " + export.visualQa().totalBlockerCount());
      lineY = detailLine(graphics, font, x + 12, lineY, w - 24, "Screen QA",
         export.visualQa().screenChromeEvidenceCount() + "/" + export.visualQa().screenChromeEvidence().size()
            + " captured / blockers " + export.visualQa().screenChromeBlockers().size());
      lineY = drawScreenChromeStyleChips(graphics, font, x + 12, lineY + 2, w - 24);
      lineY = detailLine(graphics, font, x + 12, lineY, w - 24, "Artifact", card.suggestedArtifactPath());
      lineY = drawDraftEditor(graphics, font, card, draft, x + 12, lineY + 6, w - 24);

      CreatorProfileAudit audit = export.audits().stream()
         .filter(value -> card.profileId().equals(value.profileId()))
         .findFirst()
         .orElse(null);
      if (audit != null && !audit.validationIssues().isEmpty()) {
         graphics.text(font, "Latest diagnostics", x + 12, lineY + 4, WARN, true);
         lineY += 18;
         for (var issue : audit.validationIssues().stream().limit(4).toList()) {
         graphics.textWithWordWrap(font, Component.literal(issue.code() + ": " + issue.message()), x + 12, lineY, w - 24,
               issue.severity().name().equals("ERROR") ? ERROR : WARN);
            lineY += 22;
         }
      }
      graphics.text(font, trim(font, statusLine, w - 24), x + 12, y + h - 14, MUTED, false);
   }

   private int drawDraftEditor(GuiGraphicsExtractor graphics, Font font, CreatorProfileCard card, CreatorProfileDraft draft, int x, int y, int w) {
      graphics.text(font, "Draft editor", x, y, MAGENTA, true);
      y += 14;
      if (draft == null) {
         graphics.text(font, "Create a draft to edit metadata and save generated V12 JSON.", x, y, MUTED, false);
         return y + 16;
      }
      y = draftField(graphics, font, x, y, w, "Title", draft.title(), EditField.TITLE);
      y = draftField(graphics, font, x, y, w, "Notes", draft.notes(), EditField.NOTES);
      y = draftField(graphics, font, x, y, w, "Shot", draft.screenshotPath(), EditField.SCREENSHOT);
      graphics.text(font, "Presets", x, y + 4, CYAN, false);
      int bx = x + 48;
      button(graphics, font, "fx neon", bx, y + 2, 50, () -> updateDraft(card.profileId(), drafts.get(card.profileId()).withProfileEffectPreset("neon")));
      button(graphics, font, "mat holo", bx + 54, y + 2, 54, () -> updateDraft(card.profileId(), drafts.get(card.profileId()).withMaterialEffectPreset("workbench_material", "hologram")));
      button(graphics, font, "layer hud", bx + 112, y + 2, 60, () -> updateDraft(card.profileId(), drafts.get(card.profileId()).withLayerEffectPreset("workbench_layer", "terminal_hud")));
      y += 18;
      graphics.text(font, "Create", x, y + 4, CYAN, false);
      button(graphics, font, "anchor", bx, y + 2, 48, () -> updateDraft(card.profileId(), drafts.get(card.profileId()).withAnchor("workbench_anchor", new RenderCoreVector(0.0F, 1.0F, 0.0F))));
      button(graphics, font, "include", bx + 52, y + 2, 52, () -> updateDraft(card.profileId(), drafts.get(card.profileId()).withInclude(Identifier.fromNamespaceAndPath(EchoRenderCore.MODID, "v13_neon_cube_core"))));
      button(graphics, font, "clear shot", bx + 108, y + 2, 58, () -> updateDraft(card.profileId(), drafts.get(card.profileId()).withScreenshotPath("")));
      graphics.text(font, draft.dirty() ? "dirty" : "saved", bx + 172, y + 6, draft.dirty() ? WARN : CYAN, false);
      return y + 22;
   }

   private int draftField(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, String label, String value, EditField field) {
      graphics.text(font, label.toUpperCase(Locale.ROOT), x, y + 4, CYAN, false);
      int fieldX = x + 48;
      int fieldW = Math.max(40, w - 50);
      graphics.fill(fieldX, y, fieldX + fieldW, y + 16, 0xAA061321);
      graphics.outline(fieldX, y, fieldW, 16, activeField == field ? MAGENTA : 0x445A7DFF);
      graphics.text(font, trim(font, value == null || value.isBlank() ? "(empty)" : value, fieldW - 8), fieldX + 4, y + 4,
         value == null || value.isBlank() ? MUTED : TEXT, false);
      hitboxes.add(new Hitbox(fieldX, y, fieldW, 16, () -> activeField = field));
      return y + 18;
   }

   private int drawScreenChromeStyleChips(GuiGraphicsExtractor graphics, Font font, int x, int y, int w) {
      List<String> styles = List.of("CYBERGLASS", "TERMINAL", "HOLOGRAM", "NEON", "MINIMAL");
      int chipX = x;
      int chipY = y;
      for (String style : styles) {
         long count = export.visualQa().screenChromeEvidence().stream()
            .filter(evidence -> style.equals(evidence.chromeStyle()))
            .count();
         String label = style + " " + count;
         int chipW = Math.min(w, Math.max(54, font.width(label) + 10));
         if (chipX > x && chipX + chipW > x + w) {
            chipX = x;
            chipY += 16;
         }
         int color = screenChromeStyleColor(style);
         graphics.fill(chipX, chipY, chipX + chipW, chipY + 13, 0xAA061321);
         graphics.outline(chipX, chipY, chipW, 13, color);
         graphics.centeredText(font, label, chipX + chipW / 2, chipY + 3, color);
         chipX += chipW + 5;
      }
      return chipY + 17;
   }

   private static int screenChromeStyleColor(String style) {
      return switch (style) {
         case "TERMINAL" -> 0xFF66FFB8;
         case "HOLOGRAM" -> 0xFF5A9DFF;
         case "NEON" -> MAGENTA;
         case "MINIMAL" -> MUTED;
         default -> CYAN;
      };
   }

   private void button(GuiGraphicsExtractor graphics, Font font, String label, int x, int y, int w, Runnable action) {
      EchoCyberGlassUi.button(graphics, font, x, y, w, 14, label, false, true, 0x775A7DFF);
      hitboxes.add(new Hitbox(x, y, w, 14, action));
   }

   private int detailLine(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, String label, String value) {
      graphics.text(font, label.toUpperCase(Locale.ROOT), x, y, CYAN, false);
      graphics.text(font, trim(font, value, w - 86), x + 86, y, TEXT, false);
      return y + 14;
   }

   private List<CreatorProfileCard> filteredCards() {
      String needle = search.trim().toLowerCase(Locale.ROOT);
      return export.cards().stream()
         .sorted(Comparator.comparing(card -> card.profileId().toString()))
         .filter(card -> needle.isEmpty()
            || card.profileId().toString().toLowerCase(Locale.ROOT).contains(needle)
            || card.title().toLowerCase(Locale.ROOT).contains(needle)
            || String.join(" ", card.effectPresets()).toLowerCase(Locale.ROOT).contains(needle))
         .toList();
   }

   private boolean backspace() {
      if (activeField == EditField.SEARCH) {
         if (search.isEmpty()) {
            return false;
         }
         search = search.substring(0, search.length() - 1);
         selected = 0;
         firstVisible = 0;
         return true;
      }
      CreatorProfileCard card = selectedCard();
      CreatorProfileDraft draft = card == null ? null : drafts.get(card.profileId());
      if (draft == null) {
         return false;
      }
      if (activeField == EditField.TITLE && !draft.title().isEmpty()) {
         updateDraft(card.profileId(), draft.withTitle(draft.title().substring(0, draft.title().length() - 1)));
         return true;
      }
      if (activeField == EditField.NOTES && !draft.notes().isEmpty()) {
         updateDraft(card.profileId(), draft.withNotes(draft.notes().substring(0, draft.notes().length() - 1)));
         return true;
      }
      if (activeField == EditField.SCREENSHOT && !draft.screenshotPath().isEmpty()) {
         updateDraft(card.profileId(), draft.withScreenshotPath(draft.screenshotPath().substring(0, draft.screenshotPath().length() - 1)));
         return true;
      }
      return false;
   }

   private boolean insertText(String typed) {
      if (activeField == EditField.SEARCH) {
         search += typed.toLowerCase(Locale.ROOT);
         selected = 0;
         firstVisible = 0;
         return true;
      }
      CreatorProfileCard card = selectedCard();
      CreatorProfileDraft draft = card == null ? null : drafts.get(card.profileId());
      if (draft == null) {
         return false;
      }
      if (activeField == EditField.TITLE && draft.title().length() < 80) {
         updateDraft(card.profileId(), draft.withTitle(draft.title() + typed));
         return true;
      }
      if (activeField == EditField.NOTES && draft.notes().length() < 180) {
         updateDraft(card.profileId(), draft.withNotes(draft.notes() + typed));
         return true;
      }
      if (activeField == EditField.SCREENSHOT && draft.screenshotPath().length() < 160) {
         updateDraft(card.profileId(), draft.withScreenshotPath(draft.screenshotPath() + typed));
         return true;
      }
      return false;
   }

   private CreatorProfileCard selectedCard() {
      List<CreatorProfileCard> cards = filteredCards();
      if (cards.isEmpty()) {
         return null;
      }
      return cards.get(clamp(selected, 0, cards.size() - 1));
   }

   private void ensureDraft(CreatorProfileCard card) {
      if (card == null) {
         return;
      }
      VisualProfile profile = RenderCoreProfiles.visual(card.profileId());
      if (profile == null) {
         JsonObject normalized = export.artifacts().stream()
            .filter(artifact -> card.profileId().equals(artifact.id()))
            .map(CreatorPackArtifact::json)
            .filter(json -> json.has("normalized_profile") && json.get("normalized_profile").isJsonObject())
            .map(json -> json.getAsJsonObject("normalized_profile"))
            .findFirst()
            .orElse(new JsonObject());
         drafts.putIfAbsent(card.profileId(), new CreatorProfileDraft(card.profileId(), card.title(), "", card.screenshotPath(),
            generatedPath(card.profileId()), normalized, false));
      } else {
         drafts.putIfAbsent(card.profileId(), CreatorProfileDraft.from(profile, card));
      }
      activeField = EditField.TITLE;
      statusLine = "Draft ready for " + card.profileId() + ".";
   }

   private void saveDraft(CreatorProfileCard card) {
      ensureDraft(card);
      CreatorProfileDraft draft = drafts.get(card.profileId());
      if (draft == null) {
         return;
      }
      Path root = Minecraft.getInstance().gameDirectory.toPath().resolve("rendercore_creator_drafts");
      Path target = root.resolve(draft.generatedPath().isBlank() ? generatedPath(card.profileId()) : draft.generatedPath());
      try {
         Files.createDirectories(target.getParent());
         Files.writeString(target, GSON.toJson(draft.toProfileJson()), StandardCharsets.UTF_8);
         updateDraft(card.profileId(), draft.clean());
         statusLine = "Saved generated draft to " + root.relativize(target).toString().replace('\\', '/') + ".";
      } catch (IOException exception) {
         statusLine = "Draft save failed: " + exception.getMessage();
      }
   }

   private void captureThumbnail(CreatorProfileCard card) {
      ensureDraft(card);
      VisualProfile profile = RenderCoreProfiles.visual(card.profileId());
      if (profile == null) {
         statusLine = "Cannot capture thumbnail until " + card.profileId() + " is in the active profile cache.";
         return;
      }
      Path root = Minecraft.getInstance().gameDirectory.toPath().resolve("rendercore_creator_drafts");
      ProfileScreenshotCaptureResult result = RenderCoreClientScreenshotPreviewProvider.INSTANCE.capture(profile, root);
      if (result.captured()) {
         CreatorProfileDraft draft = drafts.get(card.profileId());
         updateDraft(card.profileId(), draft.withScreenshotPath(result.relativePath()));
         statusLine = "Captured thumbnail " + result.relativePath() + ".";
      } else {
         statusLine = "Thumbnail capture skipped: " + result.skippedReason();
      }
   }

   private void updateDraft(Identifier profileId, CreatorProfileDraft draft) {
      if (profileId != null && draft != null) {
         drafts.put(profileId, draft);
      }
   }

   private static String generatedPath(Identifier id) {
      if (id == null) {
         return "";
      }
      return "assets/" + id.getNamespace() + "/rendercore/visual_profiles/" + id.getPath() + ".json";
   }

   private void layout() {
      panelW = Math.min(740, Math.max(420, width - 36));
      panelH = Math.min(420, Math.max(260, height - 34));
      panelX = (width - panelW) / 2;
      panelY = (height - panelH) / 2;
   }

   private int listWidth() {
      return Math.min(300, Math.max(190, panelW / 2 - 32));
   }

   private int visibleRows() {
      return Math.max(1, (panelH - 92) / 30);
   }

   private static String trim(Font font, String text, int width) {
      String value = text == null ? "" : text;
      if (font.width(value) <= width) {
         return value;
      }
      String suffix = "...";
      return font.plainSubstrByWidth(value, Math.max(0, width - font.width(suffix))) + suffix;
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
      return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
   }

   private record Hitbox(int x, int y, int w, int h, Runnable action) {
   }

   private enum EditField {
      SEARCH,
      TITLE,
      NOTES,
      SCREENSHOT
   }
}

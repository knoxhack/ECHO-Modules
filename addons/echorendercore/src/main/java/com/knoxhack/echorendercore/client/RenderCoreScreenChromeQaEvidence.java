package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.profile.CreatorVisualQaReport;
import com.knoxhack.echorendercore.profile.RenderCoreScreenChromeQaCatalog;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-only capture buffer for V21 screen chrome visual QA evidence.
 */
public final class RenderCoreScreenChromeQaEvidence {
   private static final Object LOCK = new Object();
   private static final ArrayList<CreatorVisualQaReport.ScreenChromeEvidence> EVIDENCE = new ArrayList<>();
   private static boolean active;

   private RenderCoreScreenChromeQaEvidence() {
   }

   public static void start() {
      synchronized (LOCK) {
         EVIDENCE.clear();
         active = true;
      }
   }

   public static CreatorVisualQaReport.ScreenChromeEvidence capture(String surfaceId, String screenshotPath) {
      String status = screenshotPath == null || screenshotPath.isBlank() ? "fail" : "pass";
      String notes = "pass".equals(status) ? "" : "Screenshot capture was unavailable for this surface.";
      CreatorVisualQaReport.ScreenChromeEvidence evidence =
         RenderCoreScreenChromeQaCatalog.evidence(surfaceId, status, screenshotPath, notes);
      if (evidence == null) {
         return null;
      }
      synchronized (LOCK) {
         if (!active) {
            active = true;
         }
         EVIDENCE.removeIf(entry -> entry.surfaceId().equals(evidence.surfaceId()));
         EVIDENCE.add(evidence);
      }
      return evidence;
   }

   public static List<CreatorVisualQaReport.ScreenChromeEvidence> evidence() {
      synchronized (LOCK) {
         return List.copyOf(EVIDENCE);
      }
   }

   public static void reset() {
      synchronized (LOCK) {
         EVIDENCE.clear();
         active = false;
      }
   }

   public static boolean active() {
      synchronized (LOCK) {
         return active;
      }
   }
}

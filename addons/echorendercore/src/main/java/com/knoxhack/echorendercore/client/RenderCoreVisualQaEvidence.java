package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.profile.CreatorAddonShowcaseCatalog;
import com.knoxhack.echorendercore.profile.CreatorVisualQaReport;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-only capture buffer for V21 advanced-FX release evidence.
 */
public final class RenderCoreVisualQaEvidence {
   private static final Object LOCK = new Object();
   private static final ArrayList<CreatorVisualQaReport.EvidenceSnapshot> SNAPSHOTS = new ArrayList<>();
   private static boolean active;

   private RenderCoreVisualQaEvidence() {
   }

   public static CreatorVisualQaReport.EvidenceSnapshot start() {
      synchronized (LOCK) {
         SNAPSHOTS.clear();
         active = true;
      }
      return capture("start");
   }

   public static CreatorVisualQaReport.EvidenceSnapshot capture(String label) {
      return capture(label, "");
   }

   public static CreatorVisualQaReport.EvidenceSnapshot capture(String label, String screenshotPath) {
      CreatorVisualQaReport.EvidenceSnapshot snapshot = new CreatorVisualQaReport.EvidenceSnapshot(
         label,
         RenderCoreEffectPipeline.statusLine(),
         RenderCoreAdvancedFxPipeline.modeLine(),
         RenderCoreAdvancedFxPipeline.fallbackReason(),
         RenderCoreAdvancedFxPipeline.unavailableReason(),
         RenderCoreAdvancedFxPipeline.lastMaskSubmissionCount(),
         RenderCoreAdvancedFxPipeline.lastSkippedSubmissions(),
         RenderCoreAdvancedFxPipeline.lastEffectCount(),
         RenderCoreAdvancedFxPipeline.lastChannelCount(),
         RenderCoreAdvancedFxPipeline.lastDownscale(),
         RenderCoreAdvancedFxPipeline.lastPassCount(),
         RenderCoreAdvancedFxPipeline.lastBloomCost(),
         RenderCoreAdvancedFxPipeline.compileFailed(),
         screenshotPath
      );
      synchronized (LOCK) {
         if (!active) {
            active = true;
         }
         SNAPSHOTS.add(snapshot);
      }
      return snapshot;
   }

   public static CreatorVisualQaReport report() {
      return CreatorVisualQaReport.fromSnapshots(
         snapshots(),
         CreatorAddonShowcaseCatalog.echoVisionCoverage(),
         RenderCoreScreenChromeQaEvidence.evidence()
      );
   }

   public static List<CreatorVisualQaReport.EvidenceSnapshot> snapshots() {
      synchronized (LOCK) {
         return List.copyOf(SNAPSHOTS);
      }
   }

   public static void reset() {
      synchronized (LOCK) {
         SNAPSHOTS.clear();
         active = false;
      }
   }

   public static boolean active() {
      synchronized (LOCK) {
         return active;
      }
   }
}

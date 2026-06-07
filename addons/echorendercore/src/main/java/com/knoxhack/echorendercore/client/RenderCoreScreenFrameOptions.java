package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.profile.VisualProfile;
import com.knoxhack.echorendercore.profile.VisualScreenChromeProfile;

public final class RenderCoreScreenFrameOptions {
   private final String label;
   private final boolean drawLabel;
   private final boolean drawScanlines;
   private final boolean scanlinesBehindContent;
   private final boolean accentBars;
   private final boolean quietFallback;
   private final RenderCoreScreenChromeStyle style;
   private final boolean backdrop;
   private final boolean edgeGlow;
   private final boolean cornerBrackets;
   private final boolean accentRails;
   private final boolean glassGlints;
   private final boolean chromaticEdge;

   public RenderCoreScreenFrameOptions(
         String label,
         boolean drawLabel,
         boolean drawScanlines,
         boolean scanlinesBehindContent,
         boolean accentBars,
         boolean quietFallback) {
      this(
         label,
         drawLabel,
         drawScanlines,
         scanlinesBehindContent,
         accentBars,
         quietFallback,
         RenderCoreScreenChromeStyle.CYBERGLASS,
         false,
         accentBars,
         accentBars,
         accentBars,
         accentBars,
         accentBars
      );
   }

   private RenderCoreScreenFrameOptions(
         String label,
         boolean drawLabel,
         boolean drawScanlines,
         boolean scanlinesBehindContent,
         boolean accentBars,
         boolean quietFallback,
         RenderCoreScreenChromeStyle style,
         boolean backdrop,
         boolean edgeGlow,
         boolean cornerBrackets,
         boolean accentRails,
         boolean glassGlints,
         boolean chromaticEdge) {
      this.label = label == null ? "" : label.strip();
      this.drawLabel = drawLabel;
      this.drawScanlines = drawScanlines;
      this.scanlinesBehindContent = scanlinesBehindContent;
      this.accentBars = accentBars;
      this.quietFallback = quietFallback;
      this.style = style == null ? RenderCoreScreenChromeStyle.CYBERGLASS : style;
      this.backdrop = backdrop;
      this.edgeGlow = edgeGlow;
      this.cornerBrackets = cornerBrackets;
      this.accentRails = accentRails;
      this.glassGlints = glassGlints;
      this.chromaticEdge = chromaticEdge;
   }

   public static Builder builder() {
      return new Builder();
   }

   public static Builder cyberglass(String label) {
      return builder()
         .style(RenderCoreScreenChromeStyle.CYBERGLASS)
         .label(label);
   }

   public static Builder terminal(String label) {
      return builder()
         .style(RenderCoreScreenChromeStyle.TERMINAL)
         .label(label)
         .backdrop(false)
         .scanlines(false)
         .glassGlints(false)
         .chromaticEdge(true);
   }

   public static Builder hologram(String label) {
      return builder()
         .style(RenderCoreScreenChromeStyle.HOLOGRAM)
         .label(label)
         .backdrop(false)
         .scanlines(false)
         .glassGlints(true)
         .chromaticEdge(true);
   }

   public static Builder neon(String label) {
      return builder()
         .style(RenderCoreScreenChromeStyle.NEON)
         .label(label)
         .backdrop(false)
         .scanlines(false)
         .glassGlints(true)
         .chromaticEdge(true);
   }

   public static Builder minimal() {
      return builder()
         .style(RenderCoreScreenChromeStyle.MINIMAL)
         .drawLabel(false)
         .scanlines(false)
         .scanlinesBehindContent(true)
         .accentRails(false)
         .edgeGlow(false)
         .cornerBrackets(false)
         .glassGlints(false)
         .chromaticEdge(false)
         .backdrop(false);
   }

   public static RenderCoreScreenFrameOptions legacy(String label) {
      return cyberglass(label)
         .drawLabel(label != null && !label.isBlank())
         .scanlines(true)
         .scanlinesBehindContent(false)
         .accentRails(true)
         .edgeGlow(true)
         .cornerBrackets(true)
         .glassGlints(true)
         .chromaticEdge(true)
         .backdrop(false)
         .quietFallback(false)
         .build();
   }

   public static RenderCoreScreenFrameOptions quiet() {
      return minimal()
         .quietFallback(true)
         .build();
   }

   public static RenderCoreScreenFrameOptions fromProfile(VisualProfile profile, RenderCoreScreenFrameOptions fallbackOptions) {
      return fromProfile(profile, fallbackOptions, false);
   }

   public static RenderCoreScreenFrameOptions fromProfile(VisualProfile profile, RenderCoreScreenFrameOptions fallbackOptions, boolean reducedMotion) {
      RenderCoreScreenFrameOptions fallback = fallbackOptions == null ? cyberglass("").build() : fallbackOptions;
      if (profile == null) {
         return fallback;
      }
      VisualScreenChromeProfile chrome = profile.screenChrome();
      if (chrome == null) {
         return fallback;
      }
      return builder()
         .style(style(chrome.style(), fallback.style()))
         .label(chrome.label())
         .drawLabel(chrome.drawLabel())
         .backdrop(chrome.backdrop())
         .edgeGlow(reducedMotion ? false : chrome.edgeGlow())
         .cornerBrackets(chrome.cornerBrackets())
         .accentRails(chrome.accentRails())
         .scanlines(reducedMotion ? false : chrome.scanlines())
         .glassGlints(reducedMotion ? false : chrome.glassGlints())
         .chromaticEdge(reducedMotion ? false : chrome.chromaticEdge())
         .quietFallback(chrome.quietFallback())
         .build();
   }

   public String label() {
      return label;
   }

   public boolean drawLabel() {
      return drawLabel;
   }

   public boolean drawScanlines() {
      return drawScanlines;
   }

   public boolean scanlines() {
      return drawScanlines;
   }

   public boolean scanlinesBehindContent() {
      return scanlinesBehindContent;
   }

   public boolean accentBars() {
      return accentBars;
   }

   public boolean quietFallback() {
      return quietFallback;
   }

   public RenderCoreScreenChromeStyle style() {
      return style;
   }

   public boolean backdrop() {
      return backdrop;
   }

   public boolean edgeGlow() {
      return edgeGlow;
   }

   public boolean cornerBrackets() {
      return cornerBrackets;
   }

   public boolean accentRails() {
      return accentRails;
   }

   public boolean glassGlints() {
      return glassGlints;
   }

   public boolean chromaticEdge() {
      return chromaticEdge;
   }

   private static RenderCoreScreenChromeStyle style(String id, RenderCoreScreenChromeStyle fallback) {
      if (id == null || id.isBlank()) {
         return fallback == null ? RenderCoreScreenChromeStyle.CYBERGLASS : fallback;
      }
      return switch (id.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_')) {
          case "minimal" -> RenderCoreScreenChromeStyle.MINIMAL;
          case "cyberglass" -> RenderCoreScreenChromeStyle.CYBERGLASS;
          case "terminal" -> RenderCoreScreenChromeStyle.TERMINAL;
          case "hologram" -> RenderCoreScreenChromeStyle.HOLOGRAM;
          case "neon" -> RenderCoreScreenChromeStyle.NEON;
          default -> fallback == null ? RenderCoreScreenChromeStyle.CYBERGLASS : fallback;
       };
    }

   public static final class Builder {
      private String label = "";
      private boolean drawLabel;
      private boolean drawScanlines;
      private boolean scanlinesBehindContent = true;
      private boolean accentRails = true;
      private boolean quietFallback;
      private RenderCoreScreenChromeStyle style = RenderCoreScreenChromeStyle.CYBERGLASS;
      private boolean backdrop = true;
      private boolean edgeGlow = true;
      private boolean cornerBrackets = true;
      private boolean glassGlints = true;
      private boolean chromaticEdge = true;

      private Builder() {
      }

      public Builder label(String label) {
         this.label = label == null ? "" : label.strip();
         this.drawLabel = !this.label.isBlank();
         return this;
      }

      public Builder drawLabel(boolean drawLabel) {
         this.drawLabel = drawLabel;
         return this;
      }

      public Builder style(RenderCoreScreenChromeStyle style) {
         this.style = style == null ? RenderCoreScreenChromeStyle.CYBERGLASS : style;
         return this;
      }

      public Builder backdrop(boolean backdrop) {
         this.backdrop = backdrop;
         return this;
      }

      public Builder edgeGlow(boolean edgeGlow) {
         this.edgeGlow = edgeGlow;
         return this;
      }

      public Builder cornerBrackets(boolean cornerBrackets) {
         this.cornerBrackets = cornerBrackets;
         return this;
      }

      public Builder accentRails(boolean accentRails) {
         this.accentRails = accentRails;
         return this;
      }

      public Builder accentBars(boolean accentBars) {
         return accentRails(accentBars);
      }

      public Builder scanlines(boolean scanlines) {
         this.drawScanlines = scanlines;
         return this;
      }

      public Builder drawScanlines(boolean drawScanlines) {
         return scanlines(drawScanlines);
      }

      public Builder scanlinesBehindContent(boolean scanlinesBehindContent) {
         this.scanlinesBehindContent = scanlinesBehindContent;
         return this;
      }

      public Builder glassGlints(boolean glassGlints) {
         this.glassGlints = glassGlints;
         return this;
      }

      public Builder chromaticEdge(boolean chromaticEdge) {
         this.chromaticEdge = chromaticEdge;
         return this;
      }

      public Builder quietFallback(boolean quietFallback) {
         this.quietFallback = quietFallback;
         return this;
      }

      public RenderCoreScreenFrameOptions build() {
         return new RenderCoreScreenFrameOptions(
            label,
            drawLabel,
            drawScanlines,
            scanlinesBehindContent,
            accentRails,
            quietFallback,
            style,
            backdrop,
            edgeGlow,
            cornerBrackets,
            accentRails,
            glassGlints,
            chromaticEdge
         );
      }
   }
}

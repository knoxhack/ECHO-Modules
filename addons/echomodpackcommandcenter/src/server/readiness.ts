import type {
  AppSettings,
  JarManifest,
  Project,
  ReadinessChecklistItem,
  ReadinessItemStatus,
  ReadinessReport,
  ScanReport
} from "../shared/types.js";

export function buildReadinessReport(
  project: Project,
  settings: AppSettings,
  scans: ScanReport[],
  jarManifest: JarManifest,
  now = new Date()
): ReadinessReport {
  const latestQuick = scans.find((scan) => scan.mode === "quick") ?? null;
  const latestDeep = scans.find((scan) => scan.mode === "deep") ?? null;
  const items = [
    quickScanItem(project, latestQuick),
    deepScanItem(latestDeep),
    builtJarsItem(project, jarManifest),
    modsFolderItem(settings, jarManifest),
    currentJarsItem(project, jarManifest),
    staleJarsItem(jarManifest)
  ];
  const counts = {
    done: items.filter((item) => item.status === "done").length,
    missing: items.filter((item) => item.status === "missing").length,
    blocked: items.filter((item) => item.status === "blocked").length,
    warning: items.filter((item) => item.status === "warning").length,
    total: items.length
  };
  const nextAction = nextActionFrom(items);
  return {
    projectSlug: project.slug,
    generatedAt: now.toISOString(),
    score: readinessScore(latestQuick, items),
    latestQuickScanId: latestQuick?.id,
    latestDeepScanId: latestDeep?.id,
    nextAction,
    counts,
    items
  };
}

function quickScanItem(project: Project, scan: ScanReport | null): ReadinessChecklistItem {
  if (!scan) {
    return item({
      id: "quick-scan",
      category: "Scan",
      label: "Run quick scan",
      status: "missing",
      detail: `No quick scan report exists yet. Run a quick scan to calculate the current ${project.name} readiness gap.`,
      actionLabel: "Run quick scan",
      targetView: "qa"
    });
  }
  const codes = findingCodes(scan);
  if (scan.findings.length === 0 && scan.status === "passed") {
    return item({
      id: "quick-scan",
      category: "Scan",
      label: "Quick scan clean",
      status: "done",
      detail: `Quick scan #${scan.id} passed with no findings.`,
      actionLabel: "View QA",
      targetView: "qa"
    });
  }
  return item({
    id: "quick-scan",
    category: "Scan",
    label: "Quick scan findings",
    status: statusFromFindings(scan),
    detail: `Quick scan #${scan.id} is ${scan.status} with ${scan.findings.length} finding(s): ${codes.join(", ") || "uncoded findings"}.`,
    actionLabel: "Open findings",
    targetView: "qa",
    relatedFindingCodes: codes
  });
}

function deepScanItem(scan: ScanReport | null): ReadinessChecklistItem {
  if (!scan) {
    return item({
      id: "deep-scan",
      category: "Scan",
      label: "Deep scan optional",
      status: "done",
      detail: "No deep scan is required for the current 100% checklist; Release Deck keeps deeper release gates separate.",
      actionLabel: "Run deep scan",
      targetView: "qa"
    });
  }
  const codes = findingCodes(scan);
  if (scan.findings.length === 0 && scan.status === "passed") {
    return item({
      id: "deep-scan",
      category: "Scan",
      label: "Deep scan clean",
      status: "done",
      detail: `Deep scan #${scan.id} passed with no findings.`,
      actionLabel: "View QA",
      targetView: "qa"
    });
  }
  return item({
    id: "deep-scan",
    category: "Scan",
    label: "Deep scan findings",
    status: statusFromFindings(scan),
    detail: `Deep scan #${scan.id} is ${scan.status} with ${scan.findings.length} finding(s): ${codes.join(", ") || "uncoded findings"}.`,
    actionLabel: "Open findings",
    targetView: "qa",
    relatedFindingCodes: codes
  });
}

function builtJarsItem(project: Project, manifest: JarManifest): ReadinessChecklistItem {
  if (manifest.summary.expected > 0 && manifest.summary.built === manifest.summary.expected && manifest.summary.missing === 0) {
    return item({
      id: "built-jars",
      category: "Jars",
      label: "Expected jars built",
      status: "done",
      detail: `${manifest.summary.built}/${manifest.summary.expected} expected ${project.slug === "echo" ? "ECHO" : "module"} jar(s) exist in the Gradle build root.`,
      actionLabel: "Open jars",
      targetView: "jars"
    });
  }
  const missing = manifest.artifacts.filter((artifact) => !artifact.exists).map((artifact) => artifact.expectedFileName);
  return item({
    id: "built-jars",
    category: "Jars",
    label: "Build expected jars",
    status: "missing",
    detail: `${manifest.summary.built}/${manifest.summary.expected} expected jar(s) are built. Missing: ${missing.join(", ") || "unknown"}.`,
    actionLabel: "Build jars",
    targetView: "jars",
    commandId: project.slug === "echo" ? "build-full-stack" : "build-module"
  });
}

function modsFolderItem(settings: AppSettings, manifest: JarManifest): ReadinessChecklistItem {
  if (manifest.targetConfigured && manifest.targetExists) {
    return item({
      id: "mods-folder",
      category: "Settings",
      label: "Mods folder configured",
      status: "done",
      detail: `Settings points to an existing mods folder: ${manifest.targetDir}.`,
      actionLabel: "Open settings",
      targetView: "settings"
    });
  }
  const configured = settings.modpackModsDir.trim();
  return item({
    id: "mods-folder",
    category: "Settings",
    label: "Configure mods folder",
    status: "blocked",
    detail: configured
      ? `Configured mods folder does not exist: ${manifest.targetDir}.`
      : "Settings > Modpack Mods Folder is empty, so jar promotion and target comparison cannot finish.",
    actionLabel: "Configure mods folder",
    targetView: "settings",
    relatedFindingCodes: [configured ? "MODS_DIR_MISSING" : "MODS_DIR_UNSET"]
  });
}

function currentJarsItem(project: Project, manifest: JarManifest): ReadinessChecklistItem {
  if (!manifest.targetConfigured || !manifest.targetExists) {
    return item({
      id: "current-jars",
      category: "Jars",
      label: "Promote current jars",
      status: "blocked",
      detail: "The target mods folder must be configured before current jars can be copied and verified.",
      actionLabel: "Configure mods folder",
      targetView: "settings",
      commandId: "promote-jars",
      relatedFindingCodes: manifest.targetConfigured ? ["MODS_DIR_MISSING"] : ["MODS_DIR_UNSET"]
    });
  }
  if (manifest.summary.current === manifest.summary.expected && manifest.summary.expected > 0) {
    return item({
      id: "current-jars",
      category: "Jars",
      label: "Target jars current",
      status: "done",
      detail: `${manifest.summary.current}/${manifest.summary.expected} expected jar(s) are current in the configured mods folder.`,
      actionLabel: "Open jars",
      targetView: "jars"
    });
  }
  return item({
    id: "current-jars",
    category: "Jars",
    label: "Promote current jars",
    status: "missing",
    detail: `${manifest.summary.current}/${manifest.summary.expected} expected jar(s) are current in the configured mods folder.`,
    actionLabel: "Promote jars",
    targetView: "jars",
    commandId: "promote-jars"
  });
}

function staleJarsItem(manifest: JarManifest): ReadinessChecklistItem {
  if (!manifest.targetConfigured || !manifest.targetExists) {
    return item({
      id: "stale-jars",
      category: "Jars",
      label: "Check stale target jars",
      status: "blocked",
      detail: "Target comparison is unavailable until Settings > Modpack Mods Folder points to an existing folder.",
      actionLabel: "Configure mods folder",
      targetView: "settings",
      relatedFindingCodes: manifest.targetConfigured ? ["MODS_DIR_MISSING"] : ["MODS_DIR_UNSET"]
    });
  }
  const staleCount = manifest.summary.stale + manifest.summary.duplicate;
  if (staleCount === 0) {
    return item({
      id: "stale-jars",
      category: "Jars",
      label: "No stale managed jars",
      status: "done",
      detail: "No stale or duplicate managed project jars were found in the target mods folder.",
      actionLabel: "Open jars",
      targetView: "jars"
    });
  }
  return item({
    id: "stale-jars",
    category: "Jars",
    label: "Quarantine stale jars",
    status: "blocked",
    detail: `${staleCount} stale or duplicate managed jar(s) need quarantine before the target can be clean.`,
    actionLabel: "Promote jars",
    targetView: "jars",
    commandId: "promote-jars",
    relatedFindingCodes: ["STALE_JAR", "JAR_COUNT_MISMATCH"]
  });
}

function item(input: Omit<ReadinessChecklistItem, "relatedFindingCodes"> & { relatedFindingCodes?: string[] }): ReadinessChecklistItem {
  return {
    ...input,
    relatedFindingCodes: input.relatedFindingCodes ?? []
  };
}

function findingCodes(scan: ScanReport): string[] {
  return Array.from(new Set(scan.findings.map((finding) => finding.code).filter((code): code is string => Boolean(code))));
}

function statusFromFindings(scan: ScanReport): ReadinessItemStatus {
  if (scan.findings.some((finding) => finding.severity === "critical" || finding.severity === "high")) {
    return "blocked";
  }
  if (scan.findings.some((finding) => finding.severity === "medium")) {
    return "warning";
  }
  return "missing";
}

function nextActionFrom(items: ReadinessChecklistItem[]): ReadinessChecklistItem | null {
  const priority = ["mods-folder", "built-jars", "current-jars", "stale-jars", "quick-scan", "deep-scan"];
  const candidates = items.filter((item) => item.status !== "done");
  return candidates.sort((left, right) => priority.indexOf(left.id) - priority.indexOf(right.id))[0] ?? null;
}

function readinessScore(latestQuick: ScanReport | null, items: ReadinessChecklistItem[]): number {
  if (!latestQuick) {
    return 0;
  }
  if (items.every((item) => item.status === "done")) {
    return 100;
  }
  const quickFindingCount = latestQuick.findings.length;
  if (quickFindingCount > 0) {
    return scanScore(latestQuick);
  }
  const penalty = items.reduce((total, item) => {
    if (item.status === "blocked") return total + 10;
    if (item.status === "missing") return total + 8;
    if (item.status === "warning") return total + 3;
    return total;
  }, 0);
  return Math.max(0, Math.min(100, 100 - penalty));
}

function scanScore(scan: ScanReport): number {
  return Math.max(0, Math.min(100, scan.summary.readinessScore ?? scan.summary.buildHealth));
}

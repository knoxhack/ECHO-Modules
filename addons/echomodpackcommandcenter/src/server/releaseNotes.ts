import fs from "node:fs";
import path from "node:path";
import type { ProjectDetail } from "../shared/types.js";
import { RELEASE_NOTES_DIR } from "./paths.js";

export function generateReleaseNotes(detail: ProjectDetail): { filePath: string; markdown: string } {
  fs.mkdirSync(RELEASE_NOTES_DIR, { recursive: true });
  const stamp = new Date().toISOString().replace(/[:.]/g, "-");
  const filePath = path.join(RELEASE_NOTES_DIR, `${detail.project.slug}-release-notes-${stamp}.md`);
  const markdown = [
    `# ${detail.project.name} Release Notes Draft`,
    "",
    `Milestone: ${detail.project.currentMilestone}`,
    `Build health: ${detail.project.buildHealth}%`,
    "",
    "## Included Modules",
    ...detail.project.modules.map((module) => `- ${module.label} ${module.version}`),
    "",
    "## Readiness",
    ...detail.roadmap.map((phase) => `- ${phase.title}: ${phase.status} (${phase.progress}%)`),
    "",
    "## QA Focus",
    ...detail.qaTracks.map((track) => `- ${track.title}: ${track.summary}`),
    "",
    "## Next Recommended Action",
    detail.project.nextRecommendedAction,
    ""
  ].join("\n");
  fs.writeFileSync(filePath, markdown, "utf-8");
  return { filePath, markdown };
}

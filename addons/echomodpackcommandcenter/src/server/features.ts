import type { FeatureCatalogResponse, FeatureCatalogSummary, FeatureRecord, FeatureStatus } from "../shared/types.js";

export const featureStatuses: FeatureStatus[] = ["implemented", "partial", "planned", "deferred", "blocked"];

export function buildFeatureCatalog(projectSlug: string, features: FeatureRecord[], generatedAt = new Date().toISOString()): FeatureCatalogResponse {
  return {
    projectSlug,
    generatedAt,
    features,
    summary: summarizeFeatures(features)
  };
}

export function summarizeFeatures(features: FeatureRecord[]): FeatureCatalogSummary {
  const statusCounts = Object.fromEntries(featureStatuses.map((status) => [status, 0])) as Record<FeatureStatus, number>;
  const categoryCounts: Record<string, number> = {};

  for (const feature of features) {
    statusCounts[feature.status] += 1;
    categoryCounts[feature.category] = (categoryCounts[feature.category] ?? 0) + 1;
  }

  return {
    total: features.length,
    statusCounts,
    categoryCounts
  };
}

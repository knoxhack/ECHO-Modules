import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { createApp } from "../src/server/index.ts";
import { CommandCenterStore } from "../src/server/db.ts";

async function tempServer() {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "noxhack-features-api-"));
  const store = new CommandCenterStore(path.join(dir, "command-center.sqlite"));
  const app = createApp(store);
  const server = await new Promise((resolve, reject) => {
    const listeningServer = app.listen(0, "127.0.0.1", () => resolve(listeningServer));
    listeningServer.once("error", reject);
  });
  const address = server.address();
  if (!address || typeof address === "string") {
    throw new Error("Expected TCP test server address");
  }
  return {
    store,
    server,
    baseUrl: `http://127.0.0.1:${address.port}`
  };
}

async function closeServer(server, store) {
  await new Promise((resolve, reject) => {
    server.close((error) => {
      if (error) reject(error);
      else resolve();
    });
  });
  store.close();
}

test("features API returns ECHO feature catalog", async () => {
  const { store, server, baseUrl } = await tempServer();
  try {
    const response = await fetch(`${baseUrl}/api/projects/echo/features`);
    assert.equal(response.status, 200);
    const body = await response.json();

    assert.equal(body.projectSlug, "echo");
    assert.equal(body.summary.total > 10, true);
    assert.equal(body.summary.statusCounts.partial > 0, true);
    assert.equal(body.features.some((feature) => feature.id === "echo-terminal"), true);
  } finally {
    await closeServer(server, store);
  }
});

test("features API returns ARCANA feature catalog", async () => {
  const { store, server, baseUrl } = await tempServer();
  try {
    const response = await fetch(`${baseUrl}/api/projects/arcana/features`);
    assert.equal(response.status, 200);
    const body = await response.json();

    assert.equal(body.projectSlug, "arcana");
    assert.equal(body.summary.statusCounts.implemented > 0, true);
    assert.equal(body.features.some((feature) => feature.id === "arcana-field-journal"), true);
  } finally {
    await closeServer(server, store);
  }
});

test("features API rejects unknown projects", async () => {
  const { store, server, baseUrl } = await tempServer();
  try {
    const response = await fetch(`${baseUrl}/api/projects/not-real/features`);
    assert.equal(response.status, 404);
    const body = await response.json();
    assert.equal(body.error, "Project not found");
  } finally {
    await closeServer(server, store);
  }
});

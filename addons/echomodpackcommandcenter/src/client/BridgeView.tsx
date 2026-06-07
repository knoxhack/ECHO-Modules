import { useEffect, useMemo, useState } from "react";
import { AlertTriangle, CheckCircle2, RefreshCw, Send, ShieldCheck, Square, TerminalSquare, XCircle } from "lucide-react";
import type { EchoBridgeJob, EchoBridgeLogChunk, EchoBridgeSafeActionRequest, EchoBridgeSession, EchoBridgeState } from "../shared/types";
import {
  cancelBridgeJob,
  confirmBridgeSafeAction,
  createBridgeSession,
  getBridgeJobLogs,
  getBridgeState,
  submitBridgePrompt
} from "./api";
import { EmptyState, InfoBanner, Metric, PageHeader, SectionTitle } from "./ui";
import { formatDate } from "./view-model";

export function BridgeView(): JSX.Element {
  const [state, setState] = useState<EchoBridgeState | null>(null);
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null);
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);
  const [promptText, setPromptText] = useState("Implement the next safe Phase 6 bridge slice and run validation.");
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [jobLogs, setJobLogs] = useState<EchoBridgeLogChunk[]>([]);

  useEffect(() => {
    void refresh();
    const timer = window.setInterval(() => void refresh(false), 2500);
    return () => window.clearInterval(timer);
  }, []);

  const selectedSession = useMemo(
    () => state?.sessions.find((session) => session.id === selectedSessionId) ?? state?.sessions[0] ?? null,
    [selectedSessionId, state]
  );
  const selectedJob = useMemo(
    () => state?.jobs.find((job) => job.id === selectedJobId) ?? activeSessionJob(state, selectedSession) ?? state?.jobs[0] ?? null,
    [selectedJobId, selectedSession, state]
  );
  const pendingActions = state?.safeActionRequests.filter((request) => request.status === "pending_confirmation") ?? [];
  const visibleLogs = jobLogs.length ? jobLogs : selectedJob?.recentLogChunks ?? [];

  useEffect(() => {
    if (!selectedJob) {
      setJobLogs([]);
      return;
    }
    getBridgeJobLogs(selectedJob.id)
      .then(setJobLogs)
      .catch(() => setJobLogs(selectedJob.recentLogChunks));
  }, [
    selectedJob?.id,
    selectedJob?.streamCursors.stdout,
    selectedJob?.streamCursors.stderr,
    selectedJob?.streamCursors.diagnostics,
    selectedJob?.streamCursors.state
  ]);

  async function refresh(showBusy = true): Promise<void> {
    if (showBusy) setBusy("refresh");
    try {
      const loaded = await getBridgeState();
      setState(loaded);
      setSelectedSessionId((current) => current ?? loaded.sessions[0]?.id ?? null);
      setSelectedJobId((current) => current ?? loaded.jobs[0]?.id ?? null);
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
    } finally {
      if (showBusy) setBusy(null);
    }
  }

  async function startSession(): Promise<EchoBridgeState> {
    setBusy("session");
    const next = await createBridgeSession({ displayName: "Command Center bridge session" });
    setState(next);
    setSelectedSessionId(next.sessions[0]?.id ?? null);
    setBusy(null);
    return next;
  }

  async function submitPrompt(): Promise<void> {
    if (!promptText.trim()) return;
    setBusy("prompt");
    try {
      const currentState = state?.sessions.length ? state : await startSession();
      const session = currentState.sessions.find((candidate) => candidate.id === selectedSessionId) ?? currentState.sessions[0];
      if (!session) return;
      const next = await submitBridgePrompt(session.id, {
        promptText,
        taskId: "phase6.cyberdex_codex_automation_loop",
        taskTitle: "CyberDex / Codex Automation Loop",
        moduleId: "echobridgecore",
        agentLane: "architect_agent",
        startCodex: true
      });
      setState(next);
      setSelectedSessionId(session.id);
      setSelectedJobId(next.jobs[0]?.id ?? null);
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
    } finally {
      setBusy(null);
    }
  }

  async function confirm(request: EchoBridgeSafeActionRequest, approved: boolean): Promise<void> {
    setBusy(request.id);
    try {
      const next = await confirmBridgeSafeAction(request.id, {
        confirmed: approved,
        note: approved ? "Approved from Command Center bridge panel." : "Rejected from Command Center bridge panel."
      });
      setState(next);
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
    } finally {
      setBusy(null);
    }
  }

  async function cancelJob(job: EchoBridgeJob): Promise<void> {
    setBusy(job.id);
    try {
      setState(await cancelBridgeJob(job.id));
      setError(null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="space-y-4">
      <PageHeader
        icon={TerminalSquare}
        eyebrow="Phase 6"
        title="Bridge Sessions"
        description="Local CyberDex and Codex session control with confirmation-gated actions."
        actions={
          <>
            <button className="secondary-button" disabled={busy !== null} onClick={() => void refresh()}>
              <RefreshCw className="h-4 w-4" />
              Refresh
            </button>
            <button className="primary-button" disabled={busy !== null} onClick={() => void startSession()}>
              <TerminalSquare className="h-4 w-4" />
              Create Session
            </button>
          </>
        }
      />

      {error ? <InfoBanner tone="red" title="Bridge API Error" detail={error} /> : null}
      {state?.executorStatus !== "configured" ? (
        <InfoBanner tone="amber" title="Codex Executor Guarded" detail={state?.executorReason ?? "Bridge state is loading."} />
      ) : null}

      <section className="grid gap-3 md:grid-cols-5">
        <Metric label="Executor" value={state?.executorStatus ?? "loading"} tone={state?.executorStatus === "configured" ? "green" : state?.executorStatus === "blocked" ? "red" : "amber"} />
        <Metric label="Probe" value={state?.executorProbe.status ?? "loading"} tone={state?.executorProbe.status === "configured" ? "green" : state?.executorProbe.status === "blocked" ? "red" : "amber"} />
        <Metric label="Sessions" value={state?.sessions.length ?? 0} />
        <Metric label="Jobs" value={state?.jobs.length ?? 0} />
        <Metric label="Safe Actions" value={pendingActions.length} tone={pendingActions.length ? "amber" : "green"} />
      </section>

      <section className="grid gap-4 xl:grid-cols-[0.82fr_1.18fr]">
        <div className="surface p-5">
          <SectionTitle icon={TerminalSquare} title="Sessions" />
          <p className="mt-3 break-all font-mono text-xs text-slate-500">{state?.workspace ?? "workspace loading"}</p>
          <div className="mt-4 space-y-2">
            {state?.sessions.length ? (
              state.sessions.map((session) => (
                <SessionButton
                  key={session.id}
                  session={session}
                  selected={selectedSession?.id === session.id}
                  onClick={() => {
                    setSelectedSessionId(session.id);
                    setSelectedJobId(session.activeJobIds[0] ?? null);
                  }}
                />
              ))
            ) : (
              <EmptyState icon={TerminalSquare} title="No bridge sessions" detail="Create a local session to queue confirmation-gated Codex job intents." />
            )}
          </div>
        </div>

        <div className="surface p-5">
          <SectionTitle icon={Send} title="Codex Prompt Intent" />
          <textarea
            className="control mt-4 min-h-[132px] w-full py-3"
            value={promptText}
            onChange={(event) => setPromptText(event.target.value)}
          />
          <div className="mt-3 flex flex-col gap-2 sm:flex-row">
            <button className="primary-button justify-center" disabled={busy !== null || !promptText.trim()} onClick={() => void submitPrompt()}>
              <Send className="h-4 w-4" />
              Submit Prompt
            </button>
            {selectedJob && !["completed", "failed", "canceled", "blocked"].includes(selectedJob.status) ? (
              <button className="danger-button justify-center" disabled={busy !== null} onClick={() => void cancelJob(selectedJob)}>
                <Square className="h-4 w-4" />
                Cancel Job
              </button>
            ) : null}
          </div>
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-[1fr_1fr]">
        <div className="surface p-5">
          <SectionTitle icon={ShieldCheck} title="Executor Probe" />
          <div className="mt-4 space-y-2">
            <div className="module-row items-start">
              <div className="min-w-0">
                <p className="text-sm font-semibold text-white">{state?.executorProbe.reason ?? "Probe loading"}</p>
                <p className="mt-1 font-mono text-xs text-slate-500">checked {state?.executorProbe.lastCheckedAt ? formatDate(state.executorProbe.lastCheckedAt) : "not yet"}</p>
              </div>
              <StatusPill value={state?.executorProbe.status ?? "loading"} tone={state?.executorProbe.status === "configured" ? "green" : state?.executorProbe.status === "blocked" ? "red" : "amber"} />
            </div>
            <div className="grid gap-2 md:grid-cols-2">
              <div className="path-chip w-full">Executable: {state?.executorProbe.executablePath || "not configured"}</div>
              <div className="path-chip w-full">Workspace: {state?.executorProbe.workspaceRoot || "not configured"}</div>
              <div className="path-chip w-full">Mode: {state?.executorConfig.argumentMode || "echo_bridge_sidecar_v1"}</div>
              <div className="path-chip w-full">Sidecar: {state?.executorConfig.sidecarEntrypoint || "none"}</div>
              <div className="path-chip w-full">Config: {state?.executorConfig.configPath || "not configured"}</div>
              <div className="path-chip w-full">Allowlist: {state?.executorConfig.allowlistedExecutableNames.join(", ") || "none"}</div>
            </div>
          </div>
        </div>
        <div className="surface p-5">
          <SectionTitle icon={ShieldCheck} title="Confirmation History" />
          <div className="mt-4 space-y-2">
            {state?.confirmationHistory.length ? (
              state.confirmationHistory.slice(0, 6).map((entry) => (
                <div key={`${entry.requestId}-${entry.createdAt}`} className="module-row items-start">
                  <div className="min-w-0">
                    <p className="font-mono text-xs text-slate-400">{entry.action} / {entry.requestId}</p>
                    <p className="mt-1 text-sm text-slate-300">{entry.note || (entry.confirmed ? "Approved" : "Rejected")}</p>
                  </div>
                  <StatusPill value={entry.status} tone={entry.status === "approved" ? "green" : entry.status === "blocked" ? "red" : "amber"} />
                </div>
              ))
            ) : (
              <EmptyState icon={ShieldCheck} title="No confirmation history" detail="Approvals and rejections will be recorded here without exposing private command payloads." />
            )}
          </div>
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
        <div className="surface p-5">
          <SectionTitle icon={TerminalSquare} title="Current Job" />
          {selectedJob ? <JobSummary job={selectedJob} /> : <EmptyState icon={TerminalSquare} title="No Codex jobs" detail="Submitted prompts will appear here as local bridge jobs." />}
        </div>
        <div className="surface p-5">
          <SectionTitle icon={AlertTriangle} title="Diagnostics" />
          <div className="mt-4 space-y-2">
            {(selectedJob?.diagnostics.length ? selectedJob.diagnostics : state?.diagnostics ?? []).map((diagnostic) => (
              <div key={diagnostic.code} className="module-row items-start">
                <div className="min-w-0">
                  <p className="font-mono text-xs text-signal-amber">{diagnostic.code}</p>
                  <p className="mt-1 text-sm leading-6 text-slate-300">{diagnostic.summary}</p>
                </div>
                <StatusPill value={diagnostic.severity} tone={diagnostic.blocking ? "red" : "amber"} />
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-[1fr_1fr]">
        <div className="surface p-5">
          <SectionTitle icon={ShieldCheck} title="Safe Action Requests" />
          <div className="mt-4 space-y-2">
            {pendingActions.length ? (
              pendingActions.map((request) => (
                <SafeActionCard key={request.id} request={request} busy={busy === request.id} onConfirm={confirm} />
              ))
            ) : (
              <EmptyState icon={ShieldCheck} title="No pending confirmations" detail="Bridge actions stay explicit, local, and recorded." />
            )}
          </div>
        </div>

        <div className="surface p-5">
          <SectionTitle icon={TerminalSquare} title="Live Log Stream" />
          <pre className="output-block mt-4 min-h-[260px]">
            {visibleLogs.length
              ? [...visibleLogs].reverse().map((log) => `[${formatDate(log.createdAt)}] ${log.stream}: ${log.text}`).join("\n")
              : "No log chunks for the selected job."}
          </pre>
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-3">
        <PathPanel title="Protected Files" items={state?.protectedFiles ?? []} />
        <PathPanel title="Safe Edit Zones" items={state?.safeEditZones ?? []} />
        <div className="surface p-5">
          <SectionTitle icon={TerminalSquare} title="Next Prompt Preview" />
          <pre className="output-block mt-4 min-h-[180px]">{selectedJob?.nextPrompt ?? state?.nextGeneratedPrompt ?? "Next prompt is not generated yet."}</pre>
        </div>
      </section>
    </div>
  );
}

function SessionButton({ session, selected, onClick }: { session: EchoBridgeSession; selected: boolean; onClick: () => void }): JSX.Element {
  return (
    <button className={`report-panel-row ${selected ? "report-panel-row-active" : ""}`} onClick={onClick}>
      <span className="flex min-w-0 items-center justify-between gap-3">
        <span className="truncate text-sm font-semibold text-white">{session.displayName}</span>
        <StatusPill value={session.status} tone={session.status === "idle" || session.status === "connected" ? "green" : "amber"} />
      </span>
      <span className="mt-2 truncate font-mono text-xs text-slate-500">{session.connectedPcBridge}</span>
      <span className="mt-1 text-xs text-slate-500">{session.activeJobIds.length} active job(s)</span>
    </button>
  );
}

function JobSummary({ job }: { job: EchoBridgeJob }): JSX.Element {
  return (
    <div className="mt-4 space-y-3">
      <div className="module-row items-start">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-white">{job.taskTitle}</p>
          <p className="mt-1 break-all font-mono text-xs text-slate-500">{job.id}</p>
        </div>
        <StatusPill value={job.status} tone={job.status === "blocked" || job.status === "failed" ? "red" : job.status === "completed" ? "green" : "amber"} />
      </div>
      <div className="grid gap-3 md:grid-cols-2">
        <Metric label="Module" value={job.moduleId} />
        <Metric label="Lane" value={job.agentLane} />
        <Metric label="Build" value={job.buildStatus} />
        <Metric label="Validation" value={job.validationStatus} />
        <Metric label="Executor PID" value={job.executorPid ?? "not started"} />
        <Metric label="Exit Code" value={job.executorExitCode ?? "not finished"} />
      </div>
      <div className="grid gap-2 md:grid-cols-2">
        {Object.entries(job.streamCursors).map(([stream, cursor]) => (
          <div key={stream} className="path-chip w-full">{stream}: {cursor}</div>
        ))}
      </div>
      <p className="text-sm leading-6 text-slate-300">{job.summary}</p>
    </div>
  );
}

function SafeActionCard({
  request,
  busy,
  onConfirm
}: {
  request: EchoBridgeSafeActionRequest;
  busy: boolean;
  onConfirm: (request: EchoBridgeSafeActionRequest, approved: boolean) => Promise<void>;
}): JSX.Element {
  return (
    <div className="finding-row">
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <p className="font-semibold text-white">{request.summary}</p>
          <StatusPill value={request.risk} tone={request.risk === "high" || request.risk === "destructive" || request.risk === "privileged" ? "red" : "amber"} />
        </div>
        <p className="mt-2 text-sm leading-6 text-slate-300">{request.developerDetails}</p>
        <p className="mt-2 font-mono text-xs text-slate-500">{request.kind} / {request.id}</p>
      </div>
      <div className="flex shrink-0 flex-col gap-2">
        <button className="primary-button" disabled={busy} onClick={() => void onConfirm(request, true)}>
          <CheckCircle2 className="h-4 w-4" />
          Approve
        </button>
        <button className="danger-button" disabled={busy} onClick={() => void onConfirm(request, false)}>
          <XCircle className="h-4 w-4" />
          Reject
        </button>
      </div>
    </div>
  );
}

function PathPanel({ title, items }: { title: string; items: string[] }): JSX.Element {
  return (
    <div className="surface p-5">
      <SectionTitle icon={ShieldCheck} title={title} />
      <div className="mt-4 space-y-2">
        {items.map((item) => (
          <div key={item} className="path-chip w-full">{item}</div>
        ))}
        {!items.length ? <p className="text-sm text-slate-500">No entries reported.</p> : null}
      </div>
    </div>
  );
}

function StatusPill({ value, tone }: { value: string; tone: "green" | "amber" | "red" }): JSX.Element {
  const className =
    tone === "green"
      ? "border-signal-green/50 text-signal-green"
      : tone === "red"
        ? "border-signal-red/50 text-signal-red"
        : "border-signal-amber/50 text-signal-amber";
  return <span className={`status-pill ${className}`}>{value}</span>;
}

function activeSessionJob(state: EchoBridgeState | null, session: EchoBridgeSession | null): EchoBridgeJob | null {
  const activeJobId = session?.activeJobIds[0];
  return state?.jobs.find((job) => job.id === activeJobId) ?? null;
}

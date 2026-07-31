# Beyond 4.0 — where BuildChecks could evolve

> Speculative. Not a commitment and not part of the release plan. `PLAN.md` remains the
> source of truth for scope and sequencing. This file captures the reasoning behind a possible
> direction so future decisions have the argument, not just the conclusion.

## The premise

AI compresses *code production*, not *code judgment*. When writing is cheap, the bottleneck
moves downstream — to reviewing, verifying, and holding a mental model of a codebase that now
changes faster than any human can read it. Demand for "keep the shape and quality on target"
grows rather than shrinks. But two things change underneath a tool like this:

1. **The author is often an agent, not a person.** The gate stops being a nag ("you forgot a
   test") and becomes a *trust boundary* — the interface where machine-generated change meets
   human accountability. The human increasingly reads the summary, not the diff, and decides
   whether to trust it.
2. **Today's signals are what AI games best.** Coverage %, counts, thresholds are proxies. An
   agent optimizing to pass a gate writes assertion-free tests that hit the number and mean
   nothing (Goodhart). Count metrics get *less* trustworthy in an AI world, not more.

So the honest version: observability stays necessary, but per-PR pass/fail on count metrics is
the part most at risk of becoming theater.

## Where this sits in the observability landscape

"Observability" is overloaded. The common thread: AI decouples code *volume* from human
*comprehension*, and every kind of observability is an instrument for re-coupling them at a
different layer — runtime behavior (OpenTelemetry, Datadog), delivery flow (DORA, SEI tools like
LinearB/Jellyfish), the AI application itself (LLM tracing/evals — LangSmith, Langfuse), and
**code health**, where BuildChecks lives (SonarQube, CodeScene).

Two axes locate it: *what* it observes — code health, not runtime or flow; and *when* — at the
**pre-merge gate**, not continuously (CodeScene's longitudinal drift view, a direction to grow
toward) or in production. So: code-health observability, at the change checkpoint, as a
deterministic trust boundary. It is complementary, not competitive — notably, SEI tools will show
velocity rising with AI while saying nothing about whether that velocity is real; the gate is
what tells you.

The discipline: a tool earns the word *observability* only to the degree it **compresses —
points scarce attention at the few things that matter** — not to the degree it aggregates more
signals. Anything that just adds numbers to look at is noise wearing the word.

## The core concept — proxy-vs-reality daylight

A metric is gameable to the exact degree you can satisfy the *measurement* without producing
the *thing it stands for*. That daylight is where gaming lives.

- **Coverage** is a proxy for "is this tested"; it literally measures "was this line executed
  while tests ran." A test that calls a function and asserts nothing gives 100% coverage and
  verifies nothing. The daylight is huge.
- **Mutation testing** (PIT/Stryker/mutmut) breaks the code deliberately and asks "did any test
  fail?" The only way to raise the score is to write an assertion that pins behavior — which is
  the real work of testing. Daylight nearly collapses. Cost: slow, so scope it to changed files.
- **Architecture rules** (ArchUnit/Konsist/dependency-cruiser/import-linter) are *relational* —
  they query the dependency graph ("does `parse` import `cli`?"), a property a local optimizer
  editing one file isn't even looking at. Gaming-resistant for a different reason, and cheap.

Two axes, therefore a signal portfolio:

| | cheap | expensive |
|---|---|---|
| **local (file)** | coverage, lint — *gameable* | mutation — *honest* |
| **relational (graph)** | architecture — *honest* | — |

## Judgment = a map of which signals lie

The signals aren't redundant votes to average. Each is authoritative about one failure mode and
blind to others: coverage is authoritative about "reached," blind to "checked"; mutation covers
exactly that blind spot; per-file lint is blind to structure; architecture covers that. So the
tool's judgment is: **when a flattering signal is green and a gaming-resistant one is red, the
honest one wins.** Averaging dilutes a real problem into "mostly green." The *contradiction* —
coverage up while mutation down — is itself the signal, and it names a specific lie: tests exist
but don't test.

Output follows: don't show N equal statuses. Show the short list of places where an honest
signal caught a flattering one lying, and collapse the rest. The scarce resource is human
attention; spend it only on contradictions.

## Is deterministic aggregation viable, or will something beat it?

The real competitor is an LLM-native review bot that reads the raw reports itself and writes the
summary. The answer for why a deterministic layer still matters: **a gate must be reproducible
to be a gate.** You cannot block a merge on a judgment that varies run to run, can be re-rolled
until it passes, or — critically, when the author is an agent — that the authoring agent can
talk out of its objection. Determinism is the product, not the limitation.

These are layers, not rivals:

- **Deterministic aggregator = the trust boundary.** Auditable, free per run, identical every
  time, blocks the merge. Humans *and* other agents rely on it because it can't be argued with.
- **LLM reviewer = triage/explanation on top.** Good at "why does this matter here" and novel
  issues no rule encodes — but it belongs *above* the gate, and its output should flow *into*
  the aggregator as one more gated signal, never *be* the gate.

## The real risk

Not a superior technique — **platform bundling.** GitHub/GitLab already ingest SARIF, have a
Checks API and merge queues. If a platform ships "aggregate your checks into one gated,
opinionated summary" natively, a standalone CLI is in trouble. Defensibility: toolchain- and
CI-agnostic (polyglot monorepos, non-GitHub CI, local dogfooding), and *opinionated* in a way a
neutral-dashboard vendor won't be. The day it becomes a neutral dashboard, the platform wins.

## Possible evolution directions

- From "did this PR pass" to "is the codebase drifting" — longitudinal quality, seeded by the
  existing fingerprint baseline. Drift, not snapshots.
- Aggregate the gaming-resistant signals: mutation, architecture/dependency-direction drift,
  semantic-diff size, and LLM-reviewer output — the one place they land, weighted, so
  coverage-theater and real quality don't look identical.
- Dual-audience artifact: a stable machine-readable contract the *authoring* agent consumes to
  self-correct inside its loop, not just HTML a human reads after.
- Allocate attention, don't just report it: "these 3 deserve human eyes; the rest is normal
  drift."

Consistent with the hard rules — all aggregation and judgment, no running tools, no platform.

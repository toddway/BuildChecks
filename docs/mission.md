# BuildChecks — mission, niche, and viability

> Purpose of this doc: clarify what BuildChecks is *for*, why it can stay relevant as platforms
> and AI change the landscape, and — honestly — the conditions under which it stops being worth
> the investment. Written to be usable both as a personal mental model and as a justification to
> stakeholders. `V4-PLAN.md` remains the source of truth for scope; `docs/beyond-4.0.md` holds
> the longer evolution argument.

## The one-line mission

**A toolchain- and CI-agnostic quality gate that turns many tools' reports into one gated,
opinionated summary — owned by the team that runs it, and trustworthy when the author is an AI.**

### What "opinionated" means (and what's earned today)

Opinionated = it makes the judgment calls *for* you, in code, instead of handing you a config
surface and calling that flexibility. Concretely: (1) it decides what deserves attention —
surfacing a few things and collapsing the rest is an editorial stance, not a dump; (2) it bakes
in a signal hierarchy — "when coverage is green and mutation is red, mutation wins" is an opinion,
not a setting; (3) it ships that stance as defaults, so a first-time maintainer gets useful output
without authoring a rulebook. Contrast SonarQube, which is deliberately *un*-opinionated at its
core — thousands of rules, and the quality gate is yours to define. It offloads the opinion onto
you; that offloading is exactly what erodes ownership (see niche #2).

Honest calibration: **today BuildChecks is only mildly opinionated** — curated gates, named by
metric, a self-explanatory report. That is more opinion than a raw aggregator. On the
gaming-resistant signals it is partway there: **architecture rules already gate today** (Konsist/
ArchUnit run as tests and emit JUnit XML, which v4 ingests), while **mutation testing is the
pending piece** (a PIT parser, targeted for 4.1). The strongest version — the signal hierarchy
and contradiction-surfacing ("coverage up, mutation down") — lands when mutation arrives. The
word points where this is going; it is partly earned, not yet fully.

## The niche (and why it survives platform features)

Platforms (GitHub Advanced Security, Checks API, merge queues) and single-vendor tools
(SonarQube) increasingly do *aggregation and gating*. So why does an independent tool have room?
Three edges, each structurally unavailable to the incumbents:

**1. Agnostic.** They are all bad at working identically across many codebases on many different
toolchains, CIs, and platforms. A platform gate assumes everyone is on that platform. A
commercial analyzer assumes you standardize on that vendor. Neither is true across a **portfolio
of client engagements**, where you cannot dictate each client's stack — Jenkins here, Bitrise
there, GitHub Actions elsewhere; Android on one, iOS on the next. BuildChecks is the agnostic
layer that rides on top of whatever each project already uses and produces the *same* gated
summary for all of them.

**2. Owned and legible.** This is the one teams feel day to day, and it may matter more than
agnosticism. Platform/server-side gates are a service applied *to* a team: the config lives
elsewhere, the rule surface is huge and opaque, and nobody on the project can say *why* a gate
fired. That produces a predictable death spiral — you don't own it, so you don't understand it;
you don't understand it, so you don't trust it; you don't trust it, so you route around it and
never fully implement it. BuildChecks inverts every step: the config is **one file in the repo**,
the gate runs in the **same build the engineer runs locally**, and gates are **few and named by
their metric with each limit's source marked**. An engineer can read the whole gate in one
sitting and know exactly why it fired. Legibility is what earns trust, and trust is what makes a
gate actually get enforced.

**3. A deterministic trust boundary for AI-authored change.** Getting more valuable, not less
(see below).

What it is explicitly *not*: a platform, a Sonar/CodeQL replacement, or a tool that runs analysis
itself. The deep analyzers keep their value — CodeQL's security dataflow is real and out of
scope here. The right relationship is that *their output becomes a signal the locally-owned gate
aggregates and gates on*: keep the deep analysis, move the **gate and the summary** to where the
team owns them. It aggregates and judges. That narrowness is the point.

## Why this matters more as AI writes more code

AI compresses code *production*, not code *judgment*. As agents author more change, the
bottleneck moves to reviewing and trusting it — and today's easy metrics (coverage, counts) are
exactly what an agent games (high coverage, empty assertions). The durable value is:

- **A gate must be reproducible to be a gate.** You cannot block a merge on a judgment that
  varies run to run or that the authoring agent can talk out of. Deterministic aggregation is
  the product, not a limitation. LLM reviewers sit *above* the gate, not as it.
- **Gaming-resistant signals win.** Mutation testing (honest about test quality) and
  architecture rules (honest about structure) overrule flattering ones like coverage. The
  summary's job is to surface the *contradiction* — "coverage up, mutation down" — and spend the
  human's scarce attention only there.

(Full argument: `docs/beyond-4.0.md`.)

## How to keep it on track for viability

1. **Stay agnostic and narrow.** The moment it standardizes on one platform or grows into a
   dashboard-with-everything, it loses its only advantage over the incumbents. Guard the closed
   dependency list and the "no running tools, no servers" rule.
2. **Keep the gate owned and legible.** Config stays as one readable file in the repo; the gate
   runs in the same build the engineer runs; gates stay few and named by metric with each limit's
   source marked. Legibility is the trust mechanism — protect it as fiercely as agnosticism.
   Caveat: ownership cuts both ways — a team that owns the config can also *weaken* it (coverage
   to zero the week before a deadline). Local ownership only stays honest paired with
   gaming-resistant, ratcheting signals (see #4), so trust and self-sabotage don't share a lever.
3. **Stay opinionated.** A neutral dashboard loses to the platform. The value is the judgment
   about what to hide and what to escalate — keep raising the bar on that, not on feature count.
4. **Track gaming-resistant signals first.** Every new signal should be one that resists
   Goodhart (mutation, architecture drift, dependency direction), not another count. These are
   also what keeps owned config from quietly decaying (see #2).
5. **Earn a second consumer.** One project is internal glue; a portfolio of projects is a
   capability. Adoption beyond the first repo is the real viability test (see kill criteria).
   iOS adoption counts double — a shared gate across Android *and* iOS is the proof only an
   agnostic tool can serve the portfolio.
6. **Keep the maintenance cost low.** Its low ongoing cost is part of the argument. If it starts
   demanding real headcount, the calculus changes.

## The honest case for spending time on it

**For (why it's worth doing):**
- It is already load-bearing: it is the *only* actual quality gate on a live client app.
- Low, bounded maintenance cost (closed deps, no infra).
- The AI-era gating thesis is real and ahead of what you get out of the box today.
- Agnostic portfolio tooling is a genuine fit for an agency that can't standardize clients, and
  adoption is already lined up across other Android *and* iOS projects.
- Owned and legible: engineers can read, run, and trust the gate — the thing that makes
  server-side platform gates quietly fail to get enforced.
- Positioning value: demonstrates forward thinking on AI-era code quality to clients.

**Against (why it's questionable):**
- One person maintaining it — bus factor is a fair objection even with adoption lined up.
- Overlaps with tools already in use (SonarQube is already in the client repo) and with
  platform features that expand over time.
- "Product with a mission" is a hard, mostly-unfunded road; the honest frame is *agency
  portfolio tooling*, not a broadly-adopted OSS product.

## Kill criteria (so it doesn't waste your time)

Downgrade to maintenance-mode glue — stop investing at product level — if **both** become true:

- It has **not earned a second real consumer** within ~2 quarters, **and**
- Existing tools already paid for (Sonar + platform gates) could be configured to match the
  AI-era gating with less bespoke maintenance.

Downgrading is not failure — it is refusing to sink time into a product costume. Conversely, if
it *does* pick up a second and third consumer across different toolchains, that is the signal the
niche is real and the investment is justified.

## The two-sentence pitch for stakeholders

> As AI writes more of our code, the scarce resource becomes *trusting* that code — and the easy
> metrics are exactly what AI games. BuildChecks is the agnostic, deterministic gate that gives
> us one trustworthy quality summary across every client codebase regardless of their toolchain
> — Android or iOS — and because the whole gate is one readable file the team owns and runs
> itself, engineers actually trust and enforce it, unlike a server-side gate nobody controls.
> It's cheap because it aggregates the tools we already have rather than replacing them.

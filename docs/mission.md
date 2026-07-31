# BuildChecks — mission and viability

> What this is for, why it survives platforms and AI, and when to stop.
> Scope: [PLAN.md](../PLAN.md). Long-form argument: [beyond-4.0.md](beyond-4.0.md).

## The mission

**A toolchain- and CI-agnostic quality gate that turns many tools' reports into one gated,
opinionated summary — owned by the team that runs it, and trustworthy when the author is an AI.**

## The frame: existence proof

Not a **product** — one maintainer, crowded category, no distribution; adoption is a scoreboard it
was never built to win. Not a **research instrument** either, the opposite error: that files
shippability under overhead, when "a human can steer and understand it" is *part of the claim*. A
gate that is slow to install or opaque about why it fired has falsified the thesis no matter how
well the argument reads.

> A deterministic, locally-owned, human-legible gate can produce trustworthy, digestible
> confidence about a change — regardless of whether a human or an agent wrote it.

Anyone can write the essay; "coverage is a gameable proxy" has been asserted for twenty years. The
demonstration is a tool that runs, installs cleanly, and is understood by someone who did not write
it — a higher bar where it matters, lower where it doesn't. Hence **distribution work is evidence,
not overhead**: the install path and the readable report are load-bearing for the claim, not polish.

Use outside the authoring repo is *evidence*, not a scoreboard — and there is already enough of it
(a live client Android app with engineers who did not write the tool; a multi-module KMP library on
a different CI). More substrate diversity strengthens the claim; iOS would strengthen it most. But
adoption by teams you are not on is not yours to force, so it is not a success condition.

## The niche

Three edges, structurally unavailable to platforms (GHAS, Checks API) and single-vendor tools
(SonarQube):

1. **Agnostic.** They assume you standardize. A portfolio of client engagements never does — Jenkins here, Bitrise there; Android on one, iOS the next.
2. **Owned and legible.** Server-side gates are applied *to* a team: config elsewhere, opaque rules, nobody can say why it fired — so nobody trusts it, so it gets routed around. Here it is one file in the repo, in the same build the engineer runs. Ownership cuts both ways though: a team that owns the config can weaken it, so it only stays honest paired with gaming-resistant, ratcheting signals.
3. **A deterministic trust boundary for AI-authored change.** A gate must be reproducible to be a gate — you cannot block a merge on a judgment the authoring agent can talk out of. LLM reviewers sit *above* it, not as it.

Not a platform, not a Sonar/CodeQL replacement, and it never runs the analyzers itself — their
output becomes a signal it aggregates and judges.

**Ledger.** For: already the only real gate on a live client app; low bounded maintenance; a working
gate its author can explain end to end is legible evidence of judgment that outlives any employer or
framework era. Against: one maintainer, and it overlaps tools already paid for.

## Completion, and kill criteria

Two exits — conflating them is how a finished thing becomes a permanent one.

**Completion (expected).** The three bar items in [PLAN.md](../PLAN.md) are met; v4 is *done*, and
continuing needs its own justification rather than being decided by the backlog. "Complete, in
maintenance, still useful" is the target state, not a consolation prize.

**Kill (early).** Abandon the bar if **both**: tools already paid for could match the AI-era gating
with less bespoke maintenance, **and** it stops being load-bearing on the projects that run it. Not
failure — refusing to fund a product costume. Note both triggers are observable from where you sit;
neither is a headcount of teams that chose to adopt.

**The failure mode neither exit catches**, and the likely one: never completing, never killing, just
polishing around a bar that stays open — because refining what works is more pleasant than building
the one signal that could show the thesis is wrong.

## The pitch

> As AI writes more of our code, the scarce resource becomes *trusting* that code — and the easy
> metrics are exactly what AI games. BuildChecks is the agnostic, deterministic gate that gives us
> one trustworthy quality summary across every client codebase regardless of toolchain, and because
> the whole gate is one readable file the team owns and runs itself, engineers actually trust and
> enforce it. It's cheap because it aggregates the tools we already have rather than replacing them.

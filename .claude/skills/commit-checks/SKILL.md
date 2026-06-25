---
name: commit-checks
description: Run the pre-commit gate sequence and follow the commit conventions for this repo. Use whenever you are about to commit. Covers the green-suite + golden-hash + Changeability-Index gates, one-logical-change scoping, the commit message format, and the hard prohibitions (no --no-verify, no --amend of pushed commits, no push without confirmation).
---

# Commit checks

Every commit lands with a green suite **and** matching golden hashes. There is no git pre-commit hook in this repo — these gates are run by hand, so run them.

## Gate sequence

```
./gradlew verifyImage                                              # four golden SHA-256 hashes vs C++ reference
./gradlew test jacocoTestReport changeabilityFloor -Pci.floor=90  # green suite + coverage; index must stay above 90
```

On Windows + PowerShell there is no `&&`; chain with `;` and guard: `./gradlew verifyImage; if ($?) { ./gradlew test jacocoTestReport changeabilityFloor -Pci.floor=90 }`. The Bash tool is also available.

- **Hash shifted?** Stop — that's a regression signal. Use the **`output-parity`** skill to investigate; do not rebaseline to make the gate pass.
- **Index below 90?** Fix the regression. Never lower `-Pci.floor` to pass.
- **Pure docs/markdown change** (no `.java`, build config, or scene file touched)? Output parity and the index are structurally unaffected — the test suite is the only meaningful gate. Note that reasoning rather than skipping verification blindly.

## Branching — trunk-based

All work lands on `main`. No feature branches; commit directly to trunk and keep its history linear. Push only with explicit confirmation (below).

## Scope

One logical change per commit. Don't bundle an unrelated refactor with a feature. If a refactor and a behaviour change both apply, split them — the `tdd` loop naturally produces this separation.

## Message

- Subject: reference the phase or feature; imperative mood, concise.
- Body: explain the **why** when it's non-obvious (hidden constraint, preserved C++ quirk, a regression being fixed). Skip the body for trivially self-evident changes.
- End the message with the trailer: `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`
- No emojis unless asked.

## Hard prohibitions

- Never `git commit --no-verify`. A failing gate is a real problem to fix, not to bypass.
- Never `--amend` a pushed commit. Add a new commit instead.
- Never push without **explicit** confirmation from the user.
- Never `@Disabled` a failing test, delete a test for live behaviour, or weaken the Changeability floor to get green.

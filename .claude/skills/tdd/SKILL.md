---
name: tdd
description: Drive any behaviour-changing edit to the ray tracer through Red-Green-Refactor. Use when fixing a bug, adding a primitive/BRDF/light/texture/format/accelerator/sampler/strategy/observer/display, or changing rendering behaviour. Covers writing the failing JUnit 5 test first, minimum code to pass, refactoring under green, the full-coverage standard (every logic flow tested), and the golden-image + Changeability Index gates before commit.
---

# TDD — Red, Green, Refactor

Every behaviour-changing edit follows this loop. "Small" is no excuse — small changes hide regressions. Pure refactors need no new test, but all existing tests and the four golden hashes must stay green.

## 1. Red — failing test first

- Write a JUnit 5 test that fails for the **right reason** (asserts new behaviour, not a compile error).
- Confirm the failure: `./gradlew test --tests fully.qualified.TestClass`.
- Can't make it fail before writing production code? The test is wrong — fix the test.
- New extension point (primitive / BRDF / light / texture / format / accelerator / sampler / strategy / observer / display): test the **interface contract** first, compiling against a fake or `@Disabled` stub before the impl exists.

## 2. Green — smallest code that passes

- Minimum production code to turn the new test green while keeping the rest green.
- Don't refactor mid-green; hardcoded returns and duplication are fine here.
- Run `./gradlew test`.

## 3. Refactor — clean up under green

- Improve names, collapse duplication, extract helpers. Re-run `./gradlew test` after each change.
- A refactor that turns the bar red **changed behaviour** — revert immediately, don't fix forward.
- Stop when it reads cleanly. Don't speculate about future requirements.

## Coverage standard — every logic flow gets a test

Every reachable logic flow must be exercised by **at least one** unit test. This is the bar, not a percentage. For each unit cover:

- **Happy path** — the expected/typical input.
- **Every branch** — both sides of each `if`/`switch`/ternary, each sealed-type case.
- **Edge & boundary cases** — zero, empty, negative, off-by-one limits, parallel/degenerate geometry, `t = 0` / EPSILON self-intersection, critical-angle TIR, clamps (`Math.max(0, …)`).
- **Unhappy paths** — invalid CLI flags, malformed scene JSON, unknown formats, miss/`-1.0` returns, validation that flips `printUsage` or throws at a boundary.

JaCoCo line ≥ 100 % and branch ≥ 90 % are the floor the Changeability Index enforces, not the goal — a flow can be "covered" by line count yet have an untested branch. Genuinely unreachable code (defensive impossible-state guards) and the JavaFX/CLI entry shell (`Main`, `Display`, `JavaFxRenderDisplay`) are exempt; everything with logic is not.

## Recipes

- **Bug fix:** reproduce as a failing test, fix, refactor.
- **New extension point:** test the interface contract first against a fake/stub.
- **Performance change:** pin behaviour with a test, add a timing assertion if relevant, then optimise.
- **Pure refactor:** no new test, but every existing test + the four golden hashes stay green.

## Never bypass the safety net

- Don't `@Disabled` a failing test to ship.
- Don't delete a test for "testing old behaviour" unless that behaviour is being removed **and the user confirmed it**.
- Don't `git commit --no-verify`. A failing hook is a real problem.

## Test conventions

- Plain JUnit 5 assertions (`assertEquals`, `assertTrue`). No AssertJ.
- Prefer **fakes** (recording `RenderDisplay`, recording `PathIntegrator`) over mocks.
- Hot-path methods use the `double[3]` out-param contract: pass a caller-owned scratch array, assert on its mutated contents.

## Gates before committing

```
./gradlew verifyImage                                              # four golden SHA-256 hashes vs C++ reference
./gradlew test jacocoTestReport changeabilityFloor -Pci.floor=90   # suite + coverage; CI must stay above 90
```

- A shifted golden hash is a **regression signal, not a new baseline** — investigate (float reordering, RNG drift, dispatch change) before anything else.
- CI below 90: fix the regression, don't lower the floor.
- Fast inner loop: `./gradlew run --args="--headless --quick"` (~2s).

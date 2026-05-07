# Ray Tracer — Working Agreement

Java 21 distributed ray tracer ported from a 2003-era C++ source. Output must stay **bit-identical** to the C++ reference render across four configs (`quick`, `dof`, `classic`, `cylinder`). The codebase is organised so that future change is cheap; preserve that.

## TDD is the default workflow — Red, Green, Refactor

Every behaviour-changing edit follows this loop. No exceptions for "small" changes — small changes are exactly where regressions hide.

### Red — write the failing test first

- Add a JUnit 5 test that fails for the **right reason** (asserts the new behaviour, not a compile error).
- Run it and confirm the failure: `./gradlew test --tests fully.qualified.TestClass`.
- If you cannot make the test fail before writing production code, the test is wrong — fix the test, not the code.

### Green — smallest code that passes

- Write the **minimum** production code that turns the new test green and keeps every other test green.
- Resist the urge to refactor mid-green. Hardcoded returns and duplicate code are acceptable here — the next step cleans them up.
- Run the full suite: `./gradlew test`.

### Refactor — clean up under a green bar

- With the suite green, improve names, collapse duplication, extract helpers. Re-run `./gradlew test` after every meaningful change.
- If a refactor turns the bar red, **revert immediately** — do not "fix forward" through a red suite. The test that broke is telling you the refactor changed behaviour.
- Stop when the code reads cleanly. Don't speculate about future requirements.

### What this looks like in practice here

- Bug fix: reproduce the bug as a failing JUnit test, fix it, refactor.
- New primitive / BRDF / light / texture / format / accelerator / sampler / strategy / observer / display: write the test against the interface contract first; the test compiles before the impl exists (use a fake or a `@Disabled` impl stub).
- Performance change: pin behaviour with a unit test, add a separate benchmark/timing assertion if relevant, then optimise.
- Pure refactor (no behaviour change): no new test required, but **all existing tests + the four golden image hashes must stay green** end-to-end.

### Never bypass the safety net

- Don't `@Disabled` a failing test to ship. Either fix the test or fix the code.
- Don't delete a test because "it's testing the old behaviour" unless the behaviour itself is being intentionally removed and the user has confirmed that.
- Don't run `git commit --no-verify` to skip hooks. If a hook fails, it's flagging a real problem.

## Output parity is the integration test

The four SHA-256 golden hashes are the contract with the C++ reference. They must not drift.

```
./gradlew verifyImage          # full-resolution gate, all four configs
./gradlew run --args="--headless --quick"   # ~2s smoke for fast iteration
```

If a golden hash shifts after your change, that is a **regression signal, not a baseline to update**. Investigate before doing anything else. Floating-point reorderings, RNG drift, and accidental dispatch changes all show up here first.

## Architectural invariants — preserve these

The codebase has just been refactored across 7 phases for SOLID/KISS/DRY. The contracts below are load-bearing; new work should fit them, not erode them.

### Package boundaries (directed graph, do not invert)

```
io  →  render  →  { scene, shading, geom }
                                      ↓
                                    math
```

- `geom` and `shading` may depend on `math`. Nothing else.
- `scene` may depend on `geom`, `shading`, `math`.
- `render` may depend on `scene`, `shading`, `geom`, `math`.
- `io` sits on top — depends on `render`. Nothing in `render`/`scene`/`shading`/`geom`/`math` may import from `io`.
- The composition root (`Main`, `Bootstrap`, `Display`, `Args`) lives in `com.raytracer` and wires everything together.

### `double[3]` no-allocation contract

Hot paths use caller-owned `double[3]` scratch arrays as out-parameters. Do not introduce a `Vec3` class on the ray-tracing path. New interface methods on `Primitive`, `BRDF`, `Light`, `Texture`, `Accelerator`, `RenderStrategy`, `PathIntegrator` follow the same pattern: pass the output array in, mutate it, return `void` (or a scalar like `double` for `intersect`).

### Sealed hierarchies

`Primitive` (`Sphere | Plane | Triangle | Cylinder | BoundedQuad`) and `Light` (`PointLight | AreaLight`) are sealed. Adding a new variant means adding it to the `permits` clause and exhaustively handling it everywhere `switch` pattern-matches on it. Prefer adding a new strategy/BRDF/texture (open extension points) over a new sealed variant.

### Named owners for C++ quirks

Every C++ behaviour quirk has exactly one owner; do not reintroduce these in another file.

| Quirk | Owner |
|---|---|
| `c = dot(V,V) − 2·r²` sphere intersect | `geom/Sphere.java` |
| `j < 15` point-light shadow caster scan limit | `shading/PointLight.java` |
| Perlin noise seed `12345L` | `shading/PerlinNoise.java` |
| `*7` stratified sampler scrambler | `render/StratifiedSampler.java` |
| `Math.max(0, V·R)` Phong clamp (C `pow(0,n)` returned 0; Java NaN) | `shading/PhongBRDF.java` |
| Per-row `0x9E3779B97F4A7C15L` reseed prime | `render/Renderer.java` |

If you are tempted to "clean up" one of these, stop and confirm with the user — they exist solely to keep the SHA-256 hashes stable.

## Build & run

- Java 21 JDK on PATH; everything else (Gradle, JavaFX) downloads via the wrapper.
- **Always prefer `./gradlew`** over the system Gradle. The system install is at `C:\Gradle\bin\gradle` (not on PATH), and the wrapper pins the version this repo expects.
- `./gradlew test` — full JUnit 5 suite.
- `./gradlew run --args="--headless --quick"` — fastest sanity check (~2s).
- `./gradlew verifyImage` — full-resolution four-config golden gate.

This repo runs on Windows + PowerShell. When chaining commands, use `;` and `if ($?) { ... }` — `&&` does not exist in Windows PowerShell 5.1. Bash is also available via the Bash tool.

## Code style

- No comments unless the **why** is non-obvious (a hidden constraint, a C++ quirk being preserved, a workaround for a known bug). Well-named identifiers describe **what**.
- No multi-paragraph docstrings. One short line is the limit.
- No emojis in code or commits unless the user explicitly asks.
- Don't add error handling, fallbacks, or validation for impossible scenarios. Trust internal callers; validate only at boundaries (CLI args, scene file parsing, image writes).
- Don't introduce abstractions for hypothetical future requirements. Three similar lines beats a premature abstraction.
- Tests use plain JUnit 5 assertions (`assertEquals`, `assertTrue`). No AssertJ. Use **fakes** (recording `RenderDisplay`, recording `PathIntegrator`) over mocks.

## Commits

- Commit per logical change with a green test suite **and** matching golden hashes.
- Reference the phase or feature in the subject. Body explains the **why** when non-obvious.
- Never `--amend` a pushed commit. Never `--no-verify`.
- Never push without explicit user confirmation.

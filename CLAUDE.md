# Ray Tracer — Working Agreement

Java 21 distributed ray tracer ported from 2003-era C++. Output must stay **bit-identical** to the C++ reference across four configs (`quick`, `dof`, `classic`, `cylinder`). The codebase is built so future change is cheap; preserve that.

## Skills — invoke when the task matches

- **`tdd`** ([.claude/skills/tdd/SKILL.md](.claude/skills/tdd/SKILL.md)) — every behaviour-changing edit: Red/Green/Refactor and the full-coverage standard.
- **`output-parity`** ([.claude/skills/output-parity/SKILL.md](.claude/skills/output-parity/SKILL.md)) — golden-hash drift, the four configs, `regenGoldens`.
- **`extend-renderer`** ([.claude/skills/extend-renderer/SKILL.md](.claude/skills/extend-renderer/SKILL.md)) — adding a primitive/BRDF/light/texture/format/sampler/strategy.
- **`commit-checks`** ([.claude/skills/commit-checks/SKILL.md](.claude/skills/commit-checks/SKILL.md)) — pre-commit gate sequence and message conventions.

Library packages (`geom`, `shading`, `render`, `io`, `scene`) and `buildSrc` each carry a nested `CLAUDE.md` with their boundary rule and any quirk owners.

## TDD is the default workflow

Drive every behaviour-changing edit through the **`tdd` skill** (Red → Green → Refactor). Every reachable logic flow gets at least one test — branches, edge/boundary cases, and unhappy paths, not just the happy path. Never `@Disabled` a failing test, delete a test for live behaviour, or `commit --no-verify`.

## Output parity is the integration test

The four SHA-256 golden hashes are the contract with the C++ reference. `./gradlew verifyImage` is the gate; a shifted hash is a **regression signal, not a baseline to update**. Use the **`output-parity` skill** ([.claude/skills/output-parity/SKILL.md](.claude/skills/output-parity/SKILL.md)) to investigate drift (float reorder, RNG drift, dispatch change) or to rebaseline intentionally.

## Architectural invariants — preserve these

Load-bearing contracts from the 7-phase SOLID/KISS/DRY refactor. Fit new work to them; the **`extend-renderer` skill** ([.claude/skills/extend-renderer/SKILL.md](.claude/skills/extend-renderer/SKILL.md)) carries the mechanics for adding a primitive/BRDF/light/texture/format/sampler/strategy.

### Package boundaries (directed, do not invert)

```
io  →  render  →  { scene, shading, geom }  →  math
```

- `geom`, `shading` depend only on `math`.
- `scene` depends on `geom`, `shading`, `math`.
- `render` depends on `scene`, `shading`, `geom`, `math`.
- `io` depends on `render`; nothing below `io` imports from it.
- The composition root (`Main`, `Bootstrap`, `Display`, `Args`) lives in `com.raytracer` and wires it together.

### `double[3]` no-allocation contract

Hot-path methods take caller-owned `double[3]` scratch arrays as out-parameters and mutate them, returning `void` (or a scalar for `intersect`) — no `Vec3` on the ray-tracing path.

### Sealed hierarchies

`Primitive` (`Sphere | Plane | Triangle | Cylinder | BoundedQuad`) and `Light` (`PointLight | AreaLight`) are sealed. Prefer a new strategy/BRDF/texture (open extension point) over a new sealed variant; a new variant means updating `permits` and every exhaustive `switch`.

### Named owners for C++ quirks

Each quirk has exactly one owner; don't reintroduce it elsewhere. To "clean up" any of these, stop and confirm with the user — they exist solely to keep the hashes stable. Each library package also repeats its own owners in a nested `CLAUDE.md`.

| Quirk | Owner |
|---|---|
| `c = dot(V,V) − 2·r²` sphere intersect | `geom/Sphere.java` |
| `j < 15` point-light shadow caster scan limit | `shading/PointLight.java` |
| Perlin noise seed `12345L` | `shading/PerlinNoise.java` |
| `*7` stratified sampler scrambler | `render/StratifiedSampler.java` |
| `Math.max(0, V·R)` Phong clamp (C `pow(0,n)`=0; Java NaN) | `shading/PhongBRDF.java` |
| Per-row `0x9E3779B97F4A7C15L` reseed prime | `Renderer.java` (composition root) |

## Build & run

Java 21 JDK on PATH; the wrapper downloads Gradle and JavaFX. **Always use `./gradlew`**, not the system Gradle (`C:\Gradle\bin\gradle`, unpinned).

- `./gradlew test` — full JUnit 5 suite.
- `./gradlew run --args="--headless --quick"` — fastest sanity check (~2s).
- `./gradlew verifyImage` — four-config golden gate.
- `./gradlew test jacocoTestReport changeabilityIndex` — ISO 25010 Changeability Index; see [docs/changeability-index.md](docs/changeability-index.md).

Windows + PowerShell: chain with `;` and `if ($?) { ... }` — there is no `&&` in PowerShell 5.1. Bash is available via the Bash tool.

## Code style

- No comments unless the **why** is non-obvious (hidden constraint, preserved C++ quirk, known-bug workaround). Names describe the **what**.
- One-line docstrings at most.
- No emojis in code or commits unless asked.
- No error handling or validation for impossible scenarios. Trust internal callers; validate only at boundaries (CLI args, scene parsing, image writes).
- No abstractions for hypothetical futures. Three similar lines beats a premature abstraction.
- Tests: plain JUnit 5 assertions, no AssertJ, **fakes** over mocks.

## Commits

Use the **`commit-checks` skill** ([.claude/skills/commit-checks/SKILL.md](.claude/skills/commit-checks/SKILL.md)) for the full gate sequence and message conventions. Non-negotiable: one logical change per commit with a green suite **and** matching golden hashes; keep the Changeability Index **above 90** (fix the regression, don't lower the floor); never `--amend` a pushed commit, never `--no-verify`, never push without explicit confirmation.

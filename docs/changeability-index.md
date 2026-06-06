# Changeability Index

A single number in `[1, 100]` that measures how easily this codebase absorbs change,
derived from the nine product-quality characteristics of **ISO/IEC 25010:2023** and
weighted by each characteristic's contribution to ease-of-change. 100 is a SOLID-ideal
codebase; 1 is the worst case. It is fully automated — every indicator is computed from
source, test, and coverage artefacts — so the score is comparable across commits and drift
is visible.

```
./gradlew test jacocoTestReport changeabilityIndex   # full computation (regenerates coverage)
./gradlew changeabilityIndex                          # reuses the last JaCoCo report
./gradlew changeabilityFloor -Pci.floor=80            # gate: fail the build below the floor
```

Outputs land in `build/reports/changeability/`:

- `index.json` — machine-readable scores (attributes + indicators).
- `report.md` — human-readable breakdown.
- console — a nine-row summary table plus the grand total.

The committed snapshot of `main` lives at [`tools/changeability/baseline.json`](../tools/changeability/baseline.json)
(**CI ≈ 95.3**); diff future `index.json` against it to spot regressions. A drop below ~85
without an explicit justification in the PR body is a signal worth reviewing.

> `changeabilityIndex` has no hard dependency on `jacocoTestReport` (so re-runs stay fast),
> but it `mustRunAfter` it — when both are on the command line, coverage is read fresh. If no
> report exists, indicators 5.5 and 7.7 score 0 and a warning is printed.

## Formula

Each attribute *A_j* aggregates its indicators *Iᵢ* by their within-attribute weight *wᵢ*:

```
A_j = ( Σ fᵢ(mᵢ) · wᵢ ) / ( Σ wᵢ )
```

where *mᵢ* is the raw measurement and *fᵢ → [0, 100]* its scoring function. Each attribute
carries a top-level weight *W_j* (Σ W_j = 15):

| Weight | Attributes | Rationale |
|---|---|---|
| **3** | Maintainability, Flexibility | Direct drivers of change cost |
| **2** | Reliability, Functional Suitability | Safety nets that make change safe |
| **1** | Performance Efficiency, Compatibility, Interaction Capability, Security, Safety | Constraints / context |

```
CI = clamp( ( Σ A_j · W_j ) / 15 , 1, 100 )
```

## Scoring primitives

Six reusable functions cover every indicator (`ScoringFunctions.kt`):

| Function | Shape |
|---|---|
| `ratioToTarget(v, t)` | `100 · min(1, v/t)` — reward up to a target |
| `passRate(p, n)` | `100 · p/n` |
| `binary(cond)` | `100` / `0` |
| `penaltyPerCount(c, p)` | `100 − p·c`, floored at 0 |
| `invertedDistance(d)` | `100 − 100·d` for `d ∈ [0,1]` |
| `piecewiseLinear(v, lo, hi)` | `100` at/below `lo`, `0` at/above `hi`, linear between |

## Indicator catalogue

Counts are over `src/main/java/` unless noted. Within-attribute weights in brackets.

### 1. Functional Suitability (W=2)
- **1.1** Test pass rate `[3]` — `passed/total` from `build/test-results`.
- **1.2** Golden-hash parity `[3]` — presence of the `verifyImage` gate (see note).
- **1.3** Scene-format completeness `[2]` — `SceneFormat` impls, target 2.
- **1.4** Output-format completeness `[2]` — `ImageWriter` impls, target 3.

### 2. Performance Efficiency (W=1)
- **2.1** Quick smoke path `[1]` — `--quick` fast path present (see note).
- **2.2** Parallel render `[2]` — `.parallel()` present in the pixel loop.
- **2.3** Hot-path no-allocation contract `[2]` — count of array-returning **compute** methods
  (those taking parameters) on the hot-path interfaces (`Primitive`, `BRDF`, `Light`,
  `Texture`, `Accelerator`, `RenderStrategy`, `PathIntegrator`, `Sampler`, `RandomSource`).
  Parameterless cached-array accessors are exempt. (See note — refined from the original plan.)

### 3. Compatibility (W=1)
- **3.1** Output-format breadth `[2]` — `ImageWriter` impls, target 3.
- **3.2** Scene-format breadth `[2]` — `SceneFormat` impls, target 2.
- **3.3** Co-existence isolation `[1]` — files in `geom`/`shading`/`scene`/`render` writing to
  `System.out`/`System.err` (penalty 25 each).

### 4. Interaction Capability (W=1)
- **4.1** CLI flag count `[1]` — distinct `--flags` in `Args.java`, target 12.
- **4.2** README ↔ Args parity `[2]` — symmetric difference of flag sets (penalty 10 each).
- **4.3** Display mode count `[1]` — `RenderDisplay` impls, target 2.

### 5. Reliability (W=2)
- **5.1** Test suite pass rate `[3]` — as 1.1.
- **5.2** Test-to-production LOC ratio `[1]` — target ≥ 0.5.
- **5.3** Boundary validation density `[2]` — `throw new Illegal*Exception` + `printUsage`
  branches, target 8.
- **5.4** Regression gate present `[2]` — `verifyImage` exists.
- **5.5** JaCoCo **line** coverage `[3]` — target ≥ 70 %.

### 6. Security (W=1)
- **6.1** File I/O via `ImageWriter` boundary `[1]` — `FileOutputStream`/`Files.write` outside
  `io/` (penalty 50 each).
- **6.2** No `Math.random` in production `[2]` — binary.
- **6.3** No mutable public fields `[2]` — penalty 5 each.

### 7. Maintainability (W=3)
- **7.1** Files ≤ 250 LOC `[2]` — penalty 15 per oversized file.
- **7.2** Mean file LOC `[2]` — 100 at ≤ 80, 0 at 200.
- **7.3** Package coupling distance `[3]` — mean `|A + I − 1|` over packages (`A` = interface
  ratio, `I = Ce/(Ce+Ca)` from internal imports).
- **7.4** Interface-to-type ratio `[2]` — target ≥ 0.20.
- **7.5** Branching-density proxy `[2]` — branch keywords per method, 100 at ≤ 3, 0 at 12.
- **7.6** Test method density `[3]` — `@Test` per production type, target ≥ 1.5.
- **7.7** JaCoCo **branch** coverage `[3]` — target ≥ 60 %.

### 8. Flexibility (W=3)
- **8.1** Multi-impl interface ratio `[3]` — interfaces with ≥ 2 impls, target ≥ 0.5.
- **8.2** Extension points `[2]` — interfaces in `render`/`io`/`shading`, target 10.
- **8.3** Sealed-hierarchy variants `[2]` — total `permits` entries, target 6.
- **8.4** Wrapper installability `[1]` — `gradle-wrapper.jar` + `application` plugin.
- **8.5** Parallel scalability `[2]` — as 2.2.

### 9. Safety (W=1)
- **9.1** Golden-hash gate present `[2]` — as 5.4.
- **9.2** TODO/FIXME count `[1]` — penalty 10 each.
- **9.3** C++-quirk owners single-sourced `[2]` — each distinctive quirk constant
  (`12345L` Perlin seed, `0x9E3779B97F4A7C15L` row-seed prime) appears in exactly one file
  (penalty 33 per violation).

## Why line and branch coverage live in different attributes

**Line** coverage (breadth — is the code exercised at all?) is a Faultlessness signal under
**Reliability** (5.5). **Branch** coverage (depth — is every decision path exercised?) is a
Testability signal under **Maintainability** (7.7). Both read the same `jacocoTestReport.xml`,
so the marginal cost is one plugin and one task.

## Notes on faithful-but-pragmatic indicators

- **1.2 / 2.1** — true golden-hash parity and a timed smoke render require running renders,
  which is too slow for a routine gauge. Both use fast static proxies (gate presence / fast-path
  presence); the real enforcement is `./gradlew verifyImage`, run separately.
- **2.3** — the original plan counted `new double[` inside `trace()`, but that method
  legitimately allocates local `double[3]` scratch. The contract that actually matters
  (per `CLAUDE.md`) is that hot-path **interface** methods use caller-owned out-params rather
  than returning fresh arrays, so the indicator measures that instead.
- **9.3** — uses the two machine-checkable distinctive constants as representatives of the
  quirk-owner table; the other quirks (`j < 15`, `*7`) are too lexically common to grep
  reliably.

## Extending the index

Indicators and weights live in `AttributeRegistry.kt`; raw extraction in `MetricsCollector.kt`,
`TestResultsReader.kt`, `CoverageReader.kt`; the source→measure mapping in
`IndicatorEvaluator.kt`; aggregation in `Aggregator.kt`; rendering in `ReportWriter.kt`. All live
in `buildSrc/` and are unit-tested (`./gradlew :buildSrc:test`). Add an indicator by registering
it, wiring its measure, and pinning the scoring with a test — the registry invariant
(`Σ W_j = 15`, every indicator evaluated) is enforced by the existing suite.

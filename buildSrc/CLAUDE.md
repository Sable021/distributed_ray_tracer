# buildSrc — Changeability Index

The ISO/IEC 25010 Changeability Index tooling. See [docs/changeability-index.md](../docs/changeability-index.md) for the formula and full indicator catalogue.

- Indicators and weights: `AttributeRegistry.kt` — invariant `Σ W_j = 15`, every indicator evaluated, enforced by the suite.
- Raw extraction: `MetricsCollector.kt`, `TestResultsReader.kt`, `CoverageReader.kt`. The source scanner reads **only `.java` files** under `src/main/java` / `src/test/java`, so docs and nested `CLAUDE.md` files never perturb the score.
- Source → measure: `IndicatorEvaluator.kt` (coverage targets: 5.5 line, 7.7 branch). Aggregation: `Aggregator.kt`. Rendering: `ReportWriter.kt`.

Add an indicator by registering it, wiring its measure, and pinning the scoring with a test. Run `./gradlew :buildSrc:test`.

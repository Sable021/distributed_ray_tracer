---
name: output-parity
description: Investigate and protect the bit-identical output contract with the C++ reference. Use when ./gradlew verifyImage fails, a golden SHA-256 hash shifts, you changed anything on the rendering path (geometry, shading, sampling, RNG, dispatch, float ordering), or you need to regenerate the goldens. Covers the four canonical configs, how to bisect a hash drift, and the regenGoldens escape hatch.
---

# Output parity — the bit-identical contract

Output must stay **bit-identical** to the 2003-era C++ reference. The four SHA-256 golden hashes are the contract; `verifyImage` is the integration test.

```
./gradlew verifyImage                        # render all four configs, hash each PPM, compare
./gradlew run --args="--headless --quick"    # ~2s smoke for fast iteration
```

## The four canonical configs

`verifyImage` renders each into `build/verify/golden_<name>.ppm` and compares its hash against `tools/golden/<name>.ppm.sha256`. Any drift fails the task.

| Config | Args |
|---|---|
| `quick` | `--headless --quick` |
| `dof` | `--headless --mode=dof --quick` |
| `classic` | `--headless --quick --scene=classic.scene.json` |
| `cylinder` | `--headless --quick --scene=cylinder.scene.json` |

Run one in isolation while bisecting: `./gradlew renderGolden_<name>` then hash `build/verify/golden_<name>.ppm`.

## A shifted hash is a regression signal, not a new baseline

Investigate before you even think about `regenGoldens`. Usual culprits, in order of likelihood:

- **Float reordering** — `a*b + c*d` vs `c*d + a*b`, fused multiply-add, changed accumulation order, or a refactor that reassociates a sum. The C++ order is load-bearing.
- **RNG drift** — a changed seed, a different number of draws per pixel/sample, or reordered consumption. See the per-row reseed prime (`Renderer.java`) and the `*7` stratified scrambler (`render/StratifiedSampler.java`).
- **Accidental dispatch change** — a `switch` arm reordered, a sealed variant added, a BRDF/texture/strategy selected differently, or a quirk owner's constant altered.
- **Preserved C++ quirk touched** — the named quirk owners exist solely to keep these hashes stable. Don't "clean them up" without confirming with the user.

Bisect: which of the four configs shifted? `quick`/`dof` isolate camera/DOF and sampling; `classic`/`cylinder` isolate scene parsing and specific primitives. Narrow to the package, then to the float/RNG/dispatch change.

## regenGoldens — intentional rebaseline only

```
./gradlew regenGoldens   # REWRITES tools/golden/<cfg>.ppm.sha256 from current renders
```

Only when output is *intentionally and correctly* changing (and the user has confirmed it) — at the very start of a refactor, never silently to make a red gate pass. A rebaseline that hides an unintended drift is the failure this whole gate exists to prevent.

## Relationship to other skills

- Behaviour changes go through the **`tdd`** skill; this gate is the integration check that backstops the unit tests.
- Before committing, the **`commit-checks`** skill runs `verifyImage` as part of the gate sequence.

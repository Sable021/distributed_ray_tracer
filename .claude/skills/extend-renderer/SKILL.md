---
name: extend-renderer
description: Add a new extension to the ray tracer — a Primitive, BRDF, Light, Texture, SceneFormat, ImageWriter, RenderDisplay, Accelerator, Sampler, RandomSource, RenderStrategy, PathIntegrator, or RenderObserver. Use when introducing a new variant or strategy. Covers the open-extension-point vs sealed-variant decision, the double[3] no-allocation out-param mechanics, the permits + exhaustive-switch update checklist, and where to wire the new type in.
---

# Extending the renderer

Fit new work to the architectural invariants. This skill is the *mechanics*; the rules live in the root `CLAUDE.md`.

## First decide: open extension point or sealed variant?

**Prefer an open extension point.** Adding a new `BRDF`, `Texture`, `RenderStrategy`, `Accelerator`, `Sampler`, `SceneFormat`, `ImageWriter`, `RenderDisplay`, or `RenderObserver` is additive — implement the interface, wire it in, done. The Changeability Index actively rewards this breadth (e.g. `SceneFormat` target 2, `ImageWriter` target 3, `RenderDisplay` target 2).

**Avoid a new sealed variant** unless the new thing is genuinely a new *kind of geometry or light*. `Primitive` (`Sphere | Plane | Triangle | Cylinder | BoundedQuad`) and `Light` (`PointLight | AreaLight`) are sealed; a new variant forces you to update `permits` **and every exhaustive `switch`** over the hierarchy. If the behaviour can be expressed as a strategy/BRDF/texture instead, do that.

## The `double[3]` no-allocation contract

The ray-tracing hot path has **no `Vec3` class** — vectors are caller-owned `double[3]` scratch arrays passed as out-parameters. New methods on the hot-path interfaces follow this exactly:

> Take the output array as a parameter, mutate it in place, return `void` (or a scalar for `intersect`, which returns the ray `t`). Never allocate and return a fresh `double[]` from a per-call compute method.

Hot-path interfaces under this contract: `Primitive`, `BRDF`, `Light`, `Texture`, `Accelerator`, `RenderStrategy`, `PathIntegrator`, `Sampler`, `RandomSource`. A parameterless cached-array accessor (e.g. `double[] diffuseEmission()` returning a stored array) is exempt — it allocates nothing per call. Indicator `2.3` penalises array-returning *compute* methods (those taking parameters) on these interfaces, so the contract is machine-checked.

## Checklist for a new sealed variant (only if unavoidable)

1. Add the class implementing the sealed parent.
2. Add it to the parent's `permits` clause.
3. Update **every** exhaustive `switch` over that hierarchy — the compiler flags the non-exhaustive ones once `permits` changes; chase them all down.
4. Re-run `./gradlew verifyImage` — a new dispatch arm is exactly the kind of change that shifts a golden hash.

## Where to wire it in

The composition root (`Main`, `Bootstrap`, `Display`, `Args` in `com.raytracer`) selects and assembles concrete types — register a new strategy/accelerator/display here. New `SceneFormat`s plug into the scene-parsing boundary; new `ImageWriter`s into `io`. Respect the directed package graph (`io → render → { scene, shading, geom } → math`): the new type imports downward only.

## Always pair with these skills

- **`tdd`** — write the failing interface-contract test first (compile against a fake/stub before the impl exists), then minimum code to pass. Every branch of the new type gets a test.
- **`output-parity`** — run `./gradlew verifyImage` after wiring in. Any new dispatch path, sampling change, or float order can shift a hash; investigate before committing.

## Preserved C++ quirks near these seams

If your change lands in a file that owns a C++ quirk, do not disturb the quirk (it keeps the hashes stable): `geom/Sphere.java` (`c = dot(V,V) − 2·r²`), `shading/PointLight.java` (`j < 15` shadow scan), `shading/PerlinNoise.java` (seed `12345L`), `shading/PhongBRDF.java` (`Math.max(0, V·R)` clamp), `render/StratifiedSampler.java` (`*7` scrambler), `Renderer.java` (per-row reseed prime). To clean any of these up, stop and confirm with the user.

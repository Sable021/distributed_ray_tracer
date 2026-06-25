# render package

Depends on `scene`, `shading`, `geom`, `math` — never on `io`.

Hot-path interfaces here (`Accelerator`, `RenderStrategy`, `PathIntegrator`, `Sampler`, `RandomSource`) use the `double[3]` out-param contract: take the output array, mutate it, return `void`/scalar — never allocate a fresh array in a per-call compute method. See the **`extend-renderer` skill**.

**C++ quirk owner:** `StratifiedSampler.java` keeps the `*7` scrambler. (The per-row reseed prime lives in the composition-root `Renderer.java`, not here.)

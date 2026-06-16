# Ray Tracing Concepts

The rendering techniques in this project, with pointers to the code. The program is a *distributed ray tracer*: classical recursive ray tracing plus many jittered rays per pixel to get antialiasing, soft shadows, glossy reflection, and depth of field by Monte-Carlo averaging.

Pipeline, top-down:

```
Renderer.render()          per-pixel loop, builds primary rays
   └── RayTracer.rayTrace()  recursive shading
         ├── intersectScene()   nearest-hit search
         ├── shadeObject()      Phong (diffuse + specular + ambient)
         ├── reflection()       mirror bounce      → recurse
         └── refraction()       Snell's law bend   → recurse
```

---

## 1. Ray casting and the ray equation

A ray is a half-line `P(t) = origin + t · direction`, `t > 0`. Every visible-surface query asks: for what `t > 0` does this ray first meet a primitive?

- `Ray` (`Ray.java`) holds an origin (`point`) and unit direction (`direct`).
- Intersection tests return that distance `t`, or `-1.0` on a miss (`Intersect.java:20`).
- `EPSILON = 0.0001` (`Intersect.java:17`) discards self-intersection hits — a reflected/refracted ray must not immediately re-hit the surface it left due to float error.

## 2. Camera model and primary rays

The camera is a fixed *eye point* plus a rectangular *screen plane* in front of it. Each pixel maps to a region on that plane; primary rays leave the eye through that region.

- Eye: `EYE = (-0.3, 3.0, 11.0)` (`Renderer.java:53`).
- Screen plane: `x ∈ [-3, 3]`, `y ∈ [1.25, 5.75]`, `z = 8.2` (`Renderer.java:56-60`).
- Per pixel `(j, i)`: build a sub-pixel point on the plane, `direction = sub - EYE`, normalise, fire (`Renderer.java:144-159`).

The simplest pinhole camera; depth-of-field mode (§11) generalises it.

## 3. Ray–primitive intersection

All hit tests live in `Intersect.java`.

### 3.1 Ray–sphere
Substituting the ray equation into `|P − C|² = r²` gives a quadratic solved by discriminant (`Intersect.raySphereIntersect`, `Intersect.java:31`). Smaller positive root wins; both negative → behind the ray.

> Quirk from the C++: the constant term uses `dot(V,V) − 2·r²`, not the textbook `− r²`. The scene was tuned around that mistake, so it stays (`Intersect.java:38`).

### 3.2 Ray–plane
Plane `N·X + d = 0`, so `t = −(d + N·P) / (N·D)` (`Intersect.rayPlaneIntersect`, `Intersect.java:101`). Zero denominator = ray parallel to plane.

### 3.3 Ray–triangle
Barycentric parametrisation `(β, γ)`:

```
v0 + β·(v1 − v0) + γ·(v2 − v0) = origin + t·direction
```

A 3×3 system solved by Cramer's rule (`Intersect.rayTriIntersect`, `Intersect.java:62`). Accepted only when `β > 0`, `γ > 0`, `β + γ < 1` (point inside the triangle).

### 3.4 Bounded quads
Area lights are bounded planes: an infinite plane whose hit is valid only inside a rectangle, checked against stored corners after the plane test (`RayTracer.java:96-119`).

### 3.5 Nearest-hit search
`RayTracer.intersectScene` (`RayTracer.java:58`) scans every active object, keeping the smallest positive `t`. No BVH/k-d tree — 17 primitives, a linear scan is fine.

## 4. Surface normals

`Intersect.getNormal` (`Intersect.java:119`) gives an outward unit normal per type: plane and triangle store it; sphere derives `(hit − centre) / radius`. For refraction inside an object the normal is flipped to face the incoming side (`RayTracer.java:186-189`).

## 5. The Phong shading model

Direct illumination at a hit:

```
colour = ambient
       + Σ_lights  shadow · ( kd · max(0, N·L) · objectColour · lightDiffuse
                            + ks · max(0, V·R)^n · lightSpecular )
```

`N` normal, `L` unit vector to light, `R = 2(N·L)N − L`, `V` view vector, `n` shininess (`RayTracer.shadeObject`, `RayTracer.java:256`).

- `kd`, `ks`, `n` come from the `PhongBRDF` on each `Material` (`shading/PhongBRDF.java`).
- Global ambient `(0.05, 0.05, 0.05)` is added everywhere (`RayTracer.java:24`, `:404`).
- The `max(0, V·R)` clamp before `Math.pow` is a Java fix: `Math.pow` on a negative base with non-integer exponent returns `NaN`, giving black artefacts at glancing angles (`RayTracer.java:312`).

## 6. Shadows — hard and soft

A *shadow ray* from the hit toward a light tests occlusion.

- **Hard shadows** from point lights (small spheres): one shadow ray; opaque blocker → full shadow, refractive blocker → attenuate by `0.6` per hit (`RayTracer.java:285-296`).
- **Soft shadows** from area lights: `AREA_LIGHT_SUB_SAMPLES = 4` rays toward jittered points on the light, averaged → smooth penumbra (`RayTracer.java:336-400`).

Two area lights: a ceiling panel and a back-wall panel that contributes only via reflection (`Scene.java:179-203`).

## 7. Mirror reflection

For incident `I` and normal `N`, `R = I − 2 (I·N) N` (`Intersect.reflection`, `Intersect.java:137`). A new ray fires along `R`, blended by the material's `refl` (`RayTracer.java:206-228`).

## 8. Refraction — Snell's law and total internal reflection

Across media of indices `η_i`/`η_r` (`Intersect.refraction`, `Intersect.java:153`):

```
u = η_i / η_r
T = u · I − (cosθ + u · cosφ) · N
```

with `cosφ = −I·N`, `cosθ = sqrt(1 − u²(1 − cos²φ))`. A negative discriminant means the angle exceeds the critical angle: no refracted ray, return `false` → **total internal reflection**, and the caller falls back to mirror reflection inside the medium (`RayTracer.java:197-202`).

The translucent sphere (`Scene.java:128-139`) uses `rindex = 1.5`, `refr = 0.95`, `refl = 0.15`.

## 9. Recursive ray tracing

`RayTracer.rayTrace` (`RayTracer.java:152`):

1. Cast, find nearest hit. Miss → black; light → white.
2. Local Phong colour at the hit.
3. Refractive → fire refracted (or internally reflected on TIR) ray, recurse.
4. Reflective and *outside* the medium → fire reflected ray, recurse.
5. Combine: `local + refl · reflectColour + refr · refractColour`.

Recursion stops at `maxDepth` (default 6, `Args.java:23`) — without a cap, two mirrors loop forever. The `inside` flag tracks whether the ray is inside a medium so the normal flips and the index swaps to `1.0` (air) on exit (`RayTracer.java:186-189`).

## 10. Distributed ray tracing

"Distributed" = distributed across rays: fire many perturbed rays and average, turning hard effects smooth at compute cost. Four effects use it.

### 10.1 Supersampling (antialiasing)
Each pixel is a `gridX × gridY` (default 8×8 = 64) grid; the sub-pixel point is jittered in each cell, a primary ray cast, the 64 colours averaged (`Renderer.java:150-169`). Removes jaggies.

### 10.2 Soft shadows
Same idea as §6, applied to shadow rays toward an area light.

### 10.3 Glossy reflection
A glossy surface blurs the reflection by jittering the reflected direction:

- A small square sample grid is built **perpendicular** to the perfect-reflection direction at distance `glossiness` from the hit (`RayTracer.glossyGridSample`).
- The reflected ray aims at a random point in that grid (`RayTracer.java:213-227`).

Mirror sphere `glossiness = 5.0`; stacked spheres lower (`Scene.java:96-126`).

### 10.4 Depth of field
A thin-lens camera focuses one *focal plane* and blurs the rest (`Renderer.renderDepthOfField`, `Renderer.java:184`):

1. Cast an eye ray through the pixel, intersect a synthetic focal plane at `z = SCR_Z − DOF_FOCAL_DIST` → the focus point.
2. Distribute `gridX × gridY` ray *origins* across a square aperture on the screen plane.
3. Each ray points from its lens origin to the focus point.

Focal-plane objects converge (sharp); others diverge (blurred), giving real bokeh.

## 11. Stratified sampling

Random sampling clumps and leaves gaps; *stratified* sampling takes one jittered sample per grid cell. Used in three places via `render/StratifiedSampler.cellForRay`:

- Supersampling: one sub-pixel per cell (`Renderer.java:150-156`).
- Glossy reflection: one direction per cell (`RayTracer.glossyGridSample`).
- Soft shadows: one point per cell (`AreaLight.samplePosition`).

The `(rayNum * 7) % gridSize` permutation (`render/StratifiedSampler`) decorrelates adjacent samples — a magic-number scrambler preserved verbatim from the C++. The RNG (`render/ThreadLocalRandomSource`) is a `SplittableRandom` reseeded per scanline (`Renderer.rowSeed`) so parallel renders stay reproducible.

## 12. Procedural textures

The floor and tetrahedron faces compute colour from position; each `Material` carries a `Texture albedo` sampled by `Scene.getObjectColour`.

- **`CheckerTexture`** — checkerboard with coordinates warped through `sin()` per axis.
- **`StripesTexture`** — wood-grain bands: polar radius/angle from the sin-warped hit, radius modulated by `sin(20·angle)`, quantised into two palette colours.
- **`PerlinNoise.noise`** — classic 3-D Perlin noise, fixed-seed permutation. Unused but kept for completeness.

Perlin tables are populated once in a `static {}` block with seed `12345L` for determinism.

## 13. Scene composition

The fixed scene (`Scene.initialise`, `Scene.java:47`) exercises every feature in one frame:

| Index | Object | Why |
|------:|--------|-----|
| 0 | Floor | Procedural checkerboard, slightly reflective |
| 1–4 | Walls + ceiling | Coloured diffuse; ceiling has specular highlights |
| 5 | Mirror sphere | High `refl`, glossy |
| 6, 7 | Stacked spheres | Diffuse + specular + mild reflection |
| 8 | Glass sphere | Refraction + TIR (`rindex = 1.5`) |
| 9–12 | Tetrahedron | Triangles + `stripes` texture |
| 15 | Ceiling panel light | Soft shadows |
| 16 | Back-wall light | Indirect only (skipped on primary rays) |

Indices are stable: the renderer hardcodes `SKIP_AT_DEPTH_1 = 16` so the back-wall light doesn't eclipse the view (`Scene.java:25`, `RayTracer.java:68`).

## 14. From radiance to pixels

Accumulated pixel colour is float `[0, ∞)`. The final step clamps each channel to `[0, 1]`, ×255, packs into ARGB (`Renderer.packArgb`, `Renderer.java:264`). The buffer is bottom-up (row 0 = bottom); the PPM writer (`io/PpmImageWriter`) and JavaFX display (`Display`) each invert rows their own way.

No tone mapping, gamma, or HDR — bright reflections clip to white, a deliberate choice to stay behaviourally identical to the C++ original.

## Further reading

- Whitted, *An Improved Illumination Model for Shaded Display* (1980) — recursive ray tracing.
- Cook, Porter, Carpenter, *Distributed Ray Tracing* (1984) — distributing rays for soft effects.
- Phong, *Illumination for Computer Generated Pictures* (1975) — diffuse + specular shading.
- Perlin, *An Image Synthesizer* (1985) — Perlin noise.
- Shirley & Marschner, *Fundamentals of Computer Graphics* — modern textbook.

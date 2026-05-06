# Ray Tracing Concepts

A walkthrough of the rendering techniques implemented in this project, with pointers to the code that realises each one. The program is a *distributed ray tracer*: it builds on classical recursive ray tracing by firing many jittered rays per pixel to simulate antialiasing, soft shadows, glossy reflection, and depth of field through Monte-Carlo averaging.

The pipeline can be read top-down:

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

A ray is a half-line: `P(t) = origin + t · direction`, with `t > 0`. Every visible-surface query in the program reduces to "for what `t > 0` does this ray first meet a primitive?"

- `Ray` (`src/main/java/com/raytracer/Ray.java`) holds an origin (`point`) and a unit direction (`direct`).
- All intersection tests return that parametric distance `t`, or `-1.0` on a miss (`Intersect.java:20`).

A small `EPSILON = 0.0001` (`Intersect.java:17`) is used to discard "self-intersection" hits — a reflected/refracted ray leaving a surface should not immediately re-hit the surface it just left because of floating-point error.

## 2. Camera model and primary rays

The camera is a fixed *eye point* together with a rectangular *screen plane* in front of it. Every pixel maps to a region on that plane; primary rays leave the eye and pass through that region into the scene.

- Eye: `EYE = (-0.3, 3.0, 11.0)` (`Renderer.java:53`).
- Screen plane: `x ∈ [-3, 3]`, `y ∈ [1.25, 5.75]`, at `z = 8.2` (`Renderer.java:56-60`).
- For each pixel `(j, i)` the renderer constructs a sub-pixel point on that plane, computes `direction = sub - EYE`, normalises it, and fires the ray (`Renderer.java:144-159`).

This is the simplest possible pinhole camera. The depth-of-field mode (Section 11) generalises it.

## 3. Ray–primitive intersection

Three primitive types are supported. All hit tests live in `Intersect.java`.

### 3.1 Ray–sphere
Substituting the ray equation into `|P − C|² = r²` yields a quadratic `t² + b·t + c = 0` solved with the discriminant (`Intersect.raySphereIntersect`, `Intersect.java:31`). The smaller positive root is returned; if both roots are negative the sphere is behind the ray.

> Quirk preserved from the original C++: the constant term uses `dot(V,V) − 2·r²` instead of the textbook `dot(V,V) − r²`. The scene was visually tuned around that mistake, so it is left intact (`Intersect.java:38`).

### 3.2 Ray–plane
A plane is `N·X + d = 0`. Substituting `X = P + t·D` and solving for `t` gives `t = −(d + N·P) / (N·D)` (`Intersect.rayPlaneIntersect`, `Intersect.java:101`). A zero denominator means the ray is parallel to the plane.

### 3.3 Ray–triangle
The triangle is parametrised in *barycentric coordinates* `(β, γ)`:

```
v0 + β·(v1 − v0) + γ·(v2 − v0) = origin + t·direction
```

That is one 3×3 linear system solved with Cramer's rule (`Intersect.rayTriIntersect`, `Intersect.java:62`). The hit is accepted only when `β > 0`, `γ > 0`, and `β + γ < 1`, i.e. the point lies inside the triangle.

### 3.4 Bounded quads
Area lights are *bounded planes* — an infinite plane, but the hit is only valid if the intersection point falls inside a rectangular region on that plane. This is enforced after the plane test by checking the hit's coordinates against the stored corners (`RayTracer.java:96-119`).

### 3.5 Nearest-hit search
`RayTracer.intersectScene` (`RayTracer.java:58`) walks every active object and keeps the smallest positive `t`. There is no acceleration structure (BVH, k-d tree); the scene has only 17 primitives, so a linear scan is fine.

## 4. Surface normals

A consistent outward-pointing unit normal is needed for shading and for reflection/refraction. `Intersect.getNormal` (`Intersect.java:119`) handles each type:

- **Plane**: stored on the object.
- **Triangle**: stored on the object.
- **Sphere**: derived as `(hit − centre) / radius`.

For refraction, when a ray travels *inside* an object, the normal is flipped so it always faces the incoming side (`RayTracer.java:186-189`).

## 5. The Phong shading model

Local (direct) illumination at a hit point is computed as

```
colour = ambient
       + Σ_lights  shadow · ( kd · max(0, N·L) · objectColour · lightDiffuse
                            + ks · max(0, V·R)^n · lightSpecular )
```

where `N` is the surface normal, `L` is the unit vector to the light, `R = 2(N·L)N − L` is the reflection of `L` about `N`, `V` is the view vector (toward the eye), and `n` is the shininess exponent (`RayTracer.shadeObject`, `RayTracer.java:256`).

- `kd` (diffuse), `ks` (specular), and `n` come from the `PhongBRDF` on each `Material` (`shading/PhongBRDF.java`).
- A small global ambient `(0.05, 0.05, 0.05)` is added to every shaded point (`RayTracer.java:24`, `RayTracer.java:404`).
- The `max(0, V·R)` clamp before `Math.pow` is a Java-specific fix: `Math.pow` on a negative base with a non-integer exponent returns `NaN`, which would produce black-pixel artefacts at glancing angles (`RayTracer.java:312`).

## 6. Shadows — hard and soft

A *shadow ray* fired from the hit point toward a light tells us whether the point is occluded.

- **Hard shadows** are produced by point lights (modelled as small spheres). One shadow ray per light: opaque blocker → fully shadowed; refractive blocker → attenuate by `0.6` per hit (`RayTracer.java:285-296`).
- **Soft shadows** are produced by *area lights*. Instead of one shadow ray, the program fires `AREA_LIGHT_SUB_SAMPLES = 4` rays toward jittered points across the light's surface, then averages the contributions (`RayTracer.java:336-400`). Surfaces partially occluded from the light receive a smooth penumbra rather than a hard edge.

Two area lights are present: a ceiling panel above the scene and a back-wall panel that contributes only to indirect illumination via reflection (`Scene.java:179-203`).

## 7. Mirror reflection

For an incident direction `I` hitting a surface with unit normal `N`, the mirror reflection is

```
R = I − 2 (I·N) N
```

(`Intersect.reflection`, `Intersect.java:137`). The recursive ray tracer fires a new ray from the hit point along `R`, and blends its colour into the result weighted by the material's `refl` coefficient (`RayTracer.java:206-228`).

## 8. Refraction — Snell's law and total internal reflection

Refraction across a boundary between media of indices `η_i` (incoming) and `η_r` (outgoing) follows Snell's law. The implementation (`Intersect.refraction`, `Intersect.java:153`) computes

```
u   = η_i / η_r
T   = u · I − (cosθ + u · cosφ) · N
```

where `cosφ = −I·N` and `cosθ = sqrt(1 − u² (1 − cos²φ))`.

When the discriminant under the square root is negative, the angle exceeds the *critical angle*: no refracted ray exists, and the function returns `false` to signal **total internal reflection**. The caller then falls back to mirror reflection inside the medium (`RayTracer.java:197-202`).

The translucent sphere (`Scene.java:128-139`) uses `rindex = 1.5` (≈ glass), `refr = 0.95`, and a small `refl = 0.15` for surface highlights.

## 9. Recursive ray tracing

`RayTracer.rayTrace` (`RayTracer.java:152`) is the heart of the renderer:

1. Cast the ray, find the nearest hit. Miss → black; hit a light → white.
2. Compute local Phong colour at the hit.
3. If the surface is refractive, fire a refracted ray (or internally reflected ray on TIR) and recurse.
4. If the surface is reflective and we are *outside* the medium, fire a reflected ray and recurse.
5. Combine: `local + refl · reflectColour + refr · refractColour`.

Recursion terminates at `maxDepth` (default 6, `Args.java:23`). Without a depth cap, two parallel mirrors would loop forever.

The `inside` flag tracks whether the current ray is travelling inside a refractive medium so the normal can be flipped and the medium's refractive index swapped for `1.0` (air) on exit (`RayTracer.java:186-189`).

## 10. Distributed ray tracing

"Distributed" here means *distributed across rays*: instead of one deterministic ray per effect, the renderer fires many slightly-perturbed rays and averages. This converts aliased/hard effects into smooth, physically-plausible ones at the cost of compute. Four effects use this technique:

### 10.1 Supersampling (antialiasing)
Each pixel is divided into a `gridX × gridY` (default 8×8 = 64) sub-pixel grid. Within each cell the sub-pixel point is *jittered* uniformly inside the cell, a primary ray is cast, and the 64 colours are averaged (`Renderer.java:150-169`). This removes the "jaggies" you would see with a single ray per pixel.

### 10.2 Soft shadows
Already covered in Section 6 — same idea, applied to shadow rays toward an area light.

### 10.3 Glossy reflection
A perfect mirror reflects in exactly one direction; a *glossy* surface (brushed metal, slightly rough plastic) blurs the reflection. Implemented by jittering the reflected direction:

- A small square sample grid is built **perpendicular** to the perfect-reflection direction at distance `glossiness` from the hit point (`Sampling.getSampleVertex`, `Sampling.java:140`).
- The new reflected ray aims at a random point inside that grid instead of along the perfect reflection vector (`RayTracer.java:213-227`).

The mirror sphere uses `glossiness = 5.0` (subtly blurred); the stacked spheres use lower values (`Scene.java:96-126`).

### 10.4 Depth of field
A pinhole camera has every object in focus. A *thin-lens* camera focuses on one *focal plane* and blurs everything else. The DoF mode (`Renderer.renderDepthOfField`, `Renderer.java:184`) simulates this:

1. Cast a normal eye ray through the pixel, intersect a synthetic focal plane at `z = SCR_Z − DOF_FOCAL_DIST`. That intersection is the "focus point."
2. Distribute `gridX × gridY` ray *origins* across a small square aperture (the lens) on the screen plane.
3. Each ray points from its lens origin to the focus point.

Objects on the focal plane converge to a single colour from every lens position → sharp. Objects elsewhere diverge over many lens positions → blurred. This produces real "bokeh" disc highlights for free.

## 11. Stratified sampling

Naïve random sampling clumps points and leaves gaps. *Stratified* sampling divides the domain into a grid and takes one jittered sample per cell, guaranteeing coverage. The technique is used in three places, all built on top of `Sampling.getGridNumber` (`Sampling.java:180`):

- Supersampling: one jittered sub-pixel per grid cell (`Renderer.java:150-156`).
- Glossy reflection: one jittered direction per cell on the reflection sample grid (`Sampling.java:140`).
- Soft shadows: one jittered point per cell on the area light grid (`RayTracer.java:340-348`).

The `(traceNum * 7) % gridSize` index permutation (`Sampling.java:182`) decorrelates adjacent ray samples — a magic-number scrambler preserved verbatim from the C++ original.

The RNG (`Rng.java`) is a `SplittableRandom` reseeded deterministically per scanline (`Renderer.rowSeed`, `Renderer.java:282`) so renders are reproducible despite running in parallel across threads.

## 12. Procedural textures

Two surfaces use procedural (computed-from-position) colours rather than flat material colour: the floor and the four faces of the tetrahedron. Each `Material` carries a `Texture albedo` that is sampled by `Scene.getObjectColour`.

- **`CheckerTexture`** (`shading/CheckerTexture.java`): a checkerboard whose coordinates are first warped through `sin()` per axis, giving a slightly-distorted check on the floor.
- **`StripesTexture`** (`shading/StripesTexture.java`): a wood-grain-style banded pattern. Computes a polar radius and angle from the sin-warped intersection, modulates the radius by `sin(20·angle)`, and quantises into one of two palette colours.
- **`PerlinNoise.noise`** (`shading/PerlinNoise.java`): classic 3-D Perlin noise with a fixed-seed permutation table. Not currently called, but kept for completeness — the original C++ used it for `colourful`-style textures.

The Perlin tables are populated once in a `static {}` block with seed `12345L` so noise is deterministic and does not vary between runs.

## 13. Scene composition

The fixed scene (`Scene.initialise`, `Scene.java:47`) was chosen to exercise every shading feature in one frame:

| Index | Object | Why it's there |
|------:|--------|---------------|
| 0 | Floor | Procedural checkerboard texture, slightly reflective |
| 1–4 | Walls + ceiling | Coloured diffuse panels; ceiling has specular highlights |
| 5 | Mirror sphere | High-`refl`, glossy reflection |
| 6, 7 | Stacked spheres | Diffuse + specular + mild reflection |
| 8 | Glass sphere | Refraction + TIR (`rindex = 1.5`) |
| 9–12 | Tetrahedron | Triangle primitives + `strips` texture |
| 15 | Ceiling panel light | Soft shadows on the scene below |
| 16 | Back-wall light | Indirect illumination only (skipped on primary rays) |

Indices are stable: the renderer hardcodes `SKIP_AT_DEPTH_1 = 16` to avoid the back-wall light eclipsing the camera view (`Scene.java:25`, `RayTracer.java:68`).

## 14. From radiance to pixels

The accumulated colour for each pixel is in floating-point `[0, ∞)`. The final step clamps each channel to `[0, 1]`, multiplies by 255, and packs into a 32-bit ARGB integer (`Renderer.packArgb`, `Renderer.java:264`). The pixel buffer is bottom-up (row 0 = bottom of image); the PPM writer (`io/PpmImageWriter`) and the JavaFX display (`Display`) each handle the row inversion in their own way.

There is no tone mapping, gamma correction, or HDR pipeline — bright reflections clip to white. This was a deliberate choice to keep the port behaviourally identical to the 2003-era C++ original.

## Further reading

- Whitted, *An Improved Illumination Model for Shaded Display* (1980) — the foundational recursive ray tracing paper.
- Cook, Porter, Carpenter, *Distributed Ray Tracing* (1984) — origin of the "distribute rays for soft effects" idea, including DoF and glossy reflection.
- Phong, *Illumination for Computer Generated Pictures* (1975) — the diffuse + specular shading model.
- Perlin, *An Image Synthesizer* (1985) — original Perlin noise.
- Shirley & Marschner, *Fundamentals of Computer Graphics* — modern textbook covering all of the above.

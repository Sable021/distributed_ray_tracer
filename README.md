# Distributed Ray Tracer

A Java 21 port of a 2003-era distributed ray tracer: Phong shading, reflection, refraction, soft shadows from area lights, and Perlin-noise textures. Renders a 1440×1080 image to a JavaFX window and to `raytracing.ppm`. Scene geometry, materials, lights, and camera load from JSON via `--scene=PATH`. The original C++ source is preserved in `legacy/cpp/`.

## Requirements

Java 21 (JDK) on your PATH. Nothing else — the Gradle wrapper (`gradlew`) downloads Gradle and JavaFX on first run.

## Running

```
./gradlew run                                   # JavaFX window + raytracing.ppm (~10 min)
./gradlew run --args="--headless"               # PPM only, no window
./gradlew run --args="--mode=dof"               # depth-of-field (lens blur)
./gradlew run --args="--headless --quick"       # ~2s smoke test (grid 1, depth 2)
./gradlew run --args="--headless --format=png"  # write PNG (or bmp) instead of PPM
./gradlew run --args="--out=myrender.png --format=png"   # custom output path
./gradlew run --args="--scene=classic.scene.json"        # load scene from JSON
./gradlew run --args="--width=1920 --height=1080"        # custom resolution
./gradlew build                                 # build without running
```

The window fills in progressively, with a live ray-count overlay (primary / shadow / reflect / refract) per row, and shows elapsed time in the title when done. The screen plane is fixed at 4:3, so non-4:3 resolutions stretch the scene; render time scales with `width × height`. `classic.scene.json` mirrors the built-in scene exactly and is a good starting point.

## All flags

| Flag | Default | Description |
|------|---------|-------------|
| `--headless` | off | Skip JavaFX window, write image only |
| `--mode=supersampled` | default | Jittered NxN rays per pixel |
| `--mode=dof` | off | Depth-of-field lens simulation |
| `--grid=N` | `8` | Supersample grid side length (NxN rays per pixel) |
| `--depth=N` | `6` | Maximum ray recursion depth |
| `--quick` | off | Shorthand for `--grid=1 --depth=2` |
| `--width=N` | `1440` | Image width in pixels |
| `--height=N` | `1080` | Image height in pixels |
| `--format=ppm\|png\|bmp` | `ppm` | Output image format |
| `--out=PATH` | `raytracing.<format>` | Output file path |
| `--shadow-samples=N` | `4` | Area-light shadow sub-samples per shade call |
| `--tonemap` | off | ACES filmic tone mapping (compresses highlights) |
| `--scene=PATH` | built-in | Load scene + camera from a JSON file |

## Scene file format

JSON passed to `--scene=PATH` supports `//` line comments. Top level:

```json
{
  "camera": { ... },   // optional
  "objects": [ ... ]   // required
}
```

### Camera (all fields optional; omitted fields use built-in defaults)

| Field | Type | Description |
|-------|------|-------------|
| `eye` | `[x, y, z]` | Camera position in world space |
| `screenXLeft` / `screenXRight` | number | Horizontal screen-plane extents |
| `screenYBottom` / `screenYTop` | number | Vertical screen-plane extents |
| `screenZ` | number | Z of the screen plane |
| `dofLensWidth` / `dofLensHeight` | number | Aperture size for DoF mode |
| `dofFocalDist` | number | Focal distance for DoF mode |

### Object types

Each `"objects"` entry has a `"type"`. Material fields (below) apply to all types.

**`"plane"`** — infinite flat surface.

| Field | Type | Description |
|-------|------|-------------|
| `normal` | `[x, y, z]` | Unit outward normal |
| `dist` | number | Signed distance from origin along the normal (`P·n = dist`) |

**`"sphere"`** — solid ball.

| Field | Type | Description |
|-------|------|-------------|
| `centre` | `[x, y, z]` | Centre |
| `radius` | number | Radius |

**`"triangle"`** — single-sided, anticlockwise winding when viewed from the front.

| Field | Type | Description |
|-------|------|-------------|
| `vertices` | `[[x,y,z] × 3]` | Three corner positions |
| `normal` | `[x, y, z]` | Unit normal (precomputed, not derived from vertices) |

**`"cylinder"`** — finite capped cylinder. Lying on its side, set `centre` y = `radius` so the curved surface rests on `y = 0`.

| Field | Type | Description |
|-------|------|-------------|
| `centre` | `[x, y, z]` | Axis midpoint (not a base centre) |
| `axis` | `[x, y, z]` | Unit vector along the length |
| `radius` | number | Cross-section / cap radius |
| `height` | number | Total length along the axis |

**`"area_light"`** — rectangular emitter (plane geometry, shaded as a light).

| Field | Type | Description |
|-------|------|-------------|
| `normal` | `[x, y, z]` | Unit normal pointing away from the lit side |
| `dist` | number | Plane distance (as `"plane"`) |
| `corners` | `[[x,y,z] × 4]` | Four rectangle corners, in order |
| `colour` | `[r, g, b]` | Emitted colour |
| `skipPrimaryRays` | boolean | Primary rays skip this light (for back-wall fill lights) |

### Material fields (all optional, default `0` / `false` / `null`)

| Field | Type | Description |
|-------|------|-------------|
| `colour` | `[r, g, b]` | Base surface colour in [0, 1] |
| `diffuse` | number | Phong diffuse coefficient k_d |
| `specular_r` | number | Phong specular-reflection coefficient k_s |
| `specular_t` | number | Specular-transmission weight (refraction blending) |
| `n` | integer | Phong shininess exponent (higher = tighter highlight) |
| `refl` | number | Mirror-reflection weight (0 none … 1 full mirror) |
| `refr` | number | Refraction weight (0 opaque … 1 fully transmissive) |
| `rindex` | number | Refractive index (1.5 glass, 1.33 water) |
| `glossiness` | number | Glossy-reflection spread (`0` = perfect mirror) |
| `texture` | string | `"checkerboard"` or `"stripes"` |
| `skipPrimaryRays` | boolean | Skip this object for depth-1 (camera) rays |

## Output

`raytracing.ppm` (P6 binary PPM) is written to the repo root — open in GIMP, IrfanView, `ffmpeg`, or ImageMagick `convert`. `--format=png`/`bmp` writes `raytracing.png`/`.bmp` instead. Outputs are gitignored; the C++ reference render at `legacy/cpp/Distributed Ray Tracer/raytracing.ppm` is kept for comparison.

## Project structure

```
src/main/java/com/raytracer/
  Main.java          entry point; routes to JavaFX or headless
  Display.java       JavaFX Application: builds JavaFxRenderDisplay + spawns render daemon
  Bootstrap.java     composition root: drives Renderer through any RenderDisplay
  Renderer.java      parallel row loop dispatching through a RenderStrategy
  RayTracer.java     PathIntegrator: recursive trace(); reports ray events to a RenderObserver
  Scene.java         built-in scene geometry and materials
  CameraConfig.java  camera and screen-plane parameters (record)
  RenderConfig.java  algorithm constants — ambient, shadow samples, etc. (record)
  SceneObject.java   primitive + Material wrapper
  Intersect.java     refraction, reflection, total-internal-reflection helpers
  geom/              sealed Primitive: Sphere, Plane, Triangle, Cylinder, BoundedQuad
  shading/           Material, BRDF (Phong), Light (Point/Area), Texture (Solid/Checker/Stripes/PerlinNoise)
  io/                ImageWriter (Ppm / ImageIo for PNG/BMP), RenderDisplay (Headless / JavaFx)
  scene/             SceneFormat: HardcodedSceneFormat, JsonSceneFormat
  render/            Accelerator, RandomSource, Sampler, PathIntegrator,
                     RenderStrategy (Pinhole / DepthOfField),
                     RenderObserver (RayCounter / ProgressReporter / CompositeObserver) + RayCounts
  VecMath.java       vector math utilities
  Args.java          CLI argument parser

classic.scene.json   JSON mirror of the built-in scene
cylinder.scene.json  classic scene with the glass sphere replaced by a glass cylinder
legacy/cpp/          original C++ source (Visual Studio 2003)
```

## VSCode

Open the repo root with the [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack); the Gradle project imports automatically. Press **F5** to run with the debugger (the `Ray Tracer` config in `.vscode/launch.json`).

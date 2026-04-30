# Distributed Ray Tracer

A Java 21 port of a 2003-era distributed ray tracer. Renders a scene featuring Phong shading, reflection, refraction, soft shadows from area lights, and Perlin-noise textures. Output is a 1440x1080 image displayed in a JavaFX window and written to `raytracing.ppm`. Scene geometry, materials, lights, and camera can be loaded from a JSON file via `--scene=PATH`.

The original C++ source is preserved in `legacy/cpp/` for reference.

## Requirements

- Java 21 (JDK) on your PATH
- No other installs needed — the Gradle wrapper (`gradlew`) downloads Gradle and JavaFX automatically on first run

## Running

### Default — JavaFX window + PPM file

```
./gradlew run
```

Opens a 1440x1080 window that fills in progressively as the scene renders (~10 minutes). A live ray-count overlay (primary / shadow / reflect / refract) updates as each row completes. When done, writes `raytracing.ppm` to the repo root and updates the window title with the elapsed time.

### Headless — PPM file only (no window)

```
./gradlew run --args="--headless"
```

Useful on machines without a display. Writes `raytracing.ppm` and exits.

### Depth-of-field mode

```
./gradlew run --args="--mode=dof"
```

Renders with a simulated lens aperture, producing a defocused background effect. Combine with `--headless` if needed.

### Quick smoke test (fast, low quality)

```
./gradlew run --args="--headless --quick"
```

Renders with a 1x1 sample grid and max depth 2. Completes in ~2 seconds. Useful for verifying the build works before committing to a full render.

### Output as PNG or BMP

```
./gradlew run --args="--headless --format=png"
./gradlew run --args="--headless --format=bmp"
```

Writes `raytracing.png` or `raytracing.bmp` instead of the default `raytracing.ppm`. Useful since most modern image viewers display PNG/BMP without extra tooling.

### Custom output path

```
./gradlew run --args="--headless --out=myrender.png --format=png"
./gradlew run --args="--headless --quick --out=/tmp/test.bmp --format=bmp"
```

Writes the image to the given path instead of the default `raytracing.<format>` in the repo root.

### Load scene from JSON

```
./gradlew run --args="--scene=classic.scene.json"
./gradlew run --args="--headless --quick --format=png --scene=my.scene.json"
```

Load scene geometry, materials, lights, and camera from a JSON file instead of the built-in defaults. `classic.scene.json` in the repo root mirrors the hardcoded scene exactly and serves as a starting point. The `"camera"` section is optional — omit it to use the default viewpoint. Supports `//` line comments.

### Custom resolution

```
./gradlew run --args="--width=1920 --height=1080"
./gradlew run --args="--headless --quick --width=400 --height=300 --format=png"
```

Renders at the specified pixel dimensions instead of the default 1440×1080. The screen-plane geometry is fixed at a 4:3 aspect ratio, so non-4:3 resolutions will visibly stretch the scene. Render time scales linearly with `width × height`.

## All flags

| Flag | Default | Description |
|------|---------|-------------|
| `--headless` | off | Skip JavaFX window, write PPM only |
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
| `--tonemap` | off | Apply ACES filmic tone mapping (compresses highlights, prevents clipping) |
| `--scene=PATH` | built-in | Load scene + camera from a JSON file |

## Scene file format

JSON files passed to `--scene=PATH` support `//` line comments. The top-level object has two keys:

```json
{
  "camera": { ... },   // optional
  "objects": [ ... ]   // required
}
```

### Camera (all fields optional — omitted fields fall back to the built-in defaults)

| Field | Type | Description |
|-------|------|-------------|
| `eye` | `[x, y, z]` | Camera position in world space |
| `screenXLeft` / `screenXRight` | number | Horizontal extents of the screen plane |
| `screenYBottom` / `screenYTop` | number | Vertical extents of the screen plane |
| `screenZ` | number | Z coordinate of the screen plane |
| `dofLensWidth` / `dofLensHeight` | number | Aperture size for depth-of-field mode |
| `dofFocalDist` | number | Focal distance for depth-of-field mode |

### Object types

Each entry in the `"objects"` array has a `"type"` field that determines its geometry. Material fields are listed separately below and apply to all types.

#### `"plane"`
An infinite flat surface.

| Field | Type | Description |
|-------|------|-------------|
| `normal` | `[x, y, z]` | Unit outward normal |
| `dist` | number | Signed distance from the world origin along the normal (`P·n = dist`) |

#### `"sphere"`
A solid ball.

| Field | Type | Description |
|-------|------|-------------|
| `centre` | `[x, y, z]` | Centre of the sphere |
| `radius` | number | Radius |

#### `"triangle"`
A single-sided triangle. Winding order is anticlockwise when viewed from the front (normal side).

| Field | Type | Description |
|-------|------|-------------|
| `vertices` | `[[x,y,z], [x,y,z], [x,y,z]]` | Three corner positions |
| `normal` | `[x, y, z]` | Unit surface normal (precomputed; not derived from vertices) |

#### `"cylinder"`
A finite capped cylinder.

| Field | Type | Description |
|-------|------|-------------|
| `centre` | `[x, y, z]` | Midpoint of the axis (not a base centre) |
| `axis` | `[x, y, z]` | Unit vector along the cylinder's length |
| `radius` | number | Radius of the circular cross-section and end caps |
| `height` | number | Total length along the axis |

The cylinder has two flat disc end-caps. When lying on its side, set the `centre` y-coordinate equal to `radius` so the curved surface rests exactly on the floor (y = 0).

#### `"area_light"`
A rectangular light source. Uses plane geometry internally but is shaded as an emitter.

| Field | Type | Description |
|-------|------|-------------|
| `normal` | `[x, y, z]` | Unit normal pointing away from the lit side |
| `dist` | number | Plane distance (same convention as `"plane"`) |
| `corners` | `[[x,y,z] × 4]` | Four corners of the rectangle (in order) |
| `colour` | `[r, g, b]` | Emitted light colour |
| `skipPrimaryRays` | boolean | If `true`, primary (camera) rays skip this light — use for back-wall fill lights that would otherwise eclipse the scene |

### Material fields (all optional, default `0` / `false` / `null`)

| Field | Type | Description |
|-------|------|-------------|
| `colour` | `[r, g, b]` | Base surface colour in [0, 1] |
| `diffuse` | number | Phong diffuse coefficient k_d |
| `specular_r` | number | Phong specular-reflection coefficient k_s |
| `specular_t` | number | Specular-transmission weight (refraction blending) |
| `n` | integer | Phong shininess exponent — higher = tighter highlight |
| `refl` | number | Mirror-reflection weight (0 = none, 1 = full mirror) |
| `refr` | number | Refraction weight (0 = opaque, 1 = fully transmissive) |
| `rindex` | number | Refractive index (e.g. `1.5` for glass, `1.33` for water) |
| `glossiness` | number | Glossy-reflection spread radius — `0` = perfect mirror |
| `texture` | string | Procedural texture: `"checkerboard"` or `"stripes"` |
| `skipPrimaryRays` | boolean | Skip this object for depth-1 (camera) rays |

## Output

By default, `raytracing.ppm` is written to the repo root in P6 (binary PPM) format. Open it with any image viewer that supports PPM (e.g. GIMP, IrfanView, `ffmpeg`, `convert` from ImageMagick). With `--format=png` or `--format=bmp`, the renderer writes `raytracing.png` or `raytracing.bmp` instead, both viewable in any standard image viewer.

Output files are excluded from git by `.gitignore`. The C++ reference render at `legacy/cpp/Distributed Ray Tracer/raytracing.ppm` is preserved for visual comparison.

## Building without running

```
./gradlew build
```

## Project structure

```
src/main/java/com/raytracer/
  Main.java          entry point; routes to JavaFX or headless
  Display.java       JavaFX window with progressive scanline upload and ray-count overlay
  Renderer.java      pixel loop (supersampled and DoF modes)
  RayTracer.java     recursive ray_trace(); tracks per-type ray counts
  Scene.java         built-in scene geometry and materials
  SceneLoader.java   Gson-based JSON scene parser
  CameraConfig.java  camera and screen-plane parameters (record)
  RenderConfig.java  algorithm constants — ambient, shadow samples, etc. (record)
  SceneObject.java   per-object material, geometry, and texture properties
  Intersect.java     ray/sphere, ray/triangle, ray/plane, ray/cylinder
  Textures.java      Perlin noise, checkerboard, strips
  Sampling.java      light grid and glossy sample helpers
  VecMath.java       vector math utilities
  PpmIO.java         P6 PPM writer
  Rng.java           deterministic RNG (SplittableRandom, fixed seed)
  Args.java          CLI argument parser

classic.scene.json   JSON mirror of the built-in scene (edit to customise)
cylinder.scene.json  classic scene with the glass sphere replaced by a glass cylinder
legacy/cpp/          original C++ source (Visual Studio 2003)
```

## VSCode

Open the repo root in VSCode with the [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) installed. The Gradle project imports automatically. Press **F5** to run with the debugger attached (uses the `Ray Tracer` launch config in `.vscode/launch.json`).

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
| `--scene=PATH` | built-in | Load scene + camera from a JSON file |

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
  SceneObject.java   per-object material, geometry, and texture properties
  Intersect.java     ray/sphere, ray/triangle, ray/plane
  Textures.java      Perlin noise, checkerboard, strips
  Sampling.java      light grid and glossy sample helpers
  VecMath.java       vector math utilities
  PpmIO.java         P6 PPM writer
  Rng.java           deterministic RNG (SplittableRandom, fixed seed)
  Args.java          CLI argument parser

classic.scene.json  JSON mirror of the built-in scene (edit to customise)
legacy/cpp/         original C++ source (Visual Studio 2003)
```

## VSCode

Open the repo root in VSCode with the [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) installed. The Gradle project imports automatically. Press **F5** to run with the debugger attached (uses the `Ray Tracer` launch config in `.vscode/launch.json`).

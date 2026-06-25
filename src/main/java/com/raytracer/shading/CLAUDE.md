# shading package

Depends only on `math`. BRDFs, lights, textures, materials — no `scene`, `render`, or `io` imports.

`Light` (`PointLight | AreaLight`) is sealed; see the **`extend-renderer` skill** before adding a variant. New BRDFs/textures are open extension points — prefer them over a new sealed variant.

**C++ quirk owners (one file each, don't reintroduce elsewhere; confirm before touching):**
- `PointLight.java` — `j < 15` shadow-caster scan limit.
- `PerlinNoise.java` — seed `12345L`.
- `PhongBRDF.java` — `Math.max(0, V·R)` clamp (C `pow(0,n)` = 0; raw Java would give NaN).

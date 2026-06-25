# geom package

Depends only on `math`. Pure geometry — no `shading`, `scene`, `render`, or `io` imports.

`Primitive` (`Sphere | Plane | Triangle | Cylinder | BoundedQuad`) is sealed; see the **`extend-renderer` skill** before adding a variant (it forces a `permits` + every-exhaustive-`switch` update).

**C++ quirk owner:** `Sphere.java` keeps the `c = dot(V,V) − 2·r²` intersect form. It is deliberately not the textbook formula — it preserves the golden hashes. Do not "fix" it; confirm with the user first.

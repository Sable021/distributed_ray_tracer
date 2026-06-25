# scene package

Depends on `geom`, `shading`, `math`. Scene construction and parsing — no `render` or `io` imports.

`SceneFormat` (implemented by `JsonSceneFormat`, `HardcodedSceneFormat`) is the open extension point here — add new formats via the **`extend-renderer` skill**. Validate at the parsing boundary (malformed scene JSON throws); trust internal callers everywhere else. No quirk owners live here.

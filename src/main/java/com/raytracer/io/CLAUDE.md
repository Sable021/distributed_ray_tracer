# io package

The top of the directed graph: depends on `render` (and below). Nothing below `io` imports from it — keep it that way.

`ImageWriter` and `RenderDisplay` are open extension points (the Changeability Index rewards output-format and display breadth). Add new writers/displays via the **`extend-renderer` skill**; `SceneFormat` lives in `scene`, not here. File I/O (`FileOutputStream` / `Files.write`) belongs only in this package — indicator 6.1 penalises it elsewhere.

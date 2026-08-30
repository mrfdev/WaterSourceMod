# WaterSourceMod domain context

## Glossary

- **Source water**: A water fluid state whose fluid reports itself as a
  source. In the overlay it receives the source marker treatment.
- **Flowing water**: A water fluid state that is not a source. In the overlay
  it receives the flowing marker treatment.
- **Waterlogged source**: A source water fluid state held inside a block with
  the waterlogged property. It can be included or excluded independently.
- **Overlay**: The temporary client-side visualization controlled by the
  configured toggle key.
- **Marker snapshot**: The last completed set of water markers valid for the
  current client level and scan area. It may remain visible during a refresh
  only while that validity scope remains unchanged.
- **Stale snapshot**: A still-valid marker snapshot retained while a replacement
  scan of the same scope is in progress. The HUD identifies this temporary
  state; scope changes invalidate it instead of retaining it.
- **Nearest marker**: The closest marker eligible under the current category,
  waterlogged, and render-distance filters. Optional HUD and world cues consume
  the same nearest-marker result.
- **Scan radius**: The number of chunks outward from the player chunk in the
  X and Z directions. Radius 0 covers only the current chunk, radius 1 covers
  3x3 chunks, and radius 2 covers 5x5.
- **Vertical scan range**: The Y-level band in which loaded blocks are
  considered, either around the player or across the full build height.
- **Rescan mode**: The rule for refreshing a marker snapshot after its initial
  scan: timed automatic refresh, client block updates, or manual requests.
- **Discovery limit**: The maximum number of markers retained in a completed
  marker snapshot.
- **Visible limit**: The maximum eligible markers drawn from a marker snapshot
  in one rendered frame.
- **Scan-area snapshot**: An immutable primitive grid describing whether each
  chunk in one scan scope was loaded, skipped, or is still pending. It never
  contains chunk references and never causes a chunk request.
- **Scan boundary**: An optional world-space projection of the scan-area
  snapshot. The current chunk and loaded, skipped, or pending chunks use
  distinct frame treatments.
- **Marker pattern**: A non-color silhouette such as a filled box, pillar,
  beacon, hollow box, stripes, or dots used to distinguish marker categories.
- **Local profile**: A named bundle of overlay settings stored and applied only
  on the client.
- **Color palette**: The source, flowing, and outline colors treated as one
  selectable local preset or saved custom set.
- **Through-wall marker**: A marker rendered without depth testing. It is off
  by default because servers may treat this visibility as an x-ray-like aid.
- **Loaded chunk**: A chunk already present in the client world. The mod never
  requests additional chunks from the server.

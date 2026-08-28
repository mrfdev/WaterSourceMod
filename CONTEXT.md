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
- **Scan radius**: The number of chunks outward from the player chunk in the
  X and Z directions. Radius 1 covers 3x3 chunks; radius 2 covers 5x5.
- **Through-wall marker**: A marker rendered without depth testing. It is off
  by default because servers may treat this visibility as an x-ray-like aid.
- **Loaded chunk**: A chunk already present in the client world. The mod never
  requests additional chunks from the server.

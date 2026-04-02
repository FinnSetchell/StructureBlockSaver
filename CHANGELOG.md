# Changelog

## [1.0.0] - 2026-04-02

### Added
- `/sbs wand` — gives the Structure Wand (wooden axe) for selecting areas
- `/sbs clear` — clears the current wand selection
- `/sbs save` — saves all structure blocks in SAVE mode within the wand selection
- `/sbs save <pos1> <pos2>` — saves with explicit coordinates
- `/sbs save dry-run` — previews what would be saved without writing any files
- `/sbs save <pos1> <pos2> <filter>` — saves only structures matching a glob pattern
- `/sbs filteredsave <filter>` — saves structures matching a filter using wand selection
- `/sbs filteredsave <filter> dry-run` — dry-run variant of filtered save
- `/sbs filteredsave <filter> <pos1> <pos2>` — filtered save with explicit coordinates
- `/sbs filteredsave <filter> dry-run <pos1> <pos2>` — dry-run with explicit coordinates
- Visual overlay showing the selection bounding box, individual structure blocks, and each structure's save region
- Individual position markers appear as soon as a single wand position is selected

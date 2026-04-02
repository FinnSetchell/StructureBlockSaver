# Changelog

## [1.1.1] - 2026-04-02

### Fixed
- Structure blocks and their save regions no longer failing to highlight in the wand renderer after the 1.1.0 caching optimisation

## [1.1.0] - 2026-04-02

### Added
- `/sbs autosave on [<seconds>]` — periodically re-saves all structure blocks in the wand selection. Default interval is 30s, configurable from 5–3600s
- `/sbs autosave off` — stops the auto-save
- `/sbs list` and `/sbs list <pos1> <pos2>` — lists every structure block in the area with its mode, name, and save region size
- Scroll wheel expand/contract: while holding the wand with both positions set, scroll moves the most recently set position one block in the direction you are facing (including up/down based on pitch)
- HUD dimensions display: shows `W x H x D` above the hotbar while holding the wand with both positions set

### Changed
- Wand renderer now caches structure block data and only re-scans when the selection changes, eliminating per-frame block entity lookups
- Selections larger than 50,000 blocks skip the per-block scan and render only the bounding box
- Save, list, and auto-save scans now skip unloaded chunks instead of forcing chunk loads
- Fixed GitHub Actions workflows using JDK 25 (unavailable); now use JDK 21

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

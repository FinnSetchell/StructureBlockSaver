# Structure Block Saver

A small Fabric 1.21.1 utility mod for saving lots of structure blocks at once. If you make structure mods with hundreds of structures, clicking save on each block individually is painful — this fixes that.

> **Disclaimer:** This was vibe coded to speed up a slow manual process. It's not polished, not intended for mainstream use, and probably has rough edges. Use at your own risk.

---

## Usage

All commands require operator permissions (level 2).

### Wand

Run `/sbs wand` to get the Structure Wand (a wooden axe).

- **Right-click** a block to set position 1
- **Left-click** a block to set position 2
- **Scroll wheel** (with both positions set) to move the most recently set position one block in the direction you're facing
- Run `/sbs clear` to reset your selection

A yellow outline shows your selection. Structure blocks in SAVE mode are highlighted in cyan, and their save regions in green. While holding the wand, the selection dimensions (`W x H x D`) are shown above the hotbar.

### Saving

```
/sbs save
/sbs save <pos1> <pos2>
/sbs save dry-run
/sbs save <pos1> <pos2> <filter>
```

Running `/sbs save` with no coordinates uses your wand selection. Add `dry-run` to preview what would be saved without writing anything. Add a filter (glob pattern) to only save structures whose name matches — e.g. `mymod:village/*`.

```
/sbs filteredsave <filter>
/sbs filteredsave <filter> dry-run
/sbs filteredsave <filter> <pos1> <pos2>
/sbs filteredsave <filter> dry-run <pos1> <pos2>
```

Same as above but the filter is required and coordinates are optional.

### Auto-save

```
/sbs autosave on
/sbs autosave on <seconds>
/sbs autosave off
```

Periodically re-saves all structure blocks in your current wand selection. Defaults to every 30 seconds, configurable from 5–3600. Useful when actively building and wanting changes captured automatically.

### Listing

```
/sbs list
/sbs list <pos1> <pos2>
```

Lists every structure block in the area with its mode (SAVE, LOAD, CORNER, DATA), name, and save region size. Useful for auditing large builds.

---

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.18.6+
- Fabric API

# AKAI MPK-261

The reference for the layout and the protocol is the book at
`books/src/main/typst/akai-mpk261/`. The `.syx` files below are the examples
used in that book.

- `MPK-261.mmap`: memory map of the instrument. It contains 3 memory spaces:
  **CurrentPreset** (zone **Preset Edit Buffer**, 1544 bytes), **Global** (zone
  **Global Settings**, 377 bytes) and **UserPresets** (zone **User Presets**,
  30 presets of 1546 bytes). The 2 RAM spaces both start at `0x0000`; the SysEx
  command selects the space (`QEDIT`/`WEDIT`, `QGLOB`/`WGLOB`). The flash presets
  are addressed from `0x0000` by preset number (`QPRESET`/`WPRESET`/`PPRESET`),
  not by byte offset. The command to space link lives in `AkaiMemorySpace`, and
  `AkaiDevice` validates at load time that the mmap declares exactly those
  spaces. `DeviceMemory` address lookup alone cannot distinguish the overlapping
  spaces.
- `MPK-261.syx`: identity reply of the unit (`F0 7E ...`), not a preset dump.
- `preset_08_StudOne5.syx`: the preset used throughout the book
  `books/src/main/typst/akai-mpk261/`, preset 8, 1555 bytes.
- `dump_all.syx`: the 30 factory user presets, as a sequence of `WPRESET`
  frames, 46650 bytes.

The mmap is registered in `Manufacturer.AKAI` as the `MPK-261` device (model
code `0x25`). `Device.loadMemoryMap()` loads this file through the manufacturer
title `AKAI`. The AKAI SysEx parser loads the map when it parses a `WPRESET`,
`WEDIT` or `WGLOB` frame.

## Dump commands

The memory space is selected by name; the CLI never takes MIDI command bytes.

```text
dump-memory -m "MPK-261" -r CurrentPreset -i "<in>" -o "<out>" -f target/current.syx
dump-memory -m "MPK-261" -r Global        -i "<in>" -o "<out>" -f target/global.syx
dump-memory -m "MPK-261" -r UserPresets   -i "<in>" -o "<out>" -f target/presets.syx

read-memory -m "MPK-261" -r Global      -a 0008 -s 1 -i "<in>" -o "<out>"
read-memory -m "MPK-261" -r UserPresets -a 0C0C -s 1 -i "<in>" -o "<out>"
```

- `CurrentPreset` and `Global` are byte-addressed; the dump is split into
  blocks of 127 bytes and sent with `QEDIT` / `QGLOB`.
- `UserPresets` is preset-addressed. `dump-memory` sends `QPRESET 1` to
  `QPRESET 30` and concatenates the 30 `WPRESET` answers in one file.
  `read-memory` requests the same 30 presets, then reads the requested address
  from the whole space. The address is relative to the start of `UserPresets`
  (`0000`), so it can point inside any preset or span two consecutive presets.
  `WPRESET 0` is the edit buffer and is never used.
- Without `-r`, only a mmap that defines the single implicit `Default` space is
  accepted. This keeps the Roland dumps working unchanged.

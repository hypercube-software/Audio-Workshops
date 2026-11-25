#import "../common/settings.typ": *
#import "lib.typ": *

#part("Drum maps and bulk dump")

#chapter("Drum maps", l: "chap-drum-maps")

== The information string

The block at `0x403000` holds 32 ASCII bytes that identify the firmware of the unit. On a typical unit it reads
`GS Standard  VER=1.20MA1.10`, padded with spaces. It is a plain read-only string.

== The two drum maps

A drum map describes what every note of a drum part plays: its pitch, its level, its group, its pan, its reverb and
its chorus, plus whether it answers note-on and note-off. The unit holds 2 maps:

- The map 1, named at `0x410000`, its settings at `0x410100`.
- The map 2, named at `0x411000`, its settings at `0x411100`.

A part selects its drum map with the *Use for Rhythm Part* field of its patch block, described in
#cref(<chap-patch-params>).

== The name

The name is 12 ASCII characters at `0x410000` (map 1) and `0x411000` (map 2), padded with spaces. The factory maps
read `STANDARD` and `ROOM`, but the names are stored and can be changed.

== The settings

The settings of a map are 1024 bytes, organised as 8 arrays of 128 bytes. The index of an array entry is the MIDI
note number, from `0x00` to `0x7F`.

#figure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Offset*], [*Size*], [*Array*], [*Meaning*]),
        [`00 00`], [128], [Play Note Number], [The note number of the sound the key plays],
        [`00 80`], [128], [Level], [The level of the note],
        [`01 00`], [128], [Assign Group], [The group the note belongs to],
        [`01 80`], [128], [Pan], [The pan of the note],
        [`02 00`], [128], [Reverb], [The reverb send of the note],
        [`02 80`], [128], [Chorus], [The chorus send of the note],
        [`03 00`], [128], [Rx Note Off], [Whether the note answers note-off, `00` or `01`],
        [`03 80`], [128], [Rx Note On], [Whether the note answers note-on, `00` or `01`],
    ),
    caption: [Layout of a drum map],
)

The offsets are packed: the settings of map 1 start at `0x410100`, so the *Level* array is at `0x410180`, and the
settings of map 2 start at `0x411100`, so its *Level* array is at `0x411180`.

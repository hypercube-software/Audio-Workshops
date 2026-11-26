#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("The bulk dump", l: "chap-bulk-dump")

The bulk dump returns the whole working memory of the unit in a single answer. It lives at `0x480000` and uses the
*NIBBLES* form described in #cref(<chap-encodings>): every value occupies 2 nibbles, so a reader must recombine
2 address units into 1 byte.

== Requesting the dump

An `RQ1` at `0x480000` with the packed size `00 1D 10` asks for the whole dump. The instrument answers with a `DT1`
of `1864` values, split over several MIDI frames. A request with a shorter size is rejected.

== Payload layout

The dump is the system settings, the patch common and the 16 patch blocks, in that order:

#figure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Offset*], [*Size*], [*Block*], [*Meaning*]),
        [`00 00`], [8], [System], [The system settings in NIBBLES form],
        [`00 08`], [50], [Patch Common], [The name, the partials, the reverb and the chorus],
        [`00 3A`], [14], [Reserved], [14 bytes, read as `00`],
        [`00 48`], [112], [Patch block 1], [The first patch block, see below],
        [`00 B8`], [112], [Patch block 2], [The second patch block],
    ),
    caption: [Layout of the bulk dump],
)

The 16 patch blocks follow one another, 112 values each. The offsets are packed and in nibbles, so the block 1 is at
`0x480048`, the block 2 at `0x4800B8`, and the block 16 at `0x4807C8`.

== The patch block in NIBBLES form

A bulk patch block carries the same settings as a regular patch block, but in a compact form: the 16 receive
switches are packed into a single 2-byte mask, and the sends are stored inline.

#figure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Offset*], [*Size*], [*Parameter*], [*Meaning*]),
        [`00`], [2], [Tone Number], [The tone number, as in the regular block],
        [`02`], [2], [Rx Mask], [The 16 receive switches, one bit each],
        [`04`], [1], [Channel], [The MIDI channel minus 1],
        [`05`], [1], [Reserved], [Reads `B0` on the drum block, `81` elsewhere],
        [`06`], [1], [Pitch Key Shift], [The coarse pitch, `-24` to `+24` semitones],
        [`07`], [1], [Pitch Offset Fine], [The fine pitch],
        [`08`], [1], [Level], [The part level],
        [`09`], [1], [Part Panpot], [The part pan],
        [`0A`], [1], [Velocity Sense Depth], [The velocity sensitivity],
        [`0B`], [1], [Velocity Sense Offset], [The velocity offset],
        [`0C`], [1], [Note range low], [The lowest note],
        [`0D`], [1], [Note range high], [The highest note],
        [`0E`], [1], [Chorus Send Level], [The chorus send level],
        [`0F`], [1], [Reverb Send Level], [The reverb send level],
        [`10`], [8], [Tone Parameters], [The 8 tone parameters],
        [`18`], [2], [Reserved], [2 bytes],
        [`1A`], [12], [Scale Tuning], [12 bytes, one per note of the chromatic scale],
        [`26`], [1], [CC1], [The first controller destination],
        [`27`], [1], [CC2], [The second controller destination],
        [`28`], [72], [Sends], [The 6 sends, 12 bytes each],
    ),
    caption: [Layout of a bulk patch block],
)

== The bulk drum maps

The 2 drum maps are not part of the bulk dump at `0x480000`. Each one is requested separately, also in NIBBLES form:

#figure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed size*], [*Map*], [*Content*]),
        [`49 00 00`], [`00 02 00`], [Map 1 key], [The note numbers],
        [`49 02 00`], [`00 02 00`], [Map 1 level], [The levels],
        [`49 04 00`], [`00 02 00`], [Map 1 group], [The groups],
        [`49 06 00`], [`00 02 00`], [Map 1 pan], [The pans],
        [`49 08 00`], [`00 02 00`], [Map 1 reverb], [The reverb sends],
        [`49 0A 00`], [`00 02 00`], [Map 1 chorus], [The chorus sends],
        [`49 0C 00`], [`00 02 00`], [Map 1 Rx note], [Whether each note answers note-on and note-off],
        [`49 0E 00`], [`00 00 18`], [Map 1 name], [The 12-byte name],
        [`49 10 00`], [`00 02 00`], [Map 2 key], [The note numbers],
        [`49 12 00`], [`00 02 00`], [Map 2 level], [The levels],
        [`49 14 00`], [`00 02 00`], [Map 2 group], [The groups],
        [`49 16 00`], [`00 02 00`], [Map 2 pan], [The pans],
        [`49 18 00`], [`00 02 00`], [Map 2 reverb], [The reverb sends],
        [`49 1A 00`], [`00 02 00`], [Map 2 chorus], [The chorus sends],
        [`49 1C 00`], [`00 02 00`], [Map 2 Rx note], [Whether each note answers note-on and note-off],
        [`49 1E 00`], [`00 00 18`], [Map 2 name], [The 12-byte name],
    ),
    caption: [The bulk drum maps],
)

Each `00 02 00` request returns 256 nibbles, that is 128 values, one per note of the map. A request with a shorter
size is rejected.

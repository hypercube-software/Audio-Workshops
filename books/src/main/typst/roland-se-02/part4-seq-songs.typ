#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Songs", l: "chap-song")

This unit can store 16 songs. A song chains sixteen parts. Each part names a pattern, the patch to play it with, and a repeat count, and the
sequencer plays the parts in order. A song is 144 bytes, exactly like a pattern, but the two blocks share nothing but
their size.

#figure(
    table(
        align: left,
        columns: (auto, auto, auto, 1fr),
        fill: (x, y) => if y == 0 { luma(230) },
        stroke: 0.5pt,
        head([*Packed address*], [*Packed Size*], [*Block*], [*Content*]),
        [`20 00 00 00`], [`00 00 01 10`], [Song edit buffer], [Current song loaded into the sequencer],
        [`21 nn 00 00`], [`00 00 01 10`], [Stored song], [Song number `nn`, from 0 to 15],
    ),
    caption: [Addresses of the song blocks],
)

There are 16 songs, and like the patterns they are numbered from one on the instrument: song 0 is displayed
as `1`.

Selecting a song on the unit copies it into the edit buffer.

A stored song is played by copying it into the edit buffer: read its 144 bytes at `0x21nn0000`, then write them at
`0x20000000`.

== Header

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed Size*], [*Button*], [*Field*], [*Values*], [*Ref.*], [*Meaning*]),
        [`20 00 00 00`],
        [`00 00 00 03`],
        [`3`],
        [Tempo],
        [12 bits],
        [`03 02 00`],
        [Tempo `120` BPM encoded as `03 02 00`, see #cref(<chap-address>)],
        [`20 00 00 03`],
        [`00 00 00 01`],
        [`1`],
        [Destination],
        [`0`..`2`],
        [`00`],
        [`Int`, `Ext`, `Both`],
        [`20 00 00 04`],
        [`00 00 00 01`],
        [`2`],
        [Sync],
        [`0` `1`],
        [`00`],
        [`Int`, `MIDI`],
        [`20 00 00 05`],
        [`00 00 00 01`],
        [`4`],
        [Key trigger],
        [`0`..`2`],
        [`00`],
        [`OFF`, `On`, `trn`],
        [`20 00 00 06`],
        [`00 00 00 01`],
        [`5`],
        [Loop],
        [`0` `1`],
        [`00`],
        [`Off`, `On`],
        [`20 00 00 07`],
        [`00 00 00 09`],
        [—],
        [Reserved],
        [`00`],
        [nine zeros],
        [],
    ),
    caption: [Header of a song],
)

The *Button* column gives the entry of *SONG SETUP*, which is opened with `EDIT` + `PTN`.

The header is thinner than the pattern header: a song has a tempo, a destination, a sync source, a key trigger
and a loop flag. Shuffle, scale and direction belong to the patterns it chains — see #cref(<chap-system>).

== Parts

The 16 parts start at `0x20000010` and each one occupies 8 bytes: part 1 is at `0x20000010`, part 2 at `0x20000018`,
and so on. The table gives the addresses inside part 1.

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed Size*], [*Field*], [*Values*], [*Ref.*], [*Meaning*]),
        [`20 00 00 10`],
        [`00 00 00 01`],
        [Pattern number, high nibble],
        [`0`..`15`],
        [`04`],
        [High nibble of the pattern number],
        [`20 00 00 11`],
        [`00 00 00 01`],
        [Pattern number, low nibble],
        [`0`..`15`],
        [`0A`],
        [Low nibble of the pattern number (`04 0A` is `74`)],
        [`20 00 00 12`],
        [`00 00 00 01`],
        [Patch bank],
        [`1`..`4`],
        [`02`],
        [`1` = A, `2` = B, `3` = C, `4` = User],
        [`20 00 00 13`],
        [`00 00 00 01`],
        [Patch number],
        [`0`..`127`],
        [`5C`],
        [Patch number minus one (`5C` is the patch displayed as `93`)],
        [`20 00 00 14`],
        [`00 00 00 01`],
        [Repeats],
        [`0`..`127`],
        [`07`],
        [Repeat count minus one (`07` is 8 repetitions)],
        [`20 00 00 15`],
        [`00 00 00 03`],
        [Reserved],
        [`00`],
        [`00 00 00`],
        [],
    ),
    caption: [Content of a song part],
)

A pattern number goes up to 128, so it is stored as 2 nibbles: `74` is stored as `04 0A`. A pattern number of `0` means that the part is empty; the instrument
displays it as `---` and skips it.

The patch number, on the other hand, is stored one below the displayed number, like the pattern numbers of the
sequencer and unlike the patch number of a pattern header, which is stored as the 0-based program number. The two
conventions coincide - both store the 0-based number - but the reference point is different, and a conversion that
subtracts one twice or not at all is easy to write by accident. In the payload column below, the colours group the
fields: pattern (green), bank (blue), patch (orange), repeats (purple), reserved (grey).

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto),
        head([*Part*], [*Payload*], [*Pattern*], [*Patch*], [*Repeats*]),
        [`1`],
        [#song-part("04 0A 02 5C 07 00 00 00")],
        [`74`],
        [`B93`],
        [`8`],
        [`2`],
        [#song-part("01 0E 02 69 01 00 00 00")],
        [`30`],
        [`B106`],
        [`2`],
        [`3`],
        [#song-part("01 03 01 4C 03 00 00 00")],
        [`19`],
        [`A77`],
        [`4`],
        [`4`],
        [#song-part("01 0C 01 07 01 00 00 00")],
        [`28`],
        [`A8`],
        [`2`],
        [`5`],
        [#song-part("01 00 02 27 03 00 00 00")],
        [`16`],
        [`B40`],
        [`4`],
        [`6`],
        [#song-part("00 0E 01 3B 01 00 00 00")],
        [`14`],
        [`A60`],
        [`2`],
        [`7`],
        [#song-part("01 01 01 4C 03 00 00 00")],
        [`17`],
        [`A77`],
        [`4`],
        [`8`],
        [#song-part("02 09 01 1E 03 00 00 00")],
        [`41`],
        [`A31`],
        [`4`],
        [`9`],
        [#song-part("01 0B 02 29 00 00 00 00")],
        [`27`],
        [`B42`],
        [`1`],
        [`10`],
        [#song-part("03 08 01 07 01 00 00 00")],
        [`56`],
        [`A8`],
        [`2`],
        [`11`],
        [#song-part("03 0A 01 6A 03 00 00 00")],
        [`58`],
        [`A107`],
        [`4`],
        [`12`],
        [#song-part("04 03 01 65 01 00 00 00")],
        [`67`],
        [`A102`],
        [`2`],
        [`13`],
        [#song-part("02 07 01 6B 01 00 00 00")],
        [`39`],
        [`A108`],
        [`2`],
        [`14`],
        [#song-part("04 0A 02 5C 03 00 00 00")],
        [`74`],
        [`B93`],
        [`4`],
        [`15`],
        [#song-part("00 00 05 00 00 00 00 00")],
        [empty],
        [—],
        [`1`],
        [`16`],
        [#song-part("00 00 05 00 00 00 00 00")],
        [empty],
        [—],
        [`1`],
    ),
    caption: [The 16 parts of the first factory song],
)

The first factory song chains fourteen parts and leaves the last two empty. Part 1 replays the pattern displayed as `74`
eight times — which is pattern 73, at `0x11490000`, not `0x114A0000` — with the patch `B93`. Part 14 replays the
same pattern four times, which is the usual way such a chain is built: a chorus, a variation, and a return.

The two empty parts store a pattern number of `0`, a bank of `5`, which is outside the bank table, and a patch number of
`0`. The bank byte of an empty part therefore cannot be relied upon, and a writer that creates a song from scratch
should fill the unused parts with the same pattern that the instrument itself writes:

#figure(
    song-part("00 00 05 00 00 00 00 00"),
    caption: [An empty part],
)

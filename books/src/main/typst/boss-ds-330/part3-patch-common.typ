#import "../common/settings.typ": *
#import "lib.typ": *

#part("Patch area")

#chapter("Patch common", l: "chap-patch-common")

The patch common block at `0x400100` holds the settings shared by every part: the patch name, the reverb and the
chorus. It is 64 bytes.

== Payload layout

#figure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed size*], [*Parameter*], [*Meaning*]),
        [`40 01 00`], [`00 00 10`], [Patch Name],
        [16 ASCII characters, padded with spaces. The factory default reads `Oh! my GOD !!`],
        [`40 01 10`], [`00 00 10`], [Voice Reserve],
        [16 bytes, the number of voices reserved for each part],
        [`40 01 20`], [`00 00 10`], [Reserved],
        [16 bytes, read as `00`],
        [`40 01 30`], [`00 00 01`], [Reverb Type],
        [One of @reverb-types],
        [`40 01 31`], [`00 00 01`], [Reverb Character],
        [A reverb colour, `0x00` to `0x07`],
        [`40 01 32`], [`00 00 01`], [Reverb Pre LPF],
        [The low-pass cutoff before the reverb, `0x00` to `0x07`],
        [`40 01 33`], [`00 00 01`], [Reverb Level],
        [The reverb send level, `0x00` to `0x7F`],
        [`40 01 34`], [`00 00 01`], [Reverb Time],
        [The reverb decay time, `0x00` to `0x7F`],
        [`40 01 35`], [`00 00 01`], [Reverb Delay Feedback],
        [The reverb delay feedback, `0x00` to `0x7F`],
        [`40 01 36`], [`00 00 01`], [Reverb Send Level to Chorus],
        [The amount sent from the reverb into the chorus, `0x00` to `0x7F`],
        [`40 01 37`], [`00 00 01`], [Reserved],
        [1 byte, read as `00`],
        [`40 01 38`], [`00 00 01`], [Chorus Type],
        [One of @chorus-types],
        [`40 01 39`], [`00 00 01`], [Chorus Pre LPF],
        [The low-pass cutoff before the chorus, `0x00` to `0x07`],
        [`40 01 3A`], [`00 00 01`], [Chorus Level],
        [The chorus send level, `0x00` to `0x7F`],
        [`40 01 3B`], [`00 00 01`], [Chorus Feedback],
        [The chorus feedback, `0x00` to `0x7F`],
        [`40 01 3C`], [`00 00 01`], [Chorus Delay],
        [The chorus delay, `0x00` to `0x7F`],
        [`40 01 3D`], [`00 00 01`], [Chorus Rate],
        [The chorus modulation rate, `0x00` to `0x7F`],
        [`40 01 3E`], [`00 00 01`], [Chorus Depth],
        [The chorus modulation depth, `0x00` to `0x7F`],
        [`40 01 3F`], [`00 00 01`], [Chorus Send Level to Reverb],
        [The amount sent from the chorus into the reverb, `0x00` to `0x7F`],
    ),
    caption: [Content of the patch common block],
)

== Reverb types <reverb-types>

The reverb type at `0x400130` is an enumerated value:

#figure(
    ptable(
        columns: (auto, auto),
        head([*Value*], [*Type*]),
        [`00`], [Room 1],
        [`01`], [Room 2],
        [`02`], [Room 3],
        [`03`], [Hall 1],
        [`04`], [Hall 2],
        [`05`], [Plate],
        [`06`], [Delay],
        [`07`], [Panning Delay],
    ),
    caption: [The reverb types],
)

== Chorus types <chorus-types>

The chorus type at `0x400138` is an enumerated value:

#figure(
    ptable(
        columns: (auto, auto),
        head([*Value*], [*Type*]),
        [`00`], [Chorus 1],
        [`01`], [Chorus 2],
        [`02`], [Chorus 3],
        [`03`], [Chorus 4],
        [`04`], [Feedback Chorus],
        [`05`], [Flanger],
        [`06`], [Short Delay],
        [`07`], [Short Delay (FB)],
    ),
    caption: [The chorus types],
)

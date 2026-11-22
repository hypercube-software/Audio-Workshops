#import "../common/settings.typ": *
#import "lib.typ": *

#part("Synth engine")

#chapter("The patch block", l: "chap-patch")

A patch is 240 bytes. The same 240 bytes are reachable at 2 addresses: the *edit buffer* at `0x05000000`, the RAM copy
the synth plays, and the *user presets* at `0x06nn0000`, where `nn` is the preset number, from 0 to 127. Writing to
`0x05000000` changes the current sound, exactly like turning a knob; writing to `0x06nn0000` overwrites a stored
preset.

== Payload layout

#figure(
    ptable(
        columns: (auto, auto, 1fr),
        head([*Packed address*], [*Packed Size*], [*Content*]),
        [`05 00 00 00`],
        [`00 00 01 50`],
        [104 parameter values],
        [`05 00 01 50`],
        [`00 00 00 10`],
        [Patch name, ASCII, padded with `00`],
        [`05 00 01 60`],
        [`00 00 00 10`],
        [Reserved, `00` in every patch examined],
    ),
    caption: [Layout of the 240-byte patch block],
)

The parameter tables of #cref(<chap-params>) cover the addresses `0x05000000` to `0x0500012F`. The bytes that
follow, `0x05000130` to `0x0500014F`, are unused and read as `00`.

== The name

The name is 16 ASCII characters at `0x05000150`. Short names are padded with `00`, not with spaces, and the parser must
stop at the first `00`. The reference patch of this book, `Inbound`, occupies `0x05000150` to `0x05000156` and is followed by nine
zeros.

The name can be read alone, which spares a 240-byte read just to display a program name:

#figure(
    table(
        align: left,
        columns: (auto, auto, 1fr),
        fill: (x, y) => if y == 0 { luma(230) },
        stroke: 0.5pt,
        head([*Block*], [*Address*], [*What*]), [Current patch, name], [`05 00 01 50`],
        [16 bytes: the name], [Stored preset, name], [`06 nn 01 50`],
        [16 bytes: the name of preset `nn`], [Current patch, all], [`05 00 00 00`],
        [240 bytes: the parameters, then the name at `0x05000150`, then the reserved tail],
    ),
    caption: [Addresses of the patch and of its name],
)

The read of a name is a `RQ1`. For the name of preset 0, the MIDI frame is:

#figure(
    bytefield(
        bpr: 144,
        bitheader(
            marker: false,
            angle: 0deg,
            numbers: none,
            0, [0], 8, [1], 16, [2], 24, [3], 32, [4], 40, [5], 48, [6], 56, [7],
            64, [8], 72, [9], 80, [10], 88, [11], 96, [12], 104, [13], 112, [14],
            120, [15], 128, [16], 136, [17],
        ),
        bytes(1, fill: l_gray)[F0],
        bytes(1, fill: l_gray)[41],
        bytes(1, fill: l_gray)[10],
        bytes(4, fill: l_gray)[model 00 00 00 44],
        bytes(1, fill: l_blue)[11],
        bytes(4, fill: l_green)[address 06 00 01 50],
        bytes(4, fill: l_orange)[size 00 00 00 10],
        bytes(1, fill: l_red)[19],
        bytes(1, fill: l_gray)[F7],
    ),
    caption: [Request for the name of preset 0],
)

The address is `0x06000150`, the name of preset 0. The 4 size bytes ask for `16` characters, so the answer is a `DT1`
that carries the name alone, without the 240 bytes of the patch.

== The preset space

The instrument holds 2 kinds of presets:

- The factory presets, in banks A, B and C, live in ROM. Selecting one copies it into the edit buffer.
- The 128 user presets live in flash, at `0x06000000`. The SysEx frames read and write them there.

The edit buffer is the RAM copy that the synth plays: selecting a preset, factory or user, copies it at
`0x05000000`.

User preset `nn` starts at `0x06nn0000`, and its name is at `0x06nn0150`:

- `0x06000000` is user preset 0;
- `0x06050000` is user preset 5, `He Says Bass` on this unit;
- `0x067F0000` is user preset 127, still empty.

Each preset owns a 256-byte window: its 240 bytes of patch, then 16 bytes that read as `0`.

To save a factory preset, select it with a `CC 0` + `PC` pair and read the edit buffer at `0x05000000`. The user
presets are read and written in place, at their own address.

== Song and Sequences

A song is selected on the unit, with the mode selector and the number buttons: no MIDI message carries a pattern or
a song number. The sequencer plays the song edit buffer at `0x20000000`, and a stored song is played by copying it
there: read `0x21nn0000`, then write its 144 bytes at `0x20000000`. `Bank Select` and `Program Change` select
patches, and `Start`, `Continue`, `Stop` and `Song Position` drive the playback.

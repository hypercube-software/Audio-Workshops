#import "../common/settings.typ": *
#import "lib.typ": *

#part("Synth engine")

#chapter("The patch block", l: "chap-patch")

A patch is 240 bytes. The same 240 bytes are reachable at two addresses: the *current patch* at `0x05000000`, which is
the working buffer the synth plays, and the *user presets* at `0x06nn0000`, where `nn` is the user preset number, from
0 to 127. Writing to `0x05000000` changes the sound without touching the memory, exactly like turning a knob; writing
to `0x06nn0000` overwrites a user preset.

== Payload layout

#figure(
    ptable(
        columns: (auto, auto, 1fr),
        head([*Address*], [*Size*], [*Content*]),
        [`05 00 00 00`],
        [208],
        [104 parameter values, each stored as two nibbles],
        [`05 00 01 50`],
        [16],
        [Patch name, ASCII, padded with `00`],
        [`05 00 01 60`],
        [16],
        [Reserved, `00` in every patch examined],
    ),
    caption: [Layout of the 240-byte patch block],
)

Each parameter occupies 2 consecutive payload bytes, at an even offset `o`: the first one holds the 4 most
significant bits of the value, the second one the 4 least significant bits:

#figure(
    ptable(
        columns: 1,
        raw("payload[o]     = (value >> 4) & 0x0F\npayload[o + 1] =  value       & 0x0F\n\nvalue = (payload[o] << 4) | payload[o + 1]"),
    ),
    caption: [Encoding of a parameter value],
)

Both nibbles stay below `10`, so the block never contains a byte with its most significant bit set. A parameter that
holds `134` is stored as `08 06`.

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

- The 3 factory banks, A, B and C, in ROM. They have no address in the SysEx space: the only way to read one is
  to select it, which copies it into the edit buffer at `0x05000000`, and then to read that buffer;
- The user bank, 128 presets in flash, at `0x06000000`. It is the one that the SysEx frames reach.

User preset `nn` starts at `0x06nn0000`, and its name is at `0x06nn0150`. That address does not depend on the
bank selected on the unit.

- `0x06000000` is user preset 0;
- `0x06050000` is user preset 5, `He Says Bass` on this unit;
- `0x067F0000` is user preset 127, still empty.

Each preset owns a 256-byte window: only the first 240 bytes are used, and reading past them returns zeros.

So a factory preset is saved one at a time: select it with a `CC 0` + `PC` pair, then read `0x05000000`. The user
bank, on the other hand, is read and written in place, at its own address.

== Song and Sequences

Surprisingly, there is no way to select a sequence or a song with `Bank Select` and `Program Change`. It as to be done via SysEx.

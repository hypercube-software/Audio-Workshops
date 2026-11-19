#import "../common/settings.typ": *
#import "lib.typ": *

#part("Synth engine")

#chapter("The patch block", image: red-chapter-banner, l: "chap-patch")

A patch is 240 bytes. The same 240 bytes are reachable at two addresses: the *current patch* at `0x05000000`, which is
the working buffer the synth plays, and the *stored patches* at `06 ss 00 00`, where `ss` is a slot number between 0
and 127. Writing to `0x05000000` changes the sound without touching the memory, exactly like turning a knob; writing to
`06 ss 00 00` overwrites a stored patch.

== Payload layout

#figure(
  ptable(
    columns: (auto, auto, 1fr),
    head([*Offset*], [*Size*], [*Content*]),
    [`0`..`207`], [208], [104 parameter values, each stored as two nibbles],
    [`208`..`223`], [16], [Patch name, ASCII, padded with `00`],
    [`224`..`239`], [16], [Reserved, `00` in every patch examined],
  ),
  caption: [Layout of the 240-byte patch block],
)

Each parameter has an offset `k` between 0 and 103, and its value is stored as two nibbles in the two consecutive bytes
at payload offsets `2k` and `2k + 1`:

#figure(
  ptable(
    columns: 1,
    raw("payload[2k]     = (value >> 4) & 0x0F\npayload[2k + 1] =  value       & 0x0F\n\nvalue = (payload[2k] << 4) | payload[2k + 1]"),
  ),
  caption: [Encoding of a parameter value],
)

Both nibbles stay below `10`, so the block never contains a byte with its most significant bit set. A parameter that
holds `134` (the cutoff of the reference patch) is stored as `08 06`, at offsets `110` and `111` — that is, offset 55,
doubled.

The last documented parameter has offset 87; its two nibbles are at payload offsets 174 and 175, and the 32 bytes
that follow (payload offsets 176 to 207) are unused and read as `00`.

== The name

The name is 16 ASCII characters at offset 208. Short names are padded with `00`, not with spaces, and the parser must
stop at the first `00`. The reference patch of this book, `Inbound`, occupies offsets 208 to 214 and is followed by nine
zeros.

Because the address is a packed number, the name is also reachable on its own, which spares a 240-byte read just to
display a program name:

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    head([*Block*], [*Address*], [*What*]),
    [Current patch, name], [`05 00 01 50`], [16 bytes at offset 208 of the edit buffer],
    [Stored patch, name], [`06 ss 01 50`], [16 bytes at offset 208 of slot `ss`],
    [Current patch, all], [`05 00 00 00`], [240 bytes: parameters, name and reserved tail],
  ),
  caption: [Addresses of the patch and of its name],
)

The read of a name is a `RQ1` of size 16. For the first slot it is `F0 41 10 00 00 00 44 11 06 00 01 50 00 00 00 10 19
F7`: the address sums to 87, the size to 16, and the checksum is `-(87 + 16) & 0x7F = 0x19`.

== The slot space

There are 128 slots of 240 bytes per banks at address `06 bb pp 00`. Each slot contains a patch.

For instance:
- `06 03 00 00` is slot 0 of user bank, the patch `U001`
- `06 03 7F 00` is slot 127 of user bank, the patch `U128`. 

Each slot owns a 256 bytes window of the address space, of which only the first 240 bytes are used. Reading past the 240 bytes of a patch returns zeros.

When a program change event is received, the patch memory (ROM) is copied into the edit buffer memory (RAM) at `05 00 00 00`.

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    head([*Bank*], [*CC 0*], [*Program change*]),
    [A], [`0`], [`0` to `127` for `A001` to `A128`],
    [B], [`1`], [`0` to `127` for `B001` to `B128`],
    [C], [`2`], [`0` to `127` for `C001` to `C128`],
    [User], [`3`], [`0` to `127` for `U001` to `U128`],
  ),
  caption: [Selecting a preset],
)

The program number of a program change is 0-based, that is `A001` (bank A, patch 1) is `CC 0 = 0` followed by `PC 0`. 

== Song and Sequences

Surprisingly, there is no way to select a sequence or a song with `Bank Select` and `Program Change`. It as to be done via SysEx.

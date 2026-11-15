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

The 128 slots of `06` are the *user bank*: slot 0 is the patch `U001`, slot 127 is `U128`. Each slot owns a 16 KiB
window of the address space, of which only the first 240 bytes are used; the window of slot 1 starts at `0x06010000`
even though slot 0 is shorter than that. Reading past the 240 bytes of a patch returns zeros.

The three factory banks A, B and C have no address of their own: they are ROM. Reading one of their patches is still
possible, by selecting it first and then reading the edit buffer. With bank select CC 0 and a program change:

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

The program number of a program change is 0-based, that is `A001` is `CC 0 = 0` followed by `PC 0`. The *Ref.* values
of the parameter tables come from the user patch 1 of a factory-state unit, which is called `Inbound`; the tables
themselves are in @chap-params[chapter]

#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("SysEx messages", image: red-chapter-banner, l: "chap-sysex")

The SE-02 speaks the classic Roland SysEx dialect (Aka. `One-Way` protocol) with *RQ1* and *DT1* messages.

== Frame structure

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    head([*Bytes*], [*Field*], [*Meaning*]),
    [`F0`], [], [Start of SysEx],
    [`41`], [manufacturer], [Roland],
    [`10`], [device ID], [Always `10` on the SE-02, whatever the MIDI channel is],
    [`00 00 00 44`], [model ID], [SE-02],
    [`11` or `12`], [command], [Request (`RQ1`) or dump (`DT1`), see below],
    [`aa bb cc dd`], [address], [Four 7-bit bytes, see @sec-address],
    [`...`], [payload], [Four packed size bytes for a request; for a dump, *n* data bytes and no size field],
    [`ss`], [checksum], [See @sec-checksum],
    [`F7`], [], [End of SysEx],
  ),
  caption: [Structure of an SE-02 SysEx message],
)

== Commands

Only two commands are used in *One-Way* Roland protocol (I didn't tried the *Handshake* protocol and its 9 commands):

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    head([*Name*], [*Code*], [*Role*]),
    [`RQ1`], [`11`], [Request. The payload is a packed size on four bytes; the answer is a `DT1` with that many data bytes],
    [`DT1`], [`12`], [Data set. Carries the payload that must be stored at the address],
  ),
  caption: [The two commands of the SE-02 memory protocol],
)

A `DT1` is not acknowledged: the instrument does not answer a write. A write is verified by reading the block back and
comparing it with what was sent. Unknown addresses are simply not answered, which makes a read the safe way to probe the
memory.

=== The size field

A `RQ1` must state how many data bytes it wants. That number is carried by the *size* field placed just after the
address. Like an address, the size is packed: it is split into four 7-bit bytes, most significant group first. It counts
*data bytes* only, never the address, the command, the model identifier or the checksum.

A 240-byte block is requested with size `00 00 01 70`, because `1 * 128 + 112 = 240`; a 16-byte block uses
`00 00 00 10`. If the requested size runs past the end of the block, the instrument stops the answer at the end of the
block.

A `DT1` has no size field. Its payload starts after the address and ends before the checksum, so the length is given by
the frame itself.

=== Reading and writing a block

Reading is always the same gesture: send an `RQ1` with an address and a size, and the instrument answers with a `DT1`
carrying that many data bytes. Writing is the opposite: send a `DT1` with the same address and the data, and the
instrument stores it.

The complete request for the current patch is
`F0 41 10 00 00 00 44 11 05 00 00 00 00 00 01 70 0A F7`. It is addressed to `0x05000000`; the four bytes after the
address are the size `00 00 01 70`, that is 240; and the checksum is `0A`. In the figures below, the colours group the
fields of the frame.

#figure(
  bytefield(
    bpr: 144,
    bitheader(
      marker: false,
      angle: 0deg,
      numbers: none,
      0, [0],
      8, [1],
      16, [2],
      24, [3],
      32, [4],
      40, [5],
      48, [6],
      56, [7],
      64, [8],
      72, [9],
      80, [10],
      88, [11],
      96, [12],
      104, [13],
      112, [14],
      120, [15],
      128, [16],
      136, [17],
    ),
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[41],
    bytes(1, fill: l_gray)[10],
    bytes(4, fill: l_gray)[model 00 00 00 44],
    bytes(1, fill: l_blue)[11],
    bytes(4, fill: l_green)[address 05 00 00 00],
    bytes(4, fill: l_orange)[size 00 00 01 70],
    bytes(1, fill: l_red)[0A],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [Request for the current patch],
)

The answer is a `DT1` whose payload starts with the 88 parameter values, followed by the patch name. To write the data
back, the same payload is sent with command `12` instead of `11`; there is no size field, and the checksum is now
computed over the address and the 240 data bytes. In the figure, the payload is drawn as six bytes; it is 240 bytes on
the wire.

#figure(
  bytefield(
    bpr: 160,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[41],
    bytes(1, fill: l_gray)[10],
    bytes(4, fill: l_gray)[model 00 00 00 44],
    bytes(1, fill: l_blue)[12],
    bytes(4, fill: l_green)[address 05 00 00 00],
    bytes(6, fill: l_orange)[240 data bytes],
    bytes(1, fill: l_red)[1C],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [DT1 example],
)

@sec-checksum below explains how `0A` and `1C` are computed. The following parts describe the content of each block.

== Device inquiry

The unit answers the universal device inquiry `F0 7E 7F 06 01 F7` with its identity. The reply carries device ID
`10`, manufacturer `41` (Roland), family `44 03`, member `00 00`, then the four version bytes `00 03 00 00`.

#figure(
  bytefield(
    bpr: 120,
    bitheader(
      marker: false,
      angle: 0deg,
      numbers: none,
      0, [0],
      8, [1],
      16, [2],
      24, [3],
      32, [4],
      40, [5],
      48, [6],
      56, [7],
      64, [8],
      72, [9],
      80, [10],
      88, [11],
      96, [12],
      104, [13],
      112, [14],
    ),
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[7E],
    bytes(1, fill: l_blue)[10],
    bytes(2, fill: l_gray)[06 02],
    bytes(1, fill: l_green)[41],
    bytes(2, fill: l_purple)[44 03],
    bytes(2, fill: l_purple)[00 00],
    bytes(4, fill: l_orange)[00 03 00 00],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [Identity reply],
)

The last four bytes carry a version but it is not clean which one.

#caveat[
  The device ID of the SysEx frames is *not* the MIDI channel. It stays `10` even after the MIDI channel has been
  changed to channel 2 in the system settings. An implementation should therefore always address the unit with `10`, or
  with `7F` when a single unit is on the bus.
]

== Checksum <sec-checksum>

Every message ends with a checksum computed over the address and the payload. For a `DT1` the payload is the data; for a
`RQ1` it is the four size bytes. The rule is the usual one:

#figure(
  ptable(
    columns: 1,
    raw("checksum = (128 - (sum(address + payload) mod 128)) mod 128"),
  ),
  caption: [Checksum rule],
)

or, in C:

#figure(
  ptable(
    columns: 1,
    raw("sum = 0;\nfor (each byte b of address and payload)\n    sum = (sum + b) & 0x7F;\nchecksum = (-sum) & 0x7F;"),
  ),
  caption: [Checksum in C],
)

Four examples, all taken from real messages:

#figure(
  table(
    align: left,
    columns: (auto, auto, auto, auto),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    head([*Message*], [*Address*], [*Payload*], [*Checksum*]),
    [`RQ1`], [`00 00 00 00`], [size 16: `00 00 00 10`], [`70`],
    [`RQ1`], [`05 00 00 00`], [size 240: `00 00 01 70`], [`0A`],
    [`DT1`], [`05 00 00 00`], [240 data bytes of the patch `Inbound`], [`1C`],
    [`DT1`], [`06 00 00 00`], [the same 240 data bytes], [`1B`],
  ),
  caption: [Checksums of four messages],
)

The first request carries the size `16` in its last byte, so the sum is 16 and `-16 & 0x7F = 112 = 70`. The second one has
to express a size of 240, which does not fit in seven bits: the size is packed in 7-bit groups like the address, so 240
becomes `00 00 01 70`, that is `1 * 128 + 112`. The bytes of the message then sum to `5 + 0 + 0 + 1 + 112 = 118` — the
address byte `05` included — and `-118 & 0x7F = 10 = 0x0A`.

The last two lines are the interesting ones. Both messages carry exactly the same 240 data bytes, but their addresses
differ by one (`05` and `06`), and so do their checksums, in the other direction: `1C` and `1B`. If the two checksums
computed for the same payload do not differ by one, the summation loop is wrong.

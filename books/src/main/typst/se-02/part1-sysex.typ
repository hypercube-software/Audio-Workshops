#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("SysEx messages", image: red-chapter-banner, l: "chap-sysex")

The SE-02 speaks the classic Roland SysEx dialect (Aka. `One-Way` protocol) with *RQ1* and *DT1* messages.

== MIDI frame structure

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
the MIDI frame itself.

=== Querying with RQ1

To read a block, send an `RQ1` whose address is the first byte of the block and whose size is the number of data bytes wanted. The instrument answers with a `DT1` at the same address.

The complete request for the current patch is
`F0 41 10 00 00 00 44 11 05 00 00 00 00 00 01 70 0A F7`. It is addressed to `0x05000000`; the four bytes after the
address are the size `00 00 01 70`, that is 240; and the checksum is `0A`. In the figures below, the colours group the
fields of the MIDI frame.

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

The answer is a `DT1` whose payload starts with the 88 parameter values, followed by the patch name. 
=== Writing with DT1

To write, send the same payload back with command `12` instead of `11`, at the same address. A `DT1` has no size field: its payload starts after the address and ends before the checksum, so its length is the length of the MIDI frame. In the figure, the payload is drawn as six bytes; it is 240 bytes on the wire.

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

The universal device inquiry is `F0 7E 7F 06 01 F7`. Addressed to `7F`, it reaches every unit on the bus.

The answer carries the identity of the unit that replied.

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

The fields of the answer, in order:

- `F0`, start of the MIDI frame;
- `7E`, universal non real time;
- `10`, device ID of the unit;
- `06 02`, general information, identity reply;
- `41`, manufacturer, Roland;
- `44 03`, family code;
- `00 00`, member number;
- `00 03 00 00`, revision;
- `F7`, end of the MIDI frame.

The revision is fixed on this unit: it does not follow the revision of the firmware shown on the panel, and it must not be used to detect a version.

#caveat[
  The device ID of the SysEx MIDI frames is *not* the MIDI channel. It stays `10` even after the MIDI channel has been
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

Two examples, taken from real messages. In the figures the bytes that are summed are green, the
checksum byte is red, and the bytes that are not summed are grey.

#figure(
  bytefield(
    bpr: 144,
    bitheader(
      marker: false,
      angle: 0deg,
      numbers: none,
      0, [0], 8, [1], 16, [2], 24, [3], 32, [4], 40, [5], 48, [6], 56, [7],
      64, [8], 72, [9], 80, [10], 88, [11], 96, [12], 104, [13], 112, [14], 120, [15],
      128, [16], 136, [17],
    ),
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[41],
    bytes(1, fill: l_gray)[10],
    bytes(4, fill: l_gray)[model 00 00 00 44],
    bytes(1, fill: l_gray)[11],
    bytes(4, fill: l_green)[address 05 00 00 00],
    bytes(4, fill: l_green)[size 00 00 01 70],
    bytes(1, fill: l_red)[0A],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [Checksum of a request],
)

#figure(
  bytefield(
    bpr: 160,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[41],
    bytes(1, fill: l_gray)[10],
    bytes(4, fill: l_gray)[model 00 00 00 44],
    bytes(1, fill: l_gray)[12],
    bytes(4, fill: l_green)[address 05 00 00 00],
    bytes(6, fill: l_green)[240 data bytes],
    bytes(1, fill: l_red)[1C],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [Checksum of a dump],
)

For the request, the green bytes are `05 00 00 00` and `00 00 01 70`. Their sum is `118`, so the
checksum is `128 - 118 = 10`, written `0A`.

For the dump, the green bytes are the address and the 240 data bytes. Their sum gives `1C`.

The address takes part in the sum, so the same data sent to `06 00 00 00` instead of `05 00 00 00`
gets the checksum `1B`: the address byte is one higher, the checksum one lower.

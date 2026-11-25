#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("SysEx messages", l: "chap-sysex")

The DS-330 speaks the classic Roland SysEx dialect with *RQ1* and *DT1* messages.

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
        [`00`], [device ID], [The unit's device ID, `0x00` to `0x1F`; the factory default is `0x10`],
        [`42` or `55`], [model ID], [`42` in multi mode, `55` in single mode; shared by the Sound Canvas family],
        [`11` or `12`], [command], [Request (`RQ1`) or dump (`DT1`)],
        [`aa bb cc`], [address], [Three 7-bit bytes, see @sec-address],
        [`...`], [payload], [Three packed size bytes for a request; *n* data bytes for a dump],
        [`cc`], [checksum], [See @sec-checksum],
        [`F7`], [], [End of SysEx],
    ),
    caption: [Structure of a DS-330 SysEx message],
)

== Commands

Only 2 commands are used:

#figure(
    table(
        align: left,
        columns: (auto, auto, 1fr),
        fill: (x, y) => if y == 0 { luma(230) },
        stroke: 0.5pt,
        head([*Name*], [*Code*], [*Role*]),
        [`RQ1`], [`11`],
        [Request. The payload is a packed size on 3 bytes; the answer is a `DT1` with that many data bytes],
        [`DT1`], [`12`],
        [Data set. Carries the payload that must be stored at the address],
    ),
    caption: [The two commands of the DS-330 memory protocol],
)

A `DT1` is not acknowledged: the instrument does not answer a write. A write is verified by reading the block back
and comparing it with what was sent. Unknown addresses are simply not answered, which makes a read the safe way to
probe the memory.

=== Querying with RQ1

To read a block, send an `RQ1` whose address is the first byte of the block and whose size is the number of data
bytes wanted. The instrument answers with a `DT1` at the same address.

The frame below reads the byte at `0x40007F`, the GS mode flag. The 3 bytes after the address are the size
`00 00 01`, that is `1`; the checksum is `40`. The colours group the fields.

#figure(
    bytefield(
        bpr: 104,
        bitheader(
            marker: false,
            angle: 0deg,
            numbers: none,
            0, [0], 8, [1], 16, [2], 24, [3], 32, [4], 40, [5], 48, [6], 56, [7],
            64, [8], 72, [9], 80, [10], 88, [11], 96, [12],
        ),
        bytes(1, fill: l_gray)[F0],
        bytes(1, fill: l_gray)[41],
        bytes(1, fill: l_gray)[00],
        bytes(1, fill: l_gray)[42],
        bytes(1, fill: l_blue)[11],
        bytes(3, fill: l_green)[address 40 00 7F],
        bytes(3, fill: l_orange)[size 00 00 01],
        bytes(1, fill: l_red)[40],
        bytes(1, fill: l_gray)[F7],
    ),
    caption: [Request for the GS mode flag],
)

The answer carries 1 data byte: `42`, the value that marks the *GS* mode. The checksum is `7F`.

#figure(
    bytefield(
        bpr: 88,
        bitheader(
            marker: false,
            angle: 0deg,
            numbers: none,
            0, [0], 8, [1], 16, [2], 24, [3], 32, [4], 40, [5], 48, [6], 56, [7],
            64, [8], 72, [9], 80, [10],
        ),
        bytes(1, fill: l_gray)[F0],
        bytes(1, fill: l_gray)[41],
        bytes(1, fill: l_gray)[00],
        bytes(1, fill: l_gray)[42],
        bytes(1, fill: l_blue)[12],
        bytes(3, fill: l_green)[address 40 00 7F],
        bytes(1, fill: l_orange)[42],
        bytes(1, fill: l_red)[7F],
        bytes(1, fill: l_gray)[F7],
    ),
    caption: [Answer for the GS mode flag],
)

=== Writing with DT1

To write, send the payload back in a `DT1`, at the same address. A `DT1` has no size field: its payload starts
after the address and ends before the checksum, so the length is given by the MIDI frame itself.

A `DT1` is limited to 256 bytes: a longer payload is split into several messages, sent at least 20 milliseconds
apart.

== Single and multi mode

The DS-330 has 2 modes:

- *Single mode* uses the model ID `0x55` and addresses one tone and one drum set.
- *Multi mode* uses the model ID `0x42` and addresses the 16 parts, the patch common and the drum maps described in
  this book.

The 2 modes have their own address maps. The tables of this book describe the multi mode, whose model ID is `0x42`.

== Switching modes

Two Universal SysEx messages switch between the modes, in addition to the front-panel selector. They are ignored when
the *GS Reset RX* switch on the unit is off:

#figure(
    ptable(
        columns: (auto, auto, 1fr),
        head([*Message*], [*Bytes*], [*Mode*]),
        [GM System On], [`F0 7E 7F 09 01 F7`], [Multi],
        [GM System Off], [`F0 7E 7F 09 02 F7`], [Single],
    ),
    caption: [The messages that switch the mode],
)

A *GS reset* — the write of `00` to `0x40007F` described in #cref(<chap-system>) — also returns the unit to the multi
mode, and resets it to the GS defaults.

== Checksum <sec-checksum>

Every message ends with a checksum computed over the address and the payload. For a `DT1` the payload is the data;
for an `RQ1` it is the 3 size bytes. The rule is the usual one:

#figure(
    ptable(
        columns: 1,
        raw("checksum = (- (sum(address bytes) + sum(payload bytes))) & 0x7F"),
    ),
    caption: [Checksum rule],
)

For the request above, the summed bytes are `40 00 7F` and `00 00 01`. Their sum is
`0x40 + 0x7F + 0x01 = 0xC0`, so the checksum is `0x80 - 0x40 = 0x40`.

For the answer, the summed bytes are `40 00 7F` and `42`. Their sum is `0x40 + 0x7F + 0x42 = 0x101`, kept on
7 bits as `0x01`, so the checksum is `0x80 - 0x01 = 0x7F`.

== Device inquiry

The universal device inquiry of the MIDI specification does not identify a Sound Canvas reliably: the units of the
family share the model ID `0x42`, and a USB-MIDI interface between the computer and the unit answers on its behalf.
The model ID alone does not tell a DS-330 from an SC-55.

#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("SysEx messages", l: "chap-sysex")

The MPK-261 exchanges memory blocks and presets through MIDI frames built on a fixed pattern: the AKAI manufacturer
ID, a device ID, a model byte, a command byte that selects the operation, a 14-bit size, then a payload whose layout
depends on the command. Every command of the instrument follows that pattern; the payload is what changes.

== MIDI frame structure

#figure(
  ptable(
    columns: (auto, auto, 1fr),
    head([*Bytes*], [*Field*], [*Meaning*]),
    [`F0`], [start], [Start of SysEx],
    [`47`], [manufacturer], [AKAI],
    [`dd`], [device], [The SysEx device ID of the unit, 0 to 127; `00` from the factory (see #cref(<chap-global>))],
    [`25`], [model], [MPK-261. The other models of the series use `23` (MPK-225) and `24` (MPK-249)],
    [`cc`], [command], [See @sec-commands],
    [`ss ss`], [size], [2 bytes, 14-bit packed size: the number of bytes of the payload],
    [`...`], [payload], [Layout depends on the command],
    [`F7`], [end], [End of SysEx],
  ),
  caption: [Structure of an MPK-261 SysEx message],
)

#figure(
  bytefield(
    bpr: 112,
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
    ),
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_blue)[device],
    bytes(1, fill: l_blue)[25],
    bytes(1, fill: l_orange)[cmd],
    bytes(2, fill: l_purple)[size],
    bytes(6, fill: l_green)[payload],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [The 5 header bytes and the trailer],
)

The device byte is a setting of the unit, not a property of the protocol: it is written in the global settings at address
`0x0030` and copied into every answer. A unit that keeps the factory value therefore sends `F0 47 00 25`. On input, the
unit checks the manufacturer ID and the model, and ignores the device byte, so 2 units of the same model are told apart
by the MIDI port they answer on.

== The size field

The 2 size bytes are packed the way all large values are: the first byte holds the high 7 bits, the second the low
7 bits, and the value is `hi * 128 + lo`.

The size is the *length of the payload*: it counts the bytes between the size field and `F7`, whatever they are. A
read states the number of bytes it wants in the first byte of the payload, so the count is part of what the size counts.

#figure(
  ptable(
    columns: (auto, auto, auto, 1fr),
    head([*Message*], [*Payload*], [*Size*], [*Example*]),
    [Query, any length], [count, then address], [`00 03`], [`08 00 00` → `00 03`],
    [Write of 1 byte], [count, address, then data], [`00 04`], [`01 00 13 01` → `00 04`],
    [Write of 64 bytes], [count, address, then data], [`00 43`], [`40 05 14 …` → `00 43`],
    [Write of 127 bytes], [count, address, then data], [`01 02`], [`7F 0B 3C …` → `01 02`],
    [Acknowledgement], [count, address, then status], [`00 04`], [`01 00 13 00` → `00 04`],
    [User preset], [preset, name, then data], [`0C 0B`], [`…` → `0C 0B`],
  ),
  caption: [Payload and size of the messages of the protocol],
)

A read of 1 byte at address `0x0001` carries 3 bytes of payload: the count `01` and the 2 address bytes, so the size
is `00 03`. A read of 8 bytes at the same address carries the count `08` in the same 3 bytes: the size stays `00 03`,
and 8 bytes come back.

The count byte has its most significant bit clear, so it cannot exceed 127: a read asks for at most 127 bytes and a
write carries at most 127 data bytes. A larger block is read or written in several messages, 1 per 127-byte slice. The
size field is 14-bit, which is how a user preset declares a payload of 1547 bytes with `0C 0B`.

== Reading memory

A request for 1 byte of the global settings, at address `0x0001` — the index of the current preset — is:

#figure(
  bytefield(
    bpr: 88,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[20],
    bytes(2, fill: l_purple)[00 03],
    bytes(1, fill: l_orange)[01],
    bytes(2, fill: l_green)[00 01],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [Reading 1 byte of the global settings],
)

The answer repeats the count and the address, then carries the byte:

#figure(
  bytefield(
    bpr: 96,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[30],
    bytes(2, fill: l_purple)[00 04],
    bytes(1, fill: l_orange)[01],
    bytes(2, fill: l_green)[00 01],
    bytes(1, fill: l_blue)[07],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [The answer to that request],
)

The byte `07` is the index of the current preset, counted from zero: it is the preset displayed as 8, which is why the
displayed number is 1 more than the byte. Writing that byte loads the preset (see #cref(<chap-global>)).

The same request through the `QEDIT` command reads the colour of a pad instead, at address `0x0B3C` (see #cref(<chap-pads>)).

A name is a read of 8 bytes: the count `08`, followed by the address `0x0000`.

#figure(
  bytefield(
    bpr: 88,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[21],
    bytes(2, fill: l_purple)[00 03],
    bytes(1, fill: l_orange)[08],
    bytes(2, fill: l_green)[00 00],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [Reading the name of the edit buffer],
)

The answer is 8 bytes longer, and its size counts them:

#figure(
  bytefield(
    bpr: 152,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[31],
    bytes(2, fill: l_purple)[00 0B],
    bytes(1, fill: l_orange)[08],
    bytes(2, fill: l_green)[00 00],
    bytes(8, fill: l_blue)[StudOne5],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [The name in the answer],
)

== Writing memory

A write adds the data after the address: its payload becomes the count, the 2 address bytes and the data. Writing the
value `01` at edit buffer address `0x0013` — the range of the arpeggiator — is:

#figure(
  bytefield(
    bpr: 96,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[31],
    bytes(2, fill: l_purple)[00 04],
    bytes(1, fill: l_orange)[01],
    bytes(2, fill: l_green)[00 13],
    bytes(1, fill: l_blue)[01],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [Writing 1 byte of the edit buffer],
)

The unit answers with the `ACKE` command, whose status byte is `00` on success:

#figure(
  bytefield(
    bpr: 96,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[39],
    bytes(2, fill: l_purple)[00 04],
    bytes(1, fill: l_orange)[01],
    bytes(2, fill: l_green)[00 13],
    bytes(1, fill: l_blue)[00],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [The acknowledgement],
)

A write to the edit buffer or to the global settings changes the unit at once and is lost at power-off: the global
settings are saved by the `SGLOB` command, and a user preset by the `WPRESET` command. A block written in several
messages can be left half written, so a restore writes as few messages as possible, then reads the block back.

== The end of the MIDI frame

The payload ends with its last data byte, and `F7` follows it at once: a parser reads the size field, takes that many
bytes, and expects `F7` next. The frames of this book carry no checksum and no user reference byte, so an appended
checksum produces a MIDI frame whose size no longer matches its content.

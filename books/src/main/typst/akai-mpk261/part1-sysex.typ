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

The frame carries no checksum and no user reference byte: `F7` follows the last payload byte.

== The size field

The size counts the payload, in bytes, as a 14-bit value: 2 bytes of 7 bits, the high group first, and the value is
`hi * 128 + lo`. A read request carries its count in the payload, so the count is part of the size. A MIDI data byte
keeps 7 bits, so a count never exceeds 127: a frame carries at most 127 data bytes, and a larger block takes several
frames.

#cref(<chap-inquiry>) describes the device inquiry, which uses its own frames. #cref(<chap-edit-cmds>) gives the
commands of the 2 edit buffers, and #cref(<chap-flash-cmds>) the commands of the flash memory.

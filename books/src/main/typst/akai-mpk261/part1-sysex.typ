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
    [`ss ss`], [payload size], [2 bytes, 14-bit packing: the number of bytes of the payload],
    [`...`], [payload], [Layout depends on the command],
    [`F7`], [end], [End of SysEx],
  ),
  caption: [Structure of an MPK-261 SysEx message],
)

#figure(
  frame(
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
      bytes(1, fill: l_gray)[device],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[cmd],
      bytes(2, fill: l_purple)[size],
      bytes(6)[payload],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
  ),
  caption: [The 5 header bytes and the trailer],
)

The device byte is a setting of the unit, not a property of the protocol: it is written in the global settings at address
`0x0030` and copied into every answer. A unit that keeps the factory value therefore sends `F0 47 00 25`. On input, the
unit checks the manufacturer ID and the model, and ignores the device byte, so 2 units of the same model are told apart
by the MIDI port they answer on.

The frame carries no checksum and no user reference byte: `F7` follows the last payload byte.

== The 2 sizes

The frame carries a size, and the payload of a read or of a write carries another one:

- The *payload size* sits at `0x0005`, in the frame itself. It is 2 bytes of 7 bits, the high group first, and it counts
    the bytes between it and the `F7`: the whole payload. Most payloads hold a few bytes, so the first byte is `00` and
    the second gives the length: `00 03` for 3 bytes, `00 04` for 4. A payload above 127 bytes changes the first byte:
    the answer of `QPRESET` carries 1547 bytes, `0C 0B`;
- The *count* sits at `0x0007`, in the payload of a read or of a write. It is 1 byte, and it counts the bytes that the
    command reads or writes, after the count and the address.

A MIDI data byte keeps 7 bits, so the count never exceeds 127: a command reads or writes at most 127 bytes, and a larger
block takes several frames.

The payload size is the only packed value of the frame: 1 byte would stop at 127, and a preset dump carries 1547.

The book names the 2 fields as above: *payload size* for the frame, *count* for the payload. A size of bytes taken from
the memory is always a count, so it is never packed.

== What an address reaches

The address of a read or of a write is free: it designates any byte of the buffer, so a command starts in the middle of a
parameter, covers several parameters at once, or reads the bytes of a field and the start of the next.

Each command knows its own buffer, and the unit checks the whole block against it:

- A block that fits returns its bytes;
- A block that crosses the end of the buffer returns count `0`, without an error code;
- A write whose block does not fit answers with the status `01`.

`QGLOB` and `WGLOB` reach the 377 bytes of the global edit buffer, `QEDIT` and `WEDIT` the 1544 bytes of the preset edit
buffer. A read past the end of one of these buffers returns count `0`.

The flash memory has no such read: `QPRESET` takes a preset number, not an address, and returns the whole
1546-byte record. `PPRESET` is the only command that carries an address in the flash memory; #cref(<chap-flash-cmds>) gives its
bounds.

#cref(<chap-inquiry>) describes the device inquiry, which uses its own frames. #cref(<chap-edit-cmds>) gives the
commands of the 2 edit buffers, and #cref(<chap-flash-cmds>) the commands of the flash memory.

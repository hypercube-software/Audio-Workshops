#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Flash memory commands", l: "chap-flash-cmds")

The flash memory holds the 30 user presets and the stored settings. The preset commands carry the whole record or a
block of it; the setting commands carry a fixed payload.

The figures colour the fields as in #cref(<chap-edit-cmds>), with 1 addition: yellow is the preset number.

== User presets

`QPRESET` asks for 1 stored preset. Its payload is 1 byte, the preset number, from 1 to 30; `00` designates the
current preset (edit buffer):

#figure(
  frame(
    bytefield(
      bpr: 72,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[12],
      bytes(2, fill: l_purple)[00 01],
      bytes(1, fill: l_yellow)[08],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
    (l_yellow, [preset number]),
  ),
  caption: [QPRESET: Query user preset 8],
) <fig-qpreset>

The preset number is the number displayed by the unit, from 1 to 30. The byte at `0x0001` of the global settings
counts from zero: the reference preset is displayed as 8, carried as `08` in its dump and as `07` in the global
settings.

#caveat[
  Copying the byte of one place to the other is off by one. The 2 fields have different origins, and
  neither byte carries its convention.
]

The answer reuses the `WPRESET` frame: the preset number, the name and the whole record, in a frame of 1555 bytes. Its
payload size is `0C 0B`, that is 1547 bytes: 1 for the preset number, 8 for the name, 1538 for the data.

The answer reads the stored record: a preset modified in the edit buffer comes back as it was saved. There is no
request for all 30 presets: the front panel sends 30 MIDI frames, 1 per preset, each one complete with its own size
field.

#figure(
  frame(
    bytefield(
      bpr: 184,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[10],
      bytes(2, fill: l_purple)[0C 0B],
      bytes(1, fill: l_yellow)[08],
      bytes(8)[name],
      bytes(6)[1538 data bytes],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
    (l_yellow, [preset number]),
  ),
  caption: [WPRESET: Write user preset 8, and the answer to QPRESET],
) <fig-wpreset>

A dump of preset `00` is built from the *edit buffer*, not read from the flash memory: the bytes of the record that the edit buffer
does not hold come back as `00`. To back up a preset, dump its preset number.

`WPRESET` writes with the same frame (see #cref(<chap-preset>)). The unit answers with `ACKW`, which gives the preset
number and a status byte:

#figure(
  frame(
    bytefield(
      bpr: 80,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[11],
      bytes(2, fill: l_purple)[00 02],
      bytes(1, fill: l_yellow)[08],
      bytes(1, fill: l_red)[00],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
    (l_yellow, [preset number]),
    (l_red, [status]),
  ),
  caption: [ACKW: the answer to a write of user preset 8],
)

A `WPRESET` frame writes a user preset, and the new content reaches the edit buffer when the preset is loaded (see
#cref(<chap-global>)). A write is verified by reading the preset back with `QPRESET` and comparing the 1547-byte
payload: the status byte of `ACKW` says that the frame arrived, not that the record is valid.

`PPRESET` writes a block of bytes at an address inside the stored record, without carrying the whole preset. Its
payload is the count, the packed address, the preset number, then the data; the frame below writes 2 bytes at `0x0575`, in the
record of preset 8:

#figure(
  frame(
    bytefield(
      bpr: 112,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[32],
      bytes(2, fill: l_purple)[00 06],
      bytes(1, fill: l_orange)[02],
      bytes(2, fill: l_green)[05 75],
      bytes(1, fill: l_yellow)[08],
      bytes(2)[00 01],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
    (l_orange, [count]),
    (l_green, [address]),
    (l_yellow, [preset number]),
  ),
  caption: [PPRESET: Poke 2 bytes at 0x0575 in user preset 8],
)

The unit answers with `ACKP`, which repeats the count, the address and the preset number, then gives a status byte:

#figure(
  frame(
    bytefield(
      bpr: 104,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[3A],
      bytes(2, fill: l_purple)[00 05],
      bytes(1, fill: l_orange)[02],
      bytes(2, fill: l_green)[05 75],
      bytes(1, fill: l_yellow)[08],
      bytes(1, fill: l_red)[00],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
    (l_orange, [count]),
    (l_green, [address]),
    (l_yellow, [preset number]),
    (l_red, [status]),
  ),
  caption: [ACKP: the answer to a poke of 2 bytes at 0x0575 in user preset 8],
)

The address is an offset in the record, and the unit does not compare it with the 1546 bytes of the record: an offset
past the end continues in the next user preset.

#caveat[
  `PPRESET` pokes raw bytes into a user preset, without checking that the result forms a valid record. A user preset
  is written with the `WPRESET` command, which carries the whole record.
]

== Stored settings

3 commands reach the settings saved in flash memory, and none of them answers.

`SGLOB` sets the contrast to its 1 payload byte, then saves the 18 settings of the global edit buffer to flash memory
(see #cref(<chap-store>)):

#figure(
  frame(
    bytefield(
      bpr: 72,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[7B],
      bytes(2, fill: l_purple)[00 01],
      bytes(1)[32],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
  ),
  caption: [SGLOB: Save the global settings],
)

`FIELD4` writes 4 bytes in a stored field whose role is not established:

#figure(
  frame(
    bytefield(
      bpr: 96,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[78],
      bytes(2, fill: l_purple)[00 04],
      bytes(4)[data],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
  ),
  caption: [FIELD4: Write 4 bytes],
)

`SERIAL` writes the 16 bytes of the serial number that the device inquiry reports (see #cref(<chap-inquiry>)):

#figure(
  frame(
    bytefield(
      bpr: 192,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[79],
      bytes(2, fill: l_purple)[00 10],
      bytes(16)[serial, ASCII],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
  ),
  caption: [SERIAL: Write the serial number],
)

#import "../common/settings.typ": *
#import "lib.typ": *

#part("The preset")

#chapter("Memory layout", l: "chap-preset")

A preset gathers everything that a performance uses: the settings of the keyboard and its split, of the wheels and the
pedals, of the 64 pads, and of the 24 switches, 24 faders and 24 knobs. The unit keeps it in 2 places:

- The *edit buffer* is a RAM block of 1544 bytes, at addresses `0x0000` to `0x0C07`. The `QEDIT` command reads it and the
  `WEDIT` command writes it; a write changes the settings at once, and the unit clears the block at power-off.
- A *user preset* is 1 of the 30 presets of the flash memory. The `QPRESET` command reads 1 of them and the `WPRESET` command writes 1
  of them; the content survives a power cycle. Loading a user preset reads it into the edit buffer.

The front panel edits the edit buffer, and storing a preset writes it into the flash memory. Both forms carry the same settings in
2 different layouts, and the 2 pairs of commands take a different argument: an address for the edit buffer, a preset
number for a user preset.

== The edit buffer

The edit buffer is a *structure of arrays*: each field forms a flat array that holds 1 value per control, and the arrays
follow each other. The 1544 bytes divide as follows:

#figure(
  ptable(
    columns: (auto, auto, auto, 1fr),
    head([*Packed address*], [*Size*], [*Content*], [*Chapter*]),
    [`00 00`], [8], [Name of the preset], [#cref(<chap-header>)],
    [`00 08`], [2], [Tempo, 14-bit packed], [#cref(<chap-header>)],
    [`00 0A`], [12], [Transport and arpeggiator], [#cref(<chap-header>)],
    [`00 16`], [18], [Keyboard, split and zones], [#cref(<chap-keyboard>)],
    [`00 28`], [35], [Wheels, expression pedal and footswitches], [#cref(<chap-keyboard>)],
    [`00 4B`], [16], [Arpeggiator pattern, 1 byte per step], [#cref(<chap-header>)],
    [`00 5B`], [65], [DAW buttons, 5 records], [#cref(<chap-controls>)],
    [`01 1C`], [312], [Switches, 24 records], [#cref(<chap-controls>)],
    [`03 54`], [144], [Faders, 24 records], [#cref(<chap-controls>)],
    [`04 64`], [216], [Knobs, 24 records], [#cref(<chap-controls>)],
    [`06 3C`], [704], [Pads, 64 records], [#cref(<chap-pads>)],
    [`0B 7C`], [12], [Unidentified, identical in every preset examined], [],
  ),
  caption: [Layout of the 1544-byte preset block],
)

The 5 control blocks are structures of arrays, which is the subject of #cref(<chap-controls>). The header, the keyboard
block and the arpeggiator pattern are plain fields, in the order of the table above.

== `WPRESET`

A user preset holds 1546 bytes: the 8 bytes of the name and 1538 bytes of data. The preset number designates the preset in
the MIDI frame, and is not part of the record. The 2 commands designate a preset by its number, from 1 to 30; `00`
designates the preset that is playing.

The `WPRESET` command writes the whole record into the preset that its number designates. Its MIDI frame is 1555 bytes
(see @fig-wpreset): the usual header, the size `0C 0B`, the number and the name of the preset, 1538 bytes of data, and
`F7`.

The size `0C 0B` counts the payload that follows: the preset number, the 8 bytes of the name and 1538 bytes of data,
that is 1547 bytes. Every dump carries the same size, whatever the preset, because the MIDI frame has a fixed length.

#figure(
  ptable(
    columns: (auto, auto, 1fr),
    head([*Offset*], [*Size*], [*Content*]),
    [`0x000`], [4], [Header `F0 47 00 25`],
    [`0x004`], [1], [Command, preset dump],
    [`0x005`], [2], [Size `0C 0B`],
    [`0x007`], [1], [Preset number, 1 to 30],
    [`0x008`], [8], [Name, ASCII, padded with spaces],
    [`0x010`], [13], [Tempo, transport and arpeggiator],
    [`0x01D`], [7], [Keyboard],
    [`0x024`], [25], [Wheels and pedals],
    [`0x03D`], [704], [Pads, 64 records of 11 bytes],
    [`0x2FD`], [216], [Knobs, 24 records of 9 bytes],
    [`0x3D5`], [144], [Faders, 24 records of 6 bytes],
    [`0x465`], [312], [Switches, 24 records of 13 bytes],
    [`0x59D`], [65], [DAW buttons, 5 records of 13 bytes],
    [`0x5DE`], [11], [unidentified],
    [`0x5E9`], [17], [Split zones and split point],
    [`0x5FA`], [24], [unidentified],
    [`0x612`], [1], [`F7`],
  ),
  caption: [Regions of a user preset],
)

The offsets of this chapter are counted from the first byte of the MIDI frame, `F0` included, because that is how the
regions align. Without the 7 header bytes of a `.syx` file, every offset of the table drops by 7.

The 13 bytes at `0x010` hold the tempo, the transport and the arpeggiator, in an order that is not established. The 16
steps of the pattern have no 16-byte block of their own.

The `ACKW` answer of `WPRESET` gives the preset number and a status byte, and nothing more: a write is verified by
reading the preset back with the `QPRESET` command and comparing the 1547-byte payload.

The edit buffer keeps its content: a `WPRESET` frame writes a user preset, and the new content reaches the edit buffer
when the preset is loaded (see #cref(<chap-global>)).

#caveat[
  The `PPRESET` command pokes raw bytes into a user preset, without checking that the result forms a valid record. A
  user preset is written with the `WPRESET` command, which carries the whole record.
]

== Number and name

The preset number at `0x007` is the number displayed by the unit, from 1 to 30. The byte at `0x0001` of the global
settings counts from zero: the reference preset is displayed as 8, carried as `08` in its dump and as `07` in the
global settings.

#caveat[
  Copying the byte of one place to the other is off by one. The 2 fields have different origins, and
  neither byte carries its convention.
]

The name at `0x008` is 8 ASCII characters, padded on the right with spaces. It is the first field of the saved record,
so a dump carries it once. The edit buffer keeps its own name at `0x0000`: renaming a user preset changes
the name here, and the edit buffer shows the old name until the preset is loaded.

== Records of a user preset

A dump holds the same controls as the edit buffer, in another shape: the edit buffer is a structure of arrays, a dump
an array of structures. Each control has 1 record, and the fields of that record follow each other.

#figure(
  ptable(
    columns: (auto, auto, auto, 1fr),
    head([*Block*], [*Records*], [*Record size*], [*Fields, in the order of the record*]),
    [Pads], [64], [11], [Type, channel, note or program number, then the flags and the bank bytes, then the 2 colours],
    [Knobs], [24], [9], [Type, channel, controller, minimum, maximum, MIDI to DIN, MSB, LSB, value],
    [Faders], [24], [6], [Type, channel, controller, minimum, maximum, MIDI to DIN],
    [Switches], [24], [13], [Type, channel, controller, mode, program number, MSB, LSB, MIDI to DIN, note, velocity,
      unidentified, key 1, key 2],
    [DAW buttons], [5], [13], [The same 13 fields as a switch],
  ),
  caption: [Records of a user preset, and their fields],
)

The fields are the ones that the chapters of this part describe, with their encodings. 2 records change the order of
the fields. In the edit buffer, the MIDI to DIN flag of a knob comes after its value; in a dump, it comes before the
MSB. In a pad record the 2 colours are the last 2 bytes, at offsets 9 and 10, where the memory keeps them in 2 arrays
of 64 bytes.

#caveat[
  The position of the MIDI to DIN flag inside a pad record is not established: the values observed at offset 3 of the
  record are compatible with that flag, and with the momentary/toggle mode as well. Changing a pad goes through the edit
  buffer, with the `WEDIT` command, which is fully mapped, and the unit produces the dump. An archive keeps the records
  as they are.
]

Record `i` of a block starts at `block + i * size`, where `block` and `size` come from the table above. Pad 1 of bank A
is the first record, at `0x03D`; pad 16 of bank D is the last, at `0x2F2`. Knob 1 follows the pads at `0x2FD`, and the
first fader at `0x3D5`.

== Keyboard and split

#block(breakable: false)[
  The keyboard block of a dump holds the same content as `0x0016` to `0x001C` of the edit buffer, in a slightly
  different order:

  #kfigure(
    ptable(
      columns: (auto, auto, auto),
      head([*Offset*], [*Field*], [*Ref.*]),
      [`0x01D`], [Channel], [`00`],
      [`0x01E`], [Octave], [`04`],
      [`0x01F`], [Aftertouch], [`01`],
      [`0x020`], [flag], [`01`],
      [`0x021`], [MIDI to DIN], [`00`],
      [`0x022`], [Transpose], [`24`],
      [`0x023`], [Transport type], [`03`],
    ),
    caption: [Keyboard block of a user preset],
  )
]

The split is at the end of the MIDI frame, at `0x5E9`: the 7 zone bytes of zone A, the 7 of zone B, then the split
switch, the split note and the B channel. It carries the same information as `0x001D` to `0x0026` of the edit buffer,
in the order of the front panel rather than in the order of the memory.

== `QPRESET`

The `QPRESET` command carries 1 payload byte, the preset number (see @fig-qpreset).

The unit answers with the frame of the `WPRESET` command, byte for byte the same frame that the front panel sends when
it exports that preset. The answer reads the stored preset, not the edit buffer: a preset that was modified in the edit
buffer comes back as it was saved.

There is no request for all 30 presets: the front panel sends 30 MIDI frames, 1 per preset, each one complete with its
own size field.

== The 2 layouts

#figure(
  ptable(
    columns: (auto, auto, auto),
    head([*Property*], [*Edit buffer*], [*User preset*]),
    [Command], [`QEDIT`, `WEDIT`], [`QPRESET`, `WPRESET`],
    [Size], [1544 bytes], [1546-byte record, 1547-byte payload],
    [Layout], [Structure of arrays], [Array of structures],
    [Scope], [The preset in use], [1 of the 30 presets],
    [Lifetime], [Lost at power-off], [Kept until it is written again],
    [Address], [`0x0000` to `0x0C07`], [The preset number in the MIDI frame],
  ),
  caption: [The 2 layouts of a preset],
)

The rule of thumb is simple: use the edit buffer to *edit*, because every field is 1 byte at a known address, and use
the user preset to *store*, because it is what survives a power cycle and what a backup keeps.

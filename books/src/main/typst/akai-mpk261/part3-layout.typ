#import "../common/settings.typ": *
#import "lib.typ": *

#part("The preset")

#chapter("Memory layout", l: "chap-preset")

A preset gathers everything that a performance uses: the settings of the keyboard and its split, of the wheels and the
pedals, of the 64 pads, and of the 24 switches, 24 faders and 24 knobs. The unit keeps it in 2 places, in 2 layouts.

== The 2 layouts

- The *edit buffer* is a RAM block of 1544 bytes, at addresses `0x0000` to `0x0C07`. A write changes the settings at
    once, and the unit clears the block at power-off;
- A *user preset* is 1 of the 30 presets of the flash memory. It survives a power cycle, and a backup keeps it.

#figure(
  ptable(
    columns: (auto, auto, auto),
    head([*Property*], [*Edit buffer*], [*User preset*]),
    [Command], [`QEDIT`, `WEDIT`], [`QPRESET`, `WPRESET`],
    [Size], [1544 bytes], [1546-byte record, 1547-byte payload],
    [Controls layout], [Structure of arrays], [Array of structures],
    [Scope], [The preset in use], [1 of the 30 presets],
    [Lifetime], [Lost at power-off], [Kept until it is written again],
    [Address], [`0x0000` to `0x0C07`], [`0x0000` to `0x0C09`],
  ),
  caption: [The 2 layouts of a preset],
)

Loading a user preset reads it into the edit buffer, and storing the edit buffer writes it into the preset. The rule of
thumb is simple: use the edit buffer to *edit*, because every field is 1 byte at a known address, and use the user preset
to *store*.

== The 2 addresses

A parameter of a control has 1 value per control, and 2 addresses:

- The *array address* lies in the *edit buffer*. The values follow each other, 1 byte each, so the value of control `i`
    sits at the array address plus `i`;
- The *field address* lies in a *user preset*. The value sits at a fixed offset in the record of each control, so the
    value of control `i` sits at the field address plus `i` times the size of the record.

The 2 layouts do not order the fields the same way: a user preset holds every field of 1 control, the edit buffer every
control of 1 field, and some fields change position from one layout to the other. That is why the tables of the controls
give both addresses.

For the knobs, the type is at `0x0464` in the edit buffer and at `0x0575` in a user preset: the 24 values of the 24
knobs follow the first address, and the records are 9 bytes long.

== The edit buffer

The edit buffer holds 1544 bytes. Only the controls form arrays: each field of a control type is a flat array that holds
1 value per control, and the arrays of 1 block follow each other. The header, the keyboard and the pattern are plain
fields. #cref(<chap-controls>) describes the control blocks.

The 1544 bytes divide as follows:

#figure(
  ptable(
    columns: (auto, auto, auto, 1fr, auto),
    head([*Packed address*], [*Record size*], [*Array size*], [*Content*], [*Chapter*]),
    [`00 00`], [8], [—], [Name of the preset], [#cref(<chap-header>)],
    [`00 08`], [2], [—], [Tempo, 14-bit packed], [#cref(<chap-header>)],
    [`00 0A`], [12], [—], [Transport and arpeggiator], [#cref(<chap-header>)],
    [`00 16`], [18], [—], [Keyboard, split and zones], [#cref(<chap-keyboard>)],
    [`00 28`], [35], [—], [Wheels, expression pedal and footswitches], [#cref(<chap-keyboard>)],
    [`00 4B`], [16], [—], [Arpeggiator pattern, 1 byte per step], [#cref(<chap-header>)],
    [`00 5B`], [65], [5], [DAW buttons, 13 arrays of 5 values, 1 per field], [#cref(<chap-controls>)],
    [`01 1C`], [312], [24], [Switches, 13 arrays of 24 values, 1 per field], [#cref(<chap-controls>)],
    [`03 54`], [144], [24], [Faders, 6 arrays of 24 values, 1 per field], [#cref(<chap-controls>)],
    [`04 64`], [216], [24], [Knobs, 9 arrays of 24 values, 1 per field], [#cref(<chap-controls>)],
    [`06 3C`], [704], [64], [Pads, 11 arrays of 64 values, 1 per field], [#cref(<chap-pads>)],
    [`0B 7C`], [12], [—], [Unidentified, identical in every preset examined], [],
  ),
  caption: [Layout of the 1544-byte preset block],
)

== The user preset

A user preset holds 1546 bytes: the 8 bytes of the name and 1538 bytes of data. The preset number designates the preset
in the MIDI frame, and is not part of the record.

#figure(
  ptable(
    columns: (auto, auto, auto, 1fr),
    head([*Packed address*], [*Record size*], [*Array size*], [*Content*]),
    [`00 00`], [8], [—], [Name, ASCII, padded with spaces],
    [`00 08`], [13], [—], [Tempo, transport and arpeggiator],
    [`00 15`], [7], [—], [Keyboard],
    [`00 1C`], [25], [—], [Wheels and pedals],
    [`00 35`], [704], [64], [Pads, 64 records of 11 bytes, 1 per pad],
    [`05 75`], [216], [24], [Knobs, 24 records of 9 bytes, 1 per knob],
    [`07 4D`], [144], [24], [Faders, 24 records of 6 bytes, 1 per fader],
    [`08 5D`], [312], [24], [Switches, 24 records of 13 bytes, 1 per switch],
    [`0B 15`], [65], [5], [DAW buttons, 5 records of 13 bytes, 1 per button],
    [`0B 56`], [11], [—], [unidentified],
    [`0B 61`], [17], [—], [Split zones and split point],
    [`0B 72`], [24], [—], [unidentified],
  ),
  caption: [Regions of a user preset],
)

The regions are the same as those of the edit buffer, and the blocks keep their size: only the order changes inside
them. A block of knobs is 9 arrays of 24 values in the edit buffer, and 24 records of 9 bytes in a user preset.

The addresses start at the record: the preset number, the command and the size of the MIDI frame are not part of it. They
are the addresses that `PPRESET` takes, so a read or a poke uses them as they are.

The 13 bytes at `0x0008` hold the tempo, the transport and the arpeggiator, in an order that is not established. The 16
steps of the pattern have no 16-byte block of their own.

The record holds the same controls as the edit buffer, in another shape: the edit buffer is a structure of arrays, the
record an array of structures. Each control has 1 record, and the fields of that record follow each other.

#kfigure(
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
the fields. In the edit buffer, the MIDI to DIN flag of a knob comes after its value; in a record, it comes before the
MSB. In a pad record the 2 colours are the last 2 bytes, at offsets 9 and 10, where the memory keeps them in 2 arrays
of 64 bytes.

#caveat[
  The position of the MIDI to DIN flag inside a pad record is not established: the values observed at offset 3 of the
  record are compatible with that flag, and with the momentary/toggle mode as well. Changing a pad goes through the edit
  buffer, with the `WEDIT` command, which is fully mapped, and the unit produces the dump. An archive keeps the records
  as they are.
]

Record `i` of a block starts at the address of the block plus `i` times the record size. Pad 1 of bank A is the first
record, at `0x0035`; pad 16 of bank D is the last, at `0x056A`. Knob 1 follows the pads at `0x0575`, and the first fader
at `0x074D`.

#block(breakable: false, width: 100%)[
  The keyboard block of the record holds the same content as `0x0016` to `0x001C` of the edit buffer, in a slightly
  different order:

  #kfigure(
    ptable(
      columns: (auto, auto, auto),
      head([*Packed address*], [*Field*], [*Ref.*]),
      [`00 15`], [Channel], [`00`],
      [`00 16`], [Octave], [`04`],
      [`00 17`], [Aftertouch], [`01`],
      [`00 18`], [flag], [`01`],
      [`00 19`], [MIDI to DIN], [`00`],
      [`00 1A`], [Transpose], [`24`],
      [`00 1B`], [Transport type], [`03`],
    ),
    caption: [Keyboard block of a user preset],
  )
]

The split sits at the end of the record, at `0x0B61`: the 7 zone bytes of zone A, the 7 of zone B, then the split
switch, the split note and the B channel. It carries the same information as `0x001D` to `0x0026` of the edit buffer,
in the order of the front panel rather than in the order of the memory.

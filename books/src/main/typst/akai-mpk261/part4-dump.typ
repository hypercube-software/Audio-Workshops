#import "../common/settings.typ": *
#import "lib.typ": *

#part("User presets")

#chapter("The preset dump", l: "chap-dump")

A user preset is 1 of the 30 slots of the unit. The `WPRESET` command writes 1 whole preset into a slot in 1 message, and the `QPRESET` command
reads 1 back: they are what a librarian saves, what a backup restores, and what the front panel sends when a preset is
exported.

The 2 commands designate a slot by its number, from 1 to 30; `00` designates the preset that is playing.

== The MIDI frame

A preset dump is 1555 bytes: the usual header, the size `0C 0B`, the number and the name of the preset, 1538 bytes of
data, and `F7`.

#figure(
  bytefield(
    bpr: 184,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[10],
    bytes(2, fill: l_purple)[0C 0B],
    bytes(1, fill: l_orange)[n],
    bytes(8, fill: l_green)[name],
    bytes(6, fill: l_gray)[1538 data bytes],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [MIDI frame of a preset dump],
)

The size `0C 0B` counts the payload that follows: the preset number, the 8 bytes of the name and 1538 bytes of data,
that is 1547 bytes. Every dump carries the same size, whatever the preset, because the MIDI frame has a fixed length.

#figure(
  ptable(
    columns: (auto, auto, auto, 1fr),
    head([*Offset*], [*Size*], [*Content*], [*Status*]),
    [`0x000`], [4], [Header `F0 47 00 25`], [Fixed],
    [`0x004`], [1], [Command, preset dump], [Fixed],
    [`0x005`], [2], [Size `0C 0B`], [Fixed],
    [`0x007`], [1], [Preset number, 1 to 30], [Documented],
    [`0x008`], [8], [Name, ASCII, padded with spaces], [Documented],
    [`0x010`], [13], [Tempo, transport and arpeggiator], [Partially identified],
    [`0x01D`], [7], [Keyboard], [Documented],
    [`0x024`], [25], [Wheels and pedals], [Partially identified],
    [`0x03D`], [704], [Pads, 64 records of 11 bytes], [Documented],
    [`0x2FD`], [216], [Knobs, 24 records of 9 bytes], [Documented],
    [`0x3D5`], [144], [Faders, 24 records of 6 bytes], [Documented],
    [`0x465`], [312], [Switches, 24 records of 13 bytes], [Documented],
    [`0x59D`], [65], [DAW buttons, 5 records of 13 bytes], [Documented],
    [`0x5DE`], [11], [unidentified], [—],
    [`0x5E9`], [17], [Split zones and split point], [Documented],
    [`0x5FA`], [24], [unidentified], [—],
    [`0x612`], [1], [`F7`], [Fixed],
  ),
  caption: [Regions of a preset dump],
)

The offsets of this chapter are counted from the first byte of the MIDI frame, `F0` included, because that is how the
regions align. A tool that reads a `.syx` file and strips the 7 bytes of the header subtracts 7 from every offset of the
table.

== Reading a user preset

The `QPRESET` command carries 1 payload byte, the number of the slot:

#figure(
  bytefield(
    bpr: 72,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[12],
    bytes(2, fill: l_purple)[00 01],
    bytes(1, fill: l_orange)[n],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [Reading a user preset],
)

The unit answers with the `WPRESET` frame of figure 4.1, byte for byte the same frame that the front panel sends when
it exports that preset. The answer reads the saved copy, not the edit buffer: a preset that was modified in the edit
buffer comes back as it was saved.

There is no request for all 30 presets: the front panel sends 30 MIDI frames, 1 per preset, each one complete with its
own size field.

== Writing a user preset

The `WPRESET` command writes the whole record into the slot that its number designates. The unit answers nothing, so a tool
verifies the write by reading the preset back with the `QPRESET` command and comparing the 1547-byte payload.

The edit buffer keeps its content: a `WPRESET` frame writes a user preset, and the new content reaches the edit buffer when
the preset is loaded (see #cref(<chap-global>)).

The comparison covers every byte: the unidentified bytes are part of the preset, and a dump that drops them produces a
preset that the unit accepts and that loses part of its state.

== Number and name

The preset number at `0x007` is the number displayed by the unit, from 1 to 30. The byte at `0x0001` of the global
settings counts from zero: the reference preset is displayed as 8, carried as `08` in its dump and as `07` in the
global settings.

#caveat[
  A tool that copies the byte of one place to the other is off by one. The 2 fields have different origins, and
  neither byte carries its convention.
]

The name at `0x008` is 8 ASCII characters, padded on the right with spaces. It is the first field of the saved record,
so a dump carries it once. The edit buffer keeps its own name at `0x0000`: a tool that renames a user preset changes
the name here, and the edit buffer shows the old name until the preset is loaded.

== Records of the dump

The data of a dump holds the same controls as the edit buffer, in another shape: the edit buffer is a structure of
arrays, the dump an array of structures. Each control has 1 record, and the fields of that record follow each other.

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
  caption: [Records of the dump, and their fields],
)

2 records change the order of the fields. In the edit buffer, the MIDI to DIN flag of a knob comes after its value; in
a dump, it comes before the MSB. In a pad record the 2 colours are the last 2 bytes, at offsets 9 and 10, where the
memory keeps them in 2 arrays of 64 bytes.

#caveat[
  The position of the MIDI to DIN flag inside a pad record is not established: the values observed at offset 3 of the
  record are compatible with that flag, and with the momentary/toggle mode as well. A tool that must change a pad
  writes the edit buffer with the `WEDIT` command, which is fully mapped, and lets the unit produce the dump; a tool that only
  archives presets keeps the records as they are.
]

Record `i` of a block starts at `block + i * size`, where `block` and `size` come from the table above. Pad 1 of bank A
is the first record, at `0x03D`; pad 16 of bank D is the last, at `0x2F2`. Knob 1 follows the pads at `0x2FD`, and the
first fader at `0x3D5`.

== Keyboard and split

The keyboard block of a dump holds the same content as `0x0016` to `0x001C` of the edit buffer, in a slightly
different order: channel, octave, aftertouch, a flag, MIDI to DIN, transpose, then the transport type.

#figure(
  ptable(
    columns: (auto, auto, auto, 1fr),
    head([*Offset*], [*Field*], [*Ref.*], [*Meaning*]),
    [`0x01D`], [Channel], [`00`], [Same encoding as the edit buffer],
    [`0x01E`], [Octave], [`04`], [Plus 4],
    [`0x01F`], [Aftertouch], [`01`], [Off, on],
    [`0x020`], [flag], [`01`], [Constant],
    [`0x021`], [MIDI to DIN], [`00`], [Off, on],
    [`0x022`], [Transpose], [`24`], [Plus 36],
    [`0x023`], [Transport type], [`03`], [`00` is MMC, `04` the Pro Tools variant],
  ),
  caption: [Keyboard block of a dump],
)

The split is at the end of the MIDI frame, at `0x5E9`: the 7 zone bytes of zone A, the 7 of zone B, then the split
switch, the split note and the B channel. It carries the same information as `0x001D` to `0x0026` of the edit buffer,
in the order of the front panel rather than in the order of the memory.

== The 2 views of a preset

#figure(
  ptable(
    columns: (auto, auto, auto),
    head([*Property*], [*Edit buffer*], [*User preset*]),
    [Command], [`QEDIT`, `WEDIT`], [`QPRESET`, `WPRESET`],
    [Size], [1544 bytes], [1546-byte record, 1547-byte payload],
    [Layout], [Structure of arrays], [Array of structures],
    [Scope], [The preset in use], [1 of the 30 slots],
    [Lifetime], [Lost at power-off], [Kept until it is written again],
    [Address], [`0x0000` to `0x0C07`], [The preset number in the MIDI frame],
  ),
  caption: [The 2 views of a preset],
)

The rule of thumb is simple: use the edit buffer to *edit*, because every field is 1 byte at a known address, and use
the user preset to *store*, because it is what survives a power cycle and what a backup keeps.

#caveat[
  The `BPRESET` command writes raw bytes inside a user preset, without checking that the result forms a valid record. A tool
  writes a user preset with the `WPRESET` command, which carries the whole record.
]

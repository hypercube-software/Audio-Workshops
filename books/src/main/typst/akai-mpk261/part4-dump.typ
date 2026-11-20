#import "../common/settings.typ": *
#import "lib.typ": *

#part("Preset transfers")

#chapter("The preset dump", l: "chap-dump")

`DPRESET` moves a whole preset in 1 message, and it is the only command that reaches the 30 user presets: the
preset memory of #cref(<chap-preset>) is the edit buffer. A preset dump is what a librarian saves, what a backup
restores, and what the front panel sends when a preset is exported.

== The MIDI frame

A dump is 1555 bytes: the usual header, the size `0C 0B`, the number and the name of the preset, 1538 bytes of data,
and `F7`.

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

== Number and name

The preset number at `0x007` is the number displayed by the unit, from 1 to 30. The byte at `0x0001` of the global
memory counts from zero: the reference preset is displayed as 8, carried as `08` in its dump and as `07` in the global
memory.

#caveat[
  A tool that copies the byte of one place to the other is off by one. The 2 fields have different origins, and
  neither byte carries its convention.
]

The name at `0x008` is 8 ASCII characters, padded on the right with spaces. It is the name of the user preset, so a dump
carries the name twice: once here, once in the data, at `0x0008` of the edit buffer — the 2 are equal in a dump
produced by the unit, and a tool that renames a preset before sending it back changes both.

== Records of the dump

The data of a dump holds the same controls as the preset memory, in another shape: the preset memory is a structure
of arrays, the dump an array of structures. Each control has 1 record, and the fields of that record follow each other.

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

2 records change the order of the fields. In the preset memory, the MIDI to DIN flag of a knob comes after its
value; in a dump, it comes before the MSB. In a pad record the 2 colours are the last 2 bytes, at offsets 9 and 10,
where the memory keeps them in 2 arrays of 64 bytes.

#caveat[
  The position of the MIDI to DIN flag inside a pad record is not established: the values observed at offset 3 of the
  record are compatible with that flag, and with the momentary/toggle mode as well. A tool that must change a pad
  writes the preset memory with `WPRESET`, which is fully mapped, and lets the unit produce the dump; a tool
  that only archives presets keeps the records as they are.
]

Record `i` of a block starts at `block + i * size`, where `block` and `size` come from the table above. Pad 1 of bank A
is the first record, at `0x03D`; pad 16 of bank D is the last, at `0x2F2`. Knob 1 follows the pads at `0x2FD`, and the
first fader at `0x3D5`.

== Keyboard and split

The keyboard block of a dump holds the same content as `0x0016` to `0x001D` of the preset memory, in a slightly
different order: channel, octave, aftertouch, a flag, MIDI to DIN, transpose, then the transport type.

#figure(
  ptable(
    columns: (auto, auto, auto, 1fr),
    head([*Offset*], [*Field*], [*Ref.*], [*Meaning*]),
    [`0x01D`], [Channel], [`00`], [Same encoding as the preset memory],
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
switch, the split note and the B channel. It carries the same information as `0x001D` to `0x0026` of the preset
memory, in the order of the front panel rather than in the order of the memory.

== Receiving and sending

The unit produces a dump from its SysEx menu, which offers to send 1 preset or all 30. 2 points matter when a tool
reads them:

- A dump of *all* presets is a stream of 30 MIDI frames, each one complete, with its own size field.
- A dump is far longer than the 127 bytes a single read or a single write carries, because the first byte of its
  payload is a preset number: the size field alone gives the length of the MIDI frame. A parser that reads the byte
  after the size as a count misreads every dump.

Sending is the reverse: 1 MIDI frame per preset, addressed by its number. `DPRESET` has no acknowledgement, so a write
is verified by reading the preset back — the unit sends it from its SysEx menu — and comparing the 1547-byte payload.
The comparison covers every byte: the unidentified bytes are part of the preset, and a dump that drops them loses part
of the state of the unit.

#figure(
  ptable(
    columns: (auto, auto, auto),
    head([*Property*], [*Preset memory*], [*Preset dump*]),
    [Message], [`QPRESET`], [`DPRESET`],
    [Size], [1544 bytes], [1547-byte payload, 1555-byte MIDI frame],
    [Layout], [Structure of arrays], [Array of structures],
    [Scope], [The preset in the edit buffer], [Any of the 30 user presets],
    [Address], [2 bytes, `0x0000` to `0x0C07`], [The user preset number in the MIDI frame],
    [Splitting], [127 data bytes per message], [1 MIDI frame, whatever its size],
  ),
  caption: [The 2 views of a preset],
)

The rule of thumb is simple: use the preset memory to *edit*, because every field is 1 byte at a known address, and
use the dump to *store*, because it is the only way to write a user preset and the only object a backup can keep.

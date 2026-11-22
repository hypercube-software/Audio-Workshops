#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Pads", l: "chap-pads")

The pads are the largest block of the edit buffer: 64 records of 11 arrays, at addresses `0x063C` to `0x0B7B`. The
unit has 16 physical pads, and each one has 4 settings, 1 per bank, so a preset always describes 64 pads. Pad 1 of bank
A is the first record, pad 16 of bank A the 16th, and pad 16 of bank D the 64th.

#figure(
  ptable(
    columns: (auto, auto, auto),
    head([*Bank*], [*Pads*], [*Record*]),
    [`A`], [1 to 16], [`0`..`15`],
    [`B`], [1 to 16], [`16`..`31`],
    [`C`], [1 to 16], [`32`..`47`],
    [`D`], [1 to 16], [`48`..`63`],
  ),
  caption: [The 4 banks of pads],
)

The active bank is a live state of the global settings, at `0x0018` (see #cref(<chap-global>)). A preset that is played
on bank B still contains the settings of the 4 banks, and an editor writes them all.

== Fields

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Array*], [*Packed address*], [*Size*], [*Stored*], [*Ref.*], [*Meaning*]),
    [Type], [`06 3C`], [64], [`0`..`2`], [`02`], [Note, program change, program bank],
    [Channel], [`06 7C`], [64], [`0`..`32`], [`0A`], [Same encoding as the keyboard channel],
    [Note], [`07 3C`], [64], [`0`..`127`], [`2F`], [Note number when the type is a note; `2F` is B2],
    [Mode], [`07 7C`], [64], [`0` `1`], [`00`], [Momentary, toggle],
    [Aftertouch], [`08 3C`], [64], [`0`..`2`], [`01`], [Off, channel, polyphonic],
    [Program], [`08 7C`], [64], [`0`..`127`], [`50`], [Program number when the type is a program change or a
      program bank],
    [MSB], [`09 3C`], [64], [`0`..`127`], [`1E`], [Most significant byte of the bank],
    [LSB], [`09 7C`], [64], [`0`..`127`], [`28`], [Least significant byte of the bank],
    [MIDI to DIN], [`0A 3C`], [64], [`0` `1`], [`00`], [Whether the pad is copied to the DIN output],
    [Off colour], [`0A 7C`], [64], [`0`..`16`], [`10`], [Colour of the pad at rest],
    [On colour], [`0B 3C`], [64], [`0`..`16`], [`08`], [Colour of the pad when it is hit],
  ),
  caption: [Fields of the pads],
)

The `Ref.` column is the first pad of the reference preset, a program bank pad. On a pad of type note, the note number
sits in the *Note* array, and the program and bank arrays stay at zero.

== Type of a pad

A pad is a small MIDI generator: its type decides which of the 4 other fields are meaningful.

#figure(
  ptable(
    columns: (auto, auto, 1fr),
    head([*Stored*], [*Type*], [*Message sent*]),
    [`0`], [Note], [Note on and note off, with the note number, the channel and the aftertouch of the record],
    [`1`], [Program change], [Program change with the program number, on the channel of the record],
    [`2`], [Program bank], [Bank select MSB, bank select LSB, then program change],
  ),
  caption: [The 3 types of pad],
)

A program bank pad is the only one that uses the 3 bytes `0x087C`, `0x093C` and `0x097C` at once. The order on the
wire is the one of the MIDI specification: the most significant byte of the bank first, then the least significant,
then the program number.

== Aftertouch

The aftertouch field of a pad applies to the note pads only:

#figure(
  ptable(
    columns: (auto, auto, 1fr),
    head([*Stored*], [*Mode*], [*Behaviour*]),
    [`0`], [Off], [The pressure on the pad is ignored],
    [`1`], [Channel], [The pressure is sent as channel aftertouch for the channel of the pad],
    [`2`], [Polyphonic], [The pressure is sent as polyphonic aftertouch, with the note of the pad],
  ),
  caption: [The 3 aftertouch modes of a pad],
)

The keyboard has its own aftertouch switch, at `0x0018` of the edit buffer. Each has its own switch, and a preset
can send aftertouch from the pads, from the keys, or from both.

== Colours

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, auto),
    head([*Value*], [*Colour*], [*Value*], [*Colour*], [*Value*], [*Colour*]),
    [`00`], [Off], [`06`], [Green-blue], [`0C`], [Hot pink],
    [`01`], [Red], [`07`], [Aqua], [`0D`], [Light purple],
    [`02`], [Orange], [`08`], [Light blue], [`0E`], [Light green],
    [`03`], [Amber], [`09`], [Blue], [`0F`], [Light pink],
    [`04`], [Yellow], [`0A`], [Purple], [`10`], [Grey],
    [`05`], [Green], [`0B`], [Pink], [—], [—],
  ),
  caption: [The 17 colours of a pad],
)

The reference preset uses grey (`10`) for the rest state of all 4 banks and light blue (`08`) for the hit state of
bank A, which is why the first 16 values of the hit colour array differ from the following ones.

A colour of `00` turns the pad off: the pad still sends its message when it is hit.

== Writing 1 colour

Changing the hit colour of the first pad of bank A is a write of 1 byte at address `0x0B3C`:

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
    bytes(2, fill: l_green)[0B 3C],
    bytes(1, fill: l_blue)[03],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [Changing the colour of a pad],
)

The pad lights in amber as soon as the `ACKE` command arrives. The first pad of bank B is at `0x0B4C`, 16 bytes
further: the 16 pads of a bank are contiguous, and a whole bank of colours is a single write of 16 bytes.

#import "../common/settings.typ": *
#import "lib.typ": *

#part("Edit buffer")

#chapter("The preset block", l: "chap-preset")

The edit buffer holds the preset that the unit uses: 1544 bytes, at addresses `0x0000` to `0x0C07`, read with
the `QEDIT` command and written with the `WEDIT` command. A write changes the settings at once, and the unit loses the content at power-off.

The 30 user presets are separate objects, kept when the unit is off: the `QPRESET` command reads 1 of them and the `WPRESET` command writes 1
of them (#cref(<chap-dump>)). Loading a user preset copies it into the edit buffer.

The chapters of this part describe the edit buffer. #cref(<chap-dump>) describes the user preset, which stores the
same settings in another layout.

== Layout

#figure(
  ptable(
    columns: (auto, auto, auto, auto, 1fr),
    head([*Packed address*], [*Packed size*], [*Size*], [*Content*], [*Chapter*]),
    [`00 00`], [`00 08`], [8], [Name of the preset], [This chapter],
    [`00 08`], [`00 02`], [2], [Tempo, 14-bit packed], [This chapter],
    [`00 0A`], [`00 0C`], [12], [Transport and arpeggiator], [This chapter],
    [`00 16`], [`00 12`], [18], [Keyboard, split and zones], [#cref(<chap-keyboard>)],
    [`00 28`], [`00 23`], [35], [Wheels, expression pedal and footswitches], [#cref(<chap-keyboard>)],
    [`00 4B`], [`00 10`], [16], [Arpeggiator pattern, 1 byte per step], [This chapter],
    [`00 5B`], [`00 41`], [65], [DAW buttons, 5 records], [#cref(<chap-controls>)],
    [`01 1C`], [`02 38`], [312], [Switches, 24 records], [#cref(<chap-controls>)],
    [`03 54`], [`01 10`], [144], [Faders, 24 records], [#cref(<chap-controls>)],
    [`04 64`], [`01 58`], [216], [Knobs, 24 records], [#cref(<chap-controls>)],
    [`06 3C`], [`05 40`], [704], [Pads, 64 records], [#cref(<chap-pads>)],
    [`0B 7C`], [`00 0C`], [12], [Unidentified, identical in every preset examined], [],
  ),
  caption: [Layout of the 1544-byte preset block],
)

The 5 control blocks at the end are *structures of arrays*: each field forms a flat array, which is the subject of
#cref(<chap-controls>). The header, the keyboard block and the arpeggiator pattern are plain fields, in the order of the
table above.

== The name

The name is 8 ASCII characters at address `0x0000`, padded with spaces, without a terminator. The reference preset
occupies the 8 bytes with `StudOne5`:

#figure(
  bytefield(
    bpr: 64,
    bytes(1, fill: l_green)[53 \ S],
    bytes(1, fill: l_green)[74 \ t],
    bytes(1, fill: l_green)[75 \ u],
    bytes(1, fill: l_green)[64 \ d],
    bytes(1, fill: l_green)[4F \ O],
    bytes(1, fill: l_green)[6E \ n],
    bytes(1, fill: l_green)[65 \ e],
    bytes(1, fill: l_green)[35 \ 5],
  ),
  caption: [The name of the reference preset],
)

A shorter name is padded on the right with spaces: `TWO` travels with 5 spaces after its 3 letters. A parser reads the
8 bytes as they are and trims the trailing spaces. A zero byte is a valid character in the name for the unit, and most
editors show a name that starts with one as empty.

The name belongs to the preset in the edit buffer: a tool that renames the preset that is playing writes it at
`0x0000`. The user preset keeps its own name until the edit buffer is written to it (#cref(<chap-dump>)).

== Tempo, transport and arpeggiator

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Packed address*], [*Packed size*], [*Field*], [*Stored*], [*Ref.*], [*Meaning*]),
    [`00 08`], [`00 02`], [Tempo], [2 bytes], [`00 64`], [Beats per minute, 14-bit packed: `00 64` is 100 BPM],
    [`00 0A`], [`00 01`], [flag], [`01`], [`01`], [Constant in the presets examined],
    [`00 0B`], [`00 01`], [flag], [`04`], [`04`], [Constant in the presets examined],
    [`00 0C`], [`00 01`], [flag], [`00`], [`00`], [Constant in the presets examined],
    [`00 0D`], [`00 01`], [Time division], [`0`..`7`], [`04`], [Shared by the arpeggiator and the note repeat: `00` is
      `1/4`, `01` `1/4T`, `02` `1/8`, `03` `1/8T`, `04` `1/16`, `05` `1/16T`, `06` `1/32`, `07` `1/32T`],
    [`00 0E`], [`00 01`], [Note repeat mode], [`0` `1`], [`01`], [Momentary, toggle],
    [`00 0F`], [`00 01`], [Gate], [`0`..`99`], [`19`], [Duration of the notes produced by the note repeat and the
      arpeggiator, in percent of the step: `19` is 25 %],
    [`00 10`], [`00 01`], [Swing], [`50`..`75`], [`32`], [Delay of the even steps, in percent: `32` is 50 %, that is no
      swing],
    [`00 11`], [`00 01`], [flag], [`01`], [`01`], [Constant in the presets examined],
    [`00 12`], [`00 01`], [Arpeggiator type], [`0`..`10`], [`04`], [`00` is *Up* and `0A` is *Chord*; the values in
      between are the other types, in the order of the Edit menu],
    [`00 13`], [`00 01`], [Arpeggiator range], [`0`..`3`], [`02`], [Octaves added to the notes played],
    [`00 14`], [`00 01`], [Arpeggiator pattern], [`0` `1`], [`00`], [Off, on: the pattern of `0x004B` is used when on],
    [`00 15`], [`00 01`], [Arpeggiator length], [`1`..`16`], [`10`], [Number of steps of the pattern that are played],
  ),
  caption: [Tempo, transport and arpeggiator of the reference preset],
)

The 4 bytes that the table marks as flags (`0x000A`, `0x000B`, `0x000C` and `0x0011`) are part of the header; the front
panel has no setting for them, and they keep the same value in every preset examined. A tool writes them back
unchanged.

== The arpeggiator pattern

16 bytes follow, 1 per step, at addresses `0x004B` to `0x005A`. The value `01` enables the step and `00` disables
it:

#figure(
  bytefield(
    bpr: 128,
    bitheader(
      marker: false,
      angle: 0deg,
      numbers: none,
      0, [1],
      8, [2],
      16, [3],
      24, [4],
      32, [5],
      40, [6],
      48, [7],
      56, [8],
      64, [9],
      72, [10],
      80, [11],
      88, [12],
      96, [13],
      104, [14],
      112, [15],
      120, [16],
    ),
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_green)[01],
  ),
  caption: [The pattern of the reference preset],
)

All 16 steps are active in the reference preset. Each step is a switch, and the notes played are the ones held on the
keyboard. The length at `0x0015` decides how many of the 16 steps are read, so a pattern of 4 steps is a pattern of 16
whose last 12 bytes are `00`.

== The header at a glance

#figure(
  bytefield(
    bpr: 112,
    bytes(2, fill: l_orange)[tempo 00 64],
    bytes(3, fill: l_gray)[01 04 00],
    bytes(1, fill: l_green)[04],
    bytes(1, fill: l_blue)[01],
    bytes(2, fill: l_purple)[19 32],
    bytes(1, fill: l_gray)[01],
    bytes(1, fill: l_green)[04],
    bytes(1, fill: l_blue)[02],
    bytes(2, fill: l_purple)[00 10],
  ),
  caption: [The header of a preset],
)

The colours group the fields: tempo (orange), flags (grey), time division and arpeggiator type (green), note repeat
mode and arpeggiator range (blue), gate, swing, arpeggiator pattern and length (purple).

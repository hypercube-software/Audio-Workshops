#import "../common/settings.typ": *
#import "lib.typ": *

#let theme-colours = (
  keyboard: blue.lighten(58%),
  split: aqua.lighten(65%),
  pitchbend: teal.lighten(52%),
  modulation: green.lighten(55%),
  expression: orange.lighten(55%),
  footswitch1: yellow.lighten(52%),
  footswitch2: red.lighten(75%),
  arpeggiator: fuchsia.lighten(55%),
  blocks: gray.lighten(60%),
)

#let theme-items = (
  (theme-colours.keyboard, [Keyboard]),
  (theme-colours.split, [Split]),
  (theme-colours.pitchbend, [Pitch bend]),
  (theme-colours.modulation, [Modulation]),
  (theme-colours.expression, [Expression]),
  (theme-colours.footswitch1, [Footswitch 1]),
  (theme-colours.footswitch2, [Footswitch 2]),
  (theme-colours.arpeggiator, [Arpeggiator]),
  (theme-colours.blocks, [Control blocks]),
)

#let theme-legend = block(
  width: 100%,
  above: 6pt,
  below: 3pt,
  grid(
    columns: (1fr, 1fr, 1fr, 1fr),
    column-gutter: 8pt,
    row-gutter: 4pt,
    ..theme-items.map(item => box[
      #box(fill: item.at(0), width: 7pt, height: 7pt, radius: 1pt)
      #text(size: 8pt, item.at(1))
    ]),
  ),
)

#part("The preset")

#chapter("Memory layout", l: "chap-preset")

A preset gathers everything that a performance uses: the settings of the keyboard and its split, of the wheels and the
pedals, of the 64 pads, and of the 24 switches, 24 faders and 24 knobs. The unit keeps it in 2 places, in *2 memory layouts*. You will discover how crazy the edit buffer memory layout is.
Buckle up !

== Overview

- The *edit buffer* is a RAM block of 1544 bytes, at addresses `0x0000` to `0x0C07`. A write changes the settings at
    once, and the unit clears the block at power-off;
- A *user preset* is 1 of the 30 presets of the flash memory. It survives a power cycle, and a backup keeps it.

#figure(
  ptable(
    columns: (auto, auto, auto),
    head([*Property*], [*Edit buffer*], [*User preset*]),
    [Command], [`QEDIT`, `WEDIT`], [`QPRESET`, `WPRESET`],
    [Size], [1544 bytes], [1546-byte record, 1547-byte payload],
    [Controls memory layout], [Structure of arrays], [Array of structures],
    [Scope], [The preset in use], [1 of the 30 presets],
    [Lifetime], [Lost at power-off], [Kept until it is written again],
    [Address], [`0x0000` to `0x0C07`], [`0x0000` to `0x0C09`],
  ),
  caption: [The 2 memory layouts of a preset],
)

Loading a user preset reads it into the edit buffer, and storing the edit buffer writes it into the preset. The rule of
thumb is simple: use the edit buffer to *edit*, because every field is 1 byte at a known address, and use the user preset
to *store*.

== Controls addressing

A parameter of a control has 1 value per control, and 2 addresses:

- The *array address* lies in the *edit buffer*. A control type has 1 array per field, and each array holds 1 value
    per control: 24 for the switches, 64 for the pads. The value of control `i` sits at the array address plus `i`.
    This memory layout is *total madness*!
- The *field address* lies in a *user preset*. The value sits at a fixed offset in each control's record, so the
    value of control `i` sits at the field address plus `i` times the record's size. This memory layout is a classic.

The 2 memory layouts do not order the fields the same way: a *user preset* holds every field of a control, the *edit
buffer* every control of a field, and some fields change position from one memory layout to the other. That is why the tables of
the controls give both addresses.

For the knobs, the type is at `0x0464` in the edit buffer and at `0x0575` in a user preset: the 24 values of the 24
knobs follow the first address, and the records are 9 bytes long.

== Current preset

The edit buffer holds 1544 bytes. Only the controls form arrays: each field of a control type is a flat array that holds
1 value per control, and the arrays of a block follow each other. The other settings are plain fields, listed 1 by 1
below. #cref(<chap-controls>) describes the control blocks.

// 1 colour key per body row, in the order of the table below. `none` leaves the row white.
#let theme-rows-buffer = (
  "none", "none", "arpeggiator", "arpeggiator", "arpeggiator", "arpeggiator", "arpeggiator", "arpeggiator", "arpeggiator", "arpeggiator",
  "arpeggiator", "arpeggiator", "arpeggiator", "arpeggiator", "keyboard", "keyboard", "keyboard", "none", "keyboard", "blocks",
  "keyboard", "split", "split", "split", "split", "split", "split", "split", "split", "split",
  "split", "none", "modulation", "modulation", "modulation", "modulation", "none", "none", "pitchbend", "pitchbend",
  "none", "expression", "expression", "expression", "expression", "none", "expression", "footswitch1", "footswitch2", "footswitch1",
  "footswitch2", "footswitch1", "footswitch2", "footswitch1", "footswitch2", "none", "footswitch1", "footswitch2", "arpeggiator", "blocks",
  "blocks", "blocks", "blocks", "blocks", "none",
)

#theme-legend

#figure(
  ptable(
    fill: (x, y) => if y == 0 { luma(230) } else { theme-colours.at(theme-rows-buffer.at(y - 1), default: none) },
    columns: (2.2cm, 1.2cm, 1.4cm, 1.6cm, 3.2cm, 1.3cm, 1fr),
    head([*Pack. Addr.*], [*Record size*], [*Arrays*], [*Array length*], [*Record name*], [*Range*], [*Description*]),
    [`00 00`], [8], [—], [—], [Name], [—], [ASCII, padded on the right with spaces, without a terminator],
    [`00 08`], [2], [—], [—], [Tempo], [—], [Beats per minute, 14-bit packed: `00 64` is 100 BPM],
    [`00 0A`], [1], [—], [—], [flag], [`01`], [Unknown constant],
    [`00 0B`], [1], [—], [—], [flag], [`04`], [Unknown constant],
    [`00 0C`], [1], [—], [—], [flag], [`00`], [Unknown constant],
    [`00 0D`], [1], [—], [—], [Time division], [`00`..`07`], [Step of the arpeggiator and of the note repeat,
      see @time-division],
    [`00 0E`], [1], [—], [—], [Note repeat mode], [`0`, `1`], [Momentary, toggle],
    [`00 0F`], [1], [—], [—], [Gate], [`0`..`99`], [Duration of the notes produced by the note repeat and the arpeggiator, in percent of
      the step: `19` is 25 %],
    [`00 10`], [1], [—], [—], [Swing], [`50`..`75`], [Delay of the even steps, in percent: `32` is 50 %, that is no swing],
    [`00 11`], [1], [—], [—], [flag], [`01`], [Unknown constant],
    [`00 12`], [1], [—], [—], [Arpeggiator: type], [`00`..`0A`], [`00` is *Up* and `0A` is *Chord*; the values in between are the other
      types, in the order of the Edit menu],
    [`00 13`], [1], [—], [—], [Arpeggiator: range], [`0`..`3`], [Octaves added to the notes played],
    [`00 14`], [1], [—], [—], [Arpeggiator: pattern], [`0`, `1`], [Off, on: the pattern of `0x004B` is used when on],
    [`00 15`], [1], [—], [—], [Arpeggiator: length], [`1`..`16`], [Number of steps of the pattern that are played],
    [`00 16`], [1], [—], [—], [Keyboard: channel], [`0`..`32`], [Channel of the notes],
    [`00 17`], [1], [—], [—], [Keyboard: octave], [`1`..`7`], [Octave shift of the keys, plus 4: `04` is no shift, `01` is 3 octaves down and
      `07` 3 octaves up],
    [`00 18`], [1], [—], [—], [Keyboard: aftertouch], [`0`..`2`], [Whether the unit sends channel aftertouch],
    [`00 19`], [1], [—], [—], [flag], [`01`], [Unknown constant],
    [`00 1A`], [1], [—], [—], [Keyboard: MIDI to DIN], [`0`, `1`], [Whether the notes are copied to the DIN output],
    [`00 1B`], [1], [—], [—], [Transport: type], [`0`..`4`], [Protocol of the transport buttons, see @transport-type],
    [`00 1C`], [1], [—], [—], [Keyboard: transpose], [`0`..`72`], [Transposition in semitones, plus 36: `24` is no transposition],
    [`00 1D`], [1], [—], [—], [Split: switch], [`0`, `1`], [Off, on],
    [`00 1E`], [1], [—], [—], [Split: note], [`0`..`60`], [Index of the key where the split sits, counted from the lowest key: `00` is
      the lowest key, `3C` the highest],
    [`00 1F`], [1], [—], [—], [Split: B channel], [`0`..`31`], [Channel of zone B, see @b-channel],
    [`00 20`], [1], [—], [—], [Split: pitch bend zone], [`0`..`3`], [Off, A, B, A and B],
    [`00 21`], [1], [—], [—], [Split: modulation zone], [`0`..`3`], [Off, A, B, A and B],
    [`00 22`], [1], [—], [—], [Split: footswitch 1 zone], [`0`..`3`], [Off, A, B, A and B],
    [`00 23`], [1], [—], [—], [Split: footswitch 2 zone], [`0`..`3`], [Off, A, B, A and B],
    [`00 24`], [1], [—], [—], [Split: expression zone], [`0`..`3`], [Off, A, B, A and B],
    [`00 25`], [1], [—], [—], [Split: arpeggiator zone], [`0`..`3`], [Off, A, B, A and B],
    [`00 26`], [1], [—], [—], [Split: aftertouch zone], [`0`..`3`], [Off, A, B, A and B],
    [`00 27`], [1], [—], [—], [Unknown], [`00`], [Unknown constant],
    [`00 28`], [1], [—], [—], [Modulation: channel], [`0`..`32`], [Channel of the modulation wheel],
    [`00 29`], [1], [—], [—], [Modulation: controller], [`0`..`127`], [MIDI CC number sent by the wheel, 1 by default],
    [`00 2A`], [1], [—], [—], [Modulation: minimum], [`0`..`127`], [Value sent at the bottom of the wheel],
    [`00 2B`], [1], [—], [—], [Modulation: maximum], [`0`..`127`], [Value sent at the top of the wheel],
    [`00 2C`], [2], [—], [—], [Unknown], [—], [Probably the coarse and fine resolution of the wheel, or its copy to the DIN
      output],
    [`00 2E`], [2], [—], [—], [Unknown], [—], [Unknown],
    [`00 30`], [1], [—], [—], [Pitch bend: channel], [`0`..`32`], [Channel of the pitch bend],
    [`00 31`], [1], [—], [—], [Pitch bend: to DIN], [`0`, `1`], [Whether the bend is copied to the DIN output],
    [`00 32`], [6], [—], [—], [Unknown], [—], [Unknown],
    [`00 38`], [1], [—], [—], [Expression: channel], [`0`..`32`], [Channel of the expression pedal],
    [`00 39`], [1], [—], [—], [Expression: controller], [`0`..`127`], [MIDI CC number sent by the pedal],
    [`00 3A`], [1], [—], [—], [Expression: minimum], [`0`..`127`], [Value sent when the pedal is released],
    [`00 3B`], [1], [—], [—], [Expression: maximum], [`0`..`127`], [Value sent when the pedal is pressed],
    [`00 3C`], [2], [—], [—], [Unknown], [—], [Unknown constant],
    [`00 3E`], [1], [—], [—], [Expression: to DIN], [`0`, `1`], [Whether the pedal is copied to the DIN output],
    [`00 3F`], [1], [—], [—], [Footswitch 1: type], [`0`..`7`], [Function of the switch, see @pedals],
    [`00 40`], [1], [—], [—], [Footswitch 2: type], [`0`..`7`], [Function of the switch, see @pedals],
    [`00 41`], [1], [—], [—], [Footswitch 1: channel], [`0`..`32`], [Channel of the switch],
    [`00 42`], [1], [—], [—], [Footswitch 2: channel], [`0`..`32`], [Channel of the switch],
    [`00 43`], [1], [—], [—], [Footswitch 1: controller], [`0`..`127`], [CC number, used when the type is a controller],
    [`00 44`], [1], [—], [—], [Footswitch 2: controller], [`0`..`127`], [CC number, used when the type is a controller],
    [`00 45`], [1], [—], [—], [Footswitch 1: mode], [`0`, `1`], [Momentary, toggle],
    [`00 46`], [1], [—], [—], [Footswitch 2: mode], [`0`, `1`], [Momentary, toggle],
    [`00 47`], [2], [—], [—], [Unknown], [—], [Unknown constant],
    [`00 49`], [1], [—], [—], [Footswitch 1: to DIN], [`0`, `1`], [Whether the switch is copied to the DIN output],
    [`00 4A`], [1], [—], [—], [Footswitch 2: to DIN], [`0`, `1`], [Whether the switch is copied to the DIN output],
    [`00 4B`], [16], [—], [16], [Arpeggiator: steps], [`0`, `1`], [1 byte per step, see @pattern],
    [`00 5B`], [65], [13], [5], [DAW buttons], [—], [5 buttons, 1 array per field],
    [`01 1C`], [312], [13], [24], [Switches], [—], [24 switches, 1 array per field],
    [`03 54`], [144], [6], [24], [Faders], [—], [24 faders, 1 array per field],
    [`04 64`], [216], [9], [24], [Knobs], [—], [24 knobs, 1 array per field],
    [`06 3C`], [704], [11], [64], [Pads], [—], [64 pads, 1 array per field],
    [`0B 7C`], [12], [—], [—], [Unknown], [—], [Unknown constant],
  ),
  caption: [The 1544 bytes of the edit buffer],
)

The *Channel* fields share 1 encoding, described in @channel-encoding.

The *name* is 8 ASCII characters, padded on the right with spaces, without a terminator. A shorter name travels with
spaces after its letters, and a parser trims them. A zero byte is a valid character for the unit, and most editors show
a name that starts with one as empty.

The name belongs to the preset in the edit buffer: renaming the current preset writes it at `0x0000`. The user preset
keeps its own name until the edit buffer is written to it.

The 4 bytes that the table marks as flags (`0x000A`, `0x000B`, `0x000C` and `0x0011`) have no setting on the front
panel, and they keep the same value in every preset examined. A write sends them back unchanged.

The bytes that the table marks as `Unknown` are part of the block: a write sends them back as they are.

== User presets

A user preset holds 1546 bytes: the 8 bytes of the name and 1538 bytes of data. The preset number designates the preset
in the MIDI frame, and is not part of the record. The record holds the same settings as the edit buffer, in another
order: the control blocks are transposed, and the fields of the other settings follow each other.

// 1 colour key per body row, in the order of the table below. `none` leaves the row white.
#let theme-rows-record = (
  "none", "none", "none", "arpeggiator", "arpeggiator", "arpeggiator", "arpeggiator", "arpeggiator", "none", "keyboard",
  "keyboard", "keyboard", "none", "keyboard", "keyboard", "blocks", "modulation", "modulation", "modulation", "modulation",
  "none", "pitchbend", "none", "none", "expression", "expression", "expression", "expression", "expression", "footswitch1",
  "footswitch1", "footswitch1", "footswitch1", "none", "footswitch1", "footswitch2", "footswitch2", "footswitch2", "footswitch2", "none",
  "footswitch2", "blocks", "blocks", "blocks", "blocks", "blocks", "arpeggiator", "arpeggiator", "arpeggiator", "arpeggiator",
  "arpeggiator", "arpeggiator", "arpeggiator", "arpeggiator", "split", "split", "split", "split", "split", "none",
)

#theme-legend

#figure(
  ptable(
    fill: (x, y) => if y == 0 { luma(230) } else { theme-colours.at(theme-rows-record.at(y - 1), default: none) },
    columns: (2.2cm, 1.2cm, 1.6cm, 3.4cm, 1.3cm, 1fr),
    head([*Pack. Addr.*], [*Record size*], [*Array length*], [*Record name*], [*Range*], [*Description*]),
    [`00 00`], [8], [—], [Name], [—], [ASCII, padded on the right with spaces, without a terminator],
    [`00 08`], [1], [—], [flag], [`00`], [Unknown constant],
    [`00 09`], [2], [—], [Tempo], [—], [Beats per minute, 14-bit packed],
    [`00 0B`], [1], [—], [flag], [`01`], [Unknown constant],
    [`00 0C`], [1], [—], [Time division], [`00`..`07`], [Step of the arpeggiator and of the note repeat,
      see @time-division],
    [`00 0D`], [1], [—], [Note repeat mode], [`0`, `1`], [Momentary, toggle],
    [`00 0E`], [1], [—], [Gate], [`0`..`99`], [Duration of the notes produced by the note repeat and the
      arpeggiator, in percent of the step],
    [`00 0F`], [1], [—], [Swing], [`50`..`75`], [Delay of the even steps, in percent: `32` is 50 %, that is no
      swing],
    [`00 10`], [5], [—], [Unknown], [`00`], [Unknown constant],
    [`00 15`], [1], [—], [Keyboard: channel], [`0`..`32`], [Channel of the notes],
    [`00 16`], [1], [—], [Keyboard: octave], [`1`..`7`], [Octave shift of the keys, plus 4],
    [`00 17`], [1], [—], [Keyboard: aftertouch], [`0`..`2`], [Whether the unit sends channel aftertouch],
    [`00 18`], [1], [—], [flag], [`01`], [Unknown constant],
    [`00 19`], [1], [—], [Keyboard: MIDI to DIN], [`0`, `1`], [Whether the notes are copied to the DIN output],
    [`00 1A`], [1], [—], [Keyboard: transpose], [`0`..`72`], [Transposition in semitones, plus 36],
    [`00 1B`], [1], [—], [Transport: type], [`0`..`4`], [Protocol of the transport buttons, see @transport-type],
    [`00 1C`], [1], [—], [Modulation: channel], [`0`..`32`], [Channel of the modulation wheel],
    [`00 1D`], [1], [—], [Modulation: controller], [`0`..`127`], [MIDI CC number sent by the wheel],
    [`00 1E`], [1], [—], [Modulation: minimum], [`0`..`127`], [Value sent at the bottom of the wheel],
    [`00 1F`], [1], [—], [Modulation: maximum], [`0`..`127`], [Value sent at the top of the wheel],
    [`00 20`], [1], [—], [Unknown], [—], [Unknown],
    [`00 21`], [1], [—], [Pitch bend: channel], [`0`..`32`], [Channel of the pitch bend],
    [`00 22`], [1], [—], [Unknown], [—], [Unknown],
    [`00 23`], [1], [—], [Unknown], [`00`], [Unknown constant],
    [`00 24`], [1], [—], [Expression: channel], [`0`..`32`], [Channel of the expression pedal],
    [`00 25`], [1], [—], [Expression: controller], [`0`..`127`], [MIDI CC number sent by the pedal],
    [`00 26`], [1], [—], [Expression: minimum], [`0`..`127`], [Value sent when the pedal is released],
    [`00 27`], [1], [—], [Expression: maximum], [`0`..`127`], [Value sent when the pedal is pressed],
    [`00 28`], [1], [—], [Expression: to DIN], [`0`, `1`], [Whether the pedal is copied to the DIN output],
    [`00 29`], [1], [—], [Footswitch 1: type], [`0`..`7`], [Function of the switch, see @pedals],
    [`00 2A`], [1], [—], [Footswitch 1: channel], [`0`..`32`], [Channel of the switch],
    [`00 2B`], [1], [—], [Footswitch 1: controller], [`0`..`127`], [CC number, used when the type is a controller],
    [`00 2C`], [1], [—], [Footswitch 1: mode], [`0`, `1`], [Momentary, toggle],
    [`00 2D`], [1], [—], [Unknown], [—], [Unknown],
    [`00 2E`], [1], [—], [Footswitch 1: to DIN], [`0`, `1`], [Whether the switch is copied to the DIN output],
    [`00 2F`], [1], [—], [Footswitch 2: type], [`0`..`7`], [Function of the switch, see @pedals],
    [`00 30`], [1], [—], [Footswitch 2: channel], [`0`..`32`], [Channel of the switch],
    [`00 31`], [1], [—], [Footswitch 2: controller], [`0`..`127`], [CC number, used when the type is a controller],
    [`00 32`], [1], [—], [Footswitch 2: mode], [`0`, `1`], [Momentary, toggle],
    [`00 33`], [1], [—], [Unknown], [—], [Unknown],
    [`00 34`], [1], [—], [Footswitch 2: to DIN], [`0`, `1`], [Whether the switch is copied to the DIN output],
    [`00 35`], [704], [64], [Pads], [—], [64 pads of 11 fields],
    [`05 75`], [216], [24], [Knobs], [—], [24 knobs of 9 fields],
    [`07 4D`], [144], [24], [Faders], [—], [24 faders of 6 fields],
    [`08 5D`], [312], [24], [Switches], [—], [24 switches of 13 fields],
    [`0B 15`], [65], [5], [DAW buttons], [—], [5 buttons of 13 fields],
    [`0B 56`], [1], [—], [Arpeggiator: type], [`00`..`0A`], [Order in which the arpeggiator plays the held
      notes],
    [`0B 57`], [1], [—], [flag], [`01`], [Unknown constant],
    [`0B 58`], [1], [—], [Arpeggiator: range], [`0`..`3`], [Octaves added to the notes played],
    [`0B 59`], [1], [—], [Gate], [`0`..`99`], [Copy of the gate at `0x000E`],
    [`0B 5A`], [1], [—], [Swing], [`50`..`75`], [Copy of the swing at `0x000F`],
    [`0B 5B`], [1], [—], [Arpeggiator: pattern], [`0`, `1`], [Off, on: the pattern at `0x0B5D` is used when on],
    [`0B 5C`], [1], [—], [Arpeggiator: length], [`1`..`16`], [Number of steps of the pattern that are played],
    [`0B 5D`], [4], [—], [Arpeggiator: steps], [`00`..`0F`], [4 steps per byte, 1 bit each, `0F` is 4 active steps],
    [`0B 61`], [7], [—], [Split: zones A], [`0`, `1`], [1 byte per controller: pitch bend, modulation, footswitch 1, footswitch 2,
      expression, arpeggiator, aftertouch],
    [`0B 68`], [7], [—], [Split: zones B], [`0`, `1`], [The same 7 controllers],
    [`0B 6F`], [1], [—], [Split: switch], [`0`, `1`], [Off, on],
    [`0B 70`], [1], [—], [Split: note], [`0`..`60`], [Index of the key where the split sits, counted from the
      lowest key],
    [`0B 71`], [1], [—], [Split: B channel], [`0`..`31`], [Channel of zone B, see @b-channel],
    [`0B 72`], [24], [—], [Unknown], [—], [Unknown constant],
  ),
  caption: [The 1546 bytes of a user preset],
)

The addresses start at the record: the preset number, the command and the size of the MIDI frame are not part of it. They
are the addresses that `PPRESET` takes, so a read or a poke uses them as they are.

The regions are the same as those of the edit buffer, and the blocks keep their size: only the order changes inside
them. A block of knobs is 9 arrays of 24 values in the edit buffer, and 24 records of 9 bytes in a user preset.

Record `i` of a block starts at the block's address plus `i` times the record size. Pad 1 of bank A is the first
record, at `0x0035`; pad 16 of bank D is the last, at `0x056A`. Knob 1 follows the pads at `0x0575`, and the first fader
at `0x074D`.

The arpeggiator settings leave the head of the record: they sit at `0x0B56`, after the 5 control blocks, with the pattern
packed just after them and the split at `0x0B61`.

== Keyboard <keyboard>

The 61 keys send notes on the channel at `0x0016`, whose encoding is in @channel-encoding. 2 fields move the notes: the
octave at `0x0017`, from 3 octaves down to 3 octaves up, and the transpose at `0x001C`, from 36 semitones down to 36
semitones up.

The aftertouch switch at `0x0018` adds channel aftertouch, and the MIDI to DIN switch at `0x001A` copies the notes to the
DIN output.

The 2 wheels send their own messages:

- The *pitch bend* wheel sends its bend on the channel at `0x0030`, and the switch at `0x0031` copies it to the DIN
    output;
- The *modulation* wheel sends a controller on the channel at `0x0028`, with the controller number at `0x0029` and the
    range `0x002A` to `0x002B`.

The zone of each wheel is 1 byte of the matrix, at `0x0020` and `0x0021`; its channel is one of the record's fields. Moving a
wheel from one zone to the other changes the byte of the matrix, and the channel stays where it is.

== Split <split>

The split switch at `0x001D` cuts the keyboard in 2 zones: the keys up to the split note belong to zone A, the others to
zone B. The split note at `0x001E` is the index of the key where the cut sits, counted from the lowest key, and the B
channel at `0x001F` is the channel of zone B, with its own encoding (see @b-channel).

Beyond the notes, each controller goes to one zone, the other, or both, through the 7 zone bytes at `0x0020` to
`0x0026`: pitch bend, modulation, footswitch 1, footswitch 2, expression, arpeggiator, aftertouch. The encoding is a
pair of switches: `1` is zone A, `2` is zone B and `3` is both, which is how *Off* and *A and B* fit in 1 byte.

A preset that assigns everything to both zones behaves like a preset without a split, whatever the split switch says.
The record stores the same information as 2 groups of 7 bytes, 1 per controller and per zone.

== Pedals <pedals>

The *expression pedal* sends a controller on the channel at `0x0038`, with the controller number at `0x0039` and the range
`0x003A` to `0x003B`; the switch at `0x003E` copies it to the DIN output.

The 2 *footswitches* have the same fields, from `0x003F` to `0x004A`. The type of a *footswitch* decides which field of its
record is meaningful:

#kfigure(
  ptable(
    columns: (auto, auto, 1fr),
    head([*Stored*], [*Function*], [*Fields used*]),
    [`0`], [Controller], [Controller],
    [`1`], [Tap tempo], [—],
    [`2`], [Play], [—],
    [`3`], [Record], [—],
    [`4`], [Stop], [—],
    [`5`], [Play and stop], [—],
    [`6`], [Arpeggiator on and off], [—],
    [`7`], [Sustain], [—],
  ),
  caption: [The 8 types of a footswitch],
)

A *footswitch* of another type sends a MIDI message of its own, on the channel and with the MIDI to DIN setting of its
record: a *footswitch* set to *Sustain* sends CC 64 on the channel at `0x0041` or `0x0042`.

== Transport type <transport-type>

The *Transport: type* field at `0x001B` selects the messages that the 5 transport buttons send:

#kfigure(
  ptable(
    columns: (auto, auto),
    head([*Stored*], [*Transport type*]),
    [`00`], [MMC],
    [`01`], [MMC/MIDI Clock],
    [`02`], [MIDI Real Time],
    [`03`], [MIDI CC],
    [`04`], [Pro Tools Express (PTEX)],
  ),
  caption: [The 5 transport types],
)

== Time division <time-division>

The *Time division* field sets the step of the arpeggiator and of the note repeat:

#kfigure(
  ptable(
    columns: (auto, auto),
    head([*Stored*], [*Time division*]),
    [`00`], [`1/4`],
    [`01`], [`1/4T`],
    [`02`], [`1/8`],
    [`03`], [`1/8T`],
    [`04`], [`1/16`],
    [`05`], [`1/16T`],
    [`06`], [`1/32`],
    [`07`], [`1/32T`],
  ),
  caption: [The 8 time divisions],
)

== The arpeggiator pattern <pattern>

The pattern is 16 steps, each one on or off. The 2 memory layouts give a step a different amount of room: 1 byte in the
edit buffer, 1 bit in a user preset.

=== 1 byte per step

The 16 bytes at `0x004B` to `0x005A` of the *edit buffer* hold the steps in order. The value `01` enables the step and
`00` disables it:

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
  caption: [The pattern of the reference preset in the edit buffer],
)

All 16 steps are active in the reference preset. Each step is a switch, and the notes played are the ones held on the
keyboard. The length at `0x0015` decides how many of the 16 steps are read, so a pattern of 4 steps is a pattern of 16
whose last 12 bytes are `00`.

=== 1 bit per step

A *user preset* packs the same 16 steps in 4 bytes, at `0x0B5D` to `0x0B60`: 4 steps per byte, 1 bit each. The 4 low bits
of a byte hold its 4 steps, and the 4 high bits are `0`:

#figure(
  align(center, block(width: 9cm, bytefield(
    bpr: 8,
    numbers: none,
    note(left)[Steps 1 to 4],
    bits(1, fill: l_gray)[0], bits(1, fill: l_gray)[0], bits(1, fill: l_gray)[0], bits(1, fill: l_gray)[0],
    bits(1, fill: l_orange)[1], bits(1, fill: l_orange)[1], bits(1, fill: l_orange)[1], bits(1, fill: l_orange)[1],
    note(left)[Steps 5 to 8],
    bits(1, fill: l_gray)[0], bits(1, fill: l_gray)[0], bits(1, fill: l_gray)[0], bits(1, fill: l_gray)[0],
    bits(1, fill: l_orange)[1], bits(1, fill: l_orange)[1], bits(1, fill: l_orange)[1], bits(1, fill: l_orange)[1],
    note(left)[Steps 9 to 12],
    bits(1, fill: l_gray)[0], bits(1, fill: l_gray)[0], bits(1, fill: l_gray)[0], bits(1, fill: l_gray)[0],
    bits(1, fill: l_orange)[1], bits(1, fill: l_orange)[1], bits(1, fill: l_orange)[1], bits(1, fill: l_orange)[1],
    note(left)[Steps 13 to 16],
    bits(1, fill: l_gray)[0], bits(1, fill: l_gray)[0], bits(1, fill: l_gray)[0], bits(1, fill: l_gray)[0],
    bits(1, fill: l_orange)[1], bits(1, fill: l_orange)[1], bits(1, fill: l_orange)[1], bits(1, fill: l_orange)[1],
  ))),
  caption: [The pattern of the reference preset in a user preset],
)

Each byte of the reference preset is `0F`: the 4 low bits are on, and the 4 high bits are off. A step is the bit, and
the byte is only the grouping of 4 steps.

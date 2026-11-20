#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Keyboard, split and pedals", l: "chap-keyboard")

The 35 bytes that follow the header decide where the notes and the controllers go: the MIDI channel of the keyboard,
its transposition, the split of the keyboard in 2 zones, and the routing of the wheels and the pedals. These settings
belong to the preset: 2 presets of the same unit can drive 2 different instruments, each with its own split point.

== Keyboard

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Packed address*], [*Packed size*], [*Field*], [*Stored*], [*Ref.*], [*Meaning*]),
    [`00 16`], [`00 01`], [Channel], [`0`..`32`], [`00`], [Channel of the notes: `00` is *Common*, `01` to `10` are USB
      A1 to A16, `11` to `20` are USB B1 to B16],
    [`00 17`], [`00 01`], [Octave], [`1`..`7`], [`04`], [Octave shift of the keys, plus 4: `04` is no shift, `01` is 3
      octaves down and `07` 3 octaves up],
    [`00 18`], [`00 01`], [Aftertouch], [`0` `1`], [`01`], [Whether the unit sends channel aftertouch],
    [`00 19`], [`00 01`], [flag], [`01`], [`01`], [Constant in the presets examined],
    [`00 1A`], [`00 01`], [MIDI to DIN], [`0` `1`], [`00`], [Whether the notes are copied to the DIN output],
    [`00 1B`], [`00 01`], [Transport type], [`0`..`4`], [`03`], [Protocol of the transport buttons: `00` is MMC, `04`
      the Pro Tools variant, the other values follow the Edit menu],
    [`00 1C`], [`00 01`], [Transpose], [`0`..`72`], [`24`], [Transposition in semitones, plus 36: `24` is no
      transposition],
    [`00 1D`], [`00 01`], [Split], [`0` `1`], [`00`], [Off, on],
    [`00 1E`], [`00 01`], [Split note], [`0`..`60`], [`3B`], [Index of the key where the split sits, counted from the
      lowest key: `00` is the lowest key, `3C` the highest],
    [`00 1F`], [`00 01`], [B channel], [`0`..`31`], [`01`], [Channel of the high zone of the split],
  ),
  caption: [Keyboard settings of the reference preset],
)

The channel at `0x0016` and the B channel at `0x001F` share the same encoding; the second one counts from zero without
the *Common* entry: `00` is USB A1 and `1F` is USB B16. The *Common* channel is a setting of the global settings, at
`0x000A` (see #cref(<chap-global>)).

#figure(
  bytefield(
    bpr: 80,
    bytes(1, fill: l_green)[00],
    bytes(1, fill: l_green)[04],
    bytes(1, fill: l_green)[01],
    bytes(1, fill: l_gray)[01],
    bytes(1, fill: l_blue)[00],
    bytes(1, fill: l_blue)[03],
    bytes(1, fill: l_blue)[24],
    bytes(1, fill: l_purple)[00],
    bytes(1, fill: l_purple)[3B],
    bytes(1, fill: l_purple)[01],
  ),
  caption: [Keyboard and split of a preset],
)

The colours group the fields: channel, octave and aftertouch (green), flag (grey), MIDI to DIN, transport and
transpose (blue), split switch, split note and B channel (purple).

== The split

When the split is on, the keyboard is cut in 2 zones: the keys up to the split note belong to zone `A`, the others to
zone `B`. Beyond the notes, each controller goes to one zone, the other, or both, through 7 bytes at `0x0020` to
`0x0026`.

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Packed address*], [*Packed size*], [*Controller*], [*Stored*], [*Ref.*], [*Meaning*]),
    [`00 20`], [`00 01`], [Pitch bend], [`0`..`3`], [`03`], [Off, A, B, A and B],
    [`00 21`], [`00 01`], [Modulation wheel], [`0`..`3`], [`03`], [Off, A, B, A and B],
    [`00 22`], [`00 01`], [Footswitch 1], [`0`..`3`], [`03`], [Off, A, B, A and B],
    [`00 23`], [`00 01`], [Footswitch 2], [`0`..`3`], [`03`], [Off, A, B, A and B],
    [`00 24`], [`00 01`], [Expression pedal], [`0`..`3`], [`03`], [Off, A, B, A and B],
    [`00 25`], [`00 01`], [Arpeggiator], [`0`..`3`], [`03`], [Off, A, B, A and B],
    [`00 26`], [`00 01`], [Aftertouch], [`0`..`3`], [`03`], [Off, A, B, A and B],
    [`00 27`], [`00 01`], [unknown], [`00`], [`00`], [Unidentified],
  ),
  caption: [Assignment of the controllers to the 2 zones],
)

The encoding is a pair of switches: `1` is zone A, `2` is zone B and `3` is both, which is how *Off* and *A and B* fit
in 1 byte. A preset that assigns everything to both zones behaves like a preset without a split, whatever the split
switch says.

== Wheels

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Packed address*], [*Packed size*], [*Field*], [*Stored*], [*Ref.*], [*Meaning*]),
    [`00 28`], [`00 01`], [Modulation channel], [`0`..`32`], [`00`], [Same encoding as the keyboard channel],
    [`00 29`], [`00 01`], [Modulation controller], [`0`..`127`], [`01`], [MIDI CC number sent by the wheel, 1 by
      default],
    [`00 2A`], [`00 01`], [Modulation minimum], [`0`..`127`], [`00`], [Value sent at the bottom of the wheel],
    [`00 2B`], [`00 01`], [Modulation maximum], [`0`..`127`], [`7F`], [Value sent at the top of the wheel],
    [`00 2C`], [`00 02`], [unidentified], [—], [`7F 7F`], [Probably the coarse and fine resolution of the wheel, or its
      copy to the DIN output],
    [`00 30`], [`00 01`], [Pitch bend channel], [`0`..`32`], [`00`], [Same encoding as the keyboard channel],
    [`00 31`], [`00 01`], [Pitch bend to DIN], [`0` `1`], [`00`], [Whether the bend is copied to the DIN output],
    [`00 32`], [`00 06`], [unidentified], [—], [`00 00 00 00 7F 7F`], [—],
  ),
  caption: [Wheel settings of the reference preset],
)

A wheel has both a zone and a channel, stored apart: the zone is 1 byte of the matrix at `0x0021`, the channel is the
first field of the wheel record at `0x0028`. A tool that moves a controller from one zone to the other changes the byte
of the matrix; the channel of the record stays where it is.

A wheel always sends a continuous controller; its minimum and maximum bound the values it can produce.

== Pedals

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Packed address*], [*Packed size*], [*Field*], [*Stored*], [*Ref.*], [*Meaning*]),
    [`00 38`], [`00 01`], [Expression channel], [`0`..`32`], [`00`], [Same encoding as the keyboard channel],
    [`00 39`], [`00 01`], [Expression controller], [`0`..`127`], [`46`], [MIDI CC number sent by the pedal],
    [`00 3A`], [`00 01`], [Expression minimum], [`0`..`127`], [`14`], [Value sent when the pedal is released],
    [`00 3B`], [`00 01`], [Expression maximum], [`0`..`127`], [`7F`], [Value sent when the pedal is pressed],
    [`00 3C`], [`00 02`], [unidentified], [—], [`7F 7F`], [—],
    [`00 3E`], [`00 01`], [Expression to DIN], [`0` `1`], [`01`], [Whether the pedal is copied to the DIN output],
    [`00 3F`], [`00 01`], [Footswitch 1 type], [`0`..`7`], [`00`], [Function of the switch, see below],
    [`00 40`], [`00 01`], [Footswitch 2 type], [`0`..`7`], [`00`], [Function of the switch],
    [`00 41`], [`00 01`], [Footswitch 1 channel], [`0`..`32`], [`00`], [Channel of the switch],
    [`00 42`], [`00 01`], [Footswitch 2 channel], [`0`..`32`], [`00`], [Channel of the switch],
    [`00 43`], [`00 01`], [Footswitch 1 controller], [`0`..`127`], [`00`], [CC number, used when the type is a
      controller],
    [`00 44`], [`00 01`], [Footswitch 2 controller], [`0`..`127`], [`00`], [CC number],
    [`00 45`], [`00 01`], [Footswitch 1 mode], [`0` `1`], [`00`], [Momentary, toggle],
    [`00 46`], [`00 01`], [Footswitch 2 mode], [`0` `1`], [`00`], [Momentary, toggle],
    [`00 47`], [`00 02`], [unidentified], [—], [`00 00`], [—],
    [`00 49`], [`00 01`], [Footswitch 1 to DIN], [`0` `1`], [`00`], [Whether the switch is copied to the DIN output],
    [`00 4A`], [`00 01`], [Footswitch 2 to DIN], [`0` `1`], [`00`], [Whether the switch is copied to the DIN output],
  ),
  caption: [Pedal settings of the reference preset],
)

The type of a footswitch decides which field of the record is meaningful:

#figure(
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

A switch of another type sends a MIDI message of its own, on the channel and with the MIDI to DIN setting of its
record: a footswitch set to *Sustain* sends CC 64 on the channel at `0x0041` or `0x0042`.

== Undocumented bytes

The chapter leaves 8 bytes without a documented role: `0x002C` and `0x002D`, `0x0032` to `0x0037`, `0x003C` and
`0x003D`, and `0x0047` and `0x0048`. They keep the same values across the presets examined. They are listed here so
that a tool that rewrites the block knows they exist.

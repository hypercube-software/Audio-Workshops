#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Controls", l: "chap-controls")

The controls are described by 5 blocks that share the same organisation and the same kind of record, and differ in the
number of controls and in the fields a control of their kind needs:

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, auto, 1fr),
    head([*Block*], [*Controls*], [*Packed address*], [*Packed size*], [*Size*], [*Fields*], [*Banked by*]),
    [DAW buttons], [5], [`00 5B`], [`00 41`], [65], [13], [—],
    [Switches], [24], [`01 1C`], [`02 38`], [312], [13], [8 per bank, 3 banks],
    [Faders], [24], [`03 54`], [`01 10`], [144], [6], [8 per bank, 3 banks],
    [Knobs], [24], [`04 64`], [`01 58`], [216], [9], [8 per bank, 3 banks],
    [Pads], [64], [`06 3C`], [`05 40`], [704], [11], [16 per bank, 4 banks],
  ),
  caption: [The 5 blocks of controls],
)

== Structure of arrays

The controls are stored as *structures of arrays*: each field forms a complete array, and the arrays follow each other.
For the knobs, the 24 types come first, then the 24 channels, then the 24 controller numbers, and so on:

#figure(
  ptable(
    columns: (auto, auto, auto, 1fr),
    head([*Array*], [*Packed address*], [*Packed size*], [*Content*]),
    [Type], [`04 64`], [`00 18`], [Type of the 24 knobs, 1 byte each],
    [Channel], [`04 7C`], [`00 18`], [Channel of the 24 knobs],
    [Controller], [`05 14`], [`00 18`], [Controller number of the 24 knobs],
    [Minimum], [`05 2C`], [`00 18`], [Minimum value of the 24 knobs],
    [Maximum], [`05 44`], [`00 18`], [Maximum value of the 24 knobs],
    [MSB], [`05 5C`], [`00 18`], [Coarse value of the 24 knobs],
    [LSB], [`05 74`], [`00 18`], [Fine value of the 24 knobs],
    [Value], [`06 0C`], [`00 18`], [Current value of the 24 knobs],
    [MIDI to DIN], [`06 24`], [`00 18`], [Whether the 24 knobs are copied to the DIN output],
  ),
  caption: [The 9 arrays of the knob block],
)

A field of control `i` is the `i`-th byte of its array, counted from the first address of the field. Knob 1 has its
type at `0x0464` and its controller number at `0x0514`; knob 9, the first knob of bank B, has them at `0x046C` and
`0x051C`.

2 consequences matter for an implementation:

- A single field of a single control is 1 byte, so a tool reads or writes it alone, with a message of size `00 04`.
- A whole array of 24 bytes takes a message of size `00 1B`, and the block of 312 bytes needs several messages, because
  a single message carries at most 127 data bytes (see #cref(<chap-sysex>)).

== Banks and numbering

A physical control has 1 position on the front panel and 3 settings, 1 per bank. The memory always holds the 3: the
index of the control inside the block is `bank * 8 + position`, with the position counted from zero.

#figure(
  ptable(
    columns: (auto, auto, auto),
    head([*Bank*], [*Switch, fader, knob*], [*Pad*]),
    [`A`], [`0`..`7`], [`0`..`15`],
    [`B`], [`8`..`15`], [`16`..`31`],
    [`C`], [`16`..`23`], [`32`..`47`],
    [`D`], [—], [`48`..`63`],
  ),
  caption: [Index of a control inside its block],
)

The control bank selects which of the 3 settings the first 8 knobs of the block use; the memory of the preset holds all
3. An editor shows all of them, or it silently overwrites the settings of the other banks when it writes a whole array.

== DAW buttons

The 5 transport buttons are the first block. They have the same fields as a switch, including the 2 keystroke codes,
because a DAW button can be assigned to a keyboard shortcut instead of a MIDI message.

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Array*], [*Packed address*], [*Packed size*], [*Stored*], [*Ref.*], [*Meaning*]),
    [Type], [`00 5B`], [`00 05`], [`0`..`4`], [`00`], [CC, note, program change, program bank, keystroke],
    [Channel], [`00 60`], [`00 05`], [`0`..`32`], [`00`], [Same encoding as the keyboard channel],
    [Controller], [`00 65`], [`00 05`], [`0`..`127`], [`7C`], [CC number, note number, program number or bank,
      depending on the type],
    [Mode], [`00 6A`], [`00 05`], [`0` `1`], [`00`], [Momentary, toggle],
    [Program], [`00 6F`], [`00 05`], [`0`..`127`], [`00`], [Program number when the type is a program bank],
    [MSB], [`00 74`], [`00 05`], [`0`..`127`], [`00`], [Most significant byte of the bank, or invert switch],
    [LSB], [`00 79`], [`00 05`], [`0`..`127`], [`00`], [Least significant byte of the bank],
    [MIDI to DIN], [`00 7E`], [`00 05`], [`0` `1`], [`00`], [Whether the button is copied to the DIN output],
    [Note], [`00 83`], [`00 05`], [`0`..`127`], [`00`], [Note number when the type is a note],
    [Velocity], [`00 88`], [`00 05`], [`0`..`127`], [`7F`], [Velocity of that note],
    [unknown], [`00 8D`], [`00 05`], [`00`], [`00`], [Unidentified],
    [Key 1], [`00 92`], [`00 05`], [`0`..`83`], [`25`], [Key code when the type is a keystroke],
    [Key 2], [`00 97`], [`00 05`], [`0`..`14`], [`00`], [Modifiers of that keystroke],
  ),
  caption: [Fields of the DAW buttons],
)

The 5 buttons are, in order, *Enter*, *Left*, *Right*, *Up* and *Down*. Their key codes in the reference preset (`25`,
`34`, `35`, `32`, `33`) are the codes of *F2*, *Left*, *Right*, *Up* and *Down*: the factory setting of the Enter button
is a function key.

== Switches

The switches share the layout of the DAW buttons, with 24 controls. Their arrays are 24 bytes long:
the table below gives the first byte of each, and switch `i` sits at the `i`-th byte of its array.

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Array*], [*Packed address*], [*Packed size*], [*Stored*], [*Ref.*], [*Meaning*]),
    [Type], [`01 1C`], [`00 18`], [`0`..`4`], [`00`], [CC, note, program change, program bank, keystroke],
    [Channel], [`01 34`], [`00 18`], [`0`..`32`], [`01`], [Same encoding as the keyboard channel],
    [Controller], [`01 4C`], [`00 18`], [`0`..`127`], [`1C`], [CC number, note number, program number or bank],
    [Mode], [`01 64`], [`00 18`], [`0` `1`], [`00`], [Momentary, toggle],
    [Program], [`01 7C`], [`00 18`], [`0`..`127`], [`00`], [Program number when the type is a program bank],
    [MSB], [`02 14`], [`00 18`], [`0`..`127`], [`00`], [Most significant byte of the bank, or invert switch],
    [LSB], [`02 2C`], [`00 18`], [`0`..`127`], [`00`], [Least significant byte of the bank],
    [MIDI to DIN], [`02 44`], [`00 18`], [`0` `1`], [`00`], [Whether the switch is copied to the DIN output],
    [Note], [`02 5C`], [`00 18`], [`0`..`127`], [`00`], [Note number when the type is a note],
    [Velocity], [`02 74`], [`00 18`], [`0`..`127`], [`7F`], [Velocity of that note],
    [unknown], [`03 0C`], [`00 18`], [`00`], [`00`], [Unidentified],
    [Key 1], [`03 24`], [`00 18`], [`0`..`83`], [`00`], [Key code when the type is a keystroke],
    [Key 2], [`03 3C`], [`00 18`], [`0`..`14`], [`00`], [Modifiers of that keystroke],
  ),
  caption: [Fields of the switches],
)

A switch and a DAW button share the same record; the 5 DAW buttons are single, outside the banks of 8, and they carry
controller numbers 120 to 124 in the reference preset.

== Faders

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Array*], [*Packed address*], [*Packed size*], [*Stored*], [*Ref.*], [*Meaning*]),
    [Type], [`03 54`], [`00 18`], [`0` `1`], [`00`], [CC, aftertouch],
    [Channel], [`03 6C`], [`00 18`], [`0`..`32`], [`01`], [Same encoding as the keyboard channel],
    [Controller], [`04 04`], [`00 18`], [`0`..`127`], [`07`], [CC number sent by the fader],
    [Minimum], [`04 1C`], [`00 18`], [`0`..`127`], [`00`], [Value sent at the bottom of the travel],
    [Maximum], [`04 34`], [`00 18`], [`0`..`127`], [`7F`], [Value sent at the top of the travel],
    [MIDI to DIN], [`04 4C`], [`00 18`], [`0` `1`], [`00`], [Whether the fader is copied to the DIN output],
  ),
  caption: [Fields of the faders],
)

A fader is a continuous control: its type is either a controller or an aftertouch message, and the minimum and maximum
bound the values it sends.

== Knobs

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Array*], [*Packed address*], [*Packed size*], [*Stored*], [*Ref.*], [*Meaning*]),
    [Type], [`04 64`], [`00 18`], [`0`..`3`], [`00`], [CC, aftertouch, increment 1, increment 2],
    [Channel], [`04 7C`], [`00 18`], [`0`..`32`], [`01`], [Same encoding as the keyboard channel],
    [Controller], [`05 14`], [`00 18`], [`0`..`127`], [`2D`], [CC number sent by the knob],
    [Minimum], [`05 2C`], [`00 18`], [`0`..`127`], [`00`], [Value sent at the left stop],
    [Maximum], [`05 44`], [`00 18`], [`0`..`127`], [`7F`], [Value sent at the right stop],
    [MSB], [`05 5C`], [`00 18`], [`0`..`127`], [`7F`], [Coarse part of the current value],
    [LSB], [`05 74`], [`00 18`], [`0`..`127`], [`7F`], [Fine part of the current value],
    [Value], [`06 0C`], [`00 18`], [`0`..`127`], [`01`], [Current value, as displayed],
    [MIDI to DIN], [`06 24`], [`00 18`], [`0` `1`], [`00`], [Whether the knob is copied to the DIN output],
  ),
  caption: [Fields of the knobs],
)

The knob block stores a current value and its 2 halves: the 3 bytes follow the movement of the knob, and the unit
rewrites them. A tool treats them as read-only; a restore that writes them back gives the knob its position at
power-on.

== Keystroke codes

When the type of a switch or a DAW button is *keystroke*, the 2 last fields hold a key code and a modifier mask, and the
controller number is unused.

#figure(
  ptable(
    columns: (auto, 1fr, auto, 1fr),
    head([*Code*], [*Key*], [*Code*], [*Key*]),
    [`00`..`09`], [1 to 9, then 0], [`30`], [Backspace],
    [`0A`..`23`], [A to Z], [`31`], [Return],
    [`24`..`2F`], [F1 to F12], [`32`..`35`], [Up, Down, Left, Right],
    [`36`], [Tab], [`37`..`40`], [Punctuation keys, see below],
    [`41`], [Esc], [`42`..`47`], [Insert, Home, Page Up, Delete, End, Page Down],
    [`48`..`51`], [Numeric keypad 1 to 9, then 0], [`52`], [Left parenthesis],
    [`53`], [Space], [—], [—],
  ),
  caption: [The key codes of the keystroke mode],
)

The punctuation block runs in the order `,` `.` `/` `[` `]` `\` `'` `;` `-` `=`, at codes `37` to `40`. The code is the
position of the key on a US keyboard, so the same physical key gives the same code whatever the layout of the operating
system.

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, auto),
    head([*Mask*], [*Modifiers*], [*Mask*], [*Modifiers*], [*Mask*], [*Modifiers*]),
    [`00`], [None], [`05`], [Control-Shift], [`0A`], [Alt-Option],
    [`01`], [Control], [`06`], [Control-Alt], [`0B`], [Control-Shift-Alt],
    [`02`], [Shift], [`07`], [Control-Option], [`0C`], [Control-Option-Alt],
    [`03`], [Alt], [`08`], [Shift-Alt], [`0D`], [Control-Shift-Option],
    [`04`], [Option], [`09`], [Shift-Option], [`0E`], [Control-Shift-Alt-Option],
  ),
  caption: [The modifier masks of the keystroke mode],
)

== Writing 1 control

Writing the controller number of the first knob — `2D` to `0B`, for instance — is a single message of 1 data byte at
address `0x0514`:

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
    bytes(2, fill: l_green)[05 14],
    bytes(1, fill: l_blue)[0B],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [Changing the controller number of a knob],
)

The unit takes the new value immediately, without a store: the knob sends the other controller from that point, until
the preset is reloaded. The flash memory keeps the user preset, and the other 2 banks keep their settings.

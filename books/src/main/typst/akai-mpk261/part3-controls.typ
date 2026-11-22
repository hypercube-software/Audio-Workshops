#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Controls", l: "chap-controls")

A preset describes 5 kinds of control, and gives each one a record of n fields. The edit buffer and a user preset hold
the same records in 2 orders: #cref(<chap-preset>) gives the 2 addresses of each field, this chapter gives the fields
and their values.

#figure(
  ptable(
    columns: (auto, auto, auto),
    head([*Control*], [*Fields*], [*Record size*]),
    [DAW buttons], [13], [13],
    [Switches], [13], [13],
    [Faders], [6], [6],
    [Knobs], [9], [9],
    [Pads], [11], [11],
  ),
  caption: [The 5 kinds of control],
)

The 2 addresses of a field follow 1 rule: the *array address* counts in the edit buffer, where the field is an array of
1 value per control, and the *field address* counts in a user preset, where the field sits in the record of each control.

== Banks

A preset describes 24 switches, 24 faders, 24 knobs and 64 pads, while the panel has 8 switches, 8 faders, 8 knobs and
16 pads. The controls of the preset are virtual: the bank selector decides which of them each physical control reaches.

- The *switches*, the *faders* and the *knobs* have 3 banks, A, B and C; the 8 physical controls of a block thus address
    24 virtual controls;
- The *pads* have 4 banks, A, B, C and D; the 16 physical pads thus address 64 virtual pads.

The preset stores the 24 switches, the 24 faders, the 24 knobs and the 64 pads whatever the selected bank. The 2
layouts hold the same controls, with the same banks; only the order of the blocks and the addresses change. The bank
index is `0` for A, `1` for B, `2` for C and `3` for D.

The bank selected on the panel is a live state of the global settings, at `0x0019` for the switches, the faders and
the knobs and at `0x0018` for the pads (see #cref(<chap-global>)). The other banks keep their controls in the preset.

=== Edit buffer

The blocks follow each other in the order *switches, faders, knobs, pads*, not the order of a user preset. The
address is the start of the block plus
`bank * 8`, or `bank * 16` for a pad:

#figure(
  ptable(
    inset: (x: 5pt, y: 2pt),
    columns: (auto, auto, auto, auto, auto),
    head([*Array Pack. Addr.*], [*Control type*], [*Bank*], [*Hardware Control*], [*Virtual Control*]),
    [`01 1C`], [Switch], [`A`], [1 to 8], [1 to 8],
    [`01 24`], [Switch], [`B`], [1 to 8], [9 to 16],
    [`01 2C`], [Switch], [`C`], [1 to 8], [17 to 24],
    [`03 54`], [Fader], [`A`], [1 to 8], [1 to 8],
    [`03 5C`], [Fader], [`B`], [1 to 8], [9 to 16],
    [`03 64`], [Fader], [`C`], [1 to 8], [17 to 24],
    [`04 64`], [Knob], [`A`], [1 to 8], [1 to 8],
    [`04 6C`], [Knob], [`B`], [1 to 8], [9 to 16],
    [`04 74`], [Knob], [`C`], [1 to 8], [17 to 24],
    [`06 3C`], [Pad], [`A`], [1 to 16], [1 to 16],
    [`06 4C`], [Pad], [`B`], [1 to 16], [17 to 32],
    [`06 5C`], [Pad], [`C`], [1 to 16], [33 to 48],
    [`06 6C`], [Pad], [`D`], [1 to 16], [49 to 64],
  ),
  caption: [The virtual controls in the edit buffer],
)

The controls of a bank follow each other from the address.

=== User presets

The blocks follow each other in the order *pads, knobs, faders, switches*, not the order of the edit buffer. The
address is the start of the block plus
`bank * 8` records, or `bank * 16` for a pad:

#figure(
  ptable(
    inset: (x: 5pt, y: 2pt),
    columns: (auto, auto, auto, auto, auto),
    head([*Field Pack. Addr.*], [*Control type*], [*Bank*], [*Hardware Control*], [*Virtual Control*]),
    [`00 35`], [Pad], [`A`], [1 to 16], [1 to 16],
    [`01 65`], [Pad], [`B`], [1 to 16], [17 to 32],
    [`03 15`], [Pad], [`C`], [1 to 16], [33 to 48],
    [`04 45`], [Pad], [`D`], [1 to 16], [49 to 64],
    [`05 75`], [Knob], [`A`], [1 to 8], [1 to 8],
    [`06 3D`], [Knob], [`B`], [1 to 8], [9 to 16],
    [`07 05`], [Knob], [`C`], [1 to 8], [17 to 24],
    [`07 4D`], [Fader], [`A`], [1 to 8], [1 to 8],
    [`07 7D`], [Fader], [`B`], [1 to 8], [9 to 16],
    [`08 2D`], [Fader], [`C`], [1 to 8], [17 to 24],
    [`08 5D`], [Switch], [`A`], [1 to 8], [1 to 8],
    [`09 45`], [Switch], [`B`], [1 to 8], [9 to 16],
    [`0A 2D`], [Switch], [`C`], [1 to 8], [17 to 24],
  ),
  caption: [The virtual controls in a user preset],
)

A record is 13 bytes for a switch, 6 for a fader, 9 for a knob and 11 for a pad. Each control adds 1 record to the
address.

=== Switches

#kfigure(
  ptable(
    columns: (2.2cm, 1.2cm, 2.2cm, 1.2cm, 2.8cm, 1.3cm, 1fr),
    head([*Field Pack. Addr.*], [*Field size*], [*Array Pack. Addr.*], [*Array size*], [*Field name*], [*Range*], [*Description*]),
    [`08 5D`], [1], [`01 1C`], [24], [Type], [`0`..`4`], [CC, note, program change, program bank, keystroke],
    [`08 5E`], [1], [`01 34`], [24], [Channel], [`0`..`32`], [See @channel-encoding],
    [`08 5F`], [1], [`01 4C`], [24], [Controller], [`0`..`127`], [CC number, note number, program number or bank],
    [`08 60`], [1], [`01 64`], [24], [Mode], [`0`, `1`], [Momentary, toggle],
    [`08 61`], [1], [`01 7C`], [24], [Program], [`0`..`127`], [Program number when the type is a program bank],
    [`08 62`], [1], [`02 14`], [24], [MSB], [`0`..`127`], [MSB of the bank, or invert switch],
    [`08 63`], [1], [`02 2C`], [24], [LSB], [`0`..`127`], [LSB of the bank],
    [`08 64`], [1], [`02 5C`], [24], [MIDI to DIN], [`0`, `1`], [Whether the switch is copied to the DIN output],
    [`08 65`], [1], [`02 44`], [24], [Note], [`0`..`127`], [Note number when the type is a note],
    [`08 66`], [1], [`02 74`], [24], [Velocity], [`0`..`127`], [Velocity of that note],
    [`08 67`], [1], [`03 0C`], [24], [unknown], [—], [—],
    [`08 68`], [1], [`03 24`], [24], [Key 1], [`0`..`83`], [Key code when the type is a keystroke],
    [`08 69`], [1], [`03 3C`], [24], [Key 2], [`0`..`14`], [Modifiers of that keystroke],
  ),
  caption: [The fields of a switch],
)

A switch and a DAW button share the same record.

=== Faders

#kfigure(
  ptable(
    columns: (2.2cm, 1.2cm, 2.2cm, 1.2cm, 2.8cm, 1.3cm, 1fr),
    head([*Field Pack. Addr.*], [*Field size*], [*Array Pack. Addr.*], [*Array size*], [*Field name*], [*Range*], [*Description*]),
    [`07 4D`], [1], [`03 54`], [24], [Type], [`0`, `1`], [CC, aftertouch],
    [`07 4E`], [1], [`03 6C`], [24], [Channel], [`0`..`32`], [See @channel-encoding],
    [`07 4F`], [1], [`04 04`], [24], [Controller], [`0`..`127`], [CC number sent by the fader],
    [`07 50`], [1], [`04 1C`], [24], [Minimum], [`0`..`127`], [Value sent at the bottom of the travel],
    [`07 51`], [1], [`04 34`], [24], [Maximum], [`0`..`127`], [Value sent at the top of the travel],
    [`07 52`], [1], [`04 4C`], [24], [MIDI to DIN], [`0`, `1`], [Whether the fader is copied to the DIN output],
  ),
  caption: [The fields of a fader],
)

A fader is a continuous control: its type is either a controller or an aftertouch message, and the minimum and maximum
bound the values it sends.

=== Knobs

#kfigure(
  ptable(
    columns: (2.2cm, 1.2cm, 2.2cm, 1.2cm, 2.8cm, 1.3cm, 1fr),
    head([*Field Pack. Addr.*], [*Field size*], [*Array Pack. Addr.*], [*Array size*], [*Field name*], [*Range*], [*Description*]),
    [`05 75`], [1], [`04 64`], [24], [Type], [`0`..`3`], [CC, aftertouch, increment 1, increment 2],
    [`05 76`], [1], [`04 7C`], [24], [Channel], [`0`..`32`], [See @channel-encoding],
    [`05 77`], [1], [`05 14`], [24], [Controller], [`0`..`127`], [CC number sent by the knob],
    [`05 78`], [1], [`05 2C`], [24], [Minimum], [`0`..`127`], [Value sent at the left stop],
    [`05 79`], [1], [`05 44`], [24], [Maximum], [`0`..`127`], [Value sent at the right stop],
    [`05 7A`], [1], [`06 24`], [24], [MIDI to DIN], [`0`, `1`], [Whether the knob is copied to the DIN output],
    [`05 7B`], [1], [`05 5C`], [24], [MSB], [`0`..`127`], [Coarse part of the increment, for the Increment/Decrement 1 type],
    [`05 7C`], [1], [`05 74`], [24], [LSB], [`0`..`127`], [Fine part of the increment],
    [`05 7D`], [1], [`06 0C`], [24], [Value], [`0`..`127`], [Value sent with an increment or a decrement message],
  ),
  caption: [The fields of a knob],
)

The 3 bytes MSB, LSB and Value belong to the Increment/Decrement 1 type: they decide what a turn of the knob sends, and
the unit never rewrites them. For the other types, the minimum and maximum bound the values.

=== Pads

The unit has 16 physical pads, and each one has 4 settings, 1 per bank, so a preset always describes 64 pads. Pad 1 of
bank A is the first record, pad 16 of bank A the 16th, and pad 16 of bank D the 64th.

#kfigure(
  ptable(
    columns: (2.2cm, 1.2cm, 2.2cm, 1.2cm, 2.8cm, 1.3cm, 1fr),
    head([*Field Pack. Addr.*], [*Field size*], [*Array Pack. Addr.*], [*Array size*], [*Field name*], [*Range*], [*Description*]),
    [`00 35`], [1], [`06 3C`], [64], [Type], [`0`..`2`], [Note, program change, program bank],
    [`00 36`], [1], [`06 7C`], [64], [Channel], [`0`..`32`], [See @channel-encoding],
    [`00 37`], [1], [`07 3C`], [64], [Note], [`0`..`127`], [Note number when the type is a note; `2F` is B2],
    [`00 38`], [1], [`0A 3C`], [64], [MIDI to DIN], [`0`, `1`], [Whether the pad is copied to the DIN output],
    [`00 39`], [1], [`07 7C`], [64], [Mode], [`0`, `1`], [Momentary, toggle],
    [`00 3A`], [1], [`08 3C`], [64], [Aftertouch], [`0`..`2`], [Off, channel, polyphonic],
    [`00 3B`], [1], [`08 7C`], [64], [Program], [`0`..`127`], [Program number when the type is a program change or a program bank],
    [`00 3C`], [1], [`09 3C`], [64], [MSB], [`0`..`127`], [MSB of the bank],
    [`00 3D`], [1], [`09 7C`], [64], [LSB], [`0`..`127`], [LSB of the bank],
    [`00 3E`], [1], [`0A 7C`], [64], [Off colour], [`0`..`16`], [Colour of the pad at rest],
    [`00 3F`], [1], [`0B 3C`], [64], [On colour], [`0`..`16`], [Colour of the pad when it is hit],
  ),
  caption: [The fields of a pad],
)

On a pad of type note, the note number sits in the *Note* field, and the program and bank fields stay at zero.

==== Pad type

A pad is a small MIDI generator: its type decides which of the other fields are meaningful.

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

A program bank pad is the only one that uses the 3 fields MSB, LSB and Program at once. The order on the wire is the one
of the MIDI specification: the MSB of the bank first, then the LSB, then the program
number.

==== Colours

#kfigure(
  ptable(
    columns: (auto, auto),
    head([*Value*], [*Colour*]),
    [`00`], [Off],
    [`01`], [Red],
    [`02`], [Orange],
    [`03`], [Amber],
    [`04`], [Yellow],
    [`05`], [Green],
    [`06`], [Green-blue],
    [`07`], [Aqua],
    [`08`], [Light blue],
    [`09`], [Blue],
    [`0A`], [Purple],
    [`0B`], [Pink],
    [`0C`], [Hot pink],
    [`0D`], [Light purple],
    [`0E`], [Light green],
    [`0F`], [Light pink],
    [`10`], [Grey],
  ),
  caption: [The 17 colours of a pad],
)

The reference preset uses grey (`10`) for the rest state of all 4 banks and light blue (`08`) for the hit state of bank
A, which is why the first 16 values of the hit colour array differ from the following ones.

A colour of `00` turns the pad off: the pad still sends its message when it is hit.

==== Aftertouch

The aftertouch field of a pad applies to the note pads only:

#kfigure(
  ptable(
    columns: (auto, auto, 1fr),
    head([*Stored*], [*Mode*], [*Behaviour*]),
    [`0`], [Off], [The pressure on the pad is ignored],
    [`1`], [Channel], [The pressure is sent as channel aftertouch for the channel of the pad],
    [`2`], [Polyphonic], [The pressure is sent as polyphonic aftertouch, with the note of the pad],
  ),
  caption: [The 3 aftertouch modes of a pad],
)

The keyboard has its own aftertouch switch, at `0x0018` of the edit buffer. Each has its own switch, and a preset can
send aftertouch from the pads, from the keys, or from both.

== DAW buttons

The *5 transport buttons* have the same fields as a switch, including the 2 keystroke codes, because a DAW button can be
assigned to a keyboard shortcut instead of a MIDI message.

#kfigure(
  ptable(
    columns: (2.2cm, 1.2cm, 2.2cm, 1.2cm, 2.8cm, 1.3cm, 1fr),
    head([*Field Pack. Addr.*], [*Field size*], [*Array Pack. Addr.*], [*Array size*], [*Field name*], [*Range*], [*Description*]),
    [`0B 15`], [1], [`00 5B`], [5], [Type], [`0`..`4`], [CC, note, program change, program bank, keystroke],
    [`0B 16`], [1], [`00 60`], [5], [Channel], [`0`..`32`], [See @channel-encoding],
    [`0B 17`], [1], [`00 65`], [5], [Controller], [`0`..`127`], [CC number, note number, program number or bank, depending on the type],
    [`0B 18`], [1], [`00 6A`], [5], [Mode], [`0`, `1`], [Momentary, toggle],
    [`0B 19`], [1], [`00 6F`], [5], [Program], [`0`..`127`], [Program number when the type is a program bank],
    [`0B 1A`], [1], [`00 74`], [5], [MSB], [`0`..`127`], [MSB of the bank, or invert switch],
    [`0B 1B`], [1], [`00 79`], [5], [LSB], [`0`..`127`], [LSB of the bank],
    [`0B 1C`], [1], [`00 83`], [5], [MIDI to DIN], [`0`, `1`], [Whether the button is copied to the DIN output],
    [`0B 1D`], [1], [`00 7E`], [5], [Note], [`0`..`127`], [Note number when the type is a note],
    [`0B 1E`], [1], [`00 88`], [5], [Velocity], [`0`..`127`], [Velocity of that note],
    [`0B 1F`], [1], [`00 8D`], [5], [unknown], [—], [—],
    [`0B 20`], [1], [`00 92`], [5], [Key 1], [`0`..`83`], [Key code when the type is a keystroke],
    [`0B 21`], [1], [`00 97`], [5], [Key 2], [`0`..`14`], [Modifiers of that keystroke],
  ),
  caption: [The fields of a DAW button],
)

The 5 buttons are, in order, *Enter*, *Left*, *Right*, *Up* and *Down*. Their key codes in the reference preset (`25`,
`34`, `35`, `32`, `33`) are the codes of *F2*, *Left*, *Right*, *Up* and *Down*: the factory setting of the Enter button
is a function key.

== Keystroke codes

When the type of a switch or a DAW button is *keystroke*, the 2 last fields hold a key code and a modifier mask, and the
controller number is unused.

#kfigure(
  ptable(
    columns: (auto, auto),
    head([*Code*], [*Key*]),
    [`00`..`09`], [1 to 9, then 0],
    [`0A`..`23`], [A to Z],
    [`24`..`2F`], [F1 to F12],
    [`30`], [Backspace],
    [`31`], [Return],
    [`32`..`35`], [Up, Down, Left, Right],
    [`36`], [Tab],
    [`37`..`40`], [Punctuation keys],
    [`41`], [Esc],
    [`42`..`47`], [Insert, Home, Page Up, Delete, End, Page Down],
    [`48`..`51`], [Numeric keypad 1 to 9, then 0],
    [`52`], [Left parenthesis],
    [`53`], [Space],
  ),
  caption: [The key codes of the keystroke mode],
)

The punctuation block runs in the order `,` `.` `/` `[` `]` `\` `'` `;` `-` `=`, at codes `37` to `40`. The code is the
position of the key on a US keyboard, so the same physical key gives the same code whatever the layout of the operating
system.

#kfigure(
  ptable(
    columns: (auto, auto),
    head([*Mask*], [*Modifiers*]),
    [`00`], [None],
    [`01`], [Control],
    [`02`], [Shift],
    [`03`], [Alt],
    [`04`], [Option],
    [`05`], [Control-Shift],
    [`06`], [Control-Alt],
    [`07`], [Control-Option],
    [`08`], [Shift-Alt],
    [`09`], [Shift-Option],
    [`0A`], [Alt-Option],
    [`0B`], [Control-Shift-Alt],
    [`0C`], [Control-Option-Alt],
    [`0D`], [Control-Shift-Option],
    [`0E`], [Control-Shift-Alt-Option],
  ),
  caption: [The modifier masks of the keystroke mode],
)

== Channel <channel-encoding>

The *Channel* field of the 5 kinds of control uses the following encoding:

- `00` is *Common*: the control follows the common channel of the global settings, at `0x000A`;
- `01` to `10` are USB A1 to A16;
- `11` to `20` are USB B1 to B16.

The keyboard has its own *Channel* field, at `0x0016` of the edit buffer, with the same encoding.

== B channel <b-channel>

The *Split: B channel* field at `0x001F` of the edit buffer gives the channel of zone B of the split. It has its own encoding, with no
*Common* value:

- `00` to `0F` are USB A1 to A16;
- `10` to `1F` are USB B1 to B16.

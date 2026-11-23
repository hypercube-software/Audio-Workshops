#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Controls", l: "chap-controls")

// A field table. The body is hyphenated like the rest of the book, the header
// words are not: "Ad-dress" in a header is unreadable.
#let ftable(..args) = ptable(..args)
#let fhead(cell) = text(hyphenate: false, cell)

Among its settings, a preset holds 5 kinds of control: the DAW buttons, the switches, the faders, the knobs and the
pads. Each one is a record of n fields. This chapter gives the fields and their values.

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

== Warning

*BEWARE*: the field tables of this chapter describe 2 *memory layouts* in a single table. It is very important that you
understand how to read them.

Each table of a control gives 2 addresses for a field:

- The column *Field Address* gives the field's address in the first record, in both forms: `0x045D (08 5D)` is the
    unpacked offset and its 2 packed bytes (with its related columns *Field size* and *Field offset*, the size and the
    byte offset of that field in the record). This is, in the *user preset* memory layout, the field's address in the
    record of control 1; the record of control 2 starts 1 record size further;
- The column *Array Address* gives the field's address in the edit buffer, in both forms: `0x009C (01 1C)` is the unpacked
    offset and its 2 packed bytes (with its related columns *Field index* and *Array length*, the rank of the array and
    its number of values). This is where the field is an array of 1 value per control.

*Field index* numbers the arrays of the block: `Array Address` is the block base plus the field index times
*Array length*. The 2 functions of Section 2.2.3 give the formula behind these 2 addresses.

== Banks

A preset describes 24 switches, 24 faders, 24 knobs and 64 pads, while the panel has 8 switches, 8 faders, 8 knobs and
16 pads. The controls of the preset are virtual: the bank selector decides which of them each physical control reaches.

- The *switches*, the *faders* and the *knobs* have 3 banks, A, B and C; the 8 physical controls of a block thus address
    24 virtual controls;
- The *pads* have 4 banks, A, B, C and D; the 16 physical pads thus address 64 virtual pads.

The preset stores the 24 switches, the 24 faders, the 24 knobs and the 64 pads whatever the selected bank. The 2 memory
layouts hold the same controls, with the same banks; only the order of the blocks and the addresses change. The bank
index is `0` for A, `1` for B, `2` for C and `3` for D.

The bank selected on the panel is a live state of the global settings, at `0x0019` for the switches, the faders and
the knobs and at `0x0018` for the pads (see #cref(<chap-global>)). The other banks keep their controls in the preset.

=== User presets

The blocks follow each other in the order *pads, knobs, faders, switches*, not the order of the edit buffer. The
address is the start of the block plus
`bank * 8` records, or `bank * 16` for a pad:

#figure(
  ptable(
    inset: (x: 5pt, y: 2pt),
    columns: (auto, auto, auto, auto, auto),
    head([*Field Address*], [*Control type*], [*Bank*], [*Hardware Control*], [*Virtual Control*]),
    [`0x0035 (00 35)`], [Pad], [`A`], [1 to 16], [1 to 16],
    [`0x00E5 (01 65)`], [Pad], [`B`], [1 to 16], [17 to 32],
    [`0x0195 (03 15)`], [Pad], [`C`], [1 to 16], [33 to 48],
    [`0x0245 (04 45)`], [Pad], [`D`], [1 to 16], [49 to 64],
    [`0x02F5 (05 75)`], [Knob], [`A`], [1 to 8], [1 to 8],
    [`0x033D (06 3D)`], [Knob], [`B`], [1 to 8], [9 to 16],
    [`0x0385 (07 05)`], [Knob], [`C`], [1 to 8], [17 to 24],
    [`0x03CD (07 4D)`], [Fader], [`A`], [1 to 8], [1 to 8],
    [`0x03FD (07 7D)`], [Fader], [`B`], [1 to 8], [9 to 16],
    [`0x042D (08 2D)`], [Fader], [`C`], [1 to 8], [17 to 24],
    [`0x045D (08 5D)`], [Switch], [`A`], [1 to 8], [1 to 8],
    [`0x04C5 (09 45)`], [Switch], [`B`], [1 to 8], [9 to 16],
    [`0x052D (0A 2D)`], [Switch], [`C`], [1 to 8], [17 to 24],
  ),
  caption: [The virtual controls in a user preset],
)

A record is 13 bytes for a switch, 6 for a fader, 9 for a knob and 11 for a pad. Each control adds 1 record to the
address.

=== Edit buffer

The blocks follow each other in the order *switches, faders, knobs, pads*, not the order of a user preset. The
address is the start of the block plus
`bank * 8`, or `bank * 16` for a pad:

#figure(
  ptable(
    inset: (x: 5pt, y: 2pt),
    columns: (auto, auto, auto, auto, auto),
    head([*Array Address*], [*Control type*], [*Bank*], [*Hardware Control*], [*Virtual Control*]),
    [`0x009C (01 1C)`], [Switch], [`A`], [1 to 8], [1 to 8],
    [`0x00A4 (01 24)`], [Switch], [`B`], [1 to 8], [9 to 16],
    [`0x00AC (01 2C)`], [Switch], [`C`], [1 to 8], [17 to 24],
    [`0x01D4 (03 54)`], [Fader], [`A`], [1 to 8], [1 to 8],
    [`0x01DC (03 5C)`], [Fader], [`B`], [1 to 8], [9 to 16],
    [`0x01E4 (03 64)`], [Fader], [`C`], [1 to 8], [17 to 24],
    [`0x0264 (04 64)`], [Knob], [`A`], [1 to 8], [1 to 8],
    [`0x026C (04 6C)`], [Knob], [`B`], [1 to 8], [9 to 16],
    [`0x0274 (04 74)`], [Knob], [`C`], [1 to 8], [17 to 24],
    [`0x033C (06 3C)`], [Pad], [`A`], [1 to 16], [1 to 16],
    [`0x034C (06 4C)`], [Pad], [`B`], [1 to 16], [17 to 32],
    [`0x035C (06 5C)`], [Pad], [`C`], [1 to 16], [33 to 48],
    [`0x036C (06 6C)`], [Pad], [`D`], [1 to 16], [49 to 64],
  ),
  caption: [The virtual controls in the edit buffer],
)

The controls of a bank follow each other from the address.

=== Value address <value-address>

The tables above give the base of each block. The address of 1 value follows from the control type, the bank, the
hardware number and the field:

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, auto, auto, auto),
    head([*Control type*], [*Hardware*], [*Banks*], [*Fields*],
      table.cell(fill: l_green)[*Record base*], table.cell(fill: l_green)[*Packed Record base*],
      table.cell(fill: l_blue)[*Array base*], table.cell(fill: l_blue)[*Packed Array base*]),
    [DAW buttons], [5], [1], [13], [`0x0595`], [`0B 15`], [`0x005B`], [`00 5B`],
    [Switches], [8], [3], [13], [`0x045D`], [`08 5D`], [`0x009C`], [`01 1C`],
    [Faders], [8], [3], [6], [`0x03CD`], [`07 4D`], [`0x01D4`], [`03 54`],
    [Knobs], [8], [3], [9], [`0x02F5`], [`05 75`], [`0x0264`], [`04 64`],
    [Pads], [16], [4], [11], [`0x0035`], [`00 35`], [`0x033C`], [`06 3C`],
  ),
  caption: [The counts and the bases of the 5 control types],
)

The *Record base* and *Array base* columns are byte offsets: the formulas below use them as they are. The *Packed
Record base* and *Packed Array base* columns give the same addresses in the 2-byte form that travels in a MIDI
request: the switches have `0x045D` and `0x009C`, which are `08 5D` and `01 1C`.

*BEWARE*: never ever attempt to do address calculation with packed addresses. The packed format is only used in the
final MIDI request.

The 2 packed bytes of an offset are `offset >> 7` and `offset & 0x7F`.

The *field index* is the rank of a field's array in the edit buffer: the arrays of a block are numbered from 0, and
the *Field index* column of the control tables gives that number. An array starts at the block base plus the field
index times the array length, and it holds 1 value per virtual control of the type.

The *record index* is the rank of a control's record among the records of its control type: the bank index times the
hardware count, plus the hardware number minus 1. A record starts at the record base plus the record index times the
record size. For instance, fader 3 of bank C is record `2 * 8 + 2 = 18`, which starts at
`0x03CD + 18 * 6 = 0x0439`.

```java
// All values are always stored in 1 byte in both memory spaces, so calculation is simplified.

/**
 * The address of a value in the current preset, in the edit buffer.
 *
 * @param type       the control type: DAW buttons, switches, faders, knobs or pads
 * @param bank       the bank index: 0 for A, 1 for B, 2 for C and 3 for D
 * @param hardware   the hardware control number, from 1
 * @param fieldIndex the rank of the field's array in the edit buffer, from 0
 * @return the byte offset in the edit buffer
 */
int getValueAddressForCurrentPreset(
    ControlType type, int bank, int hardware, int fieldIndex
) {
    int h = hardwareCount[type];
    int n = h * bankCount[type];          // values per array
    return arrayBase[type] + fieldIndex * n + bank * h + (hardware - 1);
}

/**
 * The address of a value in a user preset, in its record.
 *
 * @param type        the control type: DAW buttons, switches, faders, knobs or pads
 * @param bank        the bank index: 0 for A, 1 for B, 2 for C and 3 for D
 * @param hardware    the hardware control number, from 1
 * @param fieldOffset the byte offset of the field in the record, from 0
 * @return the byte offset in the user preset record
 */
int getValueAddressForUserPresets(
    ControlType type, int bank, int hardware, int fieldOffset
) {
    int h = hardwareCount[type];
    int r = fieldCount[type];             // record size, in bytes
    return recordBase[type] + (bank * h + (hardware - 1)) * r + fieldOffset;
}
```


#block(breakable: false)[
=== Switches

There are a total of 24 virtual switches. A switch#footnote[A switch and a DAW button share the same record.]
record has 13 fields. Each field occupies 1 byte of memory:

#kfigure(
  ftable(
    columns: (1.6cm, 1.05cm, 1.15cm, 1.6cm, 1.2cm, 1.45cm, 1.55cm, 1.15cm, 1fr),
    head(
      table.cell(fill: l_green, fhead[*Field Address*]), table.cell(fill: l_green, fhead[*Field size*]),
      table.cell(fill: l_green, fhead[*Field offset*]),
      table.cell(fill: l_blue, fhead[*Array Address*]), table.cell(fill: l_blue, fhead[*Field index*]),
      table.cell(fill: l_blue, fhead[*Array length*]),
      fhead[*Field name*], fhead[*Range*], fhead[*Description*],
    ),
    [`0x045D` \ `(08 5D)`], [1], [0], [`0x009C` \ `(01 1C)`], [0], [24], [Type], [`0`..`4`], [CC, note, program change, program bank, keystroke],
    [`0x045E` \ `(08 5E)`], [1], [1], [`0x00B4` \ `(01 34)`], [1], [24], [Channel], [`0`..`32`], [See @channel-encoding],
    [`0x045F` \ `(08 5F)`], [1], [2], [`0x00CC` \ `(01 4C)`], [2], [24], [Controller], [`0`..`127`], [CC number, note number, program number or bank],
    [`0x0460` \ `(08 60)`], [1], [3], [`0x00E4` \ `(01 64)`], [3], [24], [Mode], [`0`, `1`], [Momentary, toggle],
    [`0x0461` \ `(08 61)`], [1], [4], [`0x00FC` \ `(01 7C)`], [4], [24], [Program], [`0`..`127`], [Program number when the type is a program bank],
    [`0x0462` \ `(08 62)`], [1], [5], [`0x0114` \ `(02 14)`], [5], [24], [MSB], [`0`..`127`], [MSB of the bank, or invert switch],
    [`0x0463` \ `(08 63)`], [1], [6], [`0x012C` \ `(02 2C)`], [6], [24], [LSB], [`0`..`127`], [LSB of the bank],
    [`0x0464` \ `(08 64)`], [1], [7], [`0x015C` \ `(02 5C)`], [8], [24], [MIDI to DIN], [`0`, `1`], [Whether the switch is copied to the DIN output],
    [`0x0465` \ `(08 65)`], [1], [8], [`0x0144` \ `(02 44)`], [7], [24], [Note], [`0`..`127`], [Note number when the type is a note],
    [`0x0466` \ `(08 66)`], [1], [9], [`0x0174` \ `(02 74)`], [9], [24], [Velocity], [`0`..`127`], [Velocity of that note],
    [`0x0467` \ `(08 67)`], [1], [10], [`0x018C` \ `(03 0C)`], [10], [24], [unknown], [—], [—],
    [`0x0468` \ `(08 68)`], [1], [11], [`0x01A4` \ `(03 24)`], [11], [24], [Key 1], [`0`..`83`], [Key code when the type is a keystroke],
    [`0x0469` \ `(08 69)`], [1], [12], [`0x01BC` \ `(03 3C)`], [12], [24], [Key 2], [`0`..`14`], [Modifiers of that keystroke],
  ),
  caption: [The fields of a switch],
)
]


*User preset calculation examples:* a switch record is 13 bytes, and the records are ordered by bank, 8 per bank.
Switch 9 (hardware 1 of bank B) is record `1 * 8 + 0 = 8`. The *MIDI to DIN* has field index 8, but sits at byte 7
of the record.

- *Type* (field offset 0): `0x045D + 8 * 13 + 0 = 0x04C5`, the packed address `09 45`;
- *MIDI to DIN* (field offset 7): `0x045D + 8 * 13 + 7 = 0x04CC`, the packed address `09 4C`.

*Current preset calculation examples:* a switch array holds 24 values, 8 switches and 3 banks. Switch 9 is value
`1 * 8 + 0` of each array.

- *Type* (field index 0): `0x009C + 0 * 24 + 1 * 8 + 0 = 0x00A4`, the packed address `01 24`;
- *MIDI to DIN* (field index 8): `0x009C + 8 * 24 + 1 * 8 + 0 = 0x0164`, the packed address `02 64`.

#block(breakable: false)[
=== Faders

There are a total of 24 virtual faders. A fader record has 6 fields. Each field occupies 1 byte of memory:

#kfigure(
  ftable(
    columns: (1.6cm, 1.05cm, 1.15cm, 1.6cm, 1.2cm, 1.45cm, 1.55cm, 1.15cm, 1fr),
    head(
      table.cell(fill: l_green, fhead[*Field Address*]), table.cell(fill: l_green, fhead[*Field size*]),
      table.cell(fill: l_green, fhead[*Field offset*]),
      table.cell(fill: l_blue, fhead[*Array Address*]), table.cell(fill: l_blue, fhead[*Field index*]),
      table.cell(fill: l_blue, fhead[*Array length*]),
      fhead[*Field name*], fhead[*Range*], fhead[*Description*],
    ),
    [`0x03CD` \ `(07 4D)`], [1], [0], [`0x01D4` \ `(03 54)`], [0], [24], [Type], [`0`, `1`], [CC, aftertouch],
    [`0x03CE` \ `(07 4E)`], [1], [1], [`0x01EC` \ `(03 6C)`], [1], [24], [Channel], [`0`..`32`], [See @channel-encoding],
    [`0x03CF` \ `(07 4F)`], [1], [2], [`0x0204` \ `(04 04)`], [2], [24], [Controller], [`0`..`127`], [CC number sent by the fader],
    [`0x03D0` \ `(07 50)`], [1], [3], [`0x021C` \ `(04 1C)`], [3], [24], [Minimum], [`0`..`127`], [Value sent at the bottom of the travel],
    [`0x03D1` \ `(07 51)`], [1], [4], [`0x0234` \ `(04 34)`], [4], [24], [Maximum], [`0`..`127`], [Value sent at the top of the travel],
    [`0x03D2` \ `(07 52)`], [1], [5], [`0x024C` \ `(04 4C)`], [5], [24], [MIDI to DIN], [`0`, `1`], [Whether the fader is copied to the DIN output],
  ),
  caption: [The fields of a fader],
)
]


A fader is a continuous control: its type is either a controller or an aftertouch message, and the minimum and maximum
bound the values it sends.

*User preset calculation examples:* a fader record is 6 bytes, and the records are ordered by bank, 8 per bank.
Fader 2 (hardware 2 of bank A) is record `0 * 8 + 1 = 1`; fader 3 (hardware 3 of bank C) is record
`2 * 8 + 2 = 18`.

- *Type* (field offset 0) of fader 2: `0x03CD + 1 * 6 + 0 = 0x03D3`, the packed address `07 53`;
- *Maximum* (field offset 4) of fader 3: `0x03CD + 18 * 6 + 4 = 0x043D`, the packed address `08 3D`.

*Current preset calculation examples:* a fader array holds 24 values, 8 faders and 3 banks. Fader 2 is value
`0 * 8 + 1` of each array; fader 3 is value `2 * 8 + 2`.

- *Type* (field index 0) of fader 2: `0x01D4 + 0 * 24 + 0 * 8 + 1 = 0x01D5`, the packed address `03 55`;
- *Maximum* (field index 4) of fader 3: `0x01D4 + 4 * 24 + 2 * 8 + 2 = 0x0246`, the packed address `04 46`.

#block(breakable: false)[
=== Knobs

There are a total of 24 virtual knobs. A knob record has 9 fields. Each field occupies 1 byte of memory:

#kfigure(
  ftable(
    columns: (1.6cm, 1.05cm, 1.15cm, 1.6cm, 1.2cm, 1.45cm, 1.55cm, 1.15cm, 1fr),
    head(
      table.cell(fill: l_green, fhead[*Field Address*]), table.cell(fill: l_green, fhead[*Field size*]),
      table.cell(fill: l_green, fhead[*Field offset*]),
      table.cell(fill: l_blue, fhead[*Array Address*]), table.cell(fill: l_blue, fhead[*Field index*]),
      table.cell(fill: l_blue, fhead[*Array length*]),
      fhead[*Field name*], fhead[*Range*], fhead[*Description*],
    ),
    [`0x02F5` \ `(05 75)`], [1], [0], [`0x0264` \ `(04 64)`], [0], [24], [Type], [`0`..`3`], [CC, aftertouch, increment 1, increment 2],
    [`0x02F6` \ `(05 76)`], [1], [1], [`0x027C` \ `(04 7C)`], [1], [24], [Channel], [`0`..`32`], [See @channel-encoding],
    [`0x02F7` \ `(05 77)`], [1], [2], [`0x0294` \ `(05 14)`], [2], [24], [Controller], [`0`..`127`], [CC number sent by the knob],
    [`0x02F8` \ `(05 78)`], [1], [3], [`0x02AC` \ `(05 2C)`], [3], [24], [Minimum], [`0`..`127`], [Value sent at the left stop],
    [`0x02F9` \ `(05 79)`], [1], [4], [`0x02C4` \ `(05 44)`], [4], [24], [Maximum], [`0`..`127`], [Value sent at the right stop],
    [`0x02FA` \ `(05 7A)`], [1], [5], [`0x0324` \ `(06 24)`], [8], [24], [MIDI to DIN], [`0`, `1`], [Whether the knob is copied to the DIN output],
    [`0x02FB` \ `(05 7B)`], [1], [6], [`0x02DC` \ `(05 5C)`], [5], [24], [MSB], [`0`..`127`], [Coarse part of the increment, for the Increment/Decrement 1 type],
    [`0x02FC` \ `(05 7C)`], [1], [7], [`0x02F4` \ `(05 74)`], [6], [24], [LSB], [`0`..`127`], [Fine part of the increment],
    [`0x02FD` \ `(05 7D)`], [1], [8], [`0x030C` \ `(06 0C)`], [7], [24], [Value], [`0`..`127`], [Value sent with an increment or a decrement message],
  ),
  caption: [The fields of a knob],
)
]


The 3 bytes MSB, LSB and Value belong to the Increment/Decrement 1 type: they decide what a turn of the knob sends, and
the unit never rewrites them. For the other types, the minimum and maximum bound the values.

*User preset calculation examples:* a knob record is 9 bytes, and the records are ordered by bank, 8 per bank.
Knob 1 (hardware 1 of bank A) is record `0 * 8 + 0 = 0`; knob 5 (hardware 5 of bank B) is record `1 * 8 + 4 = 12`.

- *Controller* (field offset 2) of knob 1: `0x02F5 + 0 * 9 + 2 = 0x02F7`, the packed address `05 77`;
- *MIDI to DIN* (field offset 5) of knob 5: `0x02F5 + 12 * 9 + 5 = 0x0366`, the packed address `06 66`.

*Current preset calculation examples:* a knob array holds 24 values, 8 knobs and 3 banks. Knob 1 is value
`0 * 8 + 0` of each array; knob 5 is value `1 * 8 + 4`.

- *Controller* (field index 2) of knob 1: `0x0264 + 2 * 24 + 0 * 8 + 0 = 0x0294`, the packed address `05 14`;
- *MIDI to DIN* (field index 8) of knob 5: `0x0264 + 8 * 24 + 1 * 8 + 4 = 0x0330`, the packed address `06 30`.

#block(breakable: false)[
=== Pads

There are a total of 64 virtual pads. A pad record has 11 fields. Each field occupies 1 byte of memory. The unit
has 16 physical pads, and each one has 4 settings, 1 per bank. Pad 1 of bank A is the first record, pad 16 of
bank A the 16th, and pad 16 of bank D the 64th.

#kfigure(
  ftable(
    columns: (1.6cm, 1.05cm, 1.15cm, 1.6cm, 1.2cm, 1.45cm, 1.55cm, 1.15cm, 1fr),
    head(
      table.cell(fill: l_green, fhead[*Field Address*]), table.cell(fill: l_green, fhead[*Field size*]),
      table.cell(fill: l_green, fhead[*Field offset*]),
      table.cell(fill: l_blue, fhead[*Array Address*]), table.cell(fill: l_blue, fhead[*Field index*]),
      table.cell(fill: l_blue, fhead[*Array length*]),
      fhead[*Field name*], fhead[*Range*], fhead[*Description*],
    ),
    [`0x0035` \ `(00 35)`], [1], [0], [`0x033C` \ `(06 3C)`], [0], [64], [Type], [`0`..`2`], [Note, program change, program bank],
    [`0x0036` \ `(00 36)`], [1], [1], [`0x037C` \ `(06 7C)`], [1], [64], [Channel], [`0`..`32`], [See @channel-encoding],
    [`0x0037` \ `(00 37)`], [1], [2], [`0x03BC` \ `(07 3C)`], [2], [64], [Note], [`0`..`127`], [Note number when the type is a note; `2F` is B2],
    [`0x0038` \ `(00 38)`], [1], [3], [`0x053C` \ `(0A 3C)`], [8], [64], [MIDI to DIN], [`0`, `1`], [Whether the pad is copied to the DIN output],
    [`0x0039` \ `(00 39)`], [1], [4], [`0x03FC` \ `(07 7C)`], [3], [64], [Mode], [`0`, `1`], [Momentary, toggle],
    [`0x003A` \ `(00 3A)`], [1], [5], [`0x043C` \ `(08 3C)`], [4], [64], [Aftertouch], [`0`..`2`], [Off, channel, polyphonic],
    [`0x003B` \ `(00 3B)`], [1], [6], [`0x047C` \ `(08 7C)`], [5], [64], [Program], [`0`..`127`], [Program number when the type is a program change or a program bank],
    [`0x003C` \ `(00 3C)`], [1], [7], [`0x04BC` \ `(09 3C)`], [6], [64], [MSB], [`0`..`127`], [MSB of the bank],
    [`0x003D` \ `(00 3D)`], [1], [8], [`0x04FC` \ `(09 7C)`], [7], [64], [LSB], [`0`..`127`], [LSB of the bank],
    [`0x003E` \ `(00 3E)`], [1], [9], [`0x057C` \ `(0A 7C)`], [9], [64], [Off colour], [`0`..`16`], [Colour of the pad at rest],
    [`0x003F` \ `(00 3F)`], [1], [10], [`0x05BC` \ `(0B 3C)`], [10], [64], [On colour], [`0`..`16`], [Colour of the pad when it is hit],
  ),
  caption: [The fields of a pad],
)
]


On a pad of type note, the note number sits in the *Note* field, and the program and bank fields stay at zero.

*User preset calculation examples:* a pad record is 11 bytes, and the records are ordered by bank, 16 per bank.
Pad 1 (hardware 1 of bank A) is record `0 * 16 + 0 = 0`; pad 17 (hardware 1 of bank B) is record `1 * 16 + 0 = 16`.

- *Channel* (field offset 1) of pad 1: `0x0035 + 0 * 11 + 1 = 0x0036`, the packed address `00 36`;
- *MIDI to DIN* (field offset 3) of pad 17: `0x0035 + 16 * 11 + 3 = 0x00E8`, the packed address `01 68`.

*Current preset calculation examples:* a pad array holds 64 values, 16 pads and 4 banks. Pad 1 is value
`0 * 16 + 0` of each array; pad 17 is value `1 * 16 + 0`.

- *Channel* (field index 1) of pad 1: `0x033C + 1 * 64 + 0 * 16 + 0 = 0x037C`, the packed address `06 7C`;
- *MIDI to DIN* (field index 8) of pad 17: `0x033C + 8 * 64 + 1 * 16 + 0 = 0x054C`, the packed address `0A 4C`.

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

A pad's aftertouch field applies to the note pads only:

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

#block(breakable: false)[
== DAW buttons

There are a total of 5 virtual DAW buttons. A DAW button record has 13 fields. Each field occupies 1 byte of
memory. It has the same fields as a switch, including the 2 keystroke codes, because a DAW button can be
assigned to a keyboard shortcut instead of a MIDI message.

#kfigure(
  ftable(
    columns: (1.6cm, 1.05cm, 1.15cm, 1.6cm, 1.2cm, 1.45cm, 1.55cm, 1.15cm, 1fr),
    head(
      table.cell(fill: l_green, fhead[*Field Address*]), table.cell(fill: l_green, fhead[*Field size*]),
      table.cell(fill: l_green, fhead[*Field offset*]),
      table.cell(fill: l_blue, fhead[*Array Address*]), table.cell(fill: l_blue, fhead[*Field index*]),
      table.cell(fill: l_blue, fhead[*Array length*]),
      fhead[*Field name*], fhead[*Range*], fhead[*Description*],
    ),
    [`0x0595` \ `(0B 15)`], [1], [0], [`0x005B` \ `(00 5B)`], [0], [5], [Type], [`0`..`4`], [CC, note, program change, program bank, keystroke],
    [`0x0596` \ `(0B 16)`], [1], [1], [`0x0060` \ `(00 60)`], [1], [5], [Channel], [`0`..`32`], [See @channel-encoding],
    [`0x0597` \ `(0B 17)`], [1], [2], [`0x0065` \ `(00 65)`], [2], [5], [Controller], [`0`..`127`], [CC number, note number, program number or bank, depending on the type],
    [`0x0598` \ `(0B 18)`], [1], [3], [`0x006A` \ `(00 6A)`], [3], [5], [Mode], [`0`, `1`], [Momentary, toggle],
    [`0x0599` \ `(0B 19)`], [1], [4], [`0x006F` \ `(00 6F)`], [4], [5], [Program], [`0`..`127`], [Program number when the type is a program bank],
    [`0x059A` \ `(0B 1A)`], [1], [5], [`0x0074` \ `(00 74)`], [5], [5], [MSB], [`0`..`127`], [MSB of the bank, or invert switch],
    [`0x059B` \ `(0B 1B)`], [1], [6], [`0x0079` \ `(00 79)`], [6], [5], [LSB], [`0`..`127`], [LSB of the bank],
    [`0x059C` \ `(0B 1C)`], [1], [7], [`0x0083` \ `(00 83)`], [8], [5], [MIDI to DIN], [`0`, `1`], [Whether the button is copied to the DIN output],
    [`0x059D` \ `(0B 1D)`], [1], [8], [`0x007E` \ `(00 7E)`], [7], [5], [Note], [`0`..`127`], [Note number when the type is a note],
    [`0x059E` \ `(0B 1E)`], [1], [9], [`0x0088` \ `(00 88)`], [9], [5], [Velocity], [`0`..`127`], [Velocity of that note],
    [`0x059F` \ `(0B 1F)`], [1], [10], [`0x008D` \ `(00 8D)`], [10], [5], [unknown], [—], [—],
    [`0x05A0` \ `(0B 20)`], [1], [11], [`0x0092` \ `(00 92)`], [11], [5], [Key 1], [`0`..`83`], [Key code when the type is a keystroke],
    [`0x05A1` \ `(0B 21)`], [1], [12], [`0x0097` \ `(00 97)`], [12], [5], [Key 2], [`0`..`14`], [Modifiers of that keystroke],
  ),
  caption: [The fields of a DAW button],
)
]


The 5 buttons are, in order, *Enter*, *Left*, *Right*, *Up* and *Down*. Their key codes in the reference preset (`25`,
`34`, `35`, `32`, `33`) are the codes of *F2*, *Left*, *Right*, *Up* and *Down*: the factory setting of the Enter button
is a function key.

*User preset calculation examples:* a DAW button record is 13 bytes, and the buttons are not banked: the record
index is the hardware number minus 1. Enter (hardware 1) is record 0; Down (hardware 5) is record 4.

- *Type* (field offset 0) of Enter: `0x0595 + 0 * 13 + 0 = 0x0595`, the packed address `0B 15`;
- *Key 1* (field offset 11) of Down: `0x0595 + 4 * 13 + 11 = 0x05D4`, the packed address `0B 54`.

*Current preset calculation examples:* a DAW button array holds 5 values, 1 per button. Enter is value 0 of each
array; Down is value 4.

- *Type* (field index 0) of Enter: `0x005B + 0 * 5 + 0 + 0 = 0x005B`, the packed address `00 5B`;
- *Key 1* (field index 11) of Down: `0x005B + 11 * 5 + 0 + 4 = 0x0096`, the packed address `01 16`.

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
position of the key on a US keyboard, so the same physical key gives the same code whatever the keyboard layout of the operating
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

#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Patterns", image: red-chapter-banner, l: "chap-pattern")

A pattern is 144 bytes: a 16-byte header that describes the loop, followed by 16 steps of 8 bytes. Two addresses give
access to it:

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    head([*Address*], [*Block*], [*Content*]),
    [`10 00 00 00`], [Current pattern], [The pattern the sequencer is playing],
    [`11 ss 00 00`], [Stored pattern], [Slot `ss`, from 0 to 127],
  ),
  caption: [Addresses of the pattern blocks],
)

Like a patch, a pattern exists twice: a working copy at `0x10000000` that the front panel edits, and a stored copy in
one of the 128 slots. Slot 0 is the pattern displayed as `1` and slot 127 the pattern displayed as `128`; the offset of
one between the stored number and the displayed number is a constant source of confusion when a pattern is written with
a SysEx message and then searched for on the instrument.

== Header

#figure(
  ptable(
    columns: (auto, auto, auto, auto, 1fr),
    head([*Offset*], [*Field*], [*Values*], [*Ref.*], [*Meaning*]),
    [`0`..`2`], [Tempo], [12 bits, see below], [`03 08 04`], [130.0 BPM],
    [`3`], [Patch bank], [`1`..`4`], [`01`], [Bank of the patch of the pattern: `1` = A, `2` = B, `3` = C, `4` = User],
    [`4`], [Patch number], [`0`..`127`], [`67`], [Program number of that patch, 0-based; `67` is `103`, the program number of `A104`],
    [`5`], [Scale], [`0`..`5`], [`01`], [`1/16`, `1/8`, `1/4`, `1/16T`, `1/8T`, `1/4T`],
    [`6`], [Step count], [`15`], [`0F`], [Number of steps minus one; always `15` (16 steps) in factory patterns],
    [`7`], [First step], [`0`..`15`], [`00`], [First step of the loop, minus one],
    [`8`], [Last step], [`0`..`15`], [`0F`], [Last step of the loop, minus one],
    [`9`], [Direction], [`0`..`3`], [`00`], [Forward, backward, ping-pong, random],
    [`10`], [Shuffle], [`50` + offset], [`32`], [Raw value; the displayed shuffle is the value minus `50` (`32` is `50`, that is a shuffle of `0`)],
    [`11`], [Destination], [`0`..`2`], [`00`], [`Int`, `Ext`, `Both`],
    [`12`], [Play flag], [`0` `1`], [`00`], [`0` = the pattern plays, `1` = the steps are blocked],
    [`13`], [Key trigger], [`0`..`2`], [`00`], [`OFF`, `On`, `trn`],
    [`14`..`15`], [Reserved], [`00`], [`00 00`], [],
  ),
  caption: [Header of a pattern],
)

The *Ref.* column is the header of the first factory pattern, in slot 0. Over the 128 factory patterns, the last eight
header bytes are almost constant: the direction is forward in 113 of them, the shuffle offset is zero in 112, the
destination is `Int` in all of them, the first step is `1` in all of them and the play flag is `0` in all of them. The
two bytes that change from one pattern to the other are the patch bank and number, which is expected: a pattern
remembers the patch it was written for.

The tempo is a 12-bit value stored as three nibbles, counted in tenths of a beat per minute above 40:

#figure(
  ptable(
    columns: 1,
    raw("t      = (header[0] << 8) | (header[1] << 4) | header[2]\ntempo  = 40 + t / 10\n\nt      = round((tempo - 40) * 10)\nheader = [ (t >> 8) & 0x0F, (t >> 4) & 0x0F, t & 0x0F ]"),
  ),
  caption: [Encoding of the tempo],
)

#figure(
  table(
    align: left,
    columns: (auto, auto, auto),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    head([*Tempo*], [*t*], [*Header bytes*]),
    [`40.0`], [`0`], [`00 00 00`],
    [`80.0`], [`400`], [`01 09 00`],
    [`120.0`], [`800`], [`03 02 00`],
    [`130.0`], [`900`], [`03 08 04`],
    [`200.0`], [`1600`], [`06 04 00`],
    [`300.0`], [`2600`], [`0A 02 08`],
  ),
  caption: [Six tempos and their encoding],
)

The instrument accepts tempos from 40 to 300, that is a `t` from `0` to `2600`. Twelve bits are exactly what that range
needs, and the header stores the value as three nibbles at offsets `0` to `2`.

== Steps

The 16 steps start at offset 16 and each one occupies 8 bytes, so step `n` (0 to 15) is at offsets `16 + 8n` to
`23 + 8n`.

#figure(
  ptable(
    columns: (auto, auto, auto, auto, 1fr),
    head([*Offset*], [*Field*], [*Values*], [*Ref.*], [*Meaning*]),
    [`+0`], [Note switch], [`0` `1`], [`01`], [`1` = the step emits a note, `0` = silent step],
    [`+1`], [Note number], [`0`..`127`], [`24`], [MIDI note number (`24` is the C two octaves below middle C)],
    [`+2`], [Gate], [`9`..`99`], [`54`], [Gate length, in percent, minus one (`54` is displayed as `85`)],
    [`+3`], [Glide], [`0` `1`], [`00`], [`1` = the note glides from the previous one],
    [`+4`], [PRM switch], [`0` `1`], [`00`], [`1` = the step applies the parameter value below],
    [`+5`], [Parameter ID], [`0`..`59`], [`19`], [Parameter automated by the step, from the #link(<chap-param-ids>)[list of parameter IDs] (`19` is `FLT_CUTOFF`)],
    [`+6`], [Parameter value, high nibble], [`0`..`15`], [`06`], [High nibble of the value],
    [`+7`], [Parameter value, low nibble], [`0`..`15`], [`0D`], [Low nibble of the value (`06 0D` is `109`)],
  ),
  caption: [Content of a step record],
)

The *Ref.* column is the first step of the first factory pattern: `01 24 54 00 00 19 06 0D`, a C note with a gate of
85 %, no glide, and a parameter value of `109` for the cutoff, but with its PRM switch off.

That last detail matters for anyone who copies patterns: *all eight bytes of a step are always meaningful*. The
parameter ID and value of that step are stored even though the switch is off, and so are the gate and the glide of a
step whose note is off. A step therefore has two independent switches — the note and PRM — and zeroing the fields of an
inactive one is a change of the pattern, not a neutral write.

The `+4` PRM switch is the PRM button of the unit. The `+5` parameter ID is an internal identifier used only by the
sequencer; it is neither the MIDI CC number nor the parameter offset of the patch (#cref(<chap-param-ids>)).

The gate is stored one below its displayed value, like several other lengths of this format. The displayed range is 10
to 100 %, so the stored range is 9 to 99.

== A complete pattern

The table below decodes the 16 steps of the first factory pattern, slot 0, whose header was given above. Gates and
parameter values are the *stored* values; the display adds one to the gate and shows nothing for the parameter ID,
which has no name on the instrument. In the payload column, the colours group the fields: note (green), gate (blue),
glide (purple), PRM switch, parameter ID and value (orange).

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, auto, auto),
    head([*Step*], [*Payload*], [*Note*], [*Gate*], [*Glide*], [*Parameter ID*], [*Value*]),
    [`1`], [#pattern-step("01 24 54 00 00 19 06 0D")], [`0x24 (36)`], [`0x54 (84)`], [—], [off], [`0x6D (109)`],
    [`2`], [#pattern-step("01 24 45 00 00 00 00 00")], [`0x24 (36)`], [`0x45 (69)`], [—], [off], [`0x00 (0)`],
    [`3`], [#pattern-step("01 3C 45 00 01 04 00 03")], [`0x3C (60)`], [`0x45 (69)`], [—], [`4`, `LFO_MODE`], [`0x03 (3)`],
    [`4`], [#pattern-step("01 24 45 00 00 19 08 04")], [`0x24 (36)`], [`0x45 (69)`], [—], [off], [`0x84 (132)`],
    [`5`], [#pattern-step("01 30 1D 00 01 04 00 02")], [`0x30 (48)`], [`0x1D (29)`], [—], [`4`, `LFO_MODE`], [`0x02 (2)`],
    [`6`], [#pattern-step("01 33 45 00 00 19 07 09")], [`0x33 (51)`], [`0x45 (69)`], [—], [off], [`0x79 (121)`],
    [`7`], [#pattern-step("01 2E 1D 01 00 00 00 00")], [`0x2E (46)`], [`0x1D (29)`], [on], [off], [`0x00 (0)`],
    [`8`], [#pattern-step("01 3C 45 00 01 19 03 08")], [`0x3C (60)`], [`0x45 (69)`], [—], [`25`, `FLT_CUTOFF`], [`0x38 (56)`],
    [`9`], [#pattern-step("01 3C 1D 00 01 19 01 0F")], [`0x3C (60)`], [`0x1D (29)`], [—], [`25`, `FLT_CUTOFF`], [`0x1F (31)`],
    [`10`], [#pattern-step("01 2B 1D 00 00 19 07 01")], [`0x2B (43)`], [`0x1D (29)`], [—], [off], [`0x71 (113)`],
    [`11`], [#pattern-step("01 33 1D 00 00 23 07 0F")], [`0x33 (51)`], [`0x1D (29)`], [—], [off], [`0x7F (127)`],
    [`12`], [#pattern-step("01 24 1D 00 00 19 09 04")], [`0x24 (36)`], [`0x1D (29)`], [—], [off], [`0x94 (148)`],
    [`13`], [#pattern-step("01 30 1D 00 01 17 04 0E")], [`0x30 (48)`], [`0x1D (29)`], [—], [`23`, `FLT_ATTACK2`], [`0x4E (78)`],
    [`14`], [#pattern-step("01 3C 45 00 00 00 00 00")], [`0x3C (60)`], [`0x45 (69)`], [—], [off], [`0x00 (0)`],
    [`15`], [#pattern-step("01 3F 1D 01 00 23 09 0F")], [`0x3F (63)`], [`0x1D (29)`], [on], [off], [`0x9F (159)`],
    [`16`], [#pattern-step("01 3C 45 00 01 17 02 07")], [`0x3C (60)`], [`0x45 (69)`], [—], [`23`, `FLT_ATTACK2`], [`0x27 (39)`],
  ),
  caption: [The 16 steps of the first factory pattern],
)

Three remarks on this pattern. First, the note numbers are not limited to a musical range: `24` is the lowest note the
unit will play and `60` the middle C, and the steps alternate between them freely. Second, the PRM switch is off on
most steps but their parameter values are populated anyway, which is exactly the residual data described above. Third,
the steps that do carry a PRM (`3`, `5`, `8`, `9`, `13`, `16`) alternate between two parameters, the cutoff and the
attack of the second envelope, which is the usual way a pattern of this instrument breathes.

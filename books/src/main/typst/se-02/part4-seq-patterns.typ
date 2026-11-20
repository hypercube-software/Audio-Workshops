#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Patterns", l: "chap-pattern")

A pattern is 144 bytes: a 16-byte header that describes the loop, followed by 16 steps of 8 bytes. Two addresses give
access to it:

#figure(
    table(
        align: left,
        columns: (auto, auto, auto, 1fr),
        fill: (x, y) => if y == 0 { luma(230) },
        stroke: 0.5pt,
        head([*Packed address*], [*Packed Size*], [*Block*], [*Content*]),
        [`10 00 00 00`], [`00 00 01 10`], [Pattern edit buffer], [The pattern the sequencer is playing],
        [`11 nn 00 00`], [`00 00 01 10`], [Stored pattern], [Number `nn`, from 0 to 127],
    ),
    caption: [Addresses of the pattern blocks],
)

Like a patch, a pattern exists twice: the *pattern edit buffer* at `0x10000000`, which the sequencer plays, and 128
stored patterns in flash. Pattern 0 is displayed as `1` and pattern 127 as `128`.

A stored pattern is played by copying it into the edit buffer: read its 144 bytes at `0x11nn0000`, then write them at
`0x10000000`.

== Header

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed Size*], [*Field*], [*Values*], [*Ref.*], [*Meaning*]),
        [`10 00 00 00`], [`00 00 00 03`],
        [Tempo],
        [12 bits, see #cref(<chap-address>)],
        [`03 08 04`],
        [130 BPM],
        [`10 00 00 03`], [`00 00 00 01`],
        [Patch bank],
        [`1`..`4`],
        [`01`],
        [Bank of the patch of the pattern: `1` = A, `2` = B, `3` = C, `4` = User],
        [`10 00 00 04`], [`00 00 00 01`],
        [Patch number],
        [`0`..`127`],
        [`67`],
        [Program number of that patch, 0-based; In this example `0x0167` is `103`, the program number of `A104`],
        [`10 00 00 05`], [`00 00 00 01`],
        [Scale],
        [`0`..`5`],
        [`01`],
        [`1/16`, `1/8`, `1/4`, `1/16T`, `1/8T`, `1/4T`],
        [`10 00 00 06`], [`00 00 00 01`],
        [Step count],
        [`15`],
        [`0F`],
        [Number of steps minus one; always `15` (16 steps) in factory patterns],
        [`10 00 00 07`], [`00 00 00 01`],
        [First step],
        [`0`..`15`],
        [`00`],
        [First step of the loop, minus one],
        [`10 00 00 08`], [`00 00 00 01`],
        [Last step],
        [`0`..`15`],
        [`0F`],
        [Last step of the loop, minus one],
        [`10 00 00 09`], [`00 00 00 01`],
        [Direction],
        [`0`..`3`],
        [`00`],
        [Forward, backward, ping-pong, random],
        [`10 00 00 0A`], [`00 00 00 01`],
        [Shuffle],
        [`50` + offset],
        [`32`],
        [Raw value; the displayed shuffle is the value minus `50` (`32` is `50`, that is a shuffle of `0`)],
        [`10 00 00 0B`], [`00 00 00 01`],
        [Destination],
        [`0`..`2`],
        [`00`],
        [`Int`, `Ext`, `Both`],
        [`10 00 00 0C`], [`00 00 00 01`],
        [Play flag],
        [`0` `1`],
        [`00`],
        [`0` = the pattern plays, `1` = the steps are blocked],
        [`10 00 00 0D`], [`00 00 00 01`],
        [Key trigger],
        [`0`..`2`],
        [`00`],
        [`OFF`, `On`, `trn`],
        [`10 00 00 0E`], [`00 00 00 02`],
        [Reserved],
        [`00`],
        [`00 00`],
        [],
    ),
    caption: [Header of a pattern],
)

The *Ref.* column is the header of the first factory pattern, pattern 0. Over the 128 factory patterns, the last eight
header bytes are almost constant: the direction is forward in 113 of them, the shuffle offset is zero in 112, the
destination is `Int` in all of them, the first step is `1` in all of them and the play flag is `0` in all of them. The
two bytes that change from one pattern to the other are the patch bank and number, which is expected: a pattern
remembers the patch it was written for.

The tempo field is a count in tenths of a beat per minute above 40, stored as 3 nibbles; its encoding is described
in #cref(<chap-address>).

== Steps

The 16 steps start at `0x10000010` and each one occupies 8 bytes: step 1 is at `0x10000010`, step 2 at `0x10000018`,
and so on. The table gives the addresses inside step 1.

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed Size*], [*Field*], [*Values*], [*Ref.*], [*Meaning*]),
        [`10 00 00 10`], [`00 00 00 01`],
        [Note switch],
        [`0` `1`],
        [`01`],
        [`1` = the step emits a note, `0` = silent step],
        [`10 00 00 11`], [`00 00 00 01`],
        [Note number],
        [`0`..`127`],
        [`24`],
        [MIDI note number (`24` is `C2`)],
        [`10 00 00 12`], [`00 00 00 01`],
        [Gate],
        [`9`..`99`],
        [`54`],
        [Gate length, in percent, minus one (`54` is displayed as `85`)],
        [`10 00 00 13`], [`00 00 00 01`],
        [Glide],
        [`0` `1`],
        [`00`],
        [`1` = the note glides from the previous one],
        [`10 00 00 14`], [`00 00 00 01`],
        [PRM switch],
        [`0` `1`],
        [`00`],
        [`1` = the step applies the parameter value below],
        [`10 00 00 15`], [`00 00 00 01`],
        [Parameter ID],
        [`0`..`59`],
        [`19`],
        [Parameter automated by the step, from the #link(<chap-param-ids>)[list of parameter IDs] (`19` is `FLT_CUTOFF`)],
        [`10 00 00 16`], [`00 00 00 01`],
        [Parameter value, high nibble],
        [`0`..`15`],
        [`06`],
        [High nibble of the value],
        [`10 00 00 17`], [`00 00 00 01`],
        [Parameter value, low nibble],
        [`0`..`15`],
        [`0D`],
        [Low nibble of the value (`06 0D` is `109`)],
    ),
    caption: [Content of a step record],
)

The *Ref.* column is the first step of the first factory pattern:

#figure(
    pattern-step("01 24 54 00 00 19 06 0D"),
    caption: [The reference step],
)

It plays a `C2` note with a gate of `85` %, no glide, and a parameter value of `109` for the cutoff, with its PRM
switch off.

That last detail matters for anyone who copies patterns: *all eight bytes of a step are always meaningful*. The
parameter ID and value of that step are stored even though the switch is off, and so are the gate and the glide of a
step whose note is off. A step therefore has two independent switches — the note and PRM — and zeroing the fields of an
inactive one is a change of the pattern, not a neutral write.

A step whose note switch is `0` is silent, and its other bytes keep their content. Step 10 of pattern 5:

#figure(
    pattern-step("00 2D 63 00 01 23 08 09"),
    caption: [A step with the note switch off],
)

The note is `A2`, the gate `99`, and the step carries a value for `OSC_ENV1` even though it plays nothing.

The PRM switch is the PRM button of the unit. The parameter ID is an internal identifier used only by the
sequencer; it is neither the MIDI CC number nor the address of the patch parameter (#cref(<chap-param-ids>)).

The gate is stored one below its displayed value, like several other lengths of this format. The displayed range is 10
to 100 %, so the stored range is 9 to 99.

== A complete pattern

The table below decodes the 16 steps of the first factory pattern, pattern 0, whose header was given above. Gates and
parameter values are the *stored* values; the display adds one to the gate and shows nothing for the parameter ID,
which has no name on the instrument. In the payload column, the colours group the fields: note (green), gate (blue),
glide (purple), PRM switch, parameter ID and value (orange).

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto, auto, auto),
        head([*Step*], [*Payload*], [*Note*], [*Gate*], [*Glide*], [*Parameter ID*], [*Value*]),
        [`1`],
        [#pattern-step("01 24 54 00 00 19 06 0D")],
        [`C2`],
        [`0x54 (84)`],
        [—],
        [off],
        [`0x6D (109)`],
        [`2`],
        [#pattern-step("01 24 45 00 00 00 00 00")],
        [`C2`],
        [`0x45 (69)`],
        [—],
        [off],
        [`0x00 (0)`],
        [`3`],
        [#pattern-step("01 3C 45 00 01 04 00 03")],
        [`C4`],
        [`0x45 (69)`],
        [—],
        [`4`, `LFO_MODE`],
        [`0x03 (3)`],
        [`4`],
        [#pattern-step("01 24 45 00 00 19 08 04")],
        [`C2`],
        [`0x45 (69)`],
        [—],
        [off],
        [`0x84 (132)`],
        [`5`],
        [#pattern-step("01 30 1D 00 01 04 00 02")],
        [`C3`],
        [`0x1D (29)`],
        [—],
        [`4`, `LFO_MODE`],
        [`0x02 (2)`],
        [`6`],
        [#pattern-step("01 33 45 00 00 19 07 09")],
        [`D#3`],
        [`0x45 (69)`],
        [—],
        [off],
        [`0x79 (121)`],
        [`7`],
        [#pattern-step("01 2E 1D 01 00 00 00 00")],
        [`A#2`],
        [`0x1D (29)`],
        [on],
        [off],
        [`0x00 (0)`],
        [`8`],
        [#pattern-step("01 3C 45 00 01 19 03 08")],
        [`C4`],
        [`0x45 (69)`],
        [—],
        [`25`, `FLT_CUTOFF`],
        [`0x38 (56)`],
        [`9`],
        [#pattern-step("01 3C 1D 00 01 19 01 0F")],
        [`C4`],
        [`0x1D (29)`],
        [—],
        [`25`, `FLT_CUTOFF`],
        [`0x1F (31)`],
        [`10`],
        [#pattern-step("01 2B 1D 00 00 19 07 01")],
        [`G2`],
        [`0x1D (29)`],
        [—],
        [off],
        [`0x71 (113)`],
        [`11`],
        [#pattern-step("01 33 1D 00 00 23 07 0F")],
        [`D#3`],
        [`0x1D (29)`],
        [—],
        [off],
        [`0x7F (127)`],
        [`12`],
        [#pattern-step("01 24 1D 00 00 19 09 04")],
        [`C2`],
        [`0x1D (29)`],
        [—],
        [off],
        [`0x94 (148)`],
        [`13`],
        [#pattern-step("01 30 1D 00 01 17 04 0E")],
        [`C3`],
        [`0x1D (29)`],
        [—],
        [`23`, `FLT_ATTACK2`],
        [`0x4E (78)`],
        [`14`],
        [#pattern-step("01 3C 45 00 00 00 00 00")],
        [`C4`],
        [`0x45 (69)`],
        [—],
        [off],
        [`0x00 (0)`],
        [`15`],
        [#pattern-step("01 3F 1D 01 00 23 09 0F")],
        [`D#4`],
        [`0x1D (29)`],
        [on],
        [off],
        [`0x9F (159)`],
        [`16`],
        [#pattern-step("01 3C 45 00 01 17 02 07")],
        [`C4`],
        [`0x45 (69)`],
        [—],
        [`23`, `FLT_ATTACK2`],
        [`0x27 (39)`],
    ),
    caption: [The 16 steps of the first factory pattern],
)

Three remarks on this pattern. First, the note numbers are not limited to a musical range: `C2` is the lowest note the
unit will play and `C4` the middle C, and the steps alternate between them freely. Second, the PRM switch is off on
most steps but their parameter values are populated anyway, which is exactly the residual data described above. Third,
the steps that do carry a PRM (`3`, `5`, `8`, `9`, `13`, `16`) alternate between two parameters, the cutoff and the
attack of the second envelope, which is the usual way a pattern of this instrument breathes.

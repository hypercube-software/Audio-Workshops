#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Patch parameters", image: red-chapter-banner, l: "chap-params")

The 88 parameters of a patch are grouped in eight blocks. Each table gives everything needed to read or
write 1 parameter:

- The *Packed address* column is the address of the MIDI frame, in the current patch (`0x05000000`). For a
    stored preset, the first byte becomes `06` and the second one is the preset number `nn`.
- The *Size* column is the size of the value as it is written in the payload, encoded for the `RQ1` on
    4 bytes, as described in #cref(<chap-sysex>). A 8-bit value takes 2 bytes, so the field is `00 00 00 02`;
    the 12-bit tempo takes 3 bytes, so the field is `00 00 00 03`.
- The *Range* column gives the domain of the stored value.
- The *CC* column the controller that edits it on the unit
- The *Ref.* column the value of the reference patch `Inbound`.

A 8-bit value is 2 nibbles: the first byte holds the 4 most significant bits, the second byte the 4 least
significant bits.

== Two identifiers for one parameter

A parameter is designated by two numbers that have nothing in common. Do not confuse them:

#figure(
    table(
        align: left,
        columns: (auto, auto, auto),
        fill: (x, y) => if y == 0 { luma(230) },
        stroke: 0.5pt,
        head([*Parameter*], [*Internal ID*], [*CC*]), [`FLT_CUTOFF`], [`25`],
        [`74`], [`OSC_WAVEFORM3`], [`38`],
        [`26`], [`DLY_AMOUNT`], [`0`],
        [`91`],
    ),
    caption: [Three parameters with their two identifiers],
)

The internal ID is the identifier used by the step sequencer, and the CC number is only used in MIDI control change event.
The two numbers are independent: the internal ID is not derived from the CC number. See #cref(<chap-param-ids>).

== Value domains

A stored value is a 8-bit number. The domain depends on the parameter:

- `0` to `255` for the continuous parameters (cutoff, levels, times);
- `0` to `127` for the seven parameters that a controller drives directly;
- `0` to a small maximum for switches and selectors (for example `0` to `1` for a switch, `0` to `5` for a waveform).

When a controller moves a parameter, the instrument converts the 7-bit CC value into the domain of the parameter:

#figure(
    ptable(
        columns: 1,
        raw("domain 0..255 : value = min(255, round(cc * 256 / 127))\ndomain 0..127 : value = cc\ndomain 0..max : value = min(max, round(cc * max / 127))"),
    ),
    caption: [Conversion from a controller value to a stored value],
)

The first line maps CC 0 to `0`, CC 64 to `129` and CC 127 to `255`. That conversion is needed when a message is built
from a controller value, for instance when a performance captured on the MIDI input is replayed. Values read from a
dump are already internal and must be written as they are: converting them a second time would shift every continuous
parameter.

== Common

The *common* block holds the settings that are not part of the sound itself: bend range, sensitivities, volume and
transposition.

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed Size*], [*Parameter*], [*Range*], [*CC*], [*Ref.*], [*Meaning*]),
        //@@Common@@
        [`05 00 00 00`],
        [`00 00 00 02`],
        [`COM_BENDRANGE`],
        [`0`..`24`],
        [`87`],
        [`12`],
        [Bend range, in semitones],
        [`05 00 00 02`],
        [`00 00 00 02`],
        [`COM_MOD_SENS`],
        [`0`..`127`],
        [`80`],
        [`0`],
        [Modulation lever sensitivity],
        [`05 00 00 04`],
        [`00 00 00 02`],
        [`COM_AFT_SENS1`],
        [`0`..`127`],
        [`84`],
        [`0`],
        [Aftertouch sensitivity, oscillator 1],
        [`05 00 00 06`],
        [`00 00 00 02`],
        [`COM_AFT_SENS2`],
        [`0`..`127`],
        [`85`],
        [`0`],
        [Aftertouch sensitivity, oscillator 2],
        [`05 00 00 08`],
        [`00 00 00 02`],
        [`COM_DYNAMICS`],
        [`0`..`127`],
        [`81`],
        [`0`],
        [Keyboard velocity sensitivity],
        [`05 00 00 0A`],
        [`00 00 00 02`],
        [`COM_VOLUME`],
        [`0`..`127`],
        [`115`],
        [`127`],
        [Volume of the patch],
        [`05 00 00 0C`],
        [`00 00 00 02`],
        [`COM_PWM_LF_RATE`],
        [`0`..`127`],
        [`116`],
        [`0`],
        [PWM LFO rate],
        [`05 00 00 0E`],
        [`00 00 00 02`],
        [`COM_PWM_LF_DPTH`],
        [`0`..`127`],
        [`117`],
        [`0`],
        [PWM LFO depth],
        [`05 00 00 22`],
        [`00 00 00 02`],
        [`COM_OCT`],
        [`0`..`6`],
        [`112`],
        [`3`],
        [Octave shift, `3` = no shift],
        [`05 00 00 24`],
        [`00 00 00 02`],
        [`COM_TRNS`],
        [`0`..`24`],
        [`113`],
        [`12`],
        [Transposition in semitones, `12` = none],
        [`05 00 00 26`],
        [`00 00 00 02`],
        [`COM_TRNS_SW`],
        [`0`..`1`],
        [`114`],
        [`0`],
        [Transposition on/off],
    ),
    caption: [Common parameters],
)

Octave (`0x05000022`) and transposition (`0x05000024`) are stored as absolute positions on a scale, not as signed values: a
transposition of `12` means "no transposition", `11` means one semitone down and `13` one semitone up. A user interface
that displays a signed transposition must subtract 12 from the stored value.

== Control

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed Size*], [*Parameter*], [*Range*], [*CC*], [*Ref.*], [*Meaning*]),
        //@@Control@@
        [`05 00 00 28`],
        [`00 00 00 02`],
        [`CTRL_GLIDE`],
        [`0`..`255`],
        [`5`],
        [`153`],
        [Glide time],
        [`05 00 00 2A`],
        [`00 00 00 02`],
        [`CTRL_GLIDE_TYPE`],
        [`0`..`2`],
        [`9`],
        [`1`],
        [Glide curve],
        [`05 00 00 2C`],
        [`00 00 00 02`],
        [`CTRL_WHL`],
        [`0`..`255`],
        [`13`],
        [`90`],
        [Wheel amount],
    ),
    caption: [Control parameters],
)

== Oscillators

Three oscillators share the next block. Each one has a footage (its *range*) and a waveform; the first two also have a
fine tune. Footage and waveform are selectors with six positions, stored `0` to `5`.

#kfigure(
    ptable(
        columns: (auto, auto, auto, auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed Size*], [*Parameter*], [*Range*], [*CC*], [*Ref.*], [*Meaning*]),
        //@@Oscillators@@
        [`05 00 00 32`],
        [`00 00 00 02`],
        [`OSC_RANGE1`],
        [`0`..`5`],
        [`22`],
        [`2`],
        [Footage of oscillator 1],
        [`05 00 00 34`],
        [`00 00 00 02`],
        [`OSC_RANGE2`],
        [`0`..`5`],
        [`19`],
        [`5`],
        [Footage of oscillator 2],
        [`05 00 00 36`],
        [`00 00 00 02`],
        [`OSC_RANGE3`],
        [`0`..`5`],
        [`25`],
        [`2`],
        [Footage of oscillator 3],
        [`05 00 00 3A`],
        [`00 00 00 02`],
        [`OSC_FINE1`],
        [`0`..`255`],
        [`27`],
        [`125`],
        [Fine tune of oscillator 1, `128` = centre],
        [`05 00 00 3C`],
        [`00 00 00 02`],
        [`OSC_FINE2`],
        [`0`..`255`],
        [`28`],
        [`126`],
        [Fine tune of oscillator 2, `128` = centre],
        [`05 00 00 3E`],
        [`00 00 00 02`],
        [`OSC_WAVEFORM1`],
        [`0`..`5`],
        [`24`],
        [`2`],
        [Waveform of oscillator 1],
        [`05 00 00 40`],
        [`00 00 00 02`],
        [`OSC_WAVEFORM2`],
        [`0`..`5`],
        [`20`],
        [`3`],
        [Waveform of oscillator 2],
        [`05 00 00 42`],
        [`00 00 00 02`],
        [`OSC_WAVEFORM3`],
        [`0`..`5`],
        [`26`],
        [`0`],
        [Waveform of oscillator 3],
        [`05 00 00 44`],
        [`00 00 00 02`],
        [`OSC_SYNC`],
        [`0`..`1`],
        [`21`],
        [`0`],
        [Oscillator sync],
        [`05 00 00 46`],
        [`00 00 00 02`],
        [`OSC_ENV1`],
        [`0`..`255`],
        [`29`],
        [`128`],
        [Envelope amount of oscillator 1],
        [`05 00 00 48`],
        [`00 00 00 02`],
        [`OSC_KYBD`],
        [`0`..`1`],
        [`30`],
        [`1`],
        [Keyboard tracking of the oscillators],
        [`05 00 00 4A`],
        [`00 00 00 02`],
        [`OSC_XMOD`],
        [`0`..`2`],
        [`31`],
        [`2`],
        [Cross modulation mode],
    ),
    caption: [Oscillator parameters],
)

The address increases with the oscillator number for the ranges (`0x05000032`, `0x05000034`, `0x05000036`), but the CC numbers follow the
panel layout instead (`22`, `19`, `25`). Fine tunes are at `0x0500003A` and `0x0500003C`, waveforms from
`0x0500003E` to `0x05000042`.


== Cross modulation

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed Size*], [*Parameter*], [*Range*], [*CC*], [*Ref.*], [*Meaning*]),
        //@@Cross modulation@@
        [`05 00 00 50`],
        [`00 00 00 02`],
        [`XMOD_O2FLT`],
        [`0`..`255`],
        [`16`],
        [`0`],
        [Cross modulation, oscillator 2 to the filter],
        [`05 00 00 52`],
        [`00 00 00 02`],
        [`XMOD_O3TO`],
        [`0`..`255`],
        [`17`],
        [`0`],
        [Cross modulation, oscillator 3 to the oscillators],
        [`05 00 00 54`],
        [`00 00 00 02`],
        [`XMOD_O3PW`],
        [`0`..`255`],
        [`18`],
        [`0`],
        [Cross modulation, oscillator 3 to the pulse width],
    ),
    caption: [Cross modulation parameters],
)

The mode at `0x0500004A` (`OSC_XMOD`, `0` to `2`) enables the section; the three amounts are continuous.

== Mixer

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed Size*], [*Parameter*], [*Range*], [*CC*], [*Ref.*], [*Meaning*]),
        //@@Mixer@@
        [`05 00 00 5A`],
        [`00 00 00 02`],
        [`MIX_OSC1`],
        [`0`..`255`],
        [`48`],
        [`255`],
        [Level of oscillator 1 in the mixer],
        [`05 00 00 5C`],
        [`00 00 00 02`],
        [`MIX_OSC2`],
        [`0`..`255`],
        [`49`],
        [`0`],
        [Level of oscillator 2 in the mixer],
        [`05 00 00 5E`],
        [`00 00 00 02`],
        [`MIX_OSC3`],
        [`0`..`255`],
        [`50`],
        [`0`],
        [Level of oscillator 3 in the mixer],
        [`05 00 00 60`],
        [`00 00 00 02`],
        [`MIX_FEEDBACK`],
        [`0`..`255`],
        [`51`],
        [`0`],
        [Feedback amount of the mixer],
        [`05 00 00 62`],
        [`00 00 00 02`],
        [`MIX_NOISE`],
        [`0`..`255`],
        [`41`],
        [`0`],
        [Level of the noise generator],
    ),
    caption: [Mixer parameters],
)

The feedback amount (`0x05000060`) is the only one of the five that does not come from an oscillator or from the noise
generator.

== Filter

15 parameters drive the filter and its two envelopes. They are by far the most animated part of a patch: in the
128 factory patterns, more than half of the automated steps target a filter parameter.

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed Size*], [*Parameter*], [*Range*], [*CC*], [*Ref.*], [*Meaning*]),
        //@@Filter@@
        [`05 00 00 6E`],
        [`00 00 00 02`],
        [`FLT_CUTOFF`],
        [`0`..`255`],
        [`74`],
        [`134`],
        [Cutoff frequency],
        [`05 00 00 70`],
        [`00 00 00 02`],
        [`FLT_ATTACK1`],
        [`0`..`255`],
        [`47`],
        [`0`],
        [Attack time of the filter envelope 1],
        [`05 00 00 72`],
        [`00 00 00 02`],
        [`FLT_ATTACK2`],
        [`0`..`255`],
        [`73`],
        [`0`],
        [Attack time of the filter envelope 2],
        [`05 00 00 74`],
        [`00 00 00 02`],
        [`FLT_EMPHASIS`],
        [`0`..`255`],
        [`71`],
        [`180`],
        [Resonance (emphasis)],
        [`05 00 00 76`],
        [`00 00 00 02`],
        [`FLT_DECAY1`],
        [`0`..`255`],
        [`52`],
        [`86`],
        [Decay time of the filter envelope 1],
        [`05 00 00 78`],
        [`00 00 00 02`],
        [`FLT_DECAY2`],
        [`0`..`255`],
        [`75`],
        [`136`],
        [Decay time of the filter envelope 2],
        [`05 00 00 7A`],
        [`00 00 00 02`],
        [`FLT_KEY13`],
        [`0`..`1`],
        [`57`],
        [`0`],
        [Keyboard tracking, filter envelope 1 to 3],
        [`05 00 00 7C`],
        [`00 00 00 02`],
        [`FLT_KEY23`],
        [`0`..`1`],
        [`58`],
        [`0`],
        [Keyboard tracking, filter envelope 2 to 3],
        [`05 00 00 7E`],
        [`00 00 00 02`],
        [`FLT_SUSTAIN1`],
        [`0`..`255`],
        [`53`],
        [`0`],
        [Sustain level of the filter envelope 1],
        [`05 00 01 00`],
        [`00 00 00 02`],
        [`FLT_SUSTAIN2`],
        [`0`..`255`],
        [`56`],
        [`50`],
        [Sustain level of the filter envelope 2],
        [`05 00 01 02`],
        [`00 00 00 02`],
        [`FLT_CONTOUR`],
        [`0`..`255`],
        [`59`],
        [`81`],
        [Contour amount of the filter envelope],
        [`05 00 01 04`],
        [`00 00 00 02`],
        [`FLT_MTRIG`],
        [`0`..`1`],
        [`60`],
        [`1`],
        [Multiple trigger],
        [`05 00 01 06`],
        [`00 00 00 02`],
        [`FLT_NORM`],
        [`0`..`1`],
        [`61`],
        [`1`],
        [Normal / inverted envelope],
        [`05 00 01 08`],
        [`00 00 00 02`],
        [`FLT_REL`],
        [`0`..`2`],
        [`62`],
        [`2`],
        [Release mode],
        [`05 00 01 0A`],
        [`00 00 00 02`],
        [`FLT_GATE`],
        [`0`..`1`],
        [`63`],
        [`0`],
        [Gate mode of the filter envelope],
    ),
    caption: [Filter parameters],
)

== LFO

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed Size*], [*Parameter*], [*Range*], [*CC*], [*Ref.*], [*Meaning*]),
        //@@LFO@@
        [`05 00 01 16`],
        [`00 00 00 02`],
        [`LFO_RATE`],
        [`0`..`255`],
        [`102`],
        [`206`],
        [LFO rate],
        [`05 00 01 18`],
        [`00 00 00 02`],
        [`LFO_OSC`],
        [`0`..`255`],
        [`103`],
        [`0`],
        [LFO amount on the oscillators],
        [`05 00 01 1A`],
        [`00 00 00 02`],
        [`LFO_WAVE`],
        [`0`..`8`],
        [`104`],
        [`3`],
        [LFO waveform],
        [`05 00 01 1C`],
        [`00 00 00 02`],
        [`LFO_FILTER`],
        [`0`..`255`],
        [`105`],
        [`0`],
        [LFO amount on the filter],
        [`05 00 01 1E`],
        [`00 00 00 02`],
        [`LFO_OSC_SEL`],
        [`0`..`2`],
        [`106`],
        [`1`],
        [Oscillator selection for the LFO],
        [`05 00 01 20`],
        [`00 00 00 02`],
        [`LFO_FLT`],
        [`0`..`2`],
        [`107`],
        [`1`],
        [Filter selection for the LFO],
        [`05 00 01 22`],
        [`00 00 00 02`],
        [`LFO_MODE`],
        [`0`..`2`],
        [`108`],
        [`2`],
        [LFO mode],
        [`05 00 01 24`],
        [`00 00 00 02`],
        [`LFO_SYNC`],
        [`0`..`3`],
        [`109`],
        [`3`],
        [LFO key sync],
    ),
    caption: [LFO parameters],
)

== Delay

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed Size*], [*Parameter*], [*Range*], [*CC*], [*Ref.*], [*Meaning*]),
        //@@Delay@@
        [`05 00 01 2A`],
        [`00 00 00 02`],
        [`DLY_TIME`],
        [`0`..`255`],
        [`82`],
        [`255`],
        [Delay time],
        [`05 00 01 2C`],
        [`00 00 00 02`],
        [`DLY_REGEN`],
        [`0`..`255`],
        [`83`],
        [`105`],
        [Delay regeneration],
        [`05 00 01 2E`],
        [`00 00 00 02`],
        [`DLY_AMOUNT`],
        [`0`..`255`],
        [`91`],
        [`0`],
        [Delay level],
    ),
    caption: [Delay parameters],
)

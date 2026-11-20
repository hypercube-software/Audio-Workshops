#import "../common/settings.typ": *
#import "lib.typ": *

#part("Sequencer")

#chapter("Sequencer parameters", l: "chap-param-ids")

The step sequencer can automate 60 of the parameters of a patch. A step stores the automated parameter as a *sequencer
 parameter ID*. This ID is an internal identifier: it is not the MIDI CC number.

The table below lists the 60 parameters with their ID, their MIDI CC number, the range of the stored value,
and a description.

#figure(
  ptable(
    columns: (auto, auto, auto, auto, 1fr),
    head([*ID*], [*Parameter*], [*Related CC*], [*Range*], [*Description*]),
//@@Parameter IDs@@
    [`0x00 (0)`], [`DLY_AMOUNT`], [`91`], [`0`..`255`], [Delay level],
    [`0x01 (1)`], [`DLY_REGEN`], [`83`], [`0`..`255`], [Delay regeneration],
    [`0x02 (2)`], [`DLY_TIME`], [`82`], [`0`..`255`], [Delay time],
    [`0x03 (3)`], [`LFO_SYNC`], [`109`], [`0`..`3`], [LFO key sync],
    [`0x04 (4)`], [`LFO_MODE`], [`108`], [`0`..`2`], [LFO mode],
    [`0x05 (5)`], [`LFO_FLT`], [`107`], [`0`..`2`], [Filter selection for the LFO],
    [`0x06 (6)`], [`LFO_OSC_SEL`], [`106`], [`0`..`2`], [Oscillator selection for the LFO],
    [`0x07 (7)`], [`LFO_FILTER`], [`105`], [`0`..`255`], [LFO amount on the filter],
    [`0x08 (8)`], [`LFO_WAVE`], [`104`], [`0`..`8`], [LFO waveform],
    [`0x09 (9)`], [`LFO_OSC`], [`103`], [`0`..`255`], [LFO amount on the oscillators],
    [`0x0A (10)`], [`LFO_RATE`], [`102`], [`0`..`255`], [LFO rate],
    [`0x0B (11)`], [`FLT_GATE`], [`63`], [`0`..`1`], [Gate mode of the filter envelope],
    [`0x0C (12)`], [`FLT_REL`], [`62`], [`0`..`2`], [Release mode],
    [`0x0D (13)`], [`FLT_NORM`], [`61`], [`0`..`1`], [Normal / inverted envelope],
    [`0x0E (14)`], [`FLT_MTRIG`], [`60`], [`0`..`1`], [Multiple trigger],
    [`0x0F (15)`], [`FLT_CONTOUR`], [`59`], [`0`..`255`], [Contour amount of the filter envelope],
    [`0x10 (16)`], [`FLT_SUSTAIN2`], [`56`], [`0`..`255`], [Sustain level of the filter envelope 2],
    [`0x11 (17)`], [`FLT_SUSTAIN1`], [`53`], [`0`..`255`], [Sustain level of the filter envelope 1],
    [`0x12 (18)`], [`FLT_KEY23`], [`58`], [`0`..`1`], [Keyboard tracking, filter envelope 2 to 3],
    [`0x13 (19)`], [`FLT_KEY13`], [`57`], [`0`..`1`], [Keyboard tracking, filter envelope 1 to 3],
    [`0x14 (20)`], [`FLT_DECAY2`], [`75`], [`0`..`255`], [Decay time of the filter envelope 2],
    [`0x15 (21)`], [`FLT_DECAY1`], [`52`], [`0`..`255`], [Decay time of the filter envelope 1],
    [`0x16 (22)`], [`FLT_EMPHASIS`], [`71`], [`0`..`255`], [Resonance (emphasis)],
    [`0x17 (23)`], [`FLT_ATTACK2`], [`73`], [`0`..`255`], [Attack time of the filter envelope 2],
    [`0x18 (24)`], [`FLT_ATTACK1`], [`47`], [`0`..`255`], [Attack time of the filter envelope 1],
    [`0x19 (25)`], [`FLT_CUTOFF`], [`74`], [`0`..`255`], [Cutoff frequency],
    [`0x1A (26)`], [`MIX_NOISE`], [`41`], [`0`..`255`], [Level of the noise generator],
    [`0x1B (27)`], [`MIX_FEEDBACK`], [`51`], [`0`..`255`], [Feedback amount of the mixer],
    [`0x1C (28)`], [`MIX_OSC3`], [`50`], [`0`..`255`], [Level of oscillator 3 in the mixer],
    [`0x1D (29)`], [`MIX_OSC2`], [`49`], [`0`..`255`], [Level of oscillator 2 in the mixer],
    [`0x1E (30)`], [`MIX_OSC1`], [`48`], [`0`..`255`], [Level of oscillator 1 in the mixer],
    [`0x1F (31)`], [`XMOD_O3PW`], [`18`], [`0`..`255`], [Cross modulation, oscillator 3 to the pulse width],
    [`0x20 (32)`], [`XMOD_O3TO`], [`17`], [`0`..`255`], [Cross modulation, oscillator 3 to the oscillators],
    [`0x21 (33)`], [`XMOD_O2FLT`], [`16`], [`0`..`255`], [Cross modulation, oscillator 2 to the filter],
    [`0x22 (34)`], [`OSC_XMOD`], [`31`], [`0`..`2`], [Cross modulation mode],
    [`0x23 (35)`], [`OSC_KYBD`], [`30`], [`0`..`1`], [Keyboard tracking of the oscillators],
    [`0x24 (36)`], [`OSC_ENV1`], [`29`], [`0`..`255`], [Envelope amount of oscillator 1],
    [`0x25 (37)`], [`OSC_SYNC`], [`21`], [`0`..`1`], [Oscillator sync],
    [`0x26 (38)`], [`OSC_WAVEFORM3`], [`26`], [`0`..`5`], [Waveform of oscillator 3],
    [`0x27 (39)`], [`OSC_WAVEFORM2`], [`20`], [`0`..`5`], [Waveform of oscillator 2],
    [`0x28 (40)`], [`OSC_WAVEFORM1`], [`24`], [`0`..`5`], [Waveform of oscillator 1],
    [`0x29 (41)`], [`OSC_FINE2`], [`28`], [`0`..`255`], [Fine tune of oscillator 2, `128` = centre],
    [`0x2A (42)`], [`OSC_FINE1`], [`27`], [`0`..`255`], [Fine tune of oscillator 1, `128` = centre],
    [`0x2B (43)`], [`OSC_RANGE3`], [`25`], [`0`..`5`], [Footage of oscillator 3],
    [`0x2C (44)`], [`OSC_RANGE2`], [`19`], [`0`..`5`], [Footage of oscillator 2],
    [`0x2D (45)`], [`OSC_RANGE1`], [`22`], [`0`..`5`], [Footage of oscillator 1],
    [`0x2E (46)`], [`CTRL_WHL`], [`13`], [`0`..`255`], [Wheel amount],
    [`0x2F (47)`], [`CTRL_GLIDE_TYPE`], [`9`], [`0`..`2`], [Glide curve],
    [`0x30 (48)`], [`CTRL_GLIDE`], [`5`], [`0`..`255`], [Glide time],
    [`0x31 (49)`], [`COM_TRNS_SW`], [`114`], [`0`..`1`], [Transposition on/off],
    [`0x32 (50)`], [`COM_TRNS`], [`113`], [`0`..`24`], [Transposition in semitones, `12` = none],
    [`0x33 (51)`], [`COM_OCT`], [`112`], [`0`..`6`], [Octave shift, `3` = no shift],
    [`0x34 (52)`], [`COM_PWM_LF_DPTH`], [`117`], [`0`..`127`], [PWM LFO depth],
    [`0x35 (53)`], [`COM_PWM_LF_RATE`], [`116`], [`0`..`127`], [PWM LFO rate],
    [`0x36 (54)`], [`COM_VOLUME`], [`115`], [`0`..`127`], [Volume of the patch],
    [`0x37 (55)`], [`COM_DYNAMICS`], [`81`], [`0`..`127`], [Keyboard velocity sensitivity],
    [`0x38 (56)`], [`COM_AFT_SENS2`], [`85`], [`0`..`127`], [Aftertouch sensitivity, oscillator 2],
    [`0x39 (57)`], [`COM_AFT_SENS1`], [`84`], [`0`..`127`], [Aftertouch sensitivity, oscillator 1],
    [`0x3A (58)`], [`COM_MOD_SENS`], [`80`], [`0`..`127`], [Modulation lever sensitivity],
    [`0x3B (59)`], [`COM_BENDRANGE`], [`87`], [`0`..`24`], [Bend range, in semitones],
  ),
  caption: [The parameters the sequencer can automate],
)

The *Range* column gives the domain of the stored value, as in the patch parameter tables of @chap-params. It is not
the range displayed by the front panel.

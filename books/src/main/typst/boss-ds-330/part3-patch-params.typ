#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Patch parameters", l: "chap-patch-params")

The patch parameter area at `0x401000` holds 16 blocks of 128 bytes, one per part. The block `nn` starts at the
packed address `0x40nn00`, where `nn` runs from `0x10` to `0x1F`.

== Block order

The blocks are not in channel order:

- The block `0x401000` is the drum part, on channel 10.
- The blocks `0x401100` to `0x401900` are the parts on channels 1 to 9.
- The blocks `0x401A00` to `0x401F00` are the parts on channels 11 to 16.

The *Channel* field of each block stores the MIDI channel minus 1: the drum block stores `09`, the block on channel
1 stores `00`, and the block on channel 16 stores `0F`.

== Field layout

Every field lives at the same offset in every block. The offsets below are relative to the block base; the packed
address of a field is the block base with its low byte replaced by the offset.

#figure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Offset*], [*Size*], [*Parameter*], [*Meaning*]),
        [`00`], [2], [Tone Number],
        [The tone number, split as a program number and a variation. `00 00` is the default],
        [`02`], [1], [Channel],
        [The MIDI channel minus 1, from `00` to `0F`],
        [`03`], [1], [Rx Pitch Bend], [Receive pitch bend, `00` or `01`],
        [`04`], [1], [Rx Channel Aftertouch], [Receive channel aftertouch, `00` or `01`],
        [`05`], [1], [Rx Program Change], [Receive program change, `00` or `01`],
        [`06`], [1], [Rx Control Change], [Receive control change, `00` or `01`],
        [`07`], [1], [Rx Polyphonic Aftertouch], [Receive polyphonic aftertouch, `00` or `01`],
        [`08`], [1], [Rx Note Message], [Receive note messages, `00` or `01`],
        [`09`], [1], [Rx RPN], [Receive RPN, `00` or `01`],
        [`0A`], [1], [Rx NRPN], [Receive NRPN, `00` or `01`],
        [`0B`], [1], [Rx Modulation], [Receive modulation, `00` or `01`],
        [`0C`], [1], [Rx Volume], [Receive volume, `00` or `01`],
        [`0D`], [1], [Rx Panpot], [Receive panpot, `00` or `01`],
        [`0E`], [1], [Rx Expression], [Receive expression, `00` or `01`],
        [`0F`], [1], [Rx Hold 1], [Receive hold 1 (sustain), `00` or `01`],
        [`10`], [1], [Rx Portamento], [Receive portamento, `00` or `01`],
        [`11`], [1], [Rx Sostenuto], [Receive sostenuto, `00` or `01`],
        [`12`], [1], [Rx Soft], [Receive soft, `00` or `01`],
        [`13`], [1], [Mono Poly Mode], [Mono or poly, `00` or `01`],
        [`14`], [1], [Assign Mode], [The voice assignment: `0` single, `1` limited multi, `2` full multi],
        [`15`], [1], [Use for Rhythm Part], [The drum map the part plays: `0` off, `1` map 1, `2` map 2],
        [`16`], [1], [Pitch Key Shift], [The coarse pitch, `-24` to `+24` semitones],
        [`17`], [2], [Pitch Offset Fine], [The fine pitch. The centre value is `08 00`],
        [`19`], [1], [Level], [The part level, `0x00` to `0x7F`],
        [`1A`], [1], [Velocity Sense Depth], [The velocity sensitivity],
        [`1B`], [1], [Velocity Sense Offset], [The velocity offset],
        [`1C`], [1], [Part Panpot], [The part pan, `0x00` to `0x7F`],
        [`1D`], [1], [Note range low], [The lowest note the part plays],
        [`1E`], [1], [Note range high], [The highest note the part plays],
        [`1F`], [1], [CC1], [The destination of the first controller],
        [`20`], [1], [CC2], [The destination of the second controller],
        [`21`], [1], [Chorus Send Level], [The chorus send level],
        [`22`], [1], [Reverb Send Level], [The reverb send level],
        [`23`], [13], [Reserved], [13 bytes, read as `00`],
        [`30`], [8], [Tone Parameters], [The 8 tone parameters, see below],
        [`38`], [8], [Reserved], [8 bytes, read as `00`],
        [`40`], [12], [Scale Tuning], [12 bytes, one per note of the chromatic scale],
    ),
    caption: [Layout of a patch parameter block],
)

== Tone parameters

The 8 tone parameters at offset `0x30` describe the envelope and the vibrato of the part:

#figure(
    ptable(
        columns: (auto, auto, 1fr),
        head([*Offset*], [*Parameter*], [*Meaning*]),
        [`30`], [Vibrato Rate], [The vibrato speed, `-50` to `+50`],
        [`31`], [Vibrato Depth], [The vibrato amount, `-50` to `+50`],
        [`32`], [TVF Cutoff], [The filter cutoff, `-50` to `+50`],
        [`33`], [TVF Resonance], [The filter resonance, `-50` to `+50`],
        [`34`], [Attack], [The attack time, `-50` to `+50`],
        [`35`], [Decay], [The decay time, `-50` to `+50`],
        [`36`], [Release], [The release time, `-50` to `+50`],
        [`37`], [Vibrato Delay], [The vibrato onset delay, `-50` to `+50`],
    ),
    caption: [The tone parameters],
)

The scale tuning at offset `0x40` holds 12 bytes, one per note from C to B. Each byte detunes its note in cents,
around the centre value `40`.

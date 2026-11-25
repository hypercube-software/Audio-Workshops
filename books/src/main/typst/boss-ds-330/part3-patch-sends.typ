#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Patch sends", l: "chap-patch-sends")

The patch send area at `0x402000` holds 16 blocks, one per part, in the same order as the patch parameters: the
block `0x402000` is the drum part, and the block `0x40nn00` is the part on channel `nn - 0x20`.

A block holds 6 sends. Each send routes one modulation source — the modulation wheel, the pitch bend, the channel
aftertouch, the polyphonic aftertouch and the 2 controllers — to the tone parameters of the part.

#figure(
    ptable(
        columns: (auto, auto, 1fr),
        head([*Offset*], [*Send*], [*Meaning*]),
        [`00`], [MOD], [The modulation wheel],
        [`10`], [BEND], [The pitch bend],
        [`20`], [CAF], [The channel aftertouch],
        [`30`], [PAF], [The polyphonic aftertouch],
        [`40`], [CC1], [The first controller],
        [`50`], [CC2], [The second controller],
    ),
    caption: [The 6 sends of a patch send block],
)

== A send

A send is 16 bytes. It gives a destination level to the pitch, the filter and the volume, and routes 2 LFOs:

#figure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Offset*], [*Size*], [*Parameter*], [*Meaning*]),
        [`00`], [1], [Pitch Control], [The amount applied to the pitch, `-24` to `+24` semitones],
        [`01`], [1], [TVF Cutoff Control], [The amount applied to the filter cutoff],
        [`02`], [1], [Amplitude Control], [The amount applied to the volume],
        [`03`], [4], [LFO 1], [The 4 LFO 1 destinations, see below],
        [`07`], [4], [LFO 2], [The 4 LFO 2 destinations],
        [`0B`], [5], [Reserved], [5 bytes, read as `00`],
    ),
    caption: [Layout of a send],
)

== LFO destinations

The LFO destinations are 4 bytes:

#figure(
    ptable(
        columns: (auto, auto, 1fr),
        head([*Offset*], [*Parameter*], [*Meaning*]),
        [`00`], [Rate Control], [The LFO speed, `-10.0` to `+10.0` Hz],
        [`01`], [Pitch Depth], [The pitch modulation depth, `0` to `600` cent],
        [`02`], [TVF Depth], [The filter modulation depth, `0` to `2400` cent],
        [`03`], [TVA Depth], [The volume modulation depth, `0` to `100.0` %],
    ),
    caption: [The LFO destinations],
)

The LFO 1 and LFO 2 of a send modulate the internal sounds; on some parts changing them has no audible effect.

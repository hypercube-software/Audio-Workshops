#import "../common/settings.typ": *
#import "lib.typ": *

#part("Single mode")

#chapter("The single mode", l: "chap-single-mode")

The DS-330 has two operating modes. The *multi mode*, described in the previous parts, plays 16 parts and speaks
the GS dialect with the model ID `0x42`. The *single mode* plays one tone or one drum set, and speaks its own
dialect with the model ID `0x55`. The messages that switch between the two modes are described in
#cref(<chap-sysex>).

== Address areas

The single mode is a separate address space from the multi mode, in the *BYTES* form:

#figure(
    ptable(
        columns: (auto, 1fr),
        head([*Packed address*], [*Content*]),
        [`20 00 00`], [The MIDI map: the 128 program numbers mapped to the 128 tones, identity by default],
        [`20 01 00`], [The system settings: the tune, the key shift, the receive switches and the 16 banks],
        [`21 00 00`], [The edit buffer: the tone being edited, same layout as a stored tone],
        [`22 00 00` .. `22 7F 00`], [The 128 stored tones, 39 bytes each],
        [`23 00 00`], [The drum set, 12 bytes],
    ),
    caption: [The areas of the single mode],
)

== Reaching an element

The tables of this part give the packed address, the 3 bytes that travel in a MIDI frame. To reach an element,
compute its linear address and pack it: the packed bytes `aa bb cc` stand for the linear address
`aa * 0x4000 + bb * 0x80 + cc`. The arithmetic runs on the linear address, never on the packed bytes.

The 128 tones form an array. The tone `n`, for `n` from `0` to `127`, sits at the linear address
`0x88000 + n * 0x80`, which packs as `22 n 00`. The tone `n + 1` is `0x80` further in the linear address, not `1`.

== The system settings

The system settings live at `0x200100`, a block of 23 bytes:

#figure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Offset*], [*Size*], [*Parameter*], [*Meaning*]),
        [`00`], [4], [Tune], [The overall tuning, `0x0018` to `0x07E8`; `0x0400` is 440 Hz, see #cref(<sec-master-tune>)],
        [`04`], [1], [Key Shift], [The transposition, `-24` to `+24` semitones],
        [`05`], [1], [Rx Program], [Receive program change, `00` or `01`],
        [`06`], [1], [Rx Volume], [Receive volume, `00` or `01`],
        [`07`], [16], [Banks 1 to 16], [The 16 banks, `0x01` to `0x08` each],
    ),
    caption: [Content of the single-mode system settings],
)

== The 16 banks

The 128 tones are grouped into 16 banks of 8, the General MIDI categories of #cref(<sec-gm-banks>):

#figure(
    ptable(
        columns: (auto, auto),
        head([*Bank*], [*First tone*]),
        [`1`], [`22 00 00`],
        [`2`], [`22 08 00`],
        [`3`], [`22 10 00`],
        [`4`], [`22 18 00`],
        [`5`], [`22 20 00`],
        [`6`], [`22 28 00`],
        [`7`], [`22 30 00`],
        [`8`], [`22 38 00`],
        [`9`], [`22 40 00`],
        [`10`], [`22 48 00`],
        [`11`], [`22 50 00`],
        [`12`], [`22 58 00`],
        [`13`], [`22 60 00`],
        [`14`], [`22 68 00`],
        [`15`], [`22 70 00`],
        [`16`], [`22 78 00`],
    ),
    caption: [The first tone of each bank],
)

The bank `b` holds the tones `(b - 1) * 8` to `b * 8 - 1`, that is the packed addresses `22 (b - 1) * 8 00` to
`22 b * 8 - 1 00`. The tone `b-n` is the tone of index `(b - 1) * 8 + (n - 1)`.

For instance, the program `64` is the tone `8-8`, the last tone of the bank `8` (Brass). Its index is `63`, so its
packed address is `22 3F 00`.

== The tone

A tone is 39 bytes. The same layout is used by the edit buffer at `0x210000` and by the 128 stored tones at
`0x220000` to `0x227F00`.

#figure(
    ptable(
        columns: (auto, auto, 1fr),
        head([*Offset*], [*Parameter*], [*Meaning*]),
        [`00`], [Split Point], [The note that splits the low and sub tones, `C-1` to `F#9`],
        [`01`], [Low Tone Number], [The tone of the low half, `0x00` to `0x7F`],
        [`02`], [Low Level], [The level of the low half, `0x00` to `0x7F`],
        [`03`], [Sub Tone Number], [The tone of the upper half, `0x00` to `0x7F`],
        [`04`], [Sub Level], [The level of the upper half, `0x00` to `0x7F`],
        [`05`], [Split Mode], [`0` off, `1` split, `2` dual],
        [`06`], [Fat On], [Doubles the tone, `00` or `01`],
        [`07`], [Tone Level], [The overall level, `0x00` to `0x7F`],
        [`08`], [Fat Mode], [The doubling interval, `0x00` to `0x03`],
        [`09`], [Reverb Character], [The reverb colour, `0x00` to `0x07`],
        [`0A`], [Reverb Time], [The reverb decay, `0x00` to `0x7F`],
        [`0B`], [Delay Feedback], [The reverb delay feedback, `0x00` to `0x7F`],
        [`0C`], [Chorus Delay], [The chorus delay, `0x00` to `0x7F`],
        [`0D`], [Chorus Rate], [The chorus rate, `0x00` to `0x7F`],
        [`0E`], [Chorus Depth], [The chorus depth, `0x00` to `0x7F`],
        [`0F`], [Chorus Feedback], [The chorus feedback, `0x00` to `0x7F`],
        [`10`], [Reverb On], [Turns the reverb on, `00` or `01`],
        [`11`], [Chorus On], [Turns the chorus on, `00` or `01`],
        [`12`], [Reverb Level], [The reverb send level, `0x00` to `0x7F`],
        [`13`], [Chorus Level], [The chorus send level, `0x00` to `0x7F`],
        [`14`], [Bend Range], [The pitch bend range, `0` to `+24` semitones],
        [`15`], [Modulation Depth], [The modulation depth, `0x00` to `0x7F`],
        [`16`], [Velocity Depth], [The velocity sensitivity, `0x00` to `0x7F`],
        [`17`], [Velocity Offset], [The velocity offset, `0x00` to `0x7F`],
        [`18`], [Mono Poly Mode], [Mono or poly, `00` or `01`],
        [`19`], [Vibrato Rate], [The vibrato speed, `-50` to `+50`],
        [`1A`], [Vibrato Depth], [The vibrato amount, `-50` to `+50`],
        [`1B`], [Vibrato Delay], [The vibrato onset, `-50` to `+50`],
        [`1C`], [Hold 1 On], [Sustains the notes, `00` or `01`],
        [`1D`], [Portamento On], [Turns the portamento on, `00` or `01`],
        [`1E`], [Portamento Time], [The portamento time, `0x00` to `0x7F`],
        [`1F`], [Cutoff Frequency], [The filter cutoff, `-50` to `+50`],
        [`20`], [Resonance], [The filter resonance, `-50` to `+50`],
        [`21`], [Attack Time], [The attack, `-50` to `+50`],
        [`22`], [Decay Time], [The decay, `-50` to `+50`],
        [`23`], [Release Time], [The release, `-50` to `+50`],
        [`24`], [Variation Number], [The variation of the tone, `0x00` to `0x02`],
        [`25`], [Low Variation Number], [The variation of the low tone, `0x00` to `0x02`],
        [`26`], [Sub Variation Number], [The variation of the sub tone, `0x00` to `0x02`],
    ),
    caption: [Layout of a single-mode tone],
)

== The drum set

The drum set lives at `0x230000`, a block of 12 bytes:

#figure(
    ptable(
        columns: (auto, auto, 1fr),
        head([*Offset*], [*Parameter*], [*Meaning*]),
        [`00`], [Drum Level], [The overall level, `0x00` to `0x7F`],
        [`01`], [Drum Reverb Type], [The reverb type, `0x00` to `0x07`],
        [`02`], [Drum Reverb Time], [The reverb decay, `0x00` to `0x7F`],
        [`03`], [Drum Delay Feedback], [The reverb delay feedback, `0x00` to `0x7F`],
        [`04`], [Drum Chorus Delay], [The chorus delay, `0x00` to `0x7F`],
        [`05`], [Drum Chorus Rate], [The chorus rate, `0x00` to `0x7F`],
        [`06`], [Drum Chorus Depth], [The chorus depth, `0x00` to `0x7F`],
        [`07`], [Drum Chorus Feedback], [The chorus feedback, `0x00` to `0x7F`],
        [`08`], [Drum Reverb On], [Turns the reverb on, `00` or `01`],
        [`09`], [Drum Chorus On], [Turns the chorus on, `00` or `01`],
        [`0A`], [Drum Reverb Level], [The reverb send level, `0x00` to `0x7F`],
        [`0B`], [Drum Chorus Level], [The chorus send level, `0x00` to `0x7F`],
    ),
    caption: [Layout of the single-mode drum set],
)

== The dump

A *dump all* from the front panel sends the whole single mode as DT1 frames, all with the model ID `0x55`:

#figure(
    ptable(
        columns: (auto, auto, 1fr),
        head([*Packed address*], [*Size*], [*Content*]),
        [`20 00 00`], [128], [The MIDI map, an identity mapping of the 128 program numbers to the 128 tones],
        [`20 01 00`], [23], [The system settings],
        [`22 00 00` .. `22 7F 00`], [39], [The 128 tones, one per note],
        [`23 00 00`], [12], [The drum set],
    ),
    caption: [The layout of a single-mode dump],
)

The single mode is *write-only* over SysEx: a request (`RQ1`) to these areas is not answered. The data is read
back through a dump, or written with a `DT1`.

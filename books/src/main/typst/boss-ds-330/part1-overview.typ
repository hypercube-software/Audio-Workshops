#import "../common/settings.typ": *
#import "lib.typ": *

#part("The DS-330 protocol")

#chapter("Overview", l: "chap-overview")

== Why this document?

The Boss DS-330 is a 16-part multitimbral sound module of the early 1990s. It is a *Roland Sound Canvas*
rebadged by Boss, the same family as the SC-55, the SC-33 and the SC-88. Like the other machines of this
family, its whole memory — the system settings, the 16 patch parameters, the drum maps — can be read and
written over MIDI with *SysEx* messages.

The goal of this book is to describe the format precisely enough to let people implement editors and backup
tools for the instrument.

== Disclaimer

This document is *not* an official Roland or Boss documentation. Neither Roland nor Boss has reviewed,
endorsed or approved its content. *Boss* and *DS-330* are trademarks of their respective owners.

Parts of this document were generated with the help of AI agents. Although the author has reviewed and tested the
information, it may contain errors, omissions or misinterpretations, including in field offsets, sizes, value ranges or
checksum rules.

*Use this information at your own risk.* Sending incorrect or malformed data to a synthesizer through SysEx can
overwrite your patches, put the unit in an unexpected state, or in the worst case damage the hardware. The author
accepts no responsibility for data loss, malfunction or damage to any device resulting from the use of the information
contained in this document. Always back up your data before sending anything to your instrument, and test on a
non-essential patch first.

== DS-330 Design

The DS-330 is a sound module with no keyboard: it receives notes on 16 MIDI channels and plays them. The unit
holds:

- *System settings* — the master tune, the master volume, the key shift, the master pan, and the *GS* mode flag.
- *Patch common* — one set of settings shared by every part: the patch name, the reverb and the chorus.
- *Patch parameters* — 16 blocks, one per part: the tone number, the receive switches, the level, the pan, the
  pitch and the sends.
- *Drum maps* — 2 maps of 128 notes, each note with a tune, a level, a group, a pan, a reverb and a chorus send.
- *The bulk dump* — a single request that returns the system settings, the patch common and the 16 patch blocks
  in one answer.

All of it lives in one flat address space described below. This is the *multi mode*. The unit also has a *single
mode* that plays one tone or one drum set, addressed with the model ID `0x55` and its own address areas; see
#cref(<chap-single-mode>).

== Conventions

A byte is always written as two hexadecimal digits and set in a code font: `F0`, `7F`, `12`. The addresses of
this book are always the packed ones, as they travel in the MIDI frame. In the text, the 3 packed bytes are
written as one hexadecimal literal: `0x400000`. In a frame or in a table, they keep their spaces: `40 00 00`.
The unpacked address only appears where this book explains the difference between the 2 forms.

== Addresses <sec-address>

A memory address is a big number. Because a MIDI data byte must have its most significant bit clear, the address
is *packed*: each byte carries 7 bits, most significant group first. The DS-330 uses 3 bytes, which gives a
21-bit address space.

A value that fits in 7 bits occupies 1 byte, and a larger value is spread over several bytes: `0xFF` is sent as
`01 7F`, that is `0x017F`, because `1 * 128 + 127 = 255`.

The 3 packed bytes `aa bb cc` stand for the unpacked address `aa * 0x4000 + bb * 0x80 + cc`. To reach an element,
compute this number and pack the result: the arithmetic never runs on the packed bytes, which only travel in a
MIDI frame. For instance, `40 00 00` and `40 01 00` are `0x80` apart, not `1`.

== Memory map

The memory of the instrument lives in 9 regions of the *BYTES* form, plus 1 region of the *NIBBLES* form described
in #cref(<chap-encodings>):

#figure(
    table(
        align: left,
        columns: (auto, auto, auto, 1fr),
        fill: (x, y) => if y == 0 { luma(230) },
        stroke: 0.5pt,
        head([*Packed address*], [*Packed size*], [*Form*], [*Content*]),
        [`40 00 00`], [`00 01 00`], [BYTES],
        [System settings: master tune, volume, key shift, pan, GS mode],
        [`40 01 00`], [`00 00 40`], [BYTES],
        [Patch common: name, reverb, chorus, shared by every part],
        [`40 10 00`], [`00 10 00`], [BYTES],
        [Patch parameters: 16 blocks of 128 bytes, one per part],
        [`40 20 00`], [`00 10 00`], [BYTES],
        [Patch sends: 16 blocks, the LFO sends of every part],
        [`40 30 00`], [`00 00 20`], [BYTES],
        [Information: a 32-byte version string],
        [`41 00 00`], [`00 00 0C`], [BYTES],
        [Drum map 1 name],
        [`41 01 00`], [`00 08 00`], [BYTES],
        [Drum map 1: 128 notes],
        [`41 10 00`], [`00 00 0C`], [BYTES],
        [Drum map 2 name],
        [`41 11 00`], [`00 08 00`], [BYTES],
        [Drum map 2: 128 notes],
        [`48 00 00`], [`00 1D 10`], [NIBBLES],
        [Bulk dump: system, common and 16 patch blocks in one answer],
    ),
    caption: [The regions of the DS-330 memory map],
)

The address `0x401000` is the patch parameter area: `nn` in `0x40nn00` selects a part. The block 0 is the drum
part on channel 10, the blocks 1 to 9 are the parts on channels 1 to 9, and the blocks A to F are the parts on
channels 11 to 16. See #cref(<chap-patch-params>).

== The 16 GM banks <sec-gm-banks>

The tones are organised in 16 banks of 8, the General MIDI categories, shared by the multi mode and the single
mode:

#figure(
    ptable(
        columns: (auto, 1fr),
        head([*Bank*], [*Category*]),
        [`1`], [Piano],
        [`2`], [Chromatic Percussion],
        [`3`], [Organ],
        [`4`], [Guitar],
        [`5`], [Bass],
        [`6`], [Strings],
        [`7`], [Ensemble],
        [`8`], [Brass],
        [`9`], [Reed],
        [`10`], [Pipe],
        [`11`], [Synth Lead],
        [`12`], [Synth Pad],
        [`13`], [Synth Effects],
        [`14`], [Ethnic],
        [`15`], [Percussive],
        [`16`], [Sound Effects],
    ),
    caption: [The 16 GM banks],
)

== Tone variations <sec-variations>

A capital tone is one of the 128 basic sounds, with the variation number `0`. A variation is a tone close to a
capital, numbered `1` to `127`. The multiples of `8` — the variations `8`, `16`, `24`, `32`, `40`, `48` and `56` —
are the sub capitals. A tone may carry no variation, or several.

To select a tone from a MIDI controller, send the bank select and the program change on the channel of the part, in
this order:

#figure(
    ptable(
        columns: (auto, auto, 1fr),
        head([*Step*], [*Message*], [*Value*]),
        [`1`], [Control Change 0 (Bank Select MSB)], [The variation number, `0` to `127`],
        [`2`], [Control Change 32 (Bank Select LSB)], [`0`],
        [`3`], [Program Change], [`0` to `127`],
    ),
    caption: [Selecting a tone with bank select and program change],
)

*A variation is selected in the multi mode only.* There, when a part asks for a variation that a tone does not
carry, the capital steps in as a substitute, for the variations `1` to `63`; above `63` there is no substitution
and the part stays silent.

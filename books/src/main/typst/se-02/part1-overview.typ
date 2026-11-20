#import "../common/settings.typ": *
#import "lib.typ": *

#part("The SE-02 protocol")

#chapter("Overview", image: red-chapter-banner, l: "chap1")

== Why this document?

The Roland SE-02 is a small monophonic analog synthesizer of the *Boutique* line, designed with Studio Electronics.
Like most of the Roland machines of this generation, its whole memory — system settings, patches, sequencer patterns
and songs — can be read and written over MIDI with *SysEx* messages.

The goal of this book is to describe the format precisely enough to let people implement editors, especially for the sequencer.

== Disclaimer

This document is *not* an official Roland documentation. Roland has not reviewed, endorsed or approved its content.
*Roland* and *SE-02* are trademarks of their respective owners.

Parts of this document were generated with the help of AI agents. Although the author has reviewed and tested the
information, it may contain errors, omissions or misinterpretations, including in field offsets, sizes, value ranges or
checksum rules.

*Use this information at your own risk.* Sending incorrect or malformed data to a synthesizer through SysEx can
overwrite your patches, put the unit in an unexpected state, or in the worst case damage the hardware. The author
accepts no responsibility for data loss, malfunction or damage to any device resulting from the use of the information
contained in this document. Always back up your data before sending anything to your instrument, and test on a
non-essential patch first.

== SE-02 Design

#image("assets/image.png")

Despite the apparences, *this unit is mainly a sequencer before being a synthesizer*. It is VERY important to get this right.
- The switch PATCH/SEQ/SONG does not switch the unit into a specific mode like other stynths do (multi-mode, single-mode). It is mostly used to change the goal of various front panel buttons to different parameters.
- Since PATCH is not a real mode, the unit will still react to any Transport MIDI events. So start your DAW and the SE-02 sequencer will *always* start, even in PATCH mode.

In reality, there is no "mode" as the official documentation say. You can't switch the unit to a so-call "PATCH mode" using SYSEX. There is no existence of such concept.
The only real mode of this unit is SONG:
- You have a powerful sequencer at your disposal which run ALWAYS when transport events are recevied.
- There is no way to disable it
- The unit will still act as a regular synth receinving MIDI notes

So, if you plan to build an editor alowing the user to switch to PATCH/SEQ/SONG, you will get into trouble. A way to simulate what we want is the following:

- Fake PATCH mode: clear the current sequencer pattern, so transport events will play "nothing"
- Fake SEQ mode: use a song with only 1 pattern
- SONG mode: this is the real mode of this unit

== Scope of this book

Four memory blocks are described here:

#figure(
    table(
        align: left,
        columns: (auto, 1fr, auto),
        fill: (x, y) => if y == 0 { luma(230) },
        stroke: 0.5pt,
        head([*Block*], [*Content*], [*Chapter*]),
        [System settings],
        [MIDI channel, clock source, CC output, automatic power off, screen saver],

        [#cref(<chap-system>)], [Patch], [The 88 parameters of a program, plus its 16-character name],
        [#cref(<chap-patch>)],
        [Pattern],
        [A 16-step sequence: tempo, scale, shuffle, 16 notes with gate, glide and one parameter value each],

        [#cref(<chap-pattern>)],
        [Song],
        [A chain of 16 parts, each one replaying a pattern with a patch and a repeat count],

        [#cref(<chap-song>)],
    ),
    caption: [The four blocks described in this book],
)

== Conventions

A byte is always written as two hexadecimal digits and set in a code font: `F0`, `7F`, `12`. The addresses of this book are always the packed ones, as they travel in the MIDI frame. In the text, the 4
packed bytes are written as one hexadecimal literal: `0x05000150`. In a frame or in a table, they keep their
spaces: `05 00 01 50`. The unpacked address (`0x050000D0` for the same byte) only appears where this book
explains the difference between the 2 forms.

Two words are used with a precise meaning in this book:
- The *stored value* is what the memory contains. It is *not* always what the front panel displays.
- The *parameter ID* is the internal identifier used by the step sequencer for the parameter a step automates. It is *not* the MIDI CC number; see #cref(<chap-param-ids>).

Most tables of this book have a *Ref.* column: it gives the value found in the factory patch *`Inbound`* which is used from one end of the book to the other as a reference patch.

== Addresses <sec-address>

Memory adresses are big numbers of 32-bit or 24 bits numbers. Due to MIDI specification they must be *packed*.
A MIDI data byte must have its most significant bit clear, each byte of the packed address carries only 7 bits.
The unpacked address is split into 7-bit groups, most significant group first.

Depending on the device, a packed address uses three or four bytes:
- The *Roland SE-02* uses four, which gives a 28-bit address space.
- The *Boss DS-330* and other *Roland Sound Canvas*, use three, which gives a 21-bit adress space.

A value that fits in seven bits occupies one byte, and a larger value is spread over several bytes: `0xFF` is sent as `01 7F`, that is `0x017F`, because `1 * 128 + 127 = 255`.

== Memory map

All the memory blocks of the instrument live in eight regions:

#figure(
    table(
        align: left,
        columns: (auto, auto, auto, 1fr),
        fill: (x, y) => if y == 0 { luma(230) },
        stroke: 0.5pt,
        head([*Address*], [*Size*], [*Count*], [*Content*]), [`00 00 00 00`], [16], [1],
        [System settings: MIDI channel, clock source, CC output, power off, screen saver], [`02 00 00 00`], [16], [1],
        [Unidentified region; its content does not change with the system settings], [`05 00 00 00`], [240], [1],
        [Current patch (the edit buffer)], [`06 nn 00 00`], [240], [128],
        [Stored user preset number `nn` (0 to 127)], [`10 00 00 00`], [144], [1],
        [Current pattern], [`11 nn 00 00`], [144], [128],
        [Stored pattern number `nn` (0 to 127)], [`20 00 00 00`], [144], [1],
        [Current song], [`21 nn 00 00`], [144], [16],
        [Stored song number `nn` (0 to 15)],
    ),
    caption: [The regions of the SE-02 memory map],
)

The address `0x06000000` is the user bank: `nn` is a user preset number, whatever bank is selected on the
unit. The factory presets are in ROM and have no address; a `CC 0` + `PC` pair loads one into the edit
buffer at `0x05000000`. The bank and the preset are chosen together by the *Bank Select MSB* (CC 0) and the
program change that follows.

The pattern and song blocks have the same size (144 bytes) but a different content: a pattern holds 16 steps of notes, a
song holds 16 parts of a chain. They never overlap in the address space.

== The official software editor

Studio Electronics sells a software editor for the SE-02 under the name *SE-02 Editor: Santoul Suite* (version 3.0 at
the time of writing). It is presented as the official Roland-approved editor and is available as a standalone
application and as a VST3 plug-in. The product page is #link("https://www.studioelectronics.com/products/Software-Editors/SE-02/")[studioelectronics.com/products/Software-Editors/SE-02],
and the licence is sold through the Studio Electronics store. Unfortunately it is far from complete. All sequencer settings are missing and
system settings are read only.

Parts of this reference were cross-checked against the official software editor. Its parameter descriptors were used to
confirm several CC numbers and value ranges, and its start-up requests are the reason a faithful backup should preserve
the unidentified block at `0x02000000` even though none of the documented system settings lives there.

#caveat[
    The official software editor is a third-party, paid product. It is not distributed with this book, and the author is
    not affiliated with Studio Electronics. The link and the feature list above are given for information only.
]


== Restoring the factory settings

The factory reset erases the user memory and brings back the factory patches, patterns and settings.
It is performed at power on:

+ Hold the [`1`] button and turn the power on;
+ The display shows [`rSt`];
+ Press the [`MANUAL`] button to run the reset;
+ When the display shows [`Fin`], turn the unit off and on again.

Turning the power off before [`MANUAL`] is pressed cancels the operation.

#caveat[
  The reset overwrites every user patch, pattern and song. Read the blocks over MIDI first if they
  must be kept.
]

#import "../common/settings.typ": *
#import "lib.typ": *

#part("The SE-02 protocol")

#chapter("Overview", image: red-chapter-banner, l: "chap1")

== Why this document?

The Roland SE-02 is a small monophonic analog synthesizer of the *Boutique* line, designed with Studio Electronics.
Like most of the Roland machines of this generation, its whole memory — system settings, patches, sequencer patterns
and songs — can be read and written over MIDI with *SysEx* messages.

The goal of this book is the same as the one of the Kurzweil book: describe the format precisely enough to implement it
in `Audio-Workshops`, so that a patch can be saved, edited and restored without any vendor software.

== Scope of this book

Four memory blocks are described here, in the order a developer usually needs them:

#figure(
  table(
    align: left,
    columns: (auto, 1fr, auto),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    head([*Block*], [*Content*], [*Chapter*]),
    [System settings], [MIDI channel, clock source, CC output, automatic power off, screen saver],
    [#cref(<chap-system>)],
    [Patch], [The 88 parameters of a program, plus its 16-character name],
    [#cref(<chap-patch>)],
    [Pattern], [A 16-step sequence: tempo, scale, shuffle, 16 notes with gate, glide and one parameter value each],
    [#cref(<chap-pattern>)],
    [Song], [A chain of 16 parts, each one replaying a pattern with a patch and a repeat count],
    [#cref(<chap-song>)],
  ),
  caption: [The four blocks described in this book],
)

Everything else the instrument does (the keyboard, the analog audio path, the USB audio interface) is out of scope: it
is not part of the SysEx memory.

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

== Conventions

A byte is always written as two hexadecimal digits and set in a code font: `F0`, `7F`, `12`.

In a table, an address is written as its four bytes separated by spaces: `05 00 00 00`. In the running text, the same
address is written as one hexadecimal literal: `0x05000000`. Offsets inside a block are given in decimal, and in hex
when the two are easy to confuse.

Three words are used with a precise meaning in this book:

- The *stored value* is what the block contains. It is *not* what the front panel displays: a cutoff stored as `0` is
  the lowest cutoff, not "zero Hz", and a gate stored as `84` is displayed as `85`.
- The *parameter number* is the index of a parameter in the patch block. It doubles as the last byte of the parameter
  address, which makes it easy to remember: `FLT_CUTOFF` is parameter 55, at `0x05000037`.
- The *parameter ID* is the internal identifier used by the step sequencer for the parameter a step automates. It is
  *not* the MIDI CC number; see #cref(<chap-param-ids>).

Most tables of this book have a *Ref.* column: it gives the value found in the factory patch `Inbound` of the user bank,
which is used from one end of the book to the other as a reference patch.

== Addresses <sec-address>

Roland does not send an address as a plain 32-bit number. It uses a *packed address*: because a MIDI data byte must
have its most significant bit clear, each byte of the address carries only 7 bits. The address is split into 7-bit
groups, most significant group first. Depending on the model, a packed address uses three or four bytes; the SE-02
uses four, which gives a 28-bit address space.

Packing is just the base-128 representation of the address. A value that fits in seven bits occupies one byte, and a
larger value is spread over several bytes: `0xFF` is sent as `01 7F`, that is `0x017F`, because
`1 * 128 + 127 = 255`. The same rule applies to the offset inside a block. The name of a patch starts at offset 208
(`0xD0`); that offset cannot be sent as the single byte `D0`, whose bit 7 is set, so it is sent as `01 50`
(`1 * 128 + 80 = 208`). The name of the current patch is therefore addressed with `05 00 01 50`, which is written
`0x05000150` in the text.

== Memory map

All the memory blocks of the instrument live in eight regions:

#figure(
  table(
    align: left,
    columns: (auto, auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    head([*Address*], [*Size*], [*Slots*], [*Content*]),
    [`00 00 00 00`], [16], [1], [System settings: MIDI channel, clock source, CC output, power off, screen saver],
    [`02 00 00 00`], [16], [1], [Unidentified region; its content does not change with the system settings],
    [`05 00 00 00`], [240], [1], [Current patch (the edit buffer)],
    [`06 bb pp 00`], [240], [4 × 128], [Stored patch, bank `bb`, program `pp`],
    [`10 00 00 00`], [144], [1], [Current pattern],
    [`11 nn 00 00`], [144], [128], [Stored pattern, slot `nn` (0 to 127)],
    [`20 00 00 00`], [144], [1], [Current song],
    [`21 nn 00 00`], [144], [16], [Stored song, slot `nn` (0 to 15)],
  ),
  caption: [The regions of the SE-02 memory map],
)

The `bb` byte of a stored patch gives the bank:

- `0` — bank A;
- `1` — bank B;
- `2` — bank C;
- `3` — the user bank.

That is exactly the value of the bank select controller (CC 0, most significant byte) used to pick a preset. The
program number `pp` is the value of the program change that follows it, so a preset is entirely described by a
`CC 0` + `PC` pair.

The pattern and song blocks have the same size (144 bytes) but a different content: a pattern holds 16 steps of notes, a
song holds 16 parts of a chain. They never overlap in the address space.

== The official software editor

Studio Electronics sells a software editor for the SE-02 under the name *SE-02 Editor: Santoul Suite* (version 3.0 at
the time of writing). It is presented as the official Roland-approved editor and is available as a standalone
application and as a VST3 plug-in for Windows and native Apple Silicon macOS. The product page is
#link("https://www.studioelectronics.com/products/Software-Editors/SE-02/")[studioelectronics.com/products/Software-Editors/SE-02],
and the licence is sold through the Studio Electronics store.

The official software editor is a paid product: at the time of writing, the store lists a full licence at USD 69 and an
upgrade at USD 39. Prices and system requirements change over time, so the product page is the reference. The official
software editor is not needed to read this book, and the format described here does not depend on it. It is, however, a
useful known-good implementation: a frame that is accepted by both your code and the official software editor is
probably correct.

The vendor highlights these features:

- standalone and VST3 versions;
- complete front-panel control, with an enlarged interface;
- import and export of individual patches and complete banks;
- patch blending, which morphs two patches into a new one;
- random patch generation;
- visual comparison of patches and their parameters.

Parts of this reference were cross-checked against the official software editor. Its parameter descriptors were used to
confirm several CC numbers and value ranges, and its start-up requests are the reason a faithful backup should preserve
the unidentified block at `0x02000000` even though none of the documented system settings lives there.

#caveat[
  The official software editor is a third-party, paid product. It is not distributed with this book, and the author is
  not affiliated with Studio Electronics. The link and the feature list above are given for information only.
]


#import "settings.typ": *
#part("The K2600 format")

#chapter("Overview", image: image("./assets/hd3-2481-500.svg"), l: "chap1")
#index("Sectioning")

== Why this document?

I had the opportunity to acquire a K2600R in 2026 without knowing much about V.A.S.T. To my surprise, I could not find a
single piece of software able to send patches directly via MIDI (with or without samples). Instead, I had to use the
infamous floppy drive or an emulator like the Gotek.

After some research, I finally found the official specification of the format in the form of .H files provided by
Kurzweil. Unfortunately, they are far from complete, so I decided to dig into the subject to find my way through it.

Long story short, I discovered that the disk format (PRAM#footnote[PRAM apparently stands for "Program RAM".]) uses
a *"compact"* version of the MIDI format: some fields are optionals, whereas in MIDI, they are all required. So it is really
possible to convert K26 patches and send them to the unit ONLY via MIDI.

My main goal is to send a Kurzweil patch in a single click from my software MPM (MIDI Preset Manager#footnote[MPM is
  part of my Audio Workshop written in Java, which can be found here:
  https://hypercube-software.github.io/Audio-Workshops/])

If the patch includes custom samples, things are much more difficult: the sample need to be send using the Sample Dump Standard first (see @sds[chapter]). This protocol is well known, and properly specified but very slow.

== Disclaimer

This document is *not* an official Kurzweil documentation. Kurzweil has not reviewed, endorsed or approved its content.
It is an independent effort, based on the partial official `.H` files and on the author's own analysis.

Parts of this document were generated with the help of AI agents. Although the author has reviewed and tested the
information, it may contain errors, omissions or misinterpretations, including in field offsets, sizes, value ranges,
endianness or checksum rules.

*Use this information at your own risk.* Sending incorrect or malformed data to a synthesizer through SysEx or any other
means can corrupt its memory, overwrite your existing programs or samples, put the unit in an unexpected state, or in
the worst case damage the hardware. The author accepts no responsibility for data loss, malfunction or damage to any
device resulting from the use of the information contained in this document or in the associated software. Always back
up your data before sending anything to your instrument, and test on a non-essential patch first.

== Object types

Kurzweil uses an object-oriented architecture in its devices, so things will be very familiar to any OOP developer:
- They have types
- They have objects, which are instances of a given type
- An object has a unique identifier inside a given type

So do not think for a second that an identifier *200* points to a unique object. You need the *type identifier* to know
what you are pointing to. This is why type identifiers and object identifiers work together.

#kblock[
  The official specification *`objtypes.h`* states that the number of possible objects depends on the type:
  - Types from 96 to 115 use *8-bit object identifiers*, so you can have 256 objects
  - Types from 132 to 159 use *10-bit object identifiers*, so you can have 1024 objects
  - Some type identifiers are simply not used
]

Since types identifier goes beyond 7 bits, they need to be encoded in MIDI in two bytes where bit 7 is always 0 and
never used. For instance 132/0x84 (10000100) is converted in MIDI to 0x0104 (00000001 00000100):

#figure(
  schema.render(
    schema.load((
      structures: (
        main: (
          bits: 16,
          ranges: (
            "15": (name: "0", description: "unused"),
            "14-8": (name: "0000001", description: "$01"),
            "7": (name: "0", description: "unused"),
            "6-0": (name: "0001100", description: "$04"),
          ),
        ),
      ),
    )),
    width: 90%,
  ),
  caption: [How object type 132 (PROGRAM) is encoded in MIDI],
)

Each type corresponds to various .H files provided by Kurzweil. Unfortunately, some parts are not clear. We are going to
dig into that in the next chapters...

#figure(
  table(
    align: left,
    columns: (auto, auto, auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(68.49%) } else if y >= 19 { rgb("#ccd9ee") } else { rgb("#cceee1") },
    stroke: 0.5pt,

    table.header([*Type*], [*Hexa*], [*MIDI encoding*], [*Enum*], [*Description*]),

    [97], [`$61`], [`00 61`], [INDEX], [Index type],
    [98], [`$62`], [`00 62`], [X_CODE], [Executable code type],
    [100], [`$64`], [`00 64`], [TABLE], [Miscellaneous table type],
    [101], [`$65`], [`00 65`], [LFO_SHAPE], [LFO shape table (unused)],
    [102], [`$66`], [`00 66`], [ALG_DESC], [C&H wiring diagram type],
    [103], [`$67`], [`00 67`], [INTONATION_TABLE], [Intonation table type],
    [104], [`$68`], [`00 68`], [VELOCITY_MAP], [Velocity map type],
    [105], [`$69`], [`00 69`], [PRESSURE_MAP], [Pressure map type],
    [106], [`$6A`], [`00 6A`], [EDIT], [Editor table type],
    [107], [`$6B`], [`00 6B`], [EDIT_MENU], [Edit menu type],
    [108], [`$6C`], [`00 6C`], [MENU_GROUP], [Edit menu group type],
    [109], [`$6D`], [`00 6D`], [MENU_ENTRY_LIST], [Menu entry list type],
    [110], [`$6E`], [`00 6E`], [P_LIST], [PList type],
    [111], [`$6F`], [`00 6F`], [B_LIST], [BList type],
    [112], [`$70`], [`00 70`], [SONG], [Song type],
    [113], [`$71`], [`00 71`], [EFFECT], [Effect type],
    [114], [`$72`], [`00 72`], [SHAPE], [New LFO shape function type],
    [115], [`$73`], [`00 73`], [F_PROC], [Function proc type],
    [132], [`$84`], [`01 04`], [PROGRAM], [Program type],
    [133], [`$85`], [`01 05`], [KEYMAP], [Keymap type],
    [134], [`$86`], [`01 06`], [SOUND_BLOCK], [Sound block type],
    [135], [`$87`], [`01 07`], [SETUP], [Setup type],
    [136], [`$88`], [`01 08`], [MARGE], [Marge type],
    [140], [`$8C`], [`01 0C`], [STUDIO], [Studio type ⚠️Not explicitly defined in objtypes.h],
  ),
  caption: [List of well known object types],
)

⚠️ The K2600R Musician's reference states that type *113* is a studio: this is wrong, it is *140* (chapter 7-7).

== Banks

It is very important to understand a *Bank* in the Kurzweil terminology have nothing to do with a *MIDI bank*. 

=== Object ID bank

A bank is a numbered storage area inside the unit. The numbers go from 0 to 9, and `7F` designates all the banks. Each
object stored in RAM (program, keymap, sound block, setup...) belongs to one bank. The bank is a way to group objects,
not an identifier: the object ID stays the same whatever its bank. 

Because objects are grouped by bank, the SysEx protocol has commands that act on a whole bank at once:
- `READBANK` and `DIRBANK` ask for the objects of one bank, or of all banks with `7F`,
- `DELBANK` deletes all the RAM objects of a bank,
- `MOVEBANK` moves all the objects of a bank to another bank.

The bank number is therefore part of the request, not of the object reference. The exact rules still need to be checked on the unit or in the *Musician's Reference*: whether an object can be in several banks, and what happens to the bank of an object when it is renamed or moved.

Therefore do not assume the following formula is true. It is not !

$ "Object ID" = ("Bank" times 100) + "(index from 0 to 99)" $

=== MIDI bank

On the other side, if you talk about MIDI banks, it is ONLY used to select object ID of type PROGRAM. 
$ "Program ID" = ("Bank LSB" times 128) + "Program Change" $
It is explained here: https://kurzweil.com/2024/04/08/k27-selecting-programs-via-midi/
- Bank Select MSB is never used
- Bank Select LSB select a cluster of 128 programs
  - Banks 0 to 12 are *Factory programs*
  - Banks 32 to 63 are *User programs*
  - Other Banks are unspecified or unused 

#include "part1-pram-format.typ"

#include "part1-midi-format.typ"

#include "part1-objects.typ"

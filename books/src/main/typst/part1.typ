#import "settings.typ": *
#part("The K2600 format")


#let binary_block(title, size, fill_color) = {
  rect(
    width: 100%,
    height: 40pt,
    fill: fill_color,
    stroke: 1pt + black,
    align(center + horizon)[*#title* \ #size],
  )
}
#let binary_spacer(title, fill_color) = {
  rect(
    width: 100%,
    height: 70pt,
    fill: fill_color,
    stroke: 1pt + black,
    align(center + horizon)[*#title*],
  )
}


#chapter("Overview", image: image("./assets/hd3-2481-500.svg"), l: "chap1")
#index("Sectioning")

== Why this document?

I had the opportunity to acquire a K2600R in 2026 without knowing much about V.A.S.T. To my surprise, I could not find a
single piece of software able to send patches directly via MIDI (with or without samples). Instead, I had to use the
infamous floppy drive or an emulator like the Gotek.

After some research, I finally found the official specification of the format in the form of .H files provided by
Kurzweil. Unfortunately, they are far from complete, so I decided to dig into the subject to find my way through it.

Long story short, I discovered that the disk format (PRAM#footnote[PRAM apparently stands for "Program RAM".]) files use
a "compact" version of the MIDI format: some fields are optional, whereas in MIDI they are all required. So it is really
possible to convert K26 patches and send them to the unit ONLY via MIDI.

My main goal is to send a Kurzweil patch in a single click from my software MPM (MIDI Preset Manager#footnote[MPM is
  part of an Audio Workshop written in Java, which can be found here:
  https://hypercube-software.github.io/Audio-Workshops/])

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

== PRAM format

== Endianness

The PRAM format is Big Endian.

=== Header

The `samples offset` points to the footer. The precise sample position in the file is given by *samples offset + (sample
start \* 2)*, since the bit depth is always 16 bits and the sample start is expressed in samples instead of bytes.

#stack(
  dir: ttb,
  spacing: 2pt,

  binary_block("Magic: 'PRAM'", "4 bytes", rgb("#e1f5fe")),
  binary_block("samples offset", "4 bytes", rgb("#fcb3b3")),
)

== Block content

The PRAM file content is made of multiple blocks. For some unknown reason, the block size is *NEGATIVE* and includes
itself. So to get the block size from \$FFFFFFA4, you have to compute Abs(\$FFFFFFA4)-4 = Abs(-92)-4 = 88

#stack(
  dir: ttb,
  spacing: 2pt,

  binary_block("block 1 size", "4 bytes", rgb("#b3fcb9")),
  binary_block("block 1 content", "... bytes", rgb("#61be69")),
  binary_block("block 2 size", "4 bytes", rgb("#b3fcb9")),
  binary_block("block 2 content", "... bytes", rgb("#61be69")),
  binary_spacer("...", rgb("#ffffff")),
)

Each block contains multiple Kurzweil objects.

#stack(
  dir: ttb,
  spacing: 2pt,

  grid(
    columns: (50%, 50%),
    binary_block("Object 1 identifier (compacted)", "2 bytes", rgb("#ffffff")),
    binary_block("Object 1 size", "2 bytes", rgb("#ffffff")),
  ),
  binary_block("Object 1 content", "... bytes", rgb("#8cddd0")),
  grid(
    columns: (50%, 50%),
    binary_block("Object 2 identifier (compacted)", "2 bytes", rgb("#ffffff")),
    binary_block("Object 2 size", "2 bytes", rgb("#ffffff")),
  ),
  binary_block("Object 2 content", "... bytes", rgb("#8cddd0")),
  binary_spacer("...", rgb("#ffffff")),
)

=== Object types

Kurzweil uses an object-oriented architecture in its devices, so things will be very familiar to any OOP developer:
- They have types
- They have objects, which are instances of a given type
- An object has a unique identifier inside a given type

So do not think for a second that an identifier *200* points to a unique object. You need the *type identifier* to know
what you are pointing to. This is why type identifiers and object identifiers work together.

#kblock[
  The official specification *`objtypes.h`* states that the number of possible objects depends on the type:
  - Types from 96 to 115 use 8-bit object identifiers, so you can have 256 objects
  - Types from 132 to 159 use 10-bit object identifiers, so you can have 1024 objects
  - Some type identifiers are simply not used
]

Since types identifier goes beyond 7 bits, they need to be encoded in MIDI in two bytes where bit 7 is always 0 and never used. For instance 132/0x84 (10000100) is converted in MIDI to 0x0104 (00000001 00000100):

#figure(
  schema.render(
    schema.load((
      structures: (
        main: (
          bits: 16,
          ranges: (
            "15": (name: "15", description: "always 0"),
            "14-8": (name: "0000001", description: "$01"),
            "7": (name: "7", description: "always 0"),
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

=== PRAM Objects

The object identifiers used in PRAM files are made of two fields packed into 16 bits:
- The *Object Type*. Its unpacked size should be 8 bits
- The *Object ID*.

#kblock[
  *When the highest bit is 1*:
  - The Object Type is unpacked from 5 bits to 8 bits. Its highest bit is 1
  - The Object ID is made of 10 bits

  #bytefield(
    bpr: 16,
    bits(1, fill: l_red)[1],
    bits(5, fill: l_red)[5 bits Type],
    bits(10, fill: l_blue)[10 bits Object ID],
  )
]

#kblock[
  *When the highest bit is 0*:
  - The Object Type is unpacked from 7 bits to 8 bits. Its highest bit is 0
  - The Object ID is made of 8 bits

  #bytefield(
    bpr: 16,
    bits(1, fill: l_red)[0],
    bits(7, fill: l_red)[7 bits Type],
    bits(8, fill: l_blue)[8 bits Object ID],
  )
]

Here are two examples to illustrate what is going on. Pay attention to *134* and how it is built. We read 00110 and set
bit 7 to get the unpacked value 10000110.

#figure(
  table(
    columns: 5,
    align: left + horizon,
    [Input Hexa], [Input Binary], [Object ID Size], [Object Type], [Object ID],
    [\$98C8], [10011000 11001000], [10 bits], [134 = 10000110], [200 = 0011001000],
    [\$7130], [01110001 00110000], [8 bits], [113 = 01110001], [48 = 00110000],
  ),
  caption: [Examples of packed identifiers],
) <figure>



== Footer

The footer contains 16-bit PCM samples (in Big Endian, as usual). The sample rate may vary.

#stack(
  dir: ttb,
  spacing: 2pt,

  binary_block("PCM samples", "... bytes", rgb("#44a8fa")),
)


#chapter("Objects", image: image("./assets/hd3-2481-500.svg"), l: "chap1")
#index("Sectioning")

== Intonation tables

The type INTONATION_TABLE (103) is defined in *`itbl.h`*. Each one is simply an array of 12 words of 16 bits. Intonation tables are not really
interesting per se, but some of them provide very useful information. The intonation tables 18 to 22 indicate which
versions of the objects are installed.

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,

    table.header([*ID*], [*Name*], [*Description*]),

    [18], [`Obj B1.00`], [Base ROM objects version],
    [19], [`Obj O1.00`], [Contemporary objects version],
    [20], [`Obj C1.00`], [Orchestral objects version],
    [21], [`Obj P1.00`], [Stereo Grand objects version],
    [22],
    [`J`],
    [OS version (J = Janis, C = Calvin)\
      K2000 models use OS 1.x and 2.x based on ASIC chip called "Janis"\
      K2500 models use OS 3.x and 4.x based on ASIC chip called "Calvin"],
  ),
  caption: [Kurzweil System Object Versions],
)

== Velocity Maps

A velocity map (type `VELOCITY_MAP`, 104) defines how the incoming key velocity is converted into dynamics. Its content is an array of 8 bytes. It is defined in *`vmap.h`*

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`vmapVels`], [8], [Velocity map dynamics value array (8 bytes)],
  ),
  caption: [Velocity map structure (`KFVelocityMap`)],
)

== Pressure Maps

A pressure map (type `PRESSURE_MAP`, 105) defines how the incoming channel pressure (aftertouch) is converted into dynamics. Its content is an array of 8 bytes. It is defined in *`pmap.h`*

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`pmapPrss`], [8], [Pressure map dynamics value array (8 bytes)],
  ),
  caption: [Pressure map structure (`KFPressureMap`)],
)

== Key Maps

=== Overview

A key map, also called pitch map (type `KEYMAP`, 133) associates each key of the keyboard with a sound block and its tuning and volume
information. It is made of a header followed by a variable number of entries.
It is defined in *`kmap.h`*, not *`keymap.h`*

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`name`], [Variable (max 18)], [Object name (stored with a size rounded from 2 to 18 bytes)],
    [`block`], [2], [Sound block ID],
    [`method`], [2], [Method bits (see below)],
    [`pitch`], [2], [Base pitch],
    [`cents`], [2], [Cents per entry],
    [`nelem`], [2], [Number of entries per level],
    [`esize`], [2], [Entry size],
    [`level`], [16], [Offset to timbre level arrays (8 shorts)],
    [`entries`], [Variable], [List of `KeyMapEntry` entries (quantity: `1 + nelem`)],
  ),
  caption: [Key map structure (`KFKeyMap`)],
)

==== Field 'method'

The *`method`* field is a set of bits describing which fields are present in each entry. A key map read via MIDI contains all fields, therefore the field is always `0x17` (00010111).

 The 2 bits tuning type can be:
 - 00: none
 - 01: 8 bit tuning
 - 10: 16 bit tuning

#figure(
  bytefield(
    bpr: 16,
    bitheader(
      marker: true,
      angle: -30deg,
      numbers: none,
      11,
      [#box[`tuning`]],
      13,
      [#box[`volume`]],
      14,
      [#box[`sample`]],
      15,
      [#box[`subsample`]],
    ),
    bits(11, fill: l_gray)[0],
    bits(1, fill: l_red)[1],
    bits(1, fill: l_red)[0],
    bits(1, fill: l_blue)[1],
    bits(1, fill: l_blue)[1],
    bits(1, fill: l_blue)[1],
  ),
  caption: [Method field (0x17) used in MIDI],
) <keymap-method>

==== KeyMapEntry Structure

The fields of an entry depend on the bits of the `method` field:

#figure(
  table(
    align: left,
    columns: (auto, auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Inclusion Condition*], [*Description*]),
    [`tuning` (Short)], [2], [`TUNING_SHORT` (0x10)], [Tuning value on 16 bits],
    [`tuning` (Byte)], [1], [`TUNING_BYTE` (0x08)], [Tuning value on 8 bits],
    [`atten`], [1], [`VOLUME_ATTEN` (0x04)], [Volume attenuation],
    [`sblk`], [2], [`SAMPLE_ID` (0x02)], [Sound block ID (sample block, see `SOUND_BLOCK`, type 134)],
    [`root`], [1], [`SAMPLE_ROOT` (0x01)], [Subsample number / root key],
  ),
  caption: [Key map entry structure (`KeyMapEntry`)],
)

== Master block

The master parameter block (type `TABLE`, 100 defined in *`mastb.h`*) stores the global settings of the unit. 
It is made of four blocks: 
- the master parameter block (MDB)
- the keyboard zone block (ZDB)
- the MIDI channel blocks (CDB)
- the search buffers.

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`m`], [71], [Master Parameter Block (MDB) structure],
    [`z`], [16], [Keyboard Zone Parameter Block (ZDB) structure],
    [`c`], [256], [Array of 16 MIDI Channel Parameter Block (CDB) structures, 16 bytes each],
    [`sb`], [180], [Search buffers (10 x 18 ubytes array)],
  ),
  caption: [Master parameter structure (`KFMasterParameter`)],
)

==== Master Parameter Block

It is defined in *`mdb.h`*

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Tag / identifier],
    [`rmode_rpchg_rANO_displayLink_seqTempoLock_packed`],
    [1],
    [Combined bitfields for rmode, rpchg, rANO, displayLink, seqTempoLock],

    [`scsiID`], [1], [SCSI identifier],
    [`bchan`], [1], [Base channel number],
    [`rvmap`], [2], [Recv velocity map],
    [`rpmap`], [2], [Receive pressure map],
    [`intTbl`], [2], [Intonation table],
    [`intKey`], [1], [Intonation key],
    [`sysxID`], [1], [System exclusive id number],
    [`trans`], [1], [Master receive transpose semitones],
    [`dtune`], [1], [Master tune cents],
    [`sampflags_playflags_packed`], [1], [Combined bitfields for sampflags and playflags],
    [`rfu2`], [1], [Reserved for future use],
    [`samptime`], [1], [Time to sample for],
    [`curSetup`], [2], [Actual current setup, 0 means use master zone],
    [`oldSetup`], [2], [For setup mode, remember last current setup],
    [`curBank`], [2], [Current quick access bank],
    [`curEntry`], [1], [Current quick access entry],
    [`fxflags_outA_outB_fxSwitch_secsamp_rackMix_V_packed`],
    [1],
    [Combined bitfields for fxMode, outA, outB, fxSwitch, secsamp, rackMix, V],

    [`curEffect`], [1], [Current effect, for use with fx override],
    [`localKbdChan`], [1], [Local keyboard channel for rack],
    [`fxMix`], [1], [Effects wet/dry mix, for use with fx override],
    [`echan`], [1], [Effects channel \#; kbdchan/1/2/..16 (used when override is off)],
    [`macroState_rfu3_packed`], [1], [Combined bitfields for macroState, rfu3],
    [`curSong`], [1], [Current song identifier (Song ID)],
    [`tvmap`], [2], [Master touch maps, velocity and pressure],
    [`tpmap`], [2], [Master touch maps, velocity and pressure],
    [`curDisk`], [1], [Current disk identifier],
    [`contrast`], [1], [Contrast adjust for LCD display],
    [`view`], [1], [Display mode; big name or list],
    [`confirm`], [1], [Confirmations on/off],
    [`xflags_kbdTrans_packed`], [1], [Combined bitfields for rfumode, xmode, bmode, xbuttons, xpchg and kbdTrans],
    [`xvmap`], [2], [Xmit maps, velocity and pressure],
    [`xpmap`], [2], [Xmit maps, velocity and pressure],
    [`dchan`], [1], [Drum channel],
    [`seqRecDub_seqRecMode_seqLoop_seqSync_packed`],
    [1],
    [Combined bitfields for seqSync, seqLoop, seqRecMode, seqRecDub],

    [`markList`], [10], [Marker list (10 ubytes)],
    [`seqCountOff_seqClickMode_seqClickChan_packed`],
    [1],
    [Combined bitfields for seqClickChan, seqClickMode, seqCountOff],

    [`seqClock_seqClickKey_packed`], [1], [Combined bitfields for seqClock, seqClickKey],
    [`seqKeyWait_seqClickVel_packed`], [1], [Combined bitfields for seqKeyWait, seqClickVel],
    [`bootDisk_defaultDisk_packed`], [1], [Combined bitfields for bootDisk, defaultDisk],
    [`seqClickProg`], [2], [Program associated with the sequencer click],
    [`seqQuantGrid`], [2], [Quantization grid],
    [`seqQuantAmt`], [1], [Quantization amount],
    [`seqQuantSwing`], [1], [Quantization swing],
    [`listIndex`], [1], [List index],
    [`listTop`], [1], [List top],
    [`seqTempo`], [2], [Sequencer tempo],
  ),
  caption: [Master parameter block (MDB)],
)

The `rmode_rpchg_rANO_displayLink_seqTempoLock_packed` meaning:
- *sTL*: sequencer Tempo Lock
- *dL*: display link
- *rANO*: receive all note off (hard/soft/off)
- *rpchg*: receive program change (short/extended)
- *rmode*: pressure reroute mode (monopress/polypress/normal)
#figure(
  bytefield(
    bpr: 8,
    bitheader(
      marker: false,
      angle: 0deg,
      numbers: none,
      0,
      [7],
      1,
      [6],
      2,
      [5],
      3,
      [4],
      4,
      [3],
      5,
      [2],
      6,
      [1],
      7,
      [0],
    ),
    bits(1, fill: l_purple)[sTL],
    bits(1, fill: l_purple)[dL],
    bits(2, fill: l_blue)[rANO],
    bits(2, fill: l_green)[rpchg],
    bits(2, fill: l_orange)[rmode],
  ),
  caption: [MDB bitfield sequence Tempo Lock and others],
) <mdb-bitfield>

The  `macroState_rfu3_packed` meaning:

#figure(
  bytefield(
    bpr: 8,
    bitheader(
      marker: false,
      angle: 0deg,
      numbers: none,
      0,
      [7],
      1,
      [6],
      2,
      [5],
      3,
      [4],
      4,
      [3],
      5,
      [2],
      6,
      [1],
      7,
      [0],
    ),
    bits(2, fill: l_red)[macroState],
    bits(6, fill: l_purple)[reserved],
  ),
  caption: [MDB bitfield `macroState`],
) <mdb-bitfield>

The `seqRecDub_seqRecMode_seqLoop_seqSync_packed` meaning:

- `seqRecDub`: sequencer merge or erase when recording,
- `seqRecMode`: sequencer  record mode,
- `seqLoop`: sequencer playback mode,
- `seqSync`: sequencer internal or external clock, with room for a sync method.

#figure(
  bytefield(
    bpr: 8,
    bitheader(
      marker: false,
      angle: 0deg,
      numbers: none,
      0,
      [7],
      1,
      [6],
      2,
      [5],
      3,
      [4],
      4,
      [3],
      5,
      [2],
      6,
      [1],
      7,
      [0],
    ),
    bits(1, fill: l_purple)[recDub],
    bits(3, fill: l_orange)[recMode],
    bits(2, fill: l_green)[loop],
    bits(2, fill: l_blue)[sync],
  ),
  caption: [MDB bitfield `seqRecDub_seqRecMode_seqLoop_seqSync_packed`],
) <mdb-seq-bitfield>

The `seqCountOff_seqClickMode_seqClickChan_packed` meaning:

- `seqCountOff`: sequencer count off
- `seqClickMode`: sequencer click mode,
- `seqClickChan`: sequencer click channel

#figure(
  bytefield(
    bpr: 8,
    bitheader(
      marker: false,
      angle: 0deg,
      numbers: none,
      0,
      [7],
      1,
      [6],
      2,
      [5],
      3,
      [4],
      4,
      [3],
      5,
      [2],
      6,
      [1],
      7,
      [0],
    ),
    bits(2, fill: l_purple)[countOff],
    bits(2, fill: l_orange)[clickMode],
    bits(4, fill: l_green)[clickChan],
  ),
  caption: [MDB bitfield `seqCountOff_seqClickMode_seqClickChan_packed`],
) <mdb-click-bitfield>

The `seqClock_seqClickKey_packed` meaning:

- `seqClickKey`: sequencer click key
- `seqClock`: sequencer clock

#figure(
  bytefield(
    bpr: 8,
    bitheader(
      marker: false,
      angle: 0deg,
      numbers: none,
      0,
      [7],
      1,
      [6],
      2,
      [5],
      3,
      [4],
      4,
      [3],
      5,
      [2],
      6,
      [1],
      7,
      [0],
    ),
    bits(7, fill: l_purple)[clickKey],
    bits(1, fill: l_orange)[clock],
  ),
  caption: [MDB bitfield `seqClock_seqClickKey_packed`],
) <mdb-clock-bitfield>

The `seqKeyWait_seqClickVel_packed` meaning:

- `seqClickVel`: sequence click velocity
- `seqKeyWait`: sequencer key wait on/off

#figure(
  bytefield(
    bpr: 8,
    bitheader(
      marker: false,
      angle: 0deg,
      numbers: none,
      0,
      [7],
      1,
      [6],
      2,
      [5],
      3,
      [4],
      4,
      [3],
      5,
      [2],
      6,
      [1],
      7,
      [0],
    ),
    bits(7, fill: l_purple)[clickVel],
    bits(1, fill: l_orange)[wait],
  ),
  caption: [MDB bitfield `seqKeyWait_seqClickVel_packed` (`wait` is `seqKeyWait`)],
) <mdb-keywait-bitfield>

The `bootDisk_defaultDisk_packed` meaning:

- `bootDisk` (4 bits),
- `defaultDisk` (4 bits).

#figure(
  bytefield(
    bpr: 8,
    bitheader(
      marker: false,
      angle: 0deg,
      numbers: none,
      0,
      [7],
      1,
      [6],
      2,
      [5],
      3,
      [4],
      4,
      [3],
      5,
      [2],
      6,
      [1],
      7,
      [0],
    ),
    bits(4, fill: l_purple)[bootDisk],
    bits(4, fill: l_orange)[defaultDisk],
  ),
  caption: [MDB bitfield `bootDisk_defaultDisk_packed`],
) <mdb-disk-bitfield>


==== ZDB Structure (Keyboard Zone Parameter Block)

It is defined in *`zdb.h`*

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Zone tag (= zoneSegTag + zone index \{0...nZONES-1\})],
    [`chan`], [1], [Xmit channel number],
    [`prog`], [2], [Program number],
    [`lokey`], [1], [Zone low key],
    [`hikey`], [1], [Zone high key],
    [`flags`], [1], [Xmit mode (2 bits), transmit prog chng (1 bit), transmit pitch wheel (1 bit)],
    [`trans`], [1], [Transpose],
    [`ctls`], [8], [Control mapping (on/off in high bit, dest in low 7 bits)],
  ),
  caption: [Keyboard zone parameter block (ZDB)],
)

=== CDB Structure (MIDI Channel Parameter Block)

It is defined in *`cdb.h`*

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag],
    [`chan`], [1], [Channel \#, set and forget],
    [`nlyrs`], [1], [\# layers to allocate, usually 3, except drum channel is 32],
    [`flags`], [1], [Channel enable state (1 bit), ignore program changes (1 bit)],
    [`prog`], [2], [Program number],
    [`volume`], [1], [MIDI volume (7 bits), vollock (1 bit)],
    [`pan`], [1], [MIDI pan (7 bits), panlock (1 bit)],
    [`trans`], [1], [Channel transpose; semis],
    [`dtune`], [1], [Channel detune; cents],
    [`brange`], [1], [Bend range override; prog/+-60 semitones],
    [`playflags`], [1], [Mono mode override (2 bits), portamento override (2 bits)],
    [`portRate`], [1], [Portamento rate for use with override],
    [`outflags`], [1], [Output pair override (3 bits), output headroom override (4 bits)],
    [`rfu`], [2], [Reserved for future use (2 bytes)],
  ),
  caption: [MIDI channel parameter block (CDB)],
)

== Program

A program is made of multiple segments identified by a tag.

=== Program Common segment

_Global size: 16 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x08`)],
    [`fmt`], [1], [Format],
    [`numLayers`], [1], [Number of program layers],
    [`modeFlags`], [1], [Mode flags],
    [`bendRange`], [1], [Bend range],
    [`portSlope`], [1], [Portamento slope],
    [`mixControl`], [1], [Mix control],
    [`mixRange`], [1], [Mix range],
    [`coarse1`], [1], [Coarse 1],
    [`control1`], [1], [Control 1],
    [`range1`], [1], [Range 1],
    [`dest1`], [1], [Destination 1],
    [`coarse2`], [1], [Coarse 2],
    [`control2`], [1], [Control 2],
    [`range2`], [1], [Range 2],
    [`dest2`], [1], [Destination 2],
  ),
  caption: [Program common segment (PGMSEGTAG)],
)

=== Program segment

Each segment starts with a tag byte identifying its type, followed by its parameters. The global size of a segment is
the total number of bytes of the segment, tag included. The sections below describe each segment type.

=== Zone segment

_Global size: 16 bytes_

This segment contains the same parameters as the ZDB structure of the master parameter block.

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x03`)],
    [`chan`], [1], [Xmit channel number],
    [`prog`], [2], [Program number],
    [`lokey`], [1], [Zone low key],
    [`hikey`], [1], [Zone high key],
    [`flags`], [1], [Xmit mode (2 bits), transmit prog chng (1 bit), transmit pitch wheel (1 bit)],
    [`trans`], [1], [Transpose],
    [`ctls`], [8], [Control mapping (on/off in high bit, dest in low 7 bits)],
  ),
  caption: [Zone segment (ZONESEGTAG)],
)

=== ARP segment

_Global size: 26 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag` / `loKey`], [1], [Identification tag / arpeggiator low key (value `0x04`)],
    [`hiKey`], [1], [Arpeggiator high key],
    [`initialState`], [1], [Initial state],
    [`latchMode`], [1], [Latch mode],
    [`playOrder`], [1], [Note play order],
    [`glissando`], [1], [Glissando],
    [`tempoSource`], [1], [Tempo source],
    [`onOffControl`], [1], [On/off control],
    [`clocksPerBeat`], [2], [Clock signals per beat],
    [`durationPerBeat`], [2], [Duration per beat],
    [`initialTempo`], [2], [Initial tempo],
    [`velocityMode`], [1], [Velocity mode],
    [`velocityFixed`], [1], [Fixed velocity],
    [`velocityCtrl`], [1], [Velocity control],
    [`noteShift`], [1], [Note shift],
    [`shiftLimit`], [1], [Shift limit],
    [`limitOption`], [1], [Limit option],
    [`arpSyncFlags`], [1], [Arpeggiator sync flags],
    [`rfu1`], [1], [Reserved for future use 1],
    [`rfu2`], [2], [Reserved for future use 2],
    [`rfu3`], [2], [Reserved for future use 3],
  ),
  caption: [Arpeggiator segment (ARPSEGTAG)],
)

=== ASR segment

_Global size: 8 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x10`)],
    [`rfu1`], [1], [Reserved for future use 1],
    [`trigger`], [1], [Trigger],
    [`flags`], [1], [Flags (hold, rept)],
    [`dtime`], [1], [Delay time],
    [`atime`], [1], [Attack time],
    [`rfu2`], [1], [Reserved for future use 2],
    [`rtime`], [1], [Release time],
  ),
  caption: [ASR segment (ASRSEGTAG)],
)

=== Calvin segment

_Global size: 32 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x40`)],
    [`subTag`], [1], [Secondary tag],
    [`trans`], [1], [Transpose],
    [`dtune`], [1], [Fine tuning (cents)],
    [`tkScale`], [1], [Key tracking scale],
    [`tvScale`], [1], [Velocity tracking scale],
    [`tcontrol`], [1], [Control source],
    [`trange`], [1], [Range],
    [`skeymap`], [2], [Object ID of the stereo keymap],
    [`sroot`], [1], [Stereo root key],
    [`slegato`], [1], [Stereo legato],
    [`keymap`], [2], [Object ID of the mono keymap],
    [`root`], [1], [Mono root key],
    [`legato`], [1], [Mono legato],
    [`tshift`], [1], [Pitch shift],
    [`rfu2`], [1], [Reserved for future use 2],
    [`cpitch`], [1], [Coarse pitch],
    [`fpitch`], [1], [Fine pitch],
    [`ckScale`], [1], [Coarse key tracking scale],
    [`cvScale`], [1], [Coarse velocity scale],
    [`pcontrol`], [1], [Pitch control source],
    [`prange`], [1], [Pitch range],
    [`pdepth`], [1], [Pitch depth],
    [`pmin`], [1], [Pitch minimum],
    [`pmax`], [1], [Pitch maximum],
    [`psource`], [1], [Pitch modulation source],
    [`ccr`], [2], [Pitch tracking parameters],
    [`alg`], [1], [Algorithm chosen],
    [`fineHz`], [1], [Fine frequency (Hz)],
  ),
  caption: [Calvin segment (CALSEGTAG)],
)

=== Channel segment

_Global size: 16 bytes_

This segment contains the same parameters as the CDB structure of the master parameter block.

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x02`)],
    [`chan`], [1], [Channel \#, set and forget],
    [`nlyrs`], [1], [\# layers to allocate, usually 3, except drum channel is 32],
    [`flags`], [1], [Channel enable state (1 bit), ignore program changes (1 bit)],
    [`prog`], [2], [Program number],
    [`volume`], [1], [MIDI volume (7 bits), vollock (1 bit)],
    [`pan`], [1], [MIDI pan (7 bits), panlock (1 bit)],
    [`trans`], [1], [Channel transpose; semis],
    [`dtune`], [1], [Channel detune; cents],
    [`brange`], [1], [Bend range override; prog/+-60],
    [`playflags`], [1], [Mono mode override (2 bits), portamento override (2 bits)],
    [`portRate`], [1], [Portamento rate for use with override],
    [`outflags`], [1], [Output pair override (3 bits), output headroom override (4 bits)],
    [`rfu1`], [1], [Reserved for future use 1],
    [`rfu2`], [1], [Reserved for future use 2],
  ),
  caption: [Channel segment (CHANSEGTAG)],
)

=== EFX segment

_Global size: 8 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x0F`)],
    [`chan`], [1], [Associated channel],
    [`prog`], [1], [Program],
    [`mix`], [1], [Mix],
    [`ctl1`], [1], [Control 1],
    [`out1`], [1], [Output 1],
    [`ctl2`], [1], [Control 2],
    [`out2`], [1], [Output 2],
  ),
  caption: [Effect segment (EFXSEGTAG)],
)

=== ENC segment

_Global size: 16 bytes_

The ENC segment (`ENCSEGTAG`) and the ATK segment (`ATKSEGTAG`) share the same structure. The tag value tells them
apart: `0x20` for ENC, `0x27` for ATK.

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x20` or `0x27`)],
    [`rfu1`], [1], [Reserved for future use 1],
    [`flags`], [1], [Flags (natural)],
    [`atTime`], [1], [Attack time],
    [`atKScale`], [1], [Attack key tracking scale],
    [`atVScale`], [1], [Attack velocity scale],
    [`atCtl`], [1], [Attack control source],
    [`atRange`], [1], [Attack range],
    [`dtTime`], [1], [Decay time],
    [`dtKScale`], [1], [Decay key tracking scale],
    [`dtCtl`], [1], [Decay control source],
    [`dtRange`], [1], [Decay range],
    [`rtTime`], [1], [Release time],
    [`rtKScale`], [1], [Release key tracking scale],
    [`rtCtl`], [1], [Release control source],
    [`rtRange`], [1], [Release range],
  ),
  caption: [Encoder / attack segment (ENCSEGTAG / ATKSEGTAG)],
)

=== ENV segment

_Global size: 16 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x21`)],
    [`flags`], [1], [Envelope flags (loopSeg, loopCnt)],
    [`segs`], [14], [7 pairs (Time/Level) describing the envelope segments],
  ),
  caption: [Envelope segment (ENVSEGTAG)],
)

=== FCN segment

_Global size: 4 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x18`)],
    [`op`], [1], [Operator / function],
    [`arg1`], [1], [Argument 1],
    [`arg2`], [1], [Argument 2],
  ),
  caption: [Function segment (FCNSEGTAG)],
)

=== LFO segment

_Global size: 8 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x14`)],
    [`rfu1`], [1], [Reserved for future use 1],
    [`rateCtl`], [1], [Rate control],
    [`minRate`], [1], [Minimum rate],
    [`maxRate`], [1], [Maximum rate],
    [`phase`], [1], [Wave phase],
    [`shape`], [1], [Wave shape],
    [`rfu2`], [1], [Reserved for future use 2],
  ),
  caption: [LFO segment (LFOSEGTAG)],
)

=== MASTER segment

_Global size: 72 bytes_

This segment contains the same parameters as the MDB structure of the master parameter block.

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x01`)],
    [`bitfields1`], [1], [Bitfield (seqTempoLock, displayLink, rANO, rpchg, rmode)],
    [`scsiID`], [1], [SCSI identifier],
    [`bchan`], [1], [Base channel number],
    [`rvmap`], [2], [Recv velocity map],
    [`rpmap`], [2], [Receive pressure map],
    [`intTbl`], [2], [Intonation table],
    [`intKey`], [1], [Intonation key],
    [`sysxID`], [1], [System exclusive id number],
    [`trans`], [1], [Master receive transpose semitones],
    [`dtune`], [1], [Master tune cents],
    [`sampflags`], [1], [Sample flags],
    [`playflags`], [1], [Play flags],
    [`rfu2`], [1], [Reserved for future use],
    [`samptime`], [1], [Time to sample for],
    [`curSetup`], [2], [Actual current setup, 0 means use master zone],
    [`oldSetup`], [2], [For setup mode, remember last current setup],
    [`curBank`], [2], [Current quick access bank],
    [`curEntry`], [1], [Current quick access entry],
    [`fxflags`], [1], [Effect flags],
    [`curEffect`], [1], [Current effect, for use with fx override],
    [`localKbdChan`], [1], [Local keyboard channel for rack],
    [`fxMix`], [1], [Effects wet/dry mix, for use with fx override],
    [`echan`], [1], [Effects channel \#; kbdchan/1/2/..16],
    [`bitfields2`], [1], [Bitfield (macroState, rfu3)],
    [`curSong`], [1], [Current song],
    [`tvmap`], [2], [Master touch maps, velocity and pressure],
    [`tpmap`], [2], [Master touch maps, velocity and pressure],
    [`curDisk`], [1], [Current disk],
    [`contrast`], [1], [Contrast adjust for LCD display],
    [`view`], [1], [Display mode; big name or list],
    [`confirm`], [1], [Confirmations on/off],
    [`xflags`], [1], [Transmission flags (xmit)],
    [`kbdTrans`], [1], [Keyboard transmission],
    [`xvmap`], [2], [Xmit maps, velocity and pressure],
    [`xpmap`], [2], [Xmit maps, velocity and pressure],
    [`dchan`], [1], [Drum channel],
    [`bitfields3`], [1], [Bitfield (seqRecDub, seqRecMode, seqLoop, seqSync)],
    [`markList`], [10], [Mark list],
    [`bitfields4`], [1], [Bitfield (seqCountOff, seqClickMode, seqClickChan)],
    [`bitfields5`], [1], [Bitfield (seqClickKey, seqClock)],
    [`bitfields6`], [1], [Bitfield (seqClickVel, seqKeyWait)],
    [`bitfields7`], [1], [Bitfield (bootDisk, defaultDisk)],
    [`seqClickProg`], [2], [Sequencer click program],
    [`seqQuantGrid`], [2], [Quantization grid],
    [`seqQuantAmt`], [1], [Quantization amount],
    [`seqQuantSwing`], [1], [Quantization swing],
    [`listIndex`], [1], [List index],
    [`listTop`], [1], [List top],
    [`seqTempo`], [2], [Sequencer tempo],
  ),
  caption: [Master segment (MASTSEGTAG)],
)

=== FX Root segment

_Global size: 8 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x68`)],
    [`vers`], [1], [Version],
    [`studio`], [2], [Studio ID],
    [`rfu`], [4], [Reserved for future use (4 bytes)],
  ),
  caption: [Effect root table segment (FXRTSEGTAG)],
)

=== FX Part segment

_Global size: 8 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x69`)],
    [`num`], [1], [Effect slot number],
    [`strip`], [1], [Effect strip],
    [`param`], [1], [Parameter number],
    [`adjust`], [1], [Adjustment],
    [`source`], [1], [Modulation source],
    [`depth`], [1], [Depth],
    [`rfu`], [1], [Reserved for future use],
  ),
  caption: [Effect part segment (FXPTSEGTAG)],
)

=== Hobbes segment

_Global size: 16 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x50`)],
    [`subTag`], [1], [Secondary tag],
    [`coarse`], [1], [Coarse value],
    [`fine`], [1], [Fine value],
    [`kScale`], [1], [Key tracking scale],
    [`vScale`], [1], [Velocity tracking scale],
    [`control`], [1], [Control source],
    [`range`], [1], [Range],
    [`depth`], [1], [Depth],
    [`minDepth`], [1], [Minimum depth],
    [`maxDepth`], [1], [Maximum depth],
    [`source`], [1], [Modulation source],
    [`bitfields1`], [1], [Bitfield (downShift, filtAlg)],
    [`moreTscr`], [1], [Additional parameters],
    [`bitfields2`], [1], [Bitfield (headRoom, pair, rfu2)],
    [`bitfields3`], [1], [Bitfield (rfu, panMode, pan)],
  ),
  caption: [Hobbes segment (HOBSEGTAG)],
)

=== Layer segment

_Global size: 16 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x09`)],
    [`loEnable`], [1], [Low enable],
    [`trans`], [1], [Transpose],
    [`tune`], [1], [Tune],
    [`loKey`], [1], [Layer low key],
    [`hiKey`], [1], [Layer high key],
    [`vRange`], [1], [Velocity range (vMax, vMin, vRfu)],
    [`eSwitch`], [1], [Enable switch],
    [`flags`], [1], [Flags (ignRels, ignSust, ignSost, ignSusp, atkHold, susHold)],
    [`moreFlags`], [1], [More flags (pwlDis, enbRev, opaque, stereo, channum, keyUp)],
    [`vTrig`], [1], [Velocity trigger (vt1Level, vt1Sense, vt2Level, vt2Sense)],
    [`hiEnable`], [1], [High enable],
    [`dlyCtl`], [1], [Delay control],
    [`dlyMin`], [1], [Delay minimum],
    [`dlyMax`], [1], [Delay maximum],
    [`xfade`], [1], [Crossfade],
  ),
  caption: [Layer segment (LYRSEGTAG)],
)

=== Hamm segment 1

_Global size: 32 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x78`)],
    [`version`], [1], [Version],
    [`basekey`], [1], [Key \# of base tone wheel],
    [`nwheels`], [1], [\# tone wheels],
    [`bitfields1`], [1], [Bitfield (toneswap, nopreamp, perctrig, livebars, barquant, keynoise, noizmulti, noizfiltr)],
    [`toneleak`], [1], [Tone wheel leakage level],
    [`resmap`], [1], [Keyboard resistor map \#],
    [`bartones`], [9], [Draw bar tuning (semis) (9 bytes)],
    [`volmap`], [1], [Tone wheel volume map \#],
    [`volume`], [1], [Tone wheel volume adjust],
    [`balance`], [1], [Tone wheel hi/low balance],
    [`emph_g`], [1], [Preemphasis gain],
    [`emph_f`], [1], [Preemphasis frequency],
    [`bass_g`], [1], [Bass shelf gain],
    [`bass_f`], [1], [Bass shelf frequency],
    [`par1_g`], [1], [Parametric EQ 1 gain],
    [`par1_f`], [1], [Parametric EQ 1 frequency],
    [`par1_q`], [1], [Parametric EQ 1 Q],
    [`par2_g`], [1], [Parametric EQ 2 gain],
    [`par2_f`], [1], [Parametric EQ 2 frequency],
    [`par2_q`], [1], [Parametric EQ 2 Q],
    [`treb_g`], [1], [Treble shelf gain],
    [`treb_f`], [1], [Treble shelf frequency],
    [`noizvol`], [1], [Noise volume adjust],
  ),
  caption: [Hammond segment 1 (HAMSEG1TAG)],
)

=== Hamm segment 2

_Global size: 32 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x79`)],
    [`noizpitch`], [1], [Noise pitch],
    [`noizlevel`], [1], [Noise level (dB/10)],
    [`noizdecay`], [1], [Noise decay (enum)],
    [`perc1`], [1], [Percussion parameters bitfield (percmode, percharm, percfast, percloud)],
    [`perc2`], [1], [Percussion harmonic parameters bitfield (percbarT, percbarL, percbarH)],
    [`noizvel`], [1], [Noise velocity tracking],
    [`percvel`], [1], [Percussion velocity tracking],
    [`perclevel`], [4], [Percussion levels (dB/10) (4 bytes)],
    [`percdecay`], [4], [Percussion decays (enum) (4 bytes)],
    [`perclcomp`], [4], [Loud/soft compensation (db/10) (4 bytes)],
    [`rfu2`], [4], [Reserved for future use (4 bytes)],
    [`bass_q`], [1], [Bass EQ Q parameter],
    [`treb_q`], [1], [Treble EQ Q parameter],
    [`noizrtrig`], [1], [Noise re-trigger threshold],
    [`leakmode`], [1], [Leakage mode],
    [`leslieCtl`], [1], [Leslie control source],
    [`chorusCtl`], [1], [Chorus/vibrato control source],
    [`chorusSelect`], [1], [Chorus/vibrato mode selection],
    [`rfu3`], [1], [Reserved for future use],
  ),
  caption: [Hammond segment 2 (HAMSEG2TAG)],
)

=== Hamm segment 3

_Global size: 32 bytes_

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field Name*], [*Size in Bytes*], [*Description*]),
    [`tag`], [1], [Identification tag (value `0x7A`)],
    [`drawbars`], [9], [Drawbar levels (9 bytes)],
    [`rfu6`], [1], [Reserved for future use 6],
    [`rfu7`], [1], [Reserved for future use 7],
    [`keyclick`], [1], [Key click activation switch],
    [`relclick`], [1], [Release click activation switch],
    [`noizran`], [1], [Random noise],
    [`rfu4`], [1], [Reserved for future use 4],
    [`loTune`], [1], [Low tuning adjustment],
    [`rcvMap`], [1], [MIDI receive map (0 = Kurzweil, 1 = Voce)],
    [`rfu5`], [14], [Reserved for future use (14 bytes)],
  ),
  caption: [Hammond segment 3 (HAMSEG3TAG)],
)

#include "part1-midi-protocol.typ"

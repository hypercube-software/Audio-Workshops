#import "settings.typ": *

#chapter("Objects", image: image("./assets/hd3-2481-500.svg"), l: "chap1")
#index("Sectioning")

The following data structures are shared between the PRAM format and the MIDI format. They are the direct transcription of officials .H

== Intonation tables

The type INTONATION_TABLE (103) is defined in *`itbl.h`*. Each one is simply an array of 12 words of 16 bits. Intonation
tables are not really interesting per se, but some of them provide very useful information. The intonation tables 18 to
22 indicate which versions of the objects are installed.

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

A velocity map (type `VELOCITY_MAP`, 104) defines how the incoming key velocity is converted into dynamics. Its content
is an array of 8 bytes. It is defined in *`vmap.h`*

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

A pressure map (type `PRESSURE_MAP`, 105) defines how the incoming channel pressure (aftertouch) is converted into
dynamics. Its content is an array of 8 bytes. It is defined in *`pmap.h`*

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

A key map, also called pitch map (type `KEYMAP`, 133) associates each key of the keyboard with a sound block and its
tuning and volume information. It is made of a header followed by a variable number of entries. It is defined in
*`kmap.h`*, not *`keymap.h`*

#figure(image("assets/image-19.png"), caption: [KEYMAP page])

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

=== Field 'method'

The *`method`* field is a set of bits describing which fields are present in each entry. A key map read via MIDI
contains all fields, therefore the field is always `0x17` (00010111).

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

=== KeyMapEntry Structure

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

The master parameter block (type `TABLE`, 100 defined in *`mastb.h`*) stores the global settings of the unit. It is made
of four blocks:
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

=== Master Parameter Block

It is defined in *`mdb.h`*. This object cover many things about kurzweil units:
- How the unit receive MIDI messages
- Which maps are used (Velocity, Pressure)
- How the sequencer is used (Song mode)

#figure(image("assets/image-17.png"), caption: [RECEIVE page])

#figure(image("assets/image.png"), caption: [The Master mode page])

#figure(image("assets/image-1.png"), caption: [The song page])

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

The `macroState_rfu3_packed` meaning:

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
- `seqRecMode`: sequencer record mode,
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

- `bootDisk`: boot Disk
- `defaultDisk`: default disk

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


=== Keyboard Zone Parameter Block

It is defined in *`zdb.h`* This is related to the TRASMIT page:

#figure(image("assets/image-16.png"), caption: [TRANSMIT page])

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

=== MIDI Channel Parameter Block

It is defined in *`cdb.h`*. This object is used in multichannel mode.
- Which program is used on a specific MIDI channel
- Which program parameter is overriden for this MIDI channel, especially output pair towards
  KDFX-A,KDFX-B,KDFX-C,KDFX-D.

It is related to the following page:

#figure(image("assets/image-18.png"), caption: [CHANNELS page])


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

This is related to the arpeggiator.

#figure(image("assets/image-15.png"), caption: [ARPEG page])

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

ASRs are three-section unipolar envelopes (attack, sustain, and release).
- they can be triggered by a programmable control source
- they can be delayed

It will appear multiple times inside a program. You have typically 1 ASR for the program, and multiple ones in each
program layers.

#figure(image("assets/image-23.png"), caption: [ASR page])

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

This setup the core V.A.S.T engine, more espetially the algorithm.

#image("assets/image-14.png")

*PITCH* and *AMP* are fixed whereas things vary in the middle depending of the chosen algorithm. The Calvin segment is
related to the following pages:

#figure(image("assets/image-13.png"), caption: [PITCH page])

#figure(image("assets/image-12.png"), caption: [F4 AMP page])


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

This segment contains the same parameters as the CDB structure of the master parameter block. It sounds like "Common
Data Block", but the `.h` use "Channel segment" as title. It is related to the following page:

#figure(
  image("assets/image-2.png"),
  caption: [Program Common page],
)

_Global size: 16 bytes_

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

This segment allow some overrides upon a Studio object used in a program.

#figure(image("assets/image-20.png"), caption: [KDFX page])

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

=== ENC/ATK segment

The ENC segment (`ENCSEGTAG`) and the ATK segment (`ATKSEGTAG`) share the same structure. The tag value tells them
apart: `0x20` for ENC, `0x27` for ATK. Both appear in each layer.

- ENV stands for "Enveloppe control"
- ATK stands for "Impact/attack control" which is not avaiable in K2000 units

They are related to the following pages:

#figure(image("assets/image-24.png"), caption: [AMPENV page 1])

#figure(image("assets/image-26.png"), caption: [ENVCTL page])

_Global size: 16 bytes_

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

We typically found 3 of them inside a layer. They certainly match the 3 enveloppes called AMPENV, ENV2 and ENV3:

#figure(image("assets/image-21.png"), caption: [AMPENV page 2])

#figure(image("assets/image-25.png"), caption: [ENV2 page])

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

This segment is related to the FUN page. You can have 4 of them.
- Which FUN is used
- Which input parameter is used to call it

#figure(image("assets/image-22.png"), caption: [FUN page])

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

Can be found in LFO Page

#figure(
  image("assets/image-3.png"),
  caption: [LFO page],
)

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
  image("assets/image.png"),
  caption: [The Master mode page],
)

#figure(
  image("assets/image-1.png"),
  caption: [The song page],
)

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

Defined in *`frtb.h`*, this segment assign a specific Studio to the program. It is related to the KDFX page.

#figure(image("assets/image-20.png"), caption: [KDFX page])

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

Also called "FX Patch". This segment is defined in *`frtb.h`*. The goal here, is to control some Studio parameters with
a specific modulation source. You can typically found 18 of them in a single program.

It is related to the following pages:

#figure(image("assets/image-27.png"), caption: [KDFX params page])
#figure(image("assets/image-28.png"), caption: [KDFX MOD2,3,4 pages])

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

This is related to PITCH page

#figure(image("assets/image-4.png"), caption: [Pitch page])

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

Related to the layer page:

#figure(image("assets/image-5.png"), caption: [LAYER page])

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

=== KB3 Hammond segment 1

Related to TONEWL page:

#figure(image("assets/image-6.png"), caption: [Tone Wheel page])


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

=== KB3 Hammond segment 2

Related to the following pages:

#figure(image("assets/image-9.png"), caption: [PERC page])

#figure(image("assets/image-10.png"), caption: [PERC2 page])

#figure(image("assets/image-7.png"), caption: [MISC page])

#figure(image("assets/image-11.png"), caption: [KEYCLK page])

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

=== KB3 Hammond segment 3

Related to DRAWBR page:

#figure(image("assets/image-8.png"), caption: [DRAWBR page])


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

#import "../common/settings.typ": *
#import "lib.typ": *

#part("Global settings")

#chapter("System settings", l: "chap-system")

The system settings live in a single 16-byte block at `0x00000000`. They are the only settings of the instrument that
are not stored in a patch, a pattern or a song: the MIDI channel, the clock source, the destination of the controllers
and of the program changes, the soft through, the MIDI key trigger, the two energy-saving options and the CV
calibration.

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Packed address*], [*Packed Size*], [*Button*], [*Parameter*], [*Values*], [*Meaning*]),
    [`00 00 00 00`], [`00 00 00 01`], [`1`], [MIDI TX Ch], [`1`..`16`],
    [Channel used to transmit. The menu also offers *Off* and one *OMNI* entry per channel, stored as other values],
    [`00 00 00 01`], [`00 00 00 01`], [`5`], [TX CC], [`0` `1` `2` `3`],
    [`Off`, `USB`, `MIDI`, `USB and MIDI` — where the controllers are sent],
    [`00 00 00 02`], [`00 00 00 01`], [—], [TX PC], [`0` `1` `2` `3`],
    [Same choices as *TX CC*, for the program changes. The editor writes `3` at start-up],
    [`00 00 00 03`], [`00 00 00 01`], [—], [Soft Through], [`0` `1`],
    [`Off`, `On` — the MIDI input is merged into the MIDI output],
    [`00 00 00 04`], [`00 00 00 01`], [`2`], [Chain Mode], [`0` `1`],
    [`OFF`, `On` — the program change of a patch also loads its pattern],
    [`00 00 00 05`], [`00 00 00 01`], [`7`], [Sync], [`0` `2` `3`],
    [`Int`, `MIDI IN`, external clock (USB or trigger)],
    [`00 00 00 06`], [`00 00 00 01`], [—], [MIDI Key Trigger], [`0` `1` `2`],
    [`OFF`, `On`, `trn` — notes received on the MIDI input play the patch],
    [`00 00 00 07`], [`00 00 00 01`], [`3`], [Auto Off], [`0` `1` `2`],
    [`OFF`, 30 minutes, 240 minutes],
    [`00 00 00 08`], [`00 00 00 01`], [`4`], [Screen Saver Time], [`0` `1` `2` `3`],
    [`OFF`, 1 minute, 3 minutes, 10 minutes],
    [`00 00 00 09`], [`00 00 00 01`], [—], [reserved], [`0`], [],
    [`00 00 00 0A`], [`00 00 00 02`], [—], [CV Fine Tune], [`0`..`255`],
    [Fine tune applied to the CV, stored as 2 nibbles],
    [`00 00 00 0C`], [`00 00 00 02`], [—], [CV Key Follow], [`0`..`255`],
    [Key follow applied to the CV, stored as 2 nibbles],
    [`00 00 00 0E`], [`00 00 00 01`], [—], [reserved], [`0`], [],
    [`00 00 00 0F`], [`00 00 00 01`], [—], [reserved], [`0`], [],
  ),
  caption: [Content of the system block],
)

A system setting is a 7-bit value: it takes 1 byte in the payload, not 2 nibbles, so its size field is
`00 00 00 01`. Two fields of the block are wider: the CV fine tune and the CV key follow, at `0x0000000A` and
`0x0000000C`, take 2 nibbles each and their size field is `00 00 00 02`. The 16 bytes are therefore full: ten
settings of 1 byte, 2 values of 2 nibbles, then 2 reserved bytes.

The *Button* column gives the front-panel button used in the *System Setup* of the unit (hold `EXIT` while powering
on), so that a setting can be located on the instrument. *TX CC* is button 5 at `0x00000001`, *Auto Off* is button 3 at
`0x00000007`, and *Screen Saver Time* is button 4 at `0x00000008`. Button 16 displays the firmware version; it is
read-only and not stored in the block.

== Writing the block

A complete write is a `DT1` of 16 bytes to `0x00000000`. The MIDI frame below sets the transmit channel to 1,
sends the controllers and the program changes on both USB and MIDI, keeps the internal clock, and turns the
energy-saving options off. Its checksum is `79`.

#figure(
  bytefield(
    bpr: 120,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[41],
    bytes(1, fill: l_gray)[10],
    bytes(4, fill: l_gray)[model 00 00 00 44],
    bytes(1, fill: l_blue)[12],
    bytes(4, fill: l_green)[address 00 00 00 00],
    bytes(1, fill: l_orange)[01],
    bytes(1, fill: l_orange)[03],
    bytes(1, fill: l_orange)[03],
    bytes(1, fill: l_orange)[00],
    bytes(1, fill: l_orange)[00],
    bytes(1, fill: l_orange)[00],
    bytes(1, fill: l_orange)[00],
    bytes(1, fill: l_orange)[00],
    bytes(1, fill: l_orange)[00],
    bytes(1, fill: l_orange)[00],
    bytes(1, fill: l_orange)[00],
    bytes(1, fill: l_orange)[00],
    bytes(1, fill: l_orange)[00],
    bytes(1, fill: l_orange)[00],
    bytes(1, fill: l_orange)[00],
    bytes(1, fill: l_orange)[00],
    bytes(1, fill: l_red)[79],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [Writing the system block],
)

The payload is the block itself, in order. Three bytes are not zero:

- `01` at `0x00000000`: MIDI TX Ch, the unit transmits on channel 1;
- `03` at `0x00000001`: TX CC, the controllers go out on USB and MIDI;
- `03` at `0x00000002`: TX PC, the program changes go out on USB and MIDI.

Every other byte is `00`: soft through off, chain off, internal clock, MIDI key trigger off, both
energy-saving options off, and the two CV calibration values at `0`.

Reading it back is a `RQ1` with size `00 00 00 10` and checksum `70`, as shown in #cref(<chap-sysex>).

== Sync is shared by two menus

The clock source at `0x00000005` is exposed in 2 menus:

- In *System Setup*, button 7;
- In the *SEQ SETUP* of the sequencer (`NOTE` + `GATE`), button 2.

Both write the same byte, so a single write at `0x00000005` sets the clock source of the sequencer. The *SONG SETUP*
has its own *Sync* entry, stored in the song header at `0x20000004`.

== The second system block

The block at `0x02000000` is 16 bytes of reserved internal storage. It is readable and writable at its address. On
this unit, it holds:

#figure(
    bytefield(
        bpr: 128,
        bytes(1, fill: l_orange)[00],
        bytes(1, fill: l_orange)[01],
        bytes(1, fill: l_orange)[01],
        bytes(1, fill: l_orange)[01],
        bytes(1, fill: l_orange)[00],
        bytes(1, fill: l_orange)[01],
        bytes(1, fill: l_orange)[00],
        bytes(1, fill: l_orange)[07],
        bytes(8, fill: l_gray)[00 00 00 00 00 00 00 00],
    ),
    caption: [Content of the reserved block],
)

The official software editor reads it at start-up: a faithful backup keeps these 16 bytes as they are, and a restore
writes them back.

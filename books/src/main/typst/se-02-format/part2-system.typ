#import "../common/settings.typ": *
#import "lib.typ": *

#part("Global settings")

#chapter("System settings", image: red-chapter-banner, l: "chap-system")

The system settings live in a single 16-byte block at `0x00000000`. They are the only settings of the instrument that
are not stored in a patch, a pattern or a song: the MIDI channel, the clock source, the destination of the controllers
the unit sends, and the two energy-saving options.

#figure(
  ptable(
    columns: (auto, auto, auto, auto, 1fr),
    head([*Offset*], [*Button*], [*Parameter*], [*Values*], [*Meaning*]),
    [`0`], [`1`], [MIDI Ch], [`1`..`16`],
    [Channel 1 to 16. The menu also offers *Off* and one *OMNI* entry per channel, stored as other values],
    [`1`], [`5`], [CC Out], [`0` `1` `2` `3`], [`Off`, `USB`, `MIDI`, `USB and MIDI` — where the controllers are sent],
    [`2`], [—], [unknown], [`3`], [Written as 3 by the official software editor at start-up; no menu entry has been found for it],
    [`3`], [—], [reserved], [`0`], [] ,
    [`4`], [`2`], [Chain], [`0` `1`], [`OFF`, `On` — the program change of a patch also loads its pattern],
    [`5`], [`7`], [Sync], [`0` `2` `3`], [`Int`, `MIDI IN`, external clock (USB or trigger)],
    [`6`], [—], [reserved], [`0`], [],
    [`7`], [`3`], [Auto Off], [`0` `1` `2`], [`OFF`, 30 minutes, 240 minutes],
    [`8`], [`4`], [Saver Time], [`0` `1` `2` `3`], [`OFF`, 1 minute, 3 minutes, 10 minutes],
    [`9`..`15`], [—], [reserved], [`0`], [],
  ),
  caption: [Content of the system block at `0x00000000`],
)

The *Button* column gives the front-panel button used in the *System Setup* of the unit (hold `EXIT` while powering
on), so that a setting can be located on the instrument. *CC Out* is button 5 stored in byte 1, *Auto Off* is button 3
stored in byte 7, and *Saver Time* is button 4 stored in byte 8. Button 16 displays the firmware version; it is
read-only and not stored in the block.

== Writing the block

A complete write is a `DT1` of 16 bytes to `0x00000000`. The frame below puts the unit on MIDI channel 1, sends the
controllers on USB, and expects an external clock. Its 16 data bytes are
`01 03 03 00 00 00 00 00 00 00 00 00 00 00 00 00`, and its checksum is `79`.

#figure(
  bytefield(
    bpr: 160,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[41],
    bytes(1, fill: l_gray)[10],
    bytes(4, fill: l_gray)[model 00 00 00 44],
    bytes(1, fill: l_blue)[12],
    bytes(4, fill: l_green)[address 00 00 00 00],
    bytes(6, fill: l_orange)[16 data bytes],
    bytes(1, fill: l_red)[79],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [Writing the system block],
)

Reading it back is a `RQ1` with size `00 00 00 10` and checksum `70`, as shown in #cref(<chap-sysex>).

The block is a good candidate for a "backup before anything else" habit: it is 16 bytes, and it is the only place where
the clock source is stored. A unit whose byte 5 is left at `3` waits for an external clock and its own sequencer
refuses to start.

== Sync is shared by three menus

The clock source at offset 5 is exposed three times on the unit:

- in *System Setup*, button 7;
- in the *SEQ SETUP* of the sequencer (`NOTE` + `GATE`), button 2;
- in the *SONG SETUP* (`EDIT` + `PTN`), button 2.

All three write exactly the same byte. It is therefore not necessary to visit one menu after another: a single write to
offset 5 configures all the sequencer modes at once. In other words, a pattern does *not* carry its own clock source,
and neither does a song.

== MIDI channel and device ID

Changing the MIDI channel does not change the device ID of the SysEx frames: after setting the channel to `C2`, the unit
still answers to `10`. As a consequence, an implementation cannot infer the channel from a dump, and must read the
system block to know it.

== The second system block

The block at `0x02000000` (16 bytes too) is read by the official software editor at start-up, like the block at `0x00000000`, but none of
the settings above changes it. Its role is unknown. For a faithful backup, read it and store it as it is; for a restore,
write it back unchanged — no interpretation is needed.

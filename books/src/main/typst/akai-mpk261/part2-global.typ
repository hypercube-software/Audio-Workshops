#import "../common/settings.typ": *
#import "lib.typ": *

#part("Global memory")

#chapter("System settings", l: "chap-global")

The global memory is 377 bytes, at addresses `0x0000` to `0x0278`, read with `QGLOB` and written with `WGLOB`. It
holds the settings of the unit itself: how it behaves at power-on, how its pads and keys respond to velocity, and the
state of the buttons that the front panel uses while a preset is played.

== Layout

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Packed address*], [*Packed size*], [*Parameter*], [*Stored*], [*Ref.*], [*Meaning*]),
    [`00 00`], [`00 01`], [reserved], [`00`], [`00`], [Identical in every dump examined],
    [`00 01`], [`00 01`], [Current preset], [`0`..`29`], [`07`], [Index of the preset in the edit buffer, counted from
      zero: `07` is the preset displayed as 8],
    [`00 02`], [`00 01`], [Contrast], [`0`..`99`], [`32`], [Contrast of the display, 50 by default],
    [`00 03`], [`00 01`], [Pad velocity sensitivity], [`0`..`9`], [`04`], [Sensitivity minus 1: `04` is the setting
      displayed as `5`],
    [`00 04`], [`00 01`], [Pad velocity curve], [`0`..`4`], [`00`], [`0` linear, `1` exponential 1, `2` exponential 2,
      `3` logarithmic 1, `4` logarithmic 2],
    [`00 05`], [`00 01`], [Pad threshold], [`0`..`10`], [`03`], [Velocity below which a pad hit is ignored],
    [`00 06`], [`00 01`], [Keyboard curve], [`0`..`100`], [`3C`], [Curve minus 50: `3C` is `+10`],
    [`00 07`], [`00 01`], [Keyboard gain], [`0`..`40`], [`14`], [Gain minus 20: `14` is `0`],
    [`00 08`], [`00 01`], [White key sensitivity], [`0`..`40`], [`0A`], [Sensitivity minus 20: `0A` is `-10`],
    [`00 09`], [`00 01`], [Black key sensitivity], [`0`..`40`], [`0A`], [Sensitivity minus 20: `0A` is `-10`],
    [`00 0A`], [`00 01`], [Common channel], [`0`..`31`], [`00`], [Channel shared by the presets that use *Common*: `00`
      is USB A1, `1F` is USB B16],
    [`00 0B`], [`00 01`], [Tap average], [`2`..`4`], [`03`], [Number of taps averaged by the tap tempo],
    [`00 0C`], [`00 01`], [Clock source], [`0` `1`], [`01`], [Internal, external],
    [`00 0D`], [`00 01`], [Tempo LED], [`0` `1`], [`01`], [Whether the LED follows the tempo],
    [`00 0E`], [`00 01`], [Note display], [`0` `1`], [`00`], [Note shown by value or by number],
    [`00 0F`], [`00 01`], [Transport to DIN], [`0` `1`], [`00`], [Whether transport messages are copied to the DIN
      output],
    [`00 10`], [`00 03`], [unidentified], [—], [`00 00 00`], [Firmware flags; the front panel, the MIDI ports and the
      display show the same behaviour whatever their value],
    [`00 13`], [`00 08`], [Live states], [—], [—], [State of 8 buttons, see below],
    [`00 1B`], [`00 0A`], [unidentified], [—], [—], [Internal state],
    [`00 25`], [`00 05`], [Program Change mode], [—], [—], [Volatile settings of that mode, see below],
    [`00 2A`], [`02 4F`], [unidentified], [—], [—], [Internal state],
  ),
  caption: [Content of the global memory],
)

4 settings store a value offset from the one the front panel shows: the pad sensitivity, the keyboard curve, the
keyboard gain and the 2 key sensitivities. A tool that displays them applies the translation of the last column; a tool
that writes them applies the inverse.

== Live states

The 8 bytes at `0x0013` to `0x001A` mirror the buttons of the front panel rather than a setting of the user. The unit
rewrites them as the buttons are pressed, and they read back the current state:

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Packed address*], [*Packed size*], [*State*], [*Stored*], [*Ref.*], [*Meaning*]),
    [`00 13`], [`00 01`], [Arp Latch], [`0` `1`], [`00`], [The arpeggiator keeps playing after the keys are released],
    [`00 14`], [`00 01`], [Arp], [`0` `1`], [`00`], [The arpeggiator is running],
    [`00 15`], [`00 01`], [Full Level], [`0` `1`], [`00`], [The pads send velocity 127],
    [`00 16`], [`00 01`], [16 Level], [`0` `1`], [`00`], [The pads send a fixed velocity set by a knob],
    [`00 17`], [`00 01`], [Note Repeat], [`0` `1`], [`00`], [The note repeat is armed],
    [`00 18`], [`00 01`], [Pad bank], [`0`..`3`], [`00`], [Bank `A` to `D` of the 16 pads],
    [`00 19`], [`00 01`], [Control bank], [`0`..`2`], [`00`], [Bank `A` to `C` of the knobs, faders and switches],
    [`00 1A`], [`00 01`], [Time division], [`0` `1`], [`00`], [State of the time division button of the note repeat],
  ),
  caption: [The live states of the global memory],
)

#caveat[
  These bytes are *outputs* of the front panel. Whether a write changes the state of the unit has not been established,
  so a tool treats the 8 bytes as read-only and changes the corresponding setting where it belongs: the pad bank and
  the control bank select which of the 64 pads and 24 knobs a performance uses, and the memory of the preset always
  contains all of them (see #cref(<chap-controls>)).
]

== Program Change mode

The *Program Change* mode of the front panel sends a program change when a pad is hit. Its settings are volatile: they
live in the global memory, and the unit clears them when it is powered off, so a backup of the user presets can leave
them out.

#figure(
  ptable(
    columns: (auto, auto, auto, auto, 1fr),
    head([*Packed address*], [*Packed size*], [*Setting*], [*Stored*], [*Meaning*]),
    [`00 24`], [`00 01`], [reserved], [`00`], [Most significant byte of the bank; the firmware has no setting for it],
    [`00 25`], [`00 01`], [Channel], [`0`..`31`], [Channel of the program change: Common, USB A1 to B16],
    [`00 26`], [`00 01`], [Program], [`0`..`127`], [Program number sent],
    [`00 27`], [`00 01`], [reserved], [`00`], [Least significant byte of the bank],
    [`00 28`], [`00 01`], [reserved], [`00`], [—],
    [`00 29`], [`00 01`], [MIDI to DIN], [`0` `1`], [Whether the program change is copied to the DIN output],
  ),
  caption: [Settings of the Program Change mode],
)

A *Program Bank* change needs a bank select before the program change, that is a most significant and a least
significant byte. The mode offers only a program number, and the 2 bytes that would hold the bank are reserved.

== Undocumented bytes

2 ranges have no known meaning:

- The bytes `0x0010` to `0x0012` hold flags that the firmware sets and clears; the front panel, the MIDI ports and the
  display show the same behaviour whatever their value.
- The bytes `0x001B` to `0x0278` hold internal state, including mirrors of values that exist elsewhere. 2 of them,
  `0x001E` and `0x0034`, are interface states: they move when a page is opened, and return to their previous value
  when it is left.

A tool that restores a global memory block writes these bytes back as they were read. They are the reason a backup is
a dump and not a list of settings.

== Writing the block

A write of the documented part is a single message of 16 data bytes at address `0x0000`. With the content of the
reference unit it is:

#figure(
  bytefield(
    bpr: 136,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[30],
    bytes(2, fill: l_purple)[00 13],
    bytes(1, fill: l_orange)[10],
    bytes(2, fill: l_green)[00 00],
    bytes(6, fill: l_blue)[16 data bytes],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [Writing the system settings],
)

The size is `00 13`, that is 16 data bytes plus the count and the address. The unit answers with `ACKG`, whose status
byte is `00`. A single setting can also be written alone: the contrast, at address `0x0002`, is 1 byte and takes a
message of size `00 04`.

#caveat[
  Writing the global memory overwrites settings that the user cannot restore from the front panel without knowing them,
  in particular the contrast and the keyboard response. Read the 16 documented bytes first, keep them, and write back a
  complete block rather than isolated bytes.
]

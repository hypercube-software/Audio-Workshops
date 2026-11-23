#import "../common/settings.typ": *
#import "lib.typ": *

#part("Global settings")

#chapter("System settings", l: "chap-global")

The global settings in RAM occupy 377 bytes, at addresses `0x0000` to `0x0278`, read with the `QGLOB` command and written
with the `WGLOB` command. They decide how the unit behaves at power-on, how its pads and keys respond to velocity, the
device ID of the protocol, and the state of the buttons that the front panel uses while a preset is played.

== Memory layout

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Packed address*], [*Size*], [*Parameter*], [*Stored*], [*Ref.*], [*Meaning*]),
    [`00 00`], [1], [reserved], [`00`], [`00`], [Identical in every dump examined],
    [`00 01`], [1], [Current preset], [`0`..`29`], [`07`], [Index of the preset in the edit buffer, counted from
      zero: `07` is the preset displayed as 8. Writing the byte loads that user preset],
    [`00 02`], [1], [Contrast], [`0`..`99`], [`32`], [Contrast of the display, 50 by default],
    [`00 03`], [1], [Pad velocity sensitivity], [`0`..`9`], [`04`], [Sensitivity minus 1: `04` is the setting
      displayed as `5`],
    [`00 04`], [1], [Pad velocity curve], [`0`..`4`], [`00`], [`0` linear, `1` exponential 1, `2` exponential 2,
      `3` logarithmic 1, `4` logarithmic 2],
    [`00 05`], [1], [Pad threshold], [`0`..`10`], [`03`], [Velocity below which a pad hit is ignored],
    [`00 06`], [1], [Keyboard curve], [`0`..`100`], [`3C`], [Curve minus 50: `3C` is `+10`],
    [`00 07`], [1], [Keyboard gain], [`0`..`40`], [`14`], [Gain minus 20: `14` is `0`],
    [`00 08`], [1], [White key sensitivity], [`0`..`40`], [`0A`], [Sensitivity minus 20: `0A` is `-10`],
    [`00 09`], [1], [Black key sensitivity], [`0`..`40`], [`0A`], [Sensitivity minus 20: `0A` is `-10`],
    [`00 0A`], [1], [Common channel], [`0`..`31`], [`00`], [Channel shared by the presets that use *Common*: `00`
      is USB A1, `1F` is USB B16],
    [`00 0B`], [1], [Tap average], [`2`..`4`], [`03`], [Number of taps averaged by the tap tempo],
    [`00 0C`], [1], [Clock source], [`0`, `1`], [`01`], [Internal, external],
    [`00 0D`], [1], [Tempo LED], [`0`, `1`], [`01`], [Whether the LED follows the tempo],
    [`00 0E`], [1], [Note display], [`0`, `1`], [`00`], [Note shown by value or by number],
    [`00 0F`], [1], [Transport to DIN], [`0`, `1`], [`00`], [Whether transport messages are copied to the DIN
      output],
    [`00 10`], [3], [Unknown], [—], [`00 00 00`], [Flags that the unit sets and clears; the front panel, the MIDI ports and the
      display show the same behaviour whatever their value],
    [`00 13`], [8], [Live states], [—], [—], [State of 8 buttons, see @live-states],
    [`00 1B`], [10], [Unknown], [—], [—], [Internal state],
    [`00 25`], [5], [Program Change mode], [—], [—], [Settings of that mode, see @program-change-mode],
    [`00 2A`], [5], [Unknown], [—], [—], [Internal state],
    [`00 2F`], [1], [internal], [`0`..`15`], [`00`], [Role not established; saved by the `SGLOB` command],
    [`00 30`], [1], [SysEx device ID], [`0`..`127`], [`00`], [Device ID of the SysEx protocol, see @device-id],
    [`00 31`], [328], [Unknown], [—], [—], [Internal state],
  ),
  caption: [Content of the global settings in RAM],
)

4 settings store a value offset from the one the front panel shows: the pad sensitivity, the keyboard curve, the
keyboard gain and the 2 key sensitivities. The last column gives the translation from the stored value to the
displayed one; the inverse gives the value to store.

There is no global tempo: the *Tempo* page of the global mode writes the tempo of the current preset, at
`0x0008` of the edit buffer (see #cref(<chap-preset>)).

== The device ID <device-id>

The byte at `0x0030` is the device ID of the unit: the address that SysEx uses to designate the receiver of a message,
and to tell several machines apart on the same bus. It travels after the manufacturer ID in every MIDI frame, and the
unit repeats it at `0x0D` of the identity reply (#cref(<chap-inquiry>)). A unit that keeps the factory value sends
`F0 47 00 25`.

The unit writes this ID in the frames that it sends, and it does not filter an incoming frame on the ID: a frame that
carries `00` reaches a unit that carries another value. The byte is part of the settings saved by the `SGLOB` command.

== Live states <live-states>

The 8 bytes at `0x0013` to `0x001A` mirror the buttons of the front panel rather than a setting of the user. The unit
rewrites them as the buttons are pressed, and they read back the current state:

#figure(
  ptable(
    columns: (auto, auto, auto, auto, auto, 1fr),
    head([*Packed address*], [*Size*], [*State*], [*Stored*], [*Ref.*], [*Meaning*]),
    [`00 13`], [1], [Arp Latch], [`0`, `1`], [`00`], [The arpeggiator keeps playing after the keys are released],
    [`00 14`], [1], [Arp], [`0`, `1`], [`00`], [The arpeggiator is running],
    [`00 15`], [1], [Full Level], [`0`, `1`], [`00`], [The pads send velocity 127],
    [`00 16`], [1], [16 Level], [`0`, `1`], [`00`], [The pads send a fixed velocity set by a knob],
    [`00 17`], [1], [Note Repeat], [`0`, `1`], [`00`], [The note repeat is armed],
    [`00 18`], [1], [Pad bank], [`0`..`3`], [`00`], [Bank `A` to `D` of the 16 pads],
    [`00 19`], [1], [Control bank], [`0`..`2`], [`00`], [Bank `A` to `C` of the knobs, faders and switches],
    [`00 1A`], [1], [Time division], [`0`, `1`], [`00`], [State of the time division button of the note repeat],
  ),
  caption: [The live states of the global settings],
)

#caveat[
  These bytes are *outputs* of the front panel. Whether a write changes the state of the unit has not been established,
  so the 8 bytes are read-only, and the corresponding setting changes where it belongs: the pad bank and
  the control bank select which of the 64 pads and 24 knobs a performance uses, and the memory of the preset always
  contains all of them (see #cref(<chap-controls>)).
]

The pad bank is the only live state that the `SGLOB` command saves: the unit uses the same bank after a power cycle.

== Program Change mode <program-change-mode>

The *Program Change* mode of the front panel decides what happens when a pad is hit: the pad sends a program change,
or a bank select followed by a program change, in place of a note.

The mode keeps 3 settings: the *channel* of the message, the *program number*, and the *copy to the DIN output*. The
unit saves none of them: at power-on the mode comes back to its defaults, and they belong to no user preset, so a
backup of the presets does not carry them.

#figure(
  ptable(
    columns: (auto, auto, auto, auto, 1fr),
    head([*Packed address*], [*Size*], [*Setting*], [*Stored*], [*Meaning*]),
    [`00 24`], [1], [reserved], [`00`], [MSB of the bank],
    [`00 25`], [1], [Channel], [`0`..`31`], [Channel of the program change: Common, USB A1 to B16],
    [`00 26`], [1], [Program], [`0`..`127`], [Program number sent],
    [`00 27`], [1], [reserved], [`00`], [LSB of the bank],
    [`00 28`], [1], [reserved], [`00`], [—],
    [`00 29`], [1], [MIDI to DIN], [`0`, `1`], [Whether the program change is copied to the DIN output],
  ),
  caption: [Settings of the Program Change mode],
)

A *Program Change with Bank Change* message adds a bank select before the program change, that is 2 values, the MSB
and the LSB of the bank. The memory keeps a place for them at `0x0024` and `0x0027`, and a third byte at `0x0028`,
which the reference unit leaves at `00`.

== Undocumented bytes

Three ranges have no known meaning:

- The bytes `0x0010` to `0x0012` hold flags that the unit sets and clears; the front panel, the MIDI ports and the
  display show the same behaviour whatever their value.
- The bytes `0x001B` to `0x0024` and `0x002A` to `0x002E` hold internal state.
- The bytes `0x0031` to `0x0278` hold internal state, including mirrors of values that exist elsewhere. Two of them,
  `0x001E` and `0x0034`, are interface states: they move when a page is opened, and return to their previous value
  when it is left.

A restore writes these bytes back as they were read. They are the reason a backup is a dump and not a
list of settings.

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

The payload size is `00 13`, that is 16 data bytes plus the count and the address. The unit answers with the `ACKG` command, whose status
byte is `00`. A single setting can also be written alone: the contrast, at address `0x0002`, is 1 byte and takes a
payload size of `00 04`.

These writes change the RAM: the unit uses them at once, and a power cycle brings back the value that the `SGLOB` command
(#cref(<chap-store>)) saved.

#caveat[
  Writing the global settings overwrites settings that the user cannot restore from the front panel without knowing them,
  in particular the contrast and the keyboard response. Read the 16 documented bytes first, keep them, and write back a
  complete block rather than isolated bytes.
]

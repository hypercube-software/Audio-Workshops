#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Saving the global settings", l: "chap-store")

A write to the global settings changes the RAM only: the unit uses the new settings at once, and a power cycle brings
back what the flash memory holds. The `SGLOB` command saves them.

== The payload

The `SGLOB` command carries 1 payload byte: the contrast of the display, from 0 to 99. #cref(<chap-flash-cmds>) gives
its frame.

The command is peculiar: its payload *carries the contrast only*, and the command saves *all the global settings*, not
just that byte. The payload is not the data that the command writes.

What the command does, in order:

1. It sets the contrast at `0x0002` to the payload byte, as the `CONTRAST` command (`7A`) does on its own.
2. It reads the 18 settings from the RAM of the unit and writes them to the saved block, described below, in 1
   operation.

The contrast is in that payload because of the front panel: the *Utilities* page of the global mode holds the
*Contrast* setting and the *Save Globals* action, and the frame carries the value of that page.

Every setting to save must therefore be in the RAM before the command, written with the `WGLOB` command; the payload
is only the last of them, the contrast.

The command *answers nothing*, and the unit has no command that reads the stored block back. The only way to check the
write is to power the unit off and on, which loads the block into the RAM, then read the global settings with the
`QGLOB` command.

== The saved block <sec-store-block>

The `SGLOB` command packs 18 settings into a block of 20 bytes and writes that block in the memory of the unit. The 2
first bytes of the block are constants, `01 02`; the 18 settings follow, in this order:

#figure(
  ptable(
    columns: (auto, auto, 1fr),
    head([*Packed address*], [*Size*], [*Setting*]),
    [`00 01`], [1], [Current preset: the preset that the unit loads at power-on],
    [`00 18`], [1], [Pad bank],
    [`00 0A`], [1], [Common channel],
    [`00 2F`], [1], [Internal, role not established],
    [`00 30`], [1], [SysEx device ID],
    [`00 02`], [1], [Contrast],
    [`00 0B`], [1], [Tap average],
    [`00 0D`], [1], [Tempo LED],
    [`00 0C`], [1], [Clock source],
    [`00 0F`], [1], [Transport to DIN],
    [`00 06`], [1], [Keyboard curve],
    [`00 07`], [1], [Keyboard gain],
    [`00 09`], [1], [Black key sensitivity],
    [`00 08`], [1], [White key sensitivity],
    [`00 05`], [1], [Pad threshold],
    [`00 04`], [1], [Pad velocity curve],
    [`00 03`], [1], [Pad velocity sensitivity],
    [`00 0E`], [1], [Note display],
  ),
  caption: [The settings saved by SGLOB],
)

The order of the table is the order of the block, which differs from the order of the global settings. The block is
what the unit reads at power-on: the preset it holds is the preset that the unit loads, and the pad bank it holds is
the bank that the pads use.

== The procedure

1. Read the current contrast with the `QGLOB` command, at address `0x0002`.
2. Write the settings with the `WGLOB` command; the unit acknowledges each write with the `ACKG` command.
3. Send the `SGLOB` command with the contrast value.
4. Wait for the write to complete, about 200 ms.
5. Power the unit off and on, then read the global settings with the `QGLOB` command to check.

== What SGLOB leaves out

The saved block holds 18 settings and 2 constants, and nothing else (see @sec-store-block). The global settings hold more, and these
are not part of the block:

#figure(
  ptable(
    columns: (auto, auto, 1fr),
    head([*Packed address*], [*Size*], [*Content*]),
    [`00 13`], [5], [Arp Latch, Arp, Full Level, 16 Level and Note Repeat: live states of the buttons],
    [`00 19`], [1], [Control bank],
    [`00 1A`], [1], [Time division],
    [`00 25`], [5], [Program Change mode],
    [`00 31`], [328], [Unknown bytes],
  ),
  caption: [The global settings that SGLOB leaves out],
)

These bytes change with the buttons and the pages of the front panel, so 2 reads differ there. A backup of the
settings keeps only the block above.

#caveat[
  `SGLOB` saves all 18 settings of the block, so the settings that were not written keep the value they have in
  the RAM. Read the 18 settings first, and write back the ones that do not change.
]

== The stored fields

2 commands write the fields that sit next to the block, and neither of them changes a setting of the unit:

- The `SERIAL` command writes the 16 bytes of the serial number, the one that the device inquiry reports (#cref(<chap-inquiry>));
- The `FIELD4` command writes 4 bytes of a field whose role is not established.

They carry no checksum and answer nothing.

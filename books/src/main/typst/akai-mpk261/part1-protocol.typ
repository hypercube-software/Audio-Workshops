#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Protocol", l: "chap-protocol")

== MIDI ports

The unit exposes 4 MIDI ports. Under Windows they appear as `MPK261`, `MIDIIN2 (MPK261)`, `MIDIOUT2 (MPK261)`, and so
on; under macOS and Linux they appear as `MPK261 Port 1` to `Port 4`.

#figure(
    ptable(
        columns: (auto, auto, 1fr),
        head([*Port*], [*Function*], [*Content*]),
        [`1`],
        [USB A],
        [Notes, aftertouch, controllers, pad and transport events. The official OS updater uses this port],
        [`2`],
        [USB B],
        [The unit's second USB port. A control's *Channel* field selects it with the values `11` to `20`:
            USB B1 to USB B16],
        [`3`],
        [DIN],
        [The MIDI DIN connector of the rear panel. The messages that reach it follow the *MIDI to DIN*
            settings of the presets],
        [`4`],
        [SysEx],
        [Use this port to query the device],
    ),
    caption: [The 4 MIDI ports of the MPK-261],
)

The SysEx messages of this book travel on port 4. The device inquiry of #cref(<chap-inquiry>) is the exception: every
port answers it.

== Commands <sec-commands>

The unit has no published documentation for its SysEx messages, so the book names each command after the operation
that it performs. The command byte of a frame maps to 1 of these names:

#figure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Name*], [*Code*], [*Answer*], [*Role*]),
        [`WPRESET`],
        [`10`],
        [`ACKW`],
        [Writes a whole preset into a user preset],
        [`ACKW`],
        [`11`],
        [—],
        [Answer to the `WPRESET` command: preset number and a status byte],
        [`QPRESET`],
        [`12`],
        [`WPRESET`],
        [Query a user preset out of the flash memory],
        [`QGLOB`],
        [`20`],
        [`WGLOB`],
        [Query a block of the global settings in RAM],
        [`QEDIT`],
        [`21`],
        [`WEDIT`],
        [Query a block of the *edit buffer of the current preset*],
        [`WGLOB`],
        [`30`],
        [`ACKG`],
        [Writes a block of the global settings in RAM],
        [`WEDIT`],
        [`31`],
        [`ACKE`],
        [Writes a block of the *edit buffer of the current preset*],
        [`PPRESET`],
        [`32`],
        [`ACKP`],
        [Pokes bytes into a user preset, at a given address],
        [`ACKG`],
        [`38`],
        [—],
        [Answer to the `WGLOB` command: echoed count, address and a status byte],
        [`ACKE`],
        [`39`],
        [—],
        [Answer to the `WEDIT` command],
        [`ACKP`],
        [`3A`],
        [—],
        [Answer to the `PPRESET` command: echoed count, address, preset number and a status byte],
    ),
    caption: [The commands of the 2 memories],
)

The settings of the unit that belong to no preset, and the OS, travel through their own commands:

#figure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Name*], [*Code*], [*Answer*], [*Role*]),
        [`CONTRAST`],
        [`7A`],
        [—],
        [Sets the contrast of the display, in the global settings],
        [`SGLOB`],
        [`7B`],
        [—],
        [Sets the contrast, then saves the global settings (see #cref(<chap-store>))],
        [`FIELD4`],
        [`78`],
        [—],
        [Writes 4 bytes in a stored field of the unit],
        [`SERIAL`],
        [`79`],
        [—],
        [Writes the 16 bytes of the serial number],
        [`OSCHECK`],
        [`76`],
        [`OSSUM`],
        [Checksum of the OS],
        [`OSSUM`],
        [`77`],
        [—],
        [Answer to the `OSCHECK` command],
        [`UPDATE`],
        [`72`],
        [—],
        [Enters the update mode and erases the OS (see #cref(<chap-firmware>))],
        [`XFER`],
        [`70`],
        [—],
        [Carries the OS to the unit, in update mode],
    ),
    caption: [The commands of the stored settings and of the OS],
)

A last group of 7 codes reaches the display and the unit's stored fields, and none of them has an established
memory effect:

#figure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Code*], [*Payload*], [*Observed effect*], [*Note*]),
        [`01`],
        [—],
        [Display],
        [Can freeze the display until the unit is powered off],
        [`02`],
        [text],
        [Display text],
        [—],
        [`03`],
        [—],
        [Display, pairs of nibbles],
        [—],
        [`08`],
        [2 bytes],
        [LED or state],
        [—],
        [`22`],
        [4 bytes],
        [Stored value],
        [—],
        [`50`],
        [16 bytes],
        [Stored block],
        [—],
        [`52`],
        [4 bytes],
        [Stored value],
        [—],
    ),
    caption: [The codes with no established role],
)

Each name starts with its verb: `W` writes, `Q` queries, `P` pokes, and `ACK` answers. The `P` of `PPRESET` is a tribute
to the venerable `POKE` in BASIC: the command writes bytes at an address, here inside a stored preset. `ACKP` answers it and echoes
the count, the address and the preset number; `ACKG` answers `WGLOB`, `ACKE` answers `WEDIT`, and `ACKW` answers
`WPRESET`.

The 2 queries and the 2 writes share the same MIDI frame; they differ by the memory space they address. A query and its
answer share the same frame layout: the answer repeats the count and the address, then carries the data.

Every answer that ends with a status byte carries `00` on success. A status byte other than `00` is a failure, and it
is the only diagnostic the unit gives.

#cref(<chap-sysex>) describes the frame of each command, and #cref(<chap-inquiry>) the device inquiry, which uses none
of these codes.

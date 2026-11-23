#import "../common/settings.typ": *
#import "lib.typ": *

#part("The MPK-261 protocol")

#chapter("Overview", l: "chap-overview")

== The instrument

The AKAI MPK-261 is a 61-key MIDI controller of the *MPK2* series. It turns keys, pads, wheels and pedal movements into
MIDI events, and it stores the settings that decide which event each control sends.

#image("assets/image.png")

The front panel offers:

- 61 velocity-sensitive keys, with channel aftertouch;
- 16 pads, in 4 banks (`A` to `D`), so 64 pad settings;
- 8 knobs, 8 faders and 8 switches, in 3 banks (`A` to `C`), so 24 settings of each kind;
- 5 transport buttons (Enter, Left, Right, Up, Down), used by the DAW and by the transport;
- an arpeggiator with a 16-step pattern, a note repeat function and a tap tempo;
- a pitch bend wheel, a modulation wheel, 1 expression pedal input and 2 footswitch inputs;
- a MIDI DIN output, in addition to the USB connection.

All of this lives in 30 presets, numbered 1 to 30 on the unit. A preset holds the assignment of the controls, the
arpeggiator, the keyboard split and the transport. 2 presets can send the same notes to 2 different MIDI channels,
or turn the same pad into a program change.

== Why this document

AKAI has never published the memory format of the MPK2 series, and the MPK-261 has no official editor: the software
editors AKAI commissioned from third parties were released for other instruments only. The user guide describes the
front panel; nothing describes what the unit stores, and the following document is, at the time of writing, the only
complete description of it.

This book describes the 2 memory spaces of the instrument and the SysEx messages that read and write them. It is meant
for 3 kinds of readers:

- The author of an editor or of a preset librarian;
- The author of a backup tool;
- Anyone who wants to script a change that the front panel makes tedious.

Some bytes of the 2 memory spaces carry a role that is not established. They are listed in the tables as *reserved* or
*unknown* rather than left out, because they are part of the data and must be preserved when it is written back.

== Disclaimer

This document is *not* an official AKAI documentation. AKAI has not reviewed, endorsed or approved its content. *AKAI*,
*MPK* and *MPK-261* are trademarks of their respective owners.

Parts of this document were generated with the help of AI agents. Although the author has reviewed and tested the
information, it may contain errors, omissions or misinterpretations, including in field offsets, sizes, value ranges or
message formats.

*Use this information at your own risk.* Sending incorrect or malformed data to a controller through SysEx can
overwrite your presets, put the unit in an unexpected state, or in the worst case damage the hardware. The author
accepts no responsibility for data loss, malfunction or damage to any device resulting from the use of the information
contained in this document. Always back up your presets before sending anything to the instrument, and test on a
non-essential preset first.

#block(breakable: false, width: 100%)[
    == Scope

    The book describes the global settings and the preset:

    #kfigure(
        table(
            align: left,
            columns: (auto, 1fr, auto),
            fill: (x, y) => if y == 0 { luma(230) },
            stroke: 0.5pt,
            head([*Settings*], [*Content*], [*Chapter*]),
            [Global settings],
            [377 bytes of system settings: contrast, pad and keyboard response, common channel, clock source, device ID, and
                the state of the buttons that belong to no preset],

            [#cref(<chap-global>)],
            [Preset],
            [The edit buffer, 1544 bytes: name, tempo, transport, arpeggiator, keyboard split, wheels, pedals, and the 64
                pads, 24 switches, 24 faders and 24 knobs. The 30 user presets store the same settings, which travel in a
                1555-byte MIDI frame],

            [#cref(<chap-preset>)],
        ),
        caption: [Scope],
    )
]

== Conventions

A byte is always written as 2 hexadecimal digits and set in a code font: `F0`, `7F`, `25`.

The addresses of this book are the packed ones, as they travel in the MIDI frame. A MIDI data byte carries 7 bits, so
an address travels in 2 bytes, the most significant group first. In the text, the 2 packed bytes are written as one
hexadecimal literal: `0x0B3C`. In a MIDI frame or in a table, they keep their spaces: `0B 3C`. The unpacked form of the
same byte (`0x5BC`) appears only where this book explains the difference between the 2 forms.

A table gives the packed address of a parameter and the size of its array, when it has one. In a table, the address keeps
its 2 bytes with a space, without the `0x` prefix: `04 64`. #cref(<chap-preset>) explains the 2 addresses of a parameter.

Values that do not fit in 7 bits are sent in 2 bytes, most significant group first, because a MIDI data byte has its
most significant bit clear. The book calls this encoding *14-bit packing* and always says when it applies.

Most tables of this book have a *Ref.* column: it gives the value found in a custom preset, used from one end of the
book to the other as an example preset. A dash means that the field is unused by that preset.

== The 4 MIDI ports

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

#kfigure(
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

Each name starts with its verb: `W` writes, `Q` queries, `P` pokes, and `ACK` answers. The `P` of `PPRESET` comes from
the `POKE` of BASIC: the command writes bytes at an address, here inside a stored preset. `ACKP` answers it and echoes
the count, the address and the preset number; `ACKG` answers `WGLOB`, `ACKE` answers `WEDIT`, and `ACKW` answers
`WPRESET`.

The 2 queries and the 2 writes share the same MIDI frame; they differ by the memory space they address. A query and its
answer share the same frame layout: the answer repeats the count and the address, then carries the data.

Every answer that ends with a status byte carries `00` on success. A status byte other than `00` is a failure, and it
is the only diagnostic the unit gives.

#cref(<chap-sysex>) describes the frame of each command, and #cref(<chap-inquiry>) the device inquiry, which uses none
of these codes.

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

== The 3 memories

The unit keeps its settings in 3 places, and a tool must know which one it writes:

#kfigure(
    ptable(
        columns: (auto, 1fr),
        head([*Memory*], [*Content*]),
        [ROM],
        [The factory content of the 30 presets, which the unit reads and copies back into the user presets when it
            is reset to the factory settings],
        [RAM],
        [The *edit buffer*, that is the copy of the preset that the unit uses, and the settings of the global
            memory. The unit clears it at power-off],
        [Flash],
        [The *user presets*, the global settings and the OS. What is written there survives a power cycle],
    ),
    caption: [The 3 memories of the unit],
)

The book uses 2 words with a precise meaning:

- The *edit buffer* is the copy of the preset that the unit uses. A write to the edit buffer changes the settings at
    once, and is lost at power-off unless the preset is stored.
- A *user preset* is 1 of the 30 rewritable slots. The unit ships them with factory content, and a factory reset puts
    that content back.

The edit buffer and the user presets have different layouts: the edit buffer is a *set of arrays of fields*, a user
preset is a *set of records*. #cref(<chap-preset>) and #cref(<chap-dump>) describe the 2 forms.

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

Some bytes of the 2 memories carry a role that is not established. They are listed in the tables as *reserved* or
*unidentified* rather than left out, because a tool that rewrites a whole block must know that they exist and must
preserve them.

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

#block(breakable: false)[
    == Scope of this book

    This book describes 3 blocks:

    #kfigure(
        table(
            align: left,
            columns: (auto, 1fr, auto),
            fill: (x, y) => if y == 0 { luma(230) },
            stroke: 0.5pt,
            head([*Block*], [*Content*], [*Chapter*]),
            [Global settings],
            [377 bytes of system settings: contrast, pad and keyboard response, common channel, clock source, device ID, and
                the state of the buttons that belong to no preset],

            [#cref(<chap-global>)],
            [Edit buffer],
            [1544 bytes: the preset that the unit uses — name, tempo, transport, arpeggiator, keyboard split, wheels, pedals
                and the 64 pads, 24 switches, 24 faders and 24 knobs],

            [#cref(<chap-preset>)],
            [User preset],
            [The 1555-byte MIDI frame that writes 1 of the 30 user presets, or reads it back],

            [#cref(<chap-dump>)],
        ),
        caption: [The 3 blocks of this book],
    )
]

== Conventions

A byte is always written as 2 hexadecimal digits and set in a code font: `F0`, `7F`, `25`.

The addresses of this book are the packed ones, as they travel in the MIDI frame. A MIDI data byte carries 7 bits, so
an address travels in 2 bytes, the most significant group first. In the text, the 2 packed bytes are written as one
hexadecimal literal: `0x0B3C`. In a MIDI frame or in a table, they keep their spaces: `0B 3C`. The unpacked form of the
same byte (`0x5BC`) appears only where this book explains the difference between the 2 forms.

A field table gives the packed address of the first control of its array; the byte of control `i` is the `i`-th byte of
that array. A table of blocks gives the first byte of each block in its `Packed address` column and the length of the
block in its `Packed size` column, both without the `0x` prefix.

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
        [Play],
        [Notes, aftertouch, controllers, pad and transport events. The official OS updater uses this port],
        [`2`],
        [—],
        [Reserved by the driver; no documented content],
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
that it performs. A tool reads the command byte of a frame and maps it to 1 of these names:

#kfigure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Name*], [*Code*], [*Answer*], [*Role*]),
        [`WPRESET`],
        [`10`],
        [—],
        [Writes a whole preset into a user preset],
        [`QPRESET`],
        [`12`],
        [`WPRESET`],
        [Query a user preset out of its slot],
        [`QGLOB`],
        [`20`],
        [`WGLOB`],
        [Query a block of the global settings],
        [`QEDIT`],
        [`21`],
        [`WEDIT`],
        [Query a block of the edit buffer],
        [`WGLOB`],
        [`30`],
        [`ACKG`],
        [Writes a block of the global settings],
        [`WEDIT`],
        [`31`],
        [`ACKP`],
        [Writes a block of the edit buffer],
        [`BPRESET`],
        [`32`],
        [`ACKB`],
        [Writes bytes inside a user preset, at a given offset],
        [`ACKG`],
        [`38`],
        [—],
        [Answer to the `WGLOB` command: echoed count, address and a status byte],
        [`ACKP`],
        [`39`],
        [—],
        [Answer to the `WEDIT` command],
        [`ACKB`],
        [`3A`],
        [—],
        [Status of the `BPRESET` command],
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
        [`STORE`],
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

A last group of 7 codes reaches the display and the stored fields of the unit, and none of them has an established
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

The 2 queries and the 2 writes share the same MIDI frame; they differ by the space they address. A query and its
answer share the same layout: the answer repeats the count and the address, then carries the data.

Every answer that ends with a status byte carries `00` on success. A status byte other than `00` is a failure, and it
is the only diagnostic the unit gives.

#cref(<chap-sysex>) describes the frame of each command, and #cref(<chap-inquiry>) the device inquiry, which uses none
of these codes.

== The 2 address spaces

The unit is peculiar: it exposes 2 distinct memory spaces, each reached by its own command and each starting at
address `0x0000`:

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto, auto, 1fr),
        head(
            [*Space*],
            [*Packed address*],
            [*Packed size*],
            [*Size*],
            [*Read command*],
            [*Write command*],
            [*Content*],
        ),
        [Global],
        [`00 00`],
        [`02 79`],
        [377],
        [`QGLOB`],
        [`WGLOB`],
        [System settings and live states],
        [Preset],
        [`00 00`],
        [`0C 08`],
        [1544],
        [`QEDIT`],
        [`WEDIT`],
        [The preset in the edit buffer],
    ),
    caption: [The 2 address spaces of the instrument],
)

A packed address is 2 7-bit bytes, so each space is 16384 bytes wide. The global space uses the first 377 bytes, the
preset space the first 1544. The same address designates 1 byte of each space, and a tool keeps the 2 apart.

== Memory map

The 2 spaces share the same organisation: a fixed header, the settings of the controls, then a tail of unidentified
bytes. #cref(<chap-global>) describes the global settings; #cref(<chap-preset>) and #cref(<chap-dump>) describe the
preset space.

#kfigure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed size*], [*Size*], [*Content*]),
        [`00 00`],
        [`00 10`],
        [16],
        [Documented system settings],
        [`00 10`],
        [`00 03`],
        [3],
        [Unidentified flags],
        [`00 13`],
        [`00 08`],
        [8],
        [State of the pad bank and of the control bank],
        [`00 1B`],
        [`00 0A`],
        [10],
        [Unidentified],
        [`00 25`],
        [`00 05`],
        [5],
        [Program Change mode],
        [`00 2A`],
        [`02 4F`],
        [335],
        [Unidentified],
    ),
    caption: [Organisation of the global settings],
)

#kfigure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed size*], [*Size*], [*Content*]),
        [`00 00`],
        [`00 16`],
        [22],
        [Name, tempo, transport, arpeggiator],
        [`00 16`],
        [`00 12`],
        [18],
        [Keyboard, split and zones],
        [`00 28`],
        [`00 23`],
        [35],
        [Wheels, pedals and footswitches],
        [`00 4B`],
        [`00 10`],
        [16],
        [Arpeggiator pattern, 1 byte per step],
        [`00 5B`],
        [`00 41`],
        [65],
        [DAW buttons, 5 records],
        [`01 1C`],
        [`02 38`],
        [312],
        [Switches, 24 records],
        [`03 54`],
        [`01 10`],
        [144],
        [Faders, 24 records],
        [`04 64`],
        [`01 58`],
        [216],
        [Knobs, 24 records],
        [`06 3C`],
        [`05 40`],
        [704],
        [Pads, 64 records],
        [`0B 7C`],
        [`00 0C`],
        [12],
        [Unidentified, constant in the presets examined],
    ),
    caption: [Organisation of the edit buffer],
)

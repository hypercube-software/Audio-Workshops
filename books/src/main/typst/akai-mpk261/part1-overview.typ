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

The unit keeps its settings in 3 places:

#kfigure(
    ptable(
        columns: (auto, 1fr),
        head([*Memory*], [*Content*]),
        [ROM],
        [The factory content of the 30 presets, which the unit writes back into the user presets when it
            is reset to the factory settings],
        [RAM],
        [The 2 *edit buffers*: the global edit buffer and the preset edit buffer. The unit clears them at
            power-off],
        [Flash],
        [The *user presets*, the stored global block and the OS. What is written there survives a power cycle],
    ),
    caption: [The 3 memories of the unit],
)

The book uses 2 words with a precise meaning:

- An *edit buffer* is the RAM area where a group of settings is edited. The unit has 2: the global edit buffer and the
    preset edit buffer. A write changes the settings at once, and is lost at power-off unless it is stored. This book
    calls the preset one *the edit buffer*.
- A *user preset* is 1 of the 30 rewritable slots. The unit ships them with factory content, and a factory reset puts
    that content back.

The edit buffer and the user presets hold the same settings in 2 layouts: the control matrices below show how the 2
forms relate, and #cref(<chap-preset>) gives the details.

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
*unidentified* rather than left out, because they are part of the data and must be preserved when it is written back.

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

A table gives the packed address of the first value of an array and the length of that array, both without the `0x`
prefix. The value of instance `i` is at the address plus `i`.

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
that it performs. The command byte of a frame maps to 1 of these names:

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
        [Query a block of the global settings in RAM],
        [`QEDIT`],
        [`21`],
        [`WEDIT`],
        [Query a block of the edit buffer],
        [`WGLOB`],
        [`30`],
        [`ACKG`],
        [Writes a block of the global settings in RAM],
        [`WEDIT`],
        [`31`],
        [`ACKE`],
        [Writes a block of the edit buffer],
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

Each name starts with its verb: `W` writes, `Q` queries, `P` pokes, and `ACK` answers. The `P` of `PPRESET` comes from
the `POKE` of BASIC: the command writes bytes at an address, here inside a stored preset. `ACKP` answers it and echoes
the count, the address and the preset number; `ACKG` answers `WGLOB`, and `ACKE` answers `WEDIT`.

The 2 queries and the 2 writes share the same MIDI frame; they differ by the memory space they address. A query and its
answer share the same layout: the answer repeats the count and the address, then carries the data.

Every answer that ends with a status byte carries `00` on success. A status byte other than `00` is a failure, and it
is the only diagnostic the unit gives.

#cref(<chap-sysex>) describes the frame of each command, and #cref(<chap-inquiry>) the device inquiry, which uses none
of these codes.

== Global and preset settings

The unit stores 2 groups of settings, in RAM and in flash memory:

#figure(
    ptable(
        columns: (auto, 1fr, 1fr),
        head([*Settings*], [*RAM (edit buffer)*], [*Flash memory*]),
        [Global],
        [377 bytes of global settings, read with `QGLOB` and written with `WGLOB`],
        [18 settings, saved by the `SGLOB` command in a 20-byte block; no command reads it back (see
            #cref(<chap-store>))],
        [Preset],
        [1544 bytes of preset settings, read with `QEDIT` and written with `WEDIT`],
        [30 user presets of 1546 bytes, read with `QPRESET` and written with `WPRESET`],
    ),
    caption: [RAM and Flash memory],
)

The 2 groups answer different needs:

- The *global settings* belong to no preset: contrast, pad and key response, common channel, clock source, device ID,
    and the state of 8 buttons;
- A *preset* holds 1 performance: its name, its tempo, its arpeggiator, its keyboard split and its control assignments.

The 2 areas play different roles:

- *RAM* holds the settings the unit works with. A write changes them at once, and a power cycle clears them: the preset
    in RAM can differ from every stored preset until it is saved;
- *Flash memory* holds the saved settings. The unit reads it at power-on, and a backup keeps it.

The 2 stored paths differ:

- A *user preset* has a full path: `QPRESET` reads the stored record, `WPRESET` writes it. A third special command
    `PPRESET` can be used to write part of it;
- The *stored global block* has a write path only: `SGLOB` saves it from the RAM, and no command reads it back. To see
    what it holds, power the unit off and on, then read the RAM with `QGLOB`.

== RAM address spaces

The unit can be confusing because the 2 RAM address spaces start at the very same address `0x0000`. Only the commands differ.
As usual in MIDI, we deal with packed values. A packed address is 2 bytes of 7 bits, so each address space is 16384 bytes wide.

#figure(
    ptable(
        columns: (auto, auto, auto, auto, auto, auto, 1fr),
        head(
            [*Memory space*],
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
    caption: [The RAM address spaces (edit buffers)],
)

The same address designates 1 byte of each memory space. #cref(<chap-global>) describes the 377 bytes of the global
settings; #cref(<chap-preset>) and the chapters that follow it describe the preset.

#block(breakable: false)[
    == Control matrices <sec-matrices>

    This device handle so many controls that the memory layout has to be complex. Hang on !

    A preset assigns 5 control types:

    - *Pads*: 16 pads, in 4 banks;
    - *Switches*: 8 switches, in 3 banks;
    - *Faders*: 8 faders, in 3 banks;
    - *Knobs*: 8 knobs, in 3 banks;
    - *DAW buttons*: the 5 transport buttons.

    Each control type has its own parameters. The values of 1 type form a matrix of *n* controls x *m* parameters.
]

#kfigure(
    block(
        width: 100%,
        stack(
            spacing: 10pt,
            align(
                center,
                table(
                    columns: 6,
                    stroke: 0.5pt,
                    align: center,
                    fill: (x, y) => if y == 0 { luma(230) } else if y == 3 { l_blue } else if x == 3 { l_green },
                    table.header(
                        [*Parameter*],
                        [*Control 1*],
                        [*Control 2*],
                        [*…*],
                        [*Control n−1*],
                        [*Control n*],
                    ),

                    [*Parameter 1*], [], [], [], [], [],
                    [*Parameter 2*], [], [], [], [], [],
                    [*…*], [], [], [], [], [],
                    [*Parameter m−1*], [], [], [], [], [],
                    [*Parameter m*], [], [], [], [], [],
                ),
            ),
            align(
                center,
                block(
                    width: 70%,
                    text(
                        size: 8.5pt,
                        table(
                            align: left,
                            columns: (auto, auto, 1fr),
                            stroke: 0.5pt,
                            inset: 3pt,
                            head([*Colour*], [*Flow*], [*Meaning*]),
                            [#box(fill: l_blue, width: 10pt, height: 10pt)],
                            [*Line by line*],

                            [The order of the *edit buffer*: every value of 1 parameter follows, then the next parameter],
                            [#box(fill: l_green, width: 10pt, height: 10pt)],
                            [*Column by column*],

                            [The order of a *user preset*: every parameter of 1 control follows, then the next control],
                        ),
                    ),
                ),
            ),
        ),
    ),
    caption: [The matrix of a control type],
)

The 5 matrices differ in size:

#figure(
    ptable(
        columns: (auto, auto, auto, auto),
        head([*Control type*], [*Instances*], [*Parameters*], [*Total*]),
        [Pads],
        [64],
        [11],
        [704],
        [Switches],
        [24],
        [13],
        [312],
        [Faders],
        [24],
        [6],
        [144],
        [Knobs],
        [24],
        [9],
        [216],
        [DAW buttons],
        [5],
        [13],
        [65],
    ),
    caption: [The 5 control matrices],
)

The 5 matrices hold 1441 values in all, and every value is *1 byte*.

Note that two groups of settings have only one instance (they are not related to a specific control type), they old only one column:

- The *preset head*: name, tempo, transport, arpeggiator, keyboard, wheels, pedals, split and pattern.
- The *global settings*: 377 bytes of system fields

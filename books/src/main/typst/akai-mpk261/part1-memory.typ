#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Memory", l: "chap-memory")

== ROM, RAM and flash memory

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
        [Flash memory],
        [The *user presets*, the stored global block and the OS. What is written there survives a power cycle],
    ),
    caption: [The 3 memories of the unit],
)

The book uses 2 words with a precise meaning:

- An *edit buffer* is the RAM area where a group of settings is edited. The unit has 2: the global edit buffer and the
    preset edit buffer. A write changes the settings at once, and is lost at power-off unless it is stored. This book
    calls the preset one *the edit buffer*.
- A *user preset* is 1 of the 30 presets stored in the flash memory. The unit ships them with factory content, and a
    factory reset puts that content back. Each one is rewritable.

The edit buffer and the user presets hold the same settings in 2 layouts: the control matrices below show how the 2
forms relate, and #cref(<chap-preset>) gives the details.

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
        columns: (auto, auto, auto, auto, auto, 1fr),
        head(
            [*Memory space*],
            [*Packed address*],
            [*Size*],
            [*Read command*],
            [*Write command*],
            [*Content*],
        ),
        [Global],
        [`00 00`],
        [377],
        [`QGLOB`],
        [`WGLOB`],
        [System settings and live states],
        [Preset],
        [`00 00`],
        [1544],
        [`QEDIT`],
        [`WEDIT`],
        [The preset in the edit buffer],
    ),
    caption: [The RAM address spaces (edit buffers)],
)

The same address designates 1 byte of each memory space. #cref(<chap-global>) describes the 377 bytes of the global
settings; #cref(<chap-preset>) and the chapters that follow it describe the preset.

#block(breakable: false, width: 100%)[
    == Control parameter matrices <sec-matrices>

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

== Non-matrixed parameters

The settings that follow exist once: no control matrix holds them.

- The *preset head*: name, tempo, transport, arpeggiator and the 16 steps of the pattern. It belongs to the preset, and each user preset stores its own;
- The *keyboard, the split and the pedals*: the channel and the transposition of the notes, the 2 split zones, and the routing of the wheels and of the pedals;
- The *global settings*: 377 bytes of system fields, which belong to the unit and travel in no preset.

#cref(<chap-global>) describes the global settings; #cref(<chap-preset>) and the chapters that follow describe the preset.

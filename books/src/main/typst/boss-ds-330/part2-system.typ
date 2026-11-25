#import "../common/settings.typ": *
#import "lib.typ": *

#part("System area")

#chapter("System settings", l: "chap-system")

The system settings live in a single 128-byte block at `0x400000`. They hold the master tune, the master volume,
the key shift, the master pan and the GS mode flag. The bytes between `0x400007` and `0x40007E` are reserved and
read as `00`.

#figure(
    ptable(
        columns: (auto, auto, auto, 1fr),
        head([*Packed address*], [*Packed size*], [*Parameter*], [*Meaning*]),
        [`40 00 00`], [`00 00 04`], [Master Tune],
        [The overall tuning, a 16-bit value over 4 nibbles. The default reads `00 04 00 00`, the 440 Hz reference.
         See @sec-master-tune],
        [`40 00 04`], [`00 00 01`], [Master Volume],
        [The overall volume, `0x00` to `0x7F`. The default reads `7F`],
        [`40 00 05`], [`00 00 01`], [Master Key Shift],
        [The transposition in semitones, `-24` to `+24`. The default reads `40`, the centre value],
        [`40 00 06`], [`00 00 01`], [Master Pan],
        [The overall pan, `0x00` to `0x7F`. The default reads `40`, the centre value],
        [`40 00 7F`], [`00 00 01`], [GS Reset],
        [Reads `42` when the unit runs in GS mode. A write of `00` resets the whole unit to the GS factory state],
    ),
    caption: [Content of the system block],
)

== Master Tune <sec-master-tune>

The master tune is a 16-bit value, nibblized over 4 bytes. Each byte carries 1 nibble of 4 bits, most significant
nibble first, so the bytes `b0 b1 b2 b3` assemble the value as

#figure(
    ptable(
        columns: 1,
        raw("value = b0 * 0x1000 + b1 * 0x100 + b2 * 0x10 + b3"),
    ),
    caption: [Assembling the master tune value],
)

The value `0x0400` is the reference: the A above middle C at 440.0 Hz, that is 0.0 cent. The value is expressed in
0.1-cent units, so the detuning in cents is

#figure(
    ptable(
        columns: 1,
        raw("cents = (value - 0x0400) / 10"),
    ),
    caption: [From the value to cents],
)

The range runs from `0x0018` to `0x07E8`, that is -100.0 to +100.0 cent. The default bytes read `00 04 00 00`, the
value `0x0400`: 440.0 Hz, 0.0 cent.

#figure(
    ptable(
        columns: (auto, auto, auto),
        head([*Bytes*], [*Cents*], [*A4 (Hz)*]),
        [`00 04 0C 04`], [+19.6], [445.0],
        [`00 04 07 06`], [+11.8], [443.0],
        [`00 04 04 0F`], [+7.9], [442.0],
        [`00 04 02 07`], [+3.9], [441.0],
        [`00 04 00 00`], [0.0], [440.0],
        [`00 03 0B 01`], [-7.9], [438.0],
    ),
    caption: [A few master tune values],
)

The frequency follows from the cents: `440.0 * 2^(cents / 1200)`.

== Writing the block

A complete write is a `DT1` of 128 bytes to `0x400000`. The block is written in one request: the 4 bytes of the
master tune first, then the volume, the key shift, the pan, then 120 reserved bytes, then the GS mode flag.

The GS Reset byte at `0x40007F` is the trigger of a factory reset: a write of `00` to it resets the whole unit to
the GS factory state. The read value of `42` marks the current mode, not a reset request.

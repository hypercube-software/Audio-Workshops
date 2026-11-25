#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Encodings", l: "chap-encodings")

The DS-330 stores its parameters in 2 forms. The regular regions use the *BYTES* form, and the bulk dump uses the
*NIBBLES* form.

== BYTES

In the *BYTES* form, a parameter occupies 1 byte that carries the whole value: `0x40` is the byte `40`. A wider
value occupies several bytes: the pitch fine is 2 bytes. Most of the address space uses this form.

== NIBBLES

In the *NIBBLES* form, an 8-bit value is split into 2 nibbles of 4 bits, most significant nibble first. Each nibble
is sent in its own 7-bit byte, so a value occupies 2 address units instead of 1.

The bulk dump at `0x480000` and the bulk drum maps at `0x490000` use this form. A value of `0x40` is sent as the 2
nibbles `04 00`, and a value of `0x7F` is sent as `07 0F`.

#figure(
    table(
        align: left,
        columns: (auto, auto, 1fr),
        fill: (x, y) => if y == 0 { luma(230) },
        stroke: 0.5pt,
        head([*Value*], [*NIBBLES form*], [*Meaning*]),
        [`0x40`], [`04 00`], [The high nibble `4`, then the low nibble `0`],
        [`0x7F`], [`07 0F`], [The high nibble `7`, then the low nibble `F`],
    ),
    caption: [Two values in NIBBLES form],
)

The size of a NIBBLES region counts the nibbles, not the bytes. The bulk dump declares the packed size `00 1D 10`,
that is `3728` address units, which hold `1864` values of 8 bits each. A reader must therefore read 2 nibbles and
recombine them into 1 byte for every stored value.

A wider value nibblizes the same way, 1 nibble per byte. The master tune is a 16-bit value sent over 4 bytes in the
system block. See #cref(<chap-system>).

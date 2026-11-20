#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Encodings", l: "chap-address")

Every `RQ1` and `DT1` carries an address; only a `RQ1` also carries a size. Both are numbers, and both
must be encoded before they go on the wire: they are split into bytes of 7 bits, so that the most
significant bit of every byte is clear.

Note: A `DT1` is never acknowledged: after a write, read the block back to know what the unit really holds.

== Addresses

An address is a number of 28 bits. It is written in 2 forms:

- The *unpacked* form, a natural integer, the form used internally in the device
- The *packed* form, the same number split into 4 bytes of 7 bits, most significant byte
    first. *This is the form written in the MIDI frame*.

#figure(
    table(
        align: left,
        columns: (auto, auto),
        fill: (x, y) => if y == 0 { luma(230) },
        stroke: 0.5pt,
        head([*Unpacked address*], [*Packed Address*]), [`0x05000000`],
        [`05 00 00 00`], [`0x05000040`],
        [`05 00 00 40`], [`0x05000080`],
        [`05 00 01 00`], [`0x050000D0`],
        [`05 00 01 50`], [`0x050000EE`],
        [`05 00 01 6E`],
    ),
    caption: [The 2 forms of an address in the current patch],
) <tbl-addresses>

The 2 columns are the same number. `0x050000D0` is written `05 00 01 50` because the 2 last
bytes carry the low 14 bits of the number: `0xD0 = 0x01 * 0x80 + 0x50`. Nothing above `0x7F` fits in 1
byte, so `0xD0` never appears alone in a MIDI frame.

== Sizes

A size says how many data bytes a `RQ1` asks for. So we are talking about *packed data size* here ! Don't be confused.
By the way, a `DT1` has no size field: the number of bytes it carries is given by the length of the MIDI frame itself.

Like an address, a size exists in 2 forms: an unpacked form in device memory, and a packed form written in the request. So here we encode the size of a packed payload, and we encode this value in a packed value.

#figure(
    table(
        align: left,
        columns: (auto, auto),
        fill: (x, y) => if y == 0 { luma(230) },
        stroke: 0.5pt,
        head([*Unpacked size*], [*Packed size field*]), [`16`],
        [`00 00 00 10`], [`127`],
        [`00 00 00 7F`], [`128`],
        [`00 00 01 00`], [`240`],
        [`00 00 01 70`],
    ),
    caption: [The 2 forms of a size],
) <tbl-sizes>

`240` is written `00 00 01 70` because `1 * 128 + 112 = 240`. The size counts data bytes only: the
address, the command, the model identifier and the checksum are never included. Asking for more than
the block holds is allowed, the answer stops at the end of the block.

== Values

Not the same story. Where *adresses* and *sizes* are packed, *values* use a different but compatible enconding: `Nibbles`.

- A 7-bit parameter or less, such as a character of the patch name, is transmitted as it is, in *1 byte*.
- A 8-bit parameter value is transmitted as *2 nibbles*, the most significant one first.
- A 12-bit parameter, like the tempo of a pattern or a song, uses *3 nibbles*, the most significant one first

When a payload is read, a value must be rebuilt from its nibbles; when it is written, it must be split the same way.

#figure(
    table(
        align: left,
        columns: (auto, auto, auto),
        fill: (x, y) => if y == 0 { luma(230) },
        stroke: 0.5pt,
        head([*Value*], [*Size*], [*In the payload*]), [`0x5A`], [7-bit],
        [`5A`], [`0x2A`], [8-bit],
        [`02 0A`], [`0x258`], [12-bit],
        [`02 05 08`],
    ),
    caption: [How a value is written in the payload],
) <tbl-values>

The tempo is the only value wider than 2 nibbles: the next section gives its encoding.

== Tempo

The tempo of a pattern or a song is 12 bits wide, stored as 3 nibbles. The field does not hold the tempo in beats
per minute: it holds a count in tenths of a beat per minute above 40. A field of `0x320` is 800 tenths above 40, that
is `120` BPM; it is not a tempo of `0x320` BPM. The panel displays whole BPM.

#figure(
    ptable(
        columns: 1,
        raw("t      = (header[0] << 8) | (header[1] << 4) | header[2]\ntempo  = 40 + t / 10\n\nt      = round((tempo - 40) * 10)\nheader = [ (t >> 8) & 0x0F, (t >> 4) & 0x0F, t & 0x0F ]"),
    ),
    caption: [Encoding of the tempo],
)

The instrument accepts tempos from `40` to `300` BPM, that is a count from `0x000` to `0xA28`. The field sits at the
start of the pattern header, `0x10000000`, and of the song header, `0x20000000`.

#kfigure(
    table(
        align: left,
        columns: (auto, auto, auto),
        fill: (x, y) => if y == 0 { luma(230) },
        stroke: 0.5pt,
        head([*Tempo*], [*Count t*], [*Nibbles*]),
        [`40`], [`0x000`], [`00 00 00`],
        [`80`], [`0x190`], [`01 09 00`],
        [`120`], [`0x320`], [`03 02 00`],
        [`130`], [`0x384`], [`03 08 04`],
        [`200`], [`0x640`], [`06 04 00`],
        [`300`], [`0xA28`], [`0A 02 08`],
    ),
    caption: [Six tempos and their encoding],
)

== Blocks

#figure(
    table(
        align: left,
        columns: (auto, auto, auto, auto, 1fr),
        fill: (x, y) => if y == 0 { luma(230) },
        stroke: 0.5pt,
        head([*Block*], [*Packed address*], [*Size*], [*Packed size*], [*Content*]),
        [System],
        [`00 00 00 00`],
        [16],
        [`00 00 00 10`],

        [auto off, screen saver, MIDI transmit and receive, chain mode, clock source, CV calibration],
        [System, reserved],
        [`02 00 00 00`],
        [16],
        [`00 00 00 10`],

        [16 reserved bytes, readable and writable, without effect],
        [Patch, current],
        [`05 00 00 00`],
        [240],
        [`00 00 01 70`],

        [the patch in the edit buffer], [Patch, stored], [`06 nn 00 00`], [240], [`00 00 01 70`],
        [stored patch number `nn`, readable and writable], [Pattern, current], [`10 00 00 00`], [144], [`00 00 01 10`],
        [the pattern in the edit buffer], [Pattern, stored], [`11 nn 00 00`], [144], [`00 00 01 10`],
        [stored pattern number `nn`, readable and writable], [Song, current], [`20 00 00 00`], [144], [`00 00 01 10`],
        [the song in the edit buffer], [Song, stored], [`21 nn 00 00`], [144], [`00 00 01 10`],
        [stored song number `nn`, readable and writable],
    ),
    caption: [Blocks of the SE-02 memory],
) <tbl-blocks>

`nn` is the number of the stored preset, from `00` to `7F`: it is 1 packed byte, so it never exceeds
`7F`. Reading a stored preset is the way to save it to a computer. Writing a stored preset works the same
way, at its own address: the 240 bytes are accepted, and a read back confirms them. Whether the write
survives a power cycle has not been tested.

== Write

A `DT1` does not have to carry a whole block: its payload may hold 1 value.
- 1 byte for ASCII characters
- 2 nibbles for a 8 bits value
- 3 nibbles for the 12-bit tempo of a pattern or a song.
A payload that stops in the middle of a value is ignored.

=== A 7-bits value

The patch name is a sequence of 1-byte values: a character is sent as it is and is not split into
nibbles. The message below sends `5A`, the code of `Z`, to the first character of the name of the current
patch.

#figure(
    bytefield(
        bpr: 120,
        rows: 2.6em,
        bitheader(
            marker: false,
            angle: 0deg,
            numbers: none,
            0,
            [0],
            8,
            [1],
            16,
            [2],
            24,
            [3],
            32,
            [4],
            40,
            [5],
            48,
            [6],
            56,
            [7],
            64,
            [8],
            72,
            [9],
            80,
            [10],
            88,
            [11],
            96,
            [12],
            104,
            [13],
            112,
            [14],
        ),
        bytes(1, fill: l_gray)[F0],
        bytes(1, fill: l_gray)[41],
        bytes(1, fill: l_gray)[10],
        bytes(4, fill: l_gray)[model 00 00 00 44],
        bytes(1, fill: l_gray)[12],
        bytes(4, fill: l_green)[address 05 00 01 50],
        bytes(1, fill: l_orange)[5A \ Z],
        bytes(1, fill: l_red)[50],
        bytes(1, fill: l_gray)[F7],
    ),
    caption: [A dump that writes 1 character],
)

The first bytes of the name, before and after: `Inbound` becomes `Znbound`, and the rest of the block
does not move.

#figure(
    bytefield(
        bpr: 64,
        rows: 2.6em,
        bytes(1, fill: l_orange)[49 \ I],
        bytes(1, fill: l_gray)[6E \ n],
        bytes(1, fill: l_gray)[62 \ b],
        bytes(1, fill: l_gray)[6F \ o],
        bytes(1, fill: l_gray)[75 \ u],
        bytes(1, fill: l_gray)[6E \ n],
        bytes(1, fill: l_gray)[64 \ d],
        bytes(1, fill: l_gray)[00],
    ),
    caption: [The name, before the dump],
)

#figure(
    bytefield(
        bpr: 64,
        rows: 2.6em,
        bytes(1, fill: l_red)[5A \ Z],
        bytes(1, fill: l_gray)[6E \ n],
        bytes(1, fill: l_gray)[62 \ b],
        bytes(1, fill: l_gray)[6F \ o],
        bytes(1, fill: l_gray)[75 \ u],
        bytes(1, fill: l_gray)[6E \ n],
        bytes(1, fill: l_gray)[64 \ d],
        bytes(1, fill: l_gray)[00],
    ),
    caption: [The name, after the dump],
)

=== A 8-bit value

The cutoff is parameter `55` of this chapter, at address `0x0500006E`. A 8-bit value takes
2 nibbles, the most significant one first: the message below writes `0C 08`.

#figure(
    bytefield(
        bpr: 128,
        bitheader(
            marker: false,
            angle: 0deg,
            numbers: none,
            0,
            [0],
            8,
            [1],
            16,
            [2],
            24,
            [3],
            32,
            [4],
            40,
            [5],
            48,
            [6],
            56,
            [7],
            64,
            [8],
            72,
            [9],
            80,
            [10],
            88,
            [11],
            96,
            [12],
            104,
            [13],
            112,
            [14],
            120,
            [15],
        ),
        bytes(1, fill: l_gray)[F0],
        bytes(1, fill: l_gray)[41],
        bytes(1, fill: l_gray)[10],
        bytes(4, fill: l_gray)[model 00 00 00 44],
        bytes(1, fill: l_gray)[12],
        bytes(4, fill: l_green)[address 05 00 00 6E],
        bytes(2, fill: l_orange)[0C 08],
        bytes(1, fill: l_red)[79],
        bytes(1, fill: l_gray)[F7],
    ),
    caption: [A dump that writes 1 8-bit value],
)

The cutoff and its 2 neighbours, from `0x0500006C` to `0x05000071`, before and after: `00 00` becomes
`0C 08`, and the rest of the block does not move.

#figure(
    bytefield(
        bpr: 48,
        bytes(1, fill: l_gray)[00],
        bytes(1, fill: l_gray)[00],
        bytes(1, fill: l_orange)[00],
        bytes(1, fill: l_orange)[00],
        bytes(1, fill: l_gray)[06],
        bytes(1, fill: l_gray)[05],
    ),
    caption: [The cutoff and its neighbours, before the dump],
)

#figure(
    bytefield(
        bpr: 48,
        bytes(1, fill: l_gray)[00],
        bytes(1, fill: l_gray)[00],
        bytes(1, fill: l_red)[0C],
        bytes(1, fill: l_red)[08],
        bytes(1, fill: l_gray)[06],
        bytes(1, fill: l_gray)[05],
    ),
    caption: [The cutoff and its neighbours, after the dump],
)

=== A 12-bit value

The widest value of the SE-02 is the tempo of a pattern or a song: 12 bits, 3 nibbles. At the start of the
pattern, address `0x10000000`, the message writes the 3 nibbles `02 05 08`, which is the tempo `100` BPM.

#figure(
    bytefield(
        bpr: 136,
        bitheader(
            marker: false,
            angle: 0deg,
            numbers: none,
            0,
            [0],
            8,
            [1],
            16,
            [2],
            24,
            [3],
            32,
            [4],
            40,
            [5],
            48,
            [6],
            56,
            [7],
            64,
            [8],
            72,
            [9],
            80,
            [10],
            88,
            [11],
            96,
            [12],
            104,
            [13],
            112,
            [14],
            120,
            [15],
            128,
            [16],
        ),
        bytes(1, fill: l_gray)[F0],
        bytes(1, fill: l_gray)[41],
        bytes(1, fill: l_gray)[10],
        bytes(4, fill: l_gray)[model 00 00 00 44],
        bytes(1, fill: l_gray)[12],
        bytes(4, fill: l_green)[address 10 00 00 00],
        bytes(3, fill: l_orange)[02 05 08],
        bytes(1, fill: l_red)[61],
        bytes(1, fill: l_gray)[F7],
    ),
    caption: [A dump that writes 1 12-bit value],
)

The tempo, before and after: `02 08 0A` is `105` BPM, `02 05 08` is `100` BPM.

#figure(
    bytefield(
        bpr: 48,
        bytes(1, fill: l_orange)[02],
        bytes(1, fill: l_orange)[08],
        bytes(1, fill: l_orange)[0A],
        bytes(1, fill: l_gray)[01],
        bytes(1, fill: l_gray)[00],
        bytes(1, fill: l_gray)[00],
    ),
    caption: [The tempo, before the dump],
)

#figure(
    bytefield(
        bpr: 48,
        bytes(1, fill: l_red)[02],
        bytes(1, fill: l_red)[05],
        bytes(1, fill: l_red)[08],
        bytes(1, fill: l_gray)[01],
        bytes(1, fill: l_gray)[00],
        bytes(1, fill: l_gray)[00],
    ),
    caption: [The tempo, after the dump],
)

== Checksum

The checksum closes every MIDI frame. It is computed over the address and the payload, never over the
command byte, the device ID or the model identifier:

#figure(
    ptable(
        columns: 1,
        raw("checksum = (- (sum(address bytes) + sum(payload bytes))) & 0x7F"),
    ),
    caption: [Checksum rule],
)

For the request of the current patch, the address bytes `05 00 00 00` sum to `0x05` and the size bytes
`00 00 01 70` to `0x01 + 0x70 = 0x71`, so the total is `0x76`; the checksum is `0x80 - 0x76 = 0x0A`. A MIDI
frame whose checksum does not match is ignored without any answer.

#import "../common/settings.typ": *
#let hx(b, c) = box(fill: c, inset: (x: 2.5pt, y: 2pt), radius: 2pt, raw(b))
#let hexline(..c) = block(c.pos().join(h(2pt)))

#chapter("MIDI Format", image: chapter-banner, l: "chap1")
#index("Sectioning")

If you load a K26 file in the unit, you can perfectly retreive the very same content via MIDI, using various commands. The only difference is that:
- Nothing is compacted, because the target is no longer a floppy disk
- On the contrary, everything is "expanded". Objects are complete and heavier

Still remain the limitation of MIDI: everything must be packed into 7 bits
- bit 7 is always 0 and unused.
- bit 0 to 6 contain the data

The Musician's Reference (chapter 7) describes the SysEx protocol used to read and write objects in the unit's memory.
This chapter explains the messages, their format, and how they are used by `KurzweilExplorer`. It is based on the
Musician's Reference and on the behaviour observed with the unit, unless stated otherwise.

== Message structure

Every Kurzweil SysEx message has the following format:

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Bytes*], [*field*], [*Meaning*]),
    [`F0`], [], [Start of SysEx],
    [`07`], [`manufacturer`], [Kurzweil manufacturer ID],
    [`00`], [dev-id], [Device ID, set by the SysEx ID parameter. `7F` accepts any value],
    [`78`], [unit-id], [K2600],
    [`00`-`19`], [command-id], [Message type, see the command table below],
    [...], [payload], [Command body],
    [`F7`], [], [End of SysEx],
  ),
  caption: [Kurzweil SysEx message structure],
)

== Objects: Type and ID

Each object is identified by a *type* and an *ID*. Therefore, Program 1 is "type program, ID 1".
- The *Type* says what kind of object it is (program, keymap, sound block...).
- The *ID* is an instance of that type: which program, which keymap.

The Musician's Reference describes those fields as `type(2)` and `idno(2)`. Sounds simple, right? Unfortunately, they
start to explain that a type in itself has an ID! This is very confusing. So there is 3 concepts here:
- A *type*: appears only in the documentation, not in the real MIDI payload
- A *type ID*: this is what documentation calls `type(2)`
- An *object ID*: this is `idno(2)`

Instead, they should simply explain that, what they call "type ID", is nothing more than the encoding of the type in
MIDI. It is a packed version of a 14-bit value into 2 MIDI bytes. On MIDI, a data byte only holds 7 bits. A field that
needs 8 or more bits is therefore sent as several bytes, most significant byte first.

For instance, the value `0x84` (132) is the type of a *Program*, and it is the only thing the type field carries:
- *Type* `0x84` (`1000 0100`) is split into `0000001` and `0000100`, sent as `01 04`
- Guess what ? `$0104` is called "Type's ID" in the documentation !
- *Object ID* `1` is sent as `00 01`. Is the ID of an object of a given type.

== Commands

The amount of messages handled by a Kurzweil is a little bit intimidating.

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Command*], [*Code*], [*Role*]),
    [`DUMP`], [`00`], [Asks the unit for a data dump of an object. The answer is a `LOAD`],
    [`LOAD`], [`01`], [Writes data at an offset of an existing object],
    [`DACK`], [`02`], [Acknowledges a `LOAD` or a `WRITE`],
    [`DNAK`], [`03`], [Refuses a `LOAD` or a `WRITE`, with an error code],
    [`DIR`], [`04`], [Asks for the name and size of one object. The answer is an `INFO`],
    [`INFO`], [`05`], [Answer to `DIR`, `NEW` or `DEL`],
    [`NEW`], [`06`], [Creates an object],
    [`DEL`], [`07`], [Deletes an object],
    [`CHANGE`], [`08`], [Renames an object or moves it to another ID],
    [`WRITE`], [`09`], [Writes a complete object. Also the answer to `READ` and to the front-panel Dump button],
    [`READ`], [`0A`], [Asks for a complete object. The answer is a `WRITE`],
    [`READBANK`], [`0B`], [Asks for all the objects of one or all banks. The answer is a list of `WRITE`],
    [`DIRBANK`], [`0C`], [Asks for the `INFO` of all the objects of one or all banks],
    [`ENDOFBANK`], [`0D`], [Ends a `READBANK`, `DIRBANK` or `MOVEBANK` answer],
    [`DELBANK`], [`0E`], [Deletes all the RAM objects of a bank without confirmation],
    [`MOVEBANK`], [`0F`], [Moves all the objects of a bank to another bank],
    [`LOADMACRO`], [`10`], [Loads the macro in memory],
    [`MACRODONE`], [`11`], [Answer to `LOADMACRO` (`00` success, `01` failure)],
    [`PANEL`], [`14`], [Sends front-panel key presses, 3 bytes per key event],
    [`ALLTEXT`], [`15`], [Asks for the 320 characters of the display],
    [`PARAMVALUE`], [`16`], [Asks for the value of the current parameter],
    [`PARAMNAME`], [`17`], [Asks for the name of the current parameter],
    [`GETGRAPHICS`], [`18`], [Asks for the display bitmap (2560 bytes)],
    [`SCREENREPLY`], [`19`], [Answer to `ALLTEXT`, `PARAMVALUE`, `PARAMNAME` and `GETGRAPHICS`],
  ),
  caption: [Kurzweil SysEx commands (Musician's Reference, chapter 7)],
)

=== Data formats

Object data is sent in one of two formats, given by the `form` field:

- `00`: *nibble format*. Each byte of the object becomes two MIDI bytes, the high nibble first. `4F D8 01 29` becomes
  `04 0F 0D 08 00 01 02 09`. This is simple to read and to write, but it doubles the size.
- `01`: *bit-stream format*. The bits of the object are concatenated and cut in groups of 7 bits, the last group being
  padded with zeros. `4F D8 01 29` becomes `27 76 00 12 48`.

Sizes and offsets are always expressed in unpacked bytes, whatever the format used on the wire.

=== Programs and format versions

A program has a `format` field, always at the same offset, which gives the layout of the rest of the object:

- format 2: K2000 programs,
- format 3: K2500 programs before KDFX,
- format 4: K2500 programs after KDFX (version 4 and later).

Two points from the Musician's Reference change how a program must be read or written:

- a `DUMP` or a `READ` returns the program in the *lowest possible format*. The size given by the `INFO` may therefore
  be larger than the size of the dump. When a dump is received, the size of the `WRITE` message is the one to trust, not
  the size of the `INFO`,
- a `WRITE` must not use a format higher than the unit supports. A K2000 only accepts format 2, a pre-KDFX K2500 accepts
  formats 2 and 3. A higher format is stored but does not play correctly.

=== Examples

The examples below illustrate the format.

==== DUMP

`DUMP` for program type (01 04) id 1 (00 01), offset 0 (00 00 00), size 394 (`00 03 0A`), nibble format (`00`):

#hexline(
  hx("F0", l_gray),
  hx("07", l_gray),
  hx("00", l_blue),
  hx("78", l_gray),
  hx("00", l_orange),
  hx("01", l_green),
  hx("04", l_green),
  hx("00", l_green),
  hx("01", l_green),
  hx("00", l_purple),
  hx("00", l_purple),
  hx("00", l_purple),
  hx("00", l_purple),
  hx("03", l_purple),
  hx("0A", l_purple),
  hx("00", l_purple),
  hx("F7", l_gray),
)

#figure(
  stack(
    dir: ttb,
    spacing: 0pt,
    bytefield(
      bpr: 80,
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
      ),
      bytes(1, fill: l_gray)[`F0`],
      bytes(1, fill: l_gray)[`07`],
      bytes(1, fill: l_blue)[device],
      bytes(1, fill: l_gray)[`78`],
      bytes(1, fill: l_orange)[`00`],
      bytes(2, fill: l_green)[type],
      bytes(2, fill: l_green)[ID],
      bytes(1, fill: l_purple)[`00`],
    ),
    bytefield(
      bpr: 80,
      bitheader(
        marker: false,
        angle: 0deg,
        numbers: none,
        0,
        [10],
        8,
        [11],
        16,
        [12],
        24,
        [13],
        32,
        [14],
        40,
        [15],
        48,
        [16],
      ),
      bytes(3, fill: l_purple)[offset],
      bytes(3, fill: l_purple)[size],
      bytes(1, fill: l_gray)[`F7`],
      bytes(3, fill: white)[],
    ),
  ),
  caption: [`DUMP` for program 1, nibble format (17 bytes)],
)
==== DIR

`DIR` for program type (01 04) id 1 (00 01):

#hexline(
  hx("F0", l_gray),
  hx("07", l_gray),
  hx("00", l_blue),
  hx("78", l_gray),
  hx("04", l_orange),
  hx("01", l_green),
  hx("04", l_green),
  hx("00", l_green),
  hx("01", l_green),
  hx("F7", l_gray),
)

#figure(
  bytefield(
    bpr: 80,
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
    ),
    bytes(1, fill: l_gray)[`F0`],
    bytes(1, fill: l_gray)[`07`],
    bytes(1, fill: l_blue)[device],
    bytes(1, fill: l_gray)[`78`],
    bytes(1, fill: l_orange)[`04`],
    bytes(2, fill: l_green)[type],
    bytes(2, fill: l_green)[ID],
    bytes(1, fill: l_gray)[`F7`],
  ),
  caption: [`DIR` for program 1 (10 bytes)],
)

==== READ

`READ` of program type (01 04) id 1 (00 01) in packed format:

#hexline(
  hx("F0", l_gray),
  hx("07", l_gray),
  hx("00", l_blue),
  hx("78", l_gray),
  hx("0A", l_orange),
  hx("01", l_green),
  hx("04", l_green),
  hx("00", l_green),
  hx("01", l_green),
  hx("01", l_purple),
  hx("F7", l_gray),
)

A program in packed format is `0F9A` bytes vs `184C` bytes in nibble format.

#figure(
  stack(
    dir: ttb,
    spacing: 0pt,
    bytefield(
      bpr: 80,
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
      ),
      bytes(1, fill: l_gray)[`F0`],
      bytes(1, fill: l_gray)[`07`],
      bytes(1, fill: l_blue)[device],
      bytes(1, fill: l_gray)[`78`],
      bytes(1, fill: l_orange)[`0A`],
      bytes(2, fill: l_green)[type],
      bytes(2, fill: l_green)[ID],
      bytes(1, fill: l_purple)[format],
    ),
    bytefield(
      bpr: 80,
      bitheader(
        marker: false,
        angle: 0deg,
        numbers: none,
        0,
        [10],
      ),
      bytes(1, fill: l_gray)[`F7`],
      bytes(9, fill: white)[],
    ),
  ),
  caption: [`READ` of program 1 in packed format (11 bytes)],
)

==== DIRBANK

`DIRBANK` for all programs (01 04) of all banks (7F):

#hexline(
  hx("F0", l_gray),
  hx("07", l_gray),
  hx("00", l_blue),
  hx("78", l_gray),
  hx("0C", l_orange),
  hx("01", l_green),
  hx("04", l_green),
  hx("7F", l_purple),
  hx("00", l_purple),
  hx("F7", l_gray),
)

#figure(
  bytefield(
    bpr: 80,
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
    ),
    bytes(1, fill: l_gray)[`F0`],
    bytes(1, fill: l_gray)[`07`],
    bytes(1, fill: l_blue)[device],
    bytes(1, fill: l_gray)[`78`],
    bytes(1, fill: l_orange)[`0C`],
    bytes(2, fill: l_green)[type],
    bytes(1, fill: l_purple)[bank],
    bytes(1, fill: l_purple)[ramonly],
    bytes(1, fill: l_gray)[`F7`],
  ),
  caption: [`DIRBANK` for all programs of all banks (10 bytes)],
)

==== PANEL

`PANEL` simulate buttons of the front panel. Each event is made of 3 bytes:
- Event type: `08` up, `09` down, `0A` repeat, `0D` Alpha Wheel
- Button number
- Repeat count for Alpha wheel

#figure(
  stack(
    dir: ttb,
    spacing: 0pt,
    bytefield(
      bpr: 80,
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
      ),
      bytes(1, fill: l_gray)[`F0`],
      bytes(1, fill: l_gray)[`07`],
      bytes(1, fill: l_blue)[device],
      bytes(1, fill: l_gray)[`78`],
      bytes(1, fill: l_orange)[`14`],
      bytes(1, fill: l_green)[action],
      bytes(1, fill: l_purple)[key],
      bytes(1, fill: l_purple)[repeat],
      bytes(1, fill: l_gray)[`F7`],
      bytes(1, fill: white)[],
    ),
  ),
  caption: [`PANEL` with one key event: each key is a 3-byte event],
)



=== Answers and errors

`DNAK` carries an error code:

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Code*], [*Meaning*]),
    [`1`], [The object is being edited on the unit],
    [`2`], [Incorrect checksum],
    [`3`], [ID out of range],
    [`4`], [Object not found],
    [`5`], [RAM is full],
  ),
  caption: [`DNAK` error codes],
)

A `DIR` on an unknown object returns an `INFO` with a size of zero and an empty name. That is the normal way to check
whether an object exists before writing it.

== Sample Dump Standard <sds>

=== Principle

The unit does not read or write samples through any Kurzweil commands. Instead, a sample is first pushed with the MIDI
*Sample Dump Standard* (SDS), a standard MIDI protocol that is independent of Kurzweil. When a sample is received via
SDS, the unit automtically create a *sound block* (object type 134) for us. The sound block describes the sample: the
sample rate, the loop points, and the address of the PCM data in the sample RAM.

In practice, a sample upload is therefore a conversation with the unit:

+ First you check if the sound block is already here, if not you can start SDS.
+ Then SDS transfers the PCM data and gives it a sample number
+ Then The unit create a sound block for it without any name
+ Then SysEx can update the sound block that points to this sample if needed
+ Then SysEx can create a program using this sound block

=== Indexes

Be warned that indexes in SDS are *not aligned* with those in the kurzweil.
- The sound block object id is 200
- Therefore the SDS sample id is ... 199 !

=== Messages

==== Overview

SDS uses the following messages, all starting with `F0 7E channel`, where channel is the SDS channel (it can be
different from the SysEx device ID):

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Message*], [*Code*], [*Content*]),
    [Dump header],
    [`01`],
    [Sample number, bit depth, sample period, length, loop start, loop end, loop type. 21 bytes in total],

    [Data packet], [`02`], [Packet number (0 to 127, then wrap), 120 bytes of data, XOR checksum],
    [Dump request], [`03`], [Asks the unit to send a sample],
    [ACK], [`7F`], [Packet or header accepted],
    [NAK], [`7E`], [Packet or header refused, the sender must send it again],
    [CANCEL], [`7D`], [Aborts the dump],
    [WAIT], [`7C`], [The receiver needs more time],
  ),
  caption: [Sample Dump Standard messages used by the unit],
)

Many SDS values are in 21 bits. They need to be encoded for MIDI properly. For instance, the sample period of a 44100 Hz
sample is 22675 ns. Its binary value is 0000001 0110001 0010011. This is sent in MIDI inside 3 bytes in LSB first:

#figure(
  schema.render(
    schema.load((
      structures: (
        main: (
          bits: 24,
          ranges: (
            "23": (name: "23", description: "always 0"),
            "22-16": (name: "0010011", description: "First byte sent"),
            "15": (name: "15", description: "always 0"),
            "14-8": (name: "0110001", description: "Second byte sent"),
            "7": (name: "7", description: "always 0"),
            "6-0": (name: "0000001", description: "Last byte sent"),
          ),
        ),
      ),
    )),
    width: 90%,
  ),
  caption: [How Sample period 22675 ns is encoded in MIDI],
)

==== Dump header

#figure(
  stack(
    dir: ttb,
    spacing: 0pt,
    bytefield(
      bpr: 80,
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
      ),
      bytes(1, fill: l_gray)[`F0`],
      bytes(1, fill: l_gray)[`7E`],
      bytes(1, fill: l_blue)[channel],
      bytes(1, fill: l_gray)[`01`],
      bytes(2, fill: l_orange)[sample number],
      bytes(1, fill: l_blue)[bit depth],
      bytes(3, fill: l_green)[sample period],
    ),
    bytefield(
      bpr: 80,
      bitheader(
        marker: false,
        angle: 0deg,
        numbers: none,
        0,
        [10],
        8,
        [11],
        16,
        [12],
        24,
        [13],
        32,
        [14],
        40,
        [15],
        48,
        [16],
        56,
        [17],
        64,
        [18],
        72,
        [19],
      ),
      bytes(3, fill: l_purple)[sample length],
      bytes(3, fill: yellow.lighten(40%))[loop start],
      bytes(3, fill: yellow.lighten(60%))[loop end],
      bytes(1, fill: l_blue)[loop type],
    ),
    bytefield(
      bpr: 80,
      bitheader(marker: false, angle: 0deg, numbers: none, 0, [20]),
      bytes(1, fill: l_gray)[`F7`],
      bytes(
        9,
        fill: white,
      )[],
    ),
  ),
  caption: [SDS sample dump header: 21 MIDI bytes],
) <sds-header>

The header fields are:

#figure(
  table(
    align: left,
    columns: (auto, auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Field*], [*Native size*], [*MIDI size*], [*Description*]),
    [Sample number], [14 bits], [2 bytes], [Sample number which is `sound block id - 1`],
    [Bit depth], [7 bits], [1 byte], [`16` for the K2600],
    [Sample period], [21 bits], [3 bytes], [Period in nanoseconds],
    [Sample length], [21 bits], [3 bytes], [Length in samples],
    [Loop start], [21 bits], [3 bytes], [Loop start in samples],
    [Loop end], [21 bits], [3 bytes], [Loop end in samples],
    [Loop type], [7 bits], [1 byte], [`00` forward only, `01` backward and forward],
  ),
  caption: [Fields of the SDS dump header],
)

==== Data Packet

A data packet has the structure below. It carries 40 samples. The checksum is an XOR from byte 1 to byte 124

#figure(
  stack(
    dir: ttb,
    spacing: 0pt,
    bytefield(
      bpr: 40,
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
      ),
      bytes(1, fill: l_gray)[`F0`],
      bytes(1, fill: l_gray)[`7E`],
      bytes(1, fill: l_blue)[channel],
      bytes(1, fill: l_gray)[`02`],
      bytes(1, fill: l_orange)[packet number],
    ),
    bytefield(
      bpr: 72,
      bitheader(
        marker: false,
        angle: 0deg,
        numbers: none,
        0,
        [5],
        24,
        [8],
        48,
        [11],
      ),
      bytes(3, fill: l_green)[sample 1],
      bytes(3, fill: l_green)[sample 2],
      bytes(3, fill: l_green)[...],
    ),
    bytefield(
      bpr: 72,
      bitheader(
        marker: false,
        angle: 0deg,
        numbers: none,
        0,
        [116],
        24,
        [119],
        48,
        [122],
      ),
      bytes(3, fill: l_green)[...],
      bytes(3, fill: l_green)[sample 39],
      bytes(3, fill: l_green)[sample 40],
    ),
    bytefield(
      bpr: 72,
      bitheader(
        marker: false,
        angle: 0deg,
        numbers: none,
        0,
        [125],
        8,
        [126],
      ),
      bytes(1, fill: l_orange)[CHK],
      bytes(1, fill: l_gray)[`F7`],
      bytes(7, fill: white)[``],
    ),
  ),
  caption: [SDS data packet: 127 MIDI bytes],
)

The size of each MIDI sample depends on the bitdepth. Here we focus on 16 bit samples (the bidepth of the K2600) so,
each sample is converted to 3 MIDI bytes (LSB first as usual).

==== Retries

The protocol is *synchronous*: the sender waits for an ACK after each message. If we have a NACK or a timeout, we send
the same packet again. The `SampleDumpStandard` class implements this behaviour: `sendSample` sends the header, waits
for the ACK, then sends each packet with up to three retries.

== Retreiving objects

#include "part1-memory_map.typ"

=== ALG_DESC

#block(sticky: true)[DIR_BANK to query ALG_DESC type 102/\$0066 in bank 0:]
#nonum[
```
F0 07 00 78 0C 00 66 00 00 F7
```
]

Response: 98 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [1], [0], [`3-A`],
    [2], [0], [`2-PA`],
    [3], [0], [`2-DA`],
    [4], [0], [`2-1-A`],
    [5], [0], [`2-SA`],
    [6], [0], [`2-1-MA`],
    [7], [0], [`2-1-MA`],
    [8], [0], [`1-1-1-A`],
    [9], [0], [`1-1-SA`],
    [10], [0], [`1-1-1-MA`],
    [11], [0], [`1-1-1-MA`],
    [12], [0], [`1-1-1-MA`],
    [13], [0], [`1-1-PA`],
    [14], [0], [`1-1-DA`],
    [15], [0], [`1-1-DA`],
    [16], [0], [`1-T-A`],
    [17], [0], [`1-V-A`],
    [18], [0], [`1-M-V-A`],
    [19], [0], [`LP-V-A`],
    [20], [0], [`1-M-1-A`],
    [21], [0], [`1-M-SA`],
    [22], [0], [`1-M-1-MA`],
    [23], [0], [`1-M-1-MA`],
    [24], [0], [`1-M-PA`],
    [25], [0], [`1-M-DA`],
    [26], [0], [`HS-PA`],
    [27], [0], [`HS-1-A`],
    [28], [0], [`HS-SA`],
    [29], [0], [`HM-1-HS-DA`],
    [30], [0], [`HM-1-HS-MA`],
    [31], [0], [`HM-HS-DA`],
    [33], [0], [`2-2`],
    [34], [0], [`1-2-1`],
    [35], [0], [`2-1-1`],
    [36], [0], [`1-1-2`],
    [37], [0], [`3-1`],
    [38], [0], [`1-3`],
    [39], [0], [`1-1-1-1`],
    [40], [0], [`1-1-1-MA`],
    [41], [0], [`1-1-1-MA`],
    [42], [0], [`1-1-1-MA`],
    [43], [0], [`1-1-MA-1`],
    [44], [0], [`1-1-MA-1`],
    [45], [0], [`1-MA-1-1`],
    [46], [0], [`1-MA-2`],
    [47], [0], [`2-1-MA`],
    [48], [0], [`1-2-MA`],
    [49], [0], [`2-1-MA`],
    [50], [0], [`1-2-MA`],
    [51], [0], [`3-MA`],
    [52], [0], [`2-MA-1`],
    [53], [0], [`1-MA-1-MA`],
    [54], [0], [`1-MA-1-MA`],
    [55], [0], [`1-MA-MA-1`],
    [56], [0], [`1-1-MA-MA`],
    [57], [0], [`1-1-MA-MA`],
    [58], [0], [`2-MA-MA`],
    [59], [0], [`1-MA-MA-MA`],
    [60], [0], [`1-1-1-MA`],
    [61], [0], [`1-1-MA-1`],
    [62], [0], [`1-1-MA-MA`],
    [63], [0], [`MA-1-1-1`],
    [64], [0], [`MA-1-2`],
    [65], [0], [`1-MA-1-1`],
    [66], [0], [`1-MA-1-MA`],
    [67], [0], [`1-MA-2`],
    [68], [0], [`1-MA-1-1`],
    [69], [0], [`1-MA-2`],
    [70], [0], [`1-1-1-MA`],
    [71], [0], [`1-1-1-MA`],
    [72], [0], [`MA-1-1-MA`],
    [73], [0], [`MA-1-1-MA`],
    [74], [0], [`MA-1-1-MA`],
    [75], [0], [`MA-1-1-MA`],
    [76], [0], [`MA-1-1-MA`],
    [77], [0], [`MA-1-1-MA`],
    [78], [0], [`1-MA-1-1`],
    [79], [0], [`MA-1-1-MA`],
    [80], [0], [`MA-1-AMP`],
    [81], [0], [`3-A`],
    [82], [0], [`2-1-A`],
    [83], [0], [`2-SA`],
    [84], [0], [`2-1-MA`],
    [85], [0], [`2-1-MA`],
    [86], [0], [`1-1-1-A`],
    [87], [0], [`1-1-SA`],
    [88], [0], [`1-1-1-MA`],
    [89], [0], [`1-1-1-MA`],
    [90], [0], [`1-1-1-MA`],
    [91], [0], [`1-T-A`],
    [92], [0], [`1-V-A`],
    [93], [0], [`LP-V-A`],
    [94], [0], [`LOPAS2-SHMOD-AMP`],
    [95], [0], [`1-M-1-A`],
    [96], [0], [`1-M-SA`],
    [97], [0], [`1-MA-1-MA`],
    [98], [0], [`1-MA-1-MA`],
    [99], [0], [`1-1-1-MA`],
  ),
  caption: [ALG_DESC objects in bank 0],
)

#block(sticky: true)[DIR_BANK to query ALG_DESC type 102/\$0066 in bank 1:]
#nonum[
```
F0 07 00 78 0C 00 66 01 00 F7
```
]

Response: 27 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [100], [1], [`2-1-MA`],
    [101], [1], [`2-1-AMP`],
    [102], [1], [`1-1-1-AMP`],
    [103], [1], [`1-2-AMP`],
    [104], [1], [`1-1-2.AMP`],
    [105], [1], [`2-2.AMP`],
    [106], [1], [`2-1-AMP`],
    [107], [1], [`2-1-AMP`],
    [108], [1], [`2-1-AMP`],
    [109], [1], [`2-1-AMP`],
    [110], [1], [`2-1-AMP`],
    [111], [1], [`2-1-AMP`],
    [112], [1], [`2-1-AMP`],
    [113], [1], [`2-1-AMP`],
    [114], [1], [`2-1-AMP`],
    [115], [1], [`2-1-AMP`],
    [116], [1], [`2-1-AMP`],
    [117], [1], [`2-1-AMP`],
    [118], [1], [`2-1-AMP`],
    [119], [1], [`2-1-AMP`],
    [120], [1], [`2-1-AMP`],
    [121], [1], [`2-1-AMP`],
    [122], [1], [`2-1-AMP`],
    [123], [1], [`2-1-AMP`],
    [124], [1], [`2-1-AMP`],
    [125], [1], [`2-1-AMP`],
    [126], [1], [`2-1-AMP`],
  ),
  caption: [ALG_DESC objects in bank 1],
)

=== EDIT_MENU

#block(sticky: true)[DIR_BANK to query EDIT_MENU type 107/\$006B in bank 0:]
#nonum[
```
F0 07 00 78 0C 00 6B 00 00 F7
```
]

Response: 65 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [1], [0], [`ProgramMode`],
    [2], [0], [`SetupMode`],
    [3], [0], [`QuickAccessMode`],
    [4], [0], [`EffectsMode`],
    [5], [0], [`MIDIMode:TRANSMIT`],
    [6], [0], [`MIDIMode:RECEIVE`],
    [7], [0], [`MIDIMode:CHANNELS`],
    [8], [0], [`MasterMode`],
    [9], [0], [`Master2`],
    [10], [0], [`SongMode:MAIN`],
    [11], [0], [`DiskMode`],
    [12], [0], [`SampleRecord`],
    [13], [0], [`SampleMode`],
    [14], [0], [`SongMode:MISC`],
    [15], [0], [`SongMode:MIX`],
    [16], [0], [`EditSong:EVENT`],
    [17], [0], [`EditSong:TRACK`],
    [18], [0], [`EditSong:ARRANGE`],
    [19], [0], [`EditSong:STEP`],
    [20], [0], [`EVENT:View`],
    [21], [0], [`EditSetup`],
    [22], [0], [`EditQuickAccess`],
    [23], [0], [`EditIntonationTable`],
    [24], [0], [`EditVelocityMap`],
    [25], [0], [`EditPressureMap`],
    [26], [0], [`EditSong:COMMON`],
    [27], [0], [`EditKeyMap`],
    [28], [0], [`Edit`],
    [29], [0], [`F`],
    [30], [0], [`S`],
    [31], [0], [`KDFXMode:CTRL`],
    [32], [0], [`EditStudio:INPUT`],
    [33], [0], [`EditStudio:FXBUS`],
    [34], [0], [`EditStudio:AUXFX`],
    [35], [0], [`EditStudio:OUTPUT`],
    [36], [0], [`EditStudio:FXSEND`],
    [38], [0], [`Mixdown`],
    [39], [0], [`MIDI Faders`],
    [40], [0], [`GOTO`],
    [41], [0], [`BEND`],
    [42], [0], [`COMMON`],
    [43], [0], [`ARPEG`],
    [44], [0], [`RIBCFG`],
    [45], [0], [`CH/PRG`],
    [46], [0], [`KEY/VEL`],
    [47], [0], [`PAN/VOL`],
    [48], [0], [`RFU`],
    [49], [0], [`SLIDER`],
    [50], [0], [`SLID/2`],
    [51], [0], [`FOOTSW`],
    [52], [0], [`CPEDAL`],
    [53], [0], [`RIBBON`],
    [54], [0], [`WHEEL`],
    [55], [0], [`SWITCH`],
    [56], [0], [`PRESS`],
    [60], [0], [`EditFXPreset:PARAM1`],
    [61], [0], [`EditFXPreset:PARAM2`],
    [62], [0], [`EditFXPreset:PARAM3`],
    [63], [0], [`EditFXPreset:PARAM4`],
    [70], [0], [`KDFX`],
    [71], [0], [`FXMOD2`],
    [72], [0], [`FXMOD3`],
    [73], [0], [`FXMOD4`],
    [74], [0], [`FXLFO`],
    [75], [0], [`FXASR`],
  ),
  caption: [EDIT_MENU objects in bank 0],
)

#block(sticky: true)[DIR_BANK to query EDIT_MENU type 107/\$006B in bank 1:]
#nonum[
```
F0 07 00 78 0C 00 6B 01 00 F7
```
]

Response: 18 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [100], [1], [`FXFUN`],
    [101], [1], [`FX Import`],
    [104], [1], [`GOTO`],
    [105], [1], [`COMMON`],
    [106], [1], [`LAYER`],
    [107], [1], [`KEYMAP`],
    [108], [1], [`OUTPUT`],
    [109], [1], [`PITCH`],
    [110], [1], [`AMPENV`],
    [111], [1], [`ENV2`],
    [112], [1], [`ENV3`],
    [113], [1], [`ENVCTL`],
    [114], [1], [`LFO`],
    [115], [1], [`ASR`],
    [116], [1], [`VTRIG`],
    [117], [1], [`FUN`],
    [118], [1], [`EFFECT`],
    [119], [1], [`TONEWL`],
  ),
  caption: [EDIT_MENU objects in bank 1],
)

=== MENU_GROUP

#block(sticky: true)[DIR_BANK to query MENU_GROUP type 108/\$006C in bank 0:]
#nonum[
```
F0 07 00 78 0C 00 6C 00 00 F7
```
]

Response: 15 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [1], [0], [`main`],
    [2], [0], [`edprog`],
    [3], [0], [`mngb`],
    [4], [0], [`mng2a3`],
    [5], [0], [`fx`],
    [6], [0], [`mng2a2`],
    [7], [0], [`mng2a2`],
    [8], [0], [`mng1`],
    [9], [0], [`mng2b`],
    [10], [0], [`mng2c`],
    [11], [0], [`mng2a2`],
    [12], [0], [`studio`],
    [13], [0], [`multifx`],
    [15], [0], [`mngsb`],
    [16], [0], [`kdfx`],
  ),
  caption: [MENU_GROUP objects in bank 0],
)

=== B_LIST

#block(sticky: true)[DIR_BANK to query B_LIST type 111/\$006F in bank 0:]
#nonum[
```
F0 07 00 78 0C 00 6F 00 00 F7
```
]

Response: 9 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [1], [0], [`Keys`],
    [2], [0], [`Analog`],
    [3], [0], [`Hard Edge`],
    [4], [0], [`Jazz`],
    [5], [0], [`Rock/Blues`],
    [6], [0], [`Classical`],
    [7], [0], [`Film Score`],
    [8], [0], [`Jukebox`],
    [9], [0], [`Basic QA Bank`],
  ),
  caption: [B_LIST objects in bank 0],
)

#block(sticky: true)[DIR_BANK to query B_LIST type 111/\$006F in bank 127:]
#nonum[
```
F0 07 00 78 0C 00 6F 7F 00 F7
```
]

Response: 20 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [800], [127], [`Bands`],
    [801], [127], [`Grooves`],
    [802], [127], [`World`],
    [803], [127], [`Pop`],
    [804], [127], [`More Keys`],
    [805], [127], [`More Analog`],
    [806], [127], [`Leads`],
    [807], [127], [`Trio Parts`],
    [808], [127], [`Techno`],
    [809], [127], [`Texture`],
    [900], [127], [`Piano Patch`],
    [901], [127], [`Full Orch`],
    [902], [127], [`Strings`],
    [903], [127], [`Horns`],
    [904], [127], [`Winds`],
    [905], [127], [`Solo Orch`],
    [906], [127], [`Perc Pit`],
    [907], [127], [`Perc Ens`],
    [908], [127], [`Moody`],
    [909], [127], [`Exotic`],
  ),
  caption: [B_LIST objects in bank 127],
)

=== SHAPE

#block(sticky: true)[DIR_BANK to query SHAPE type 114/\$0072 in bank 0:]
#nonum[
```
F0 07 00 78 0C 00 72 00 00 F7
```
]

Response: 26 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [1], [0], [`Sine`],
    [2], [0], [`+Sine`],
    [3], [0], [`Square`],
    [4], [0], [`+Square`],
    [5], [0], [`Triangle`],
    [6], [0], [`+Triangle`],
    [7], [0], [`Rise Saw`],
    [8], [0], [`+Rise Saw`],
    [9], [0], [`Fall Saw`],
    [10], [0], [`+Fall Saw`],
    [20], [0], [`3 Step`],
    [21], [0], [`+3 Step`],
    [22], [0], [`4 Step`],
    [23], [0], [`+4 Step`],
    [24], [0], [`5 Step`],
    [25], [0], [`+5 Step`],
    [26], [0], [`6 Step`],
    [27], [0], [`+6 Step`],
    [28], [0], [`7 Step`],
    [29], [0], [`+7 Step`],
    [30], [0], [`8 Step`],
    [31], [0], [`+8 Step`],
    [34], [0], [`10 Step`],
    [35], [0], [`+10 Step`],
    [38], [0], [`12 Step`],
    [39], [0], [`+12 Step`],
  ),
  caption: [SHAPE objects in bank 0],
)

=== FUN

#block(sticky: true)[DIR_BANK to query FUN type 115/\$0073 in bank 0:]
#nonum[
```
F0 07 00 78 0C 00 73 00 00 F7
```
]

Response: 51 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [1], [0], [`a+b`],
    [2], [0], [`a-b`],
    [3], [0], [`(a+b)/2`],
    [4], [0], [`a/2+b`],
    [5], [0], [`a/4+b/2`],
    [6], [0], [`(a+2b)/3`],
    [9], [0], [`a*b`],
    [10], [0], [`-a*b`],
    [13], [0], [`a*10^b`],
    [16], [0], [`|a+b|`],
    [17], [0], [`|a-b|`],
    [18], [0], [`min(a,b)`],
    [19], [0], [`max(a,b)`],
    [20], [0], [`Quantize B To A`],
    [22], [0], [`lowpass(f=a,b)`],
    [23], [0], [`hipass(f=a,b)`],
    [25], [0], [`b/(1-a)`],
    [27], [0], [`a(b-y)`],
    [29], [0], [`(a+b)^2`],
    [31], [0], [`sin(a+b)`],
    [32], [0], [`cos(a+b)`],
    [33], [0], [`tri(a+b)`],
    [35], [0], [`warp1(a,b)`],
    [36], [0], [`warp2(a,b)`],
    [37], [0], [`warp3(a,b)`],
    [38], [0], [`warp4(a,b)`],
    [39], [0], [`warp8(a,b)`],
    [41], [0], [`a AND b`],
    [42], [0], [`a OR b`],
    [43], [0], [`b > a`],
    [47], [0], [`ramp(f=a+b)`],
    [48], [0], [`ramp(f=a-b)`],
    [49], [0], [`ramp(f=(a+b)/2)`],
    [50], [0], [`ramp(f=a*b)`],
    [51], [0], [`ramp(f=-a*b)`],
    [52], [0], [`ramp(f=a*10^b)`],
    [53], [0], [`ramp(f=(a+b)/4)`],
    [54], [0], [`a(y+b)`],
    [55], [0], [`ay+b`],
    [56], [0], [`(a+1)y+b`],
    [57], [0], [`y+a(y+b)`],
    [58], [0], [`a|y|+b`],
    [61], [0], [`Sample B On A`],
    [62], [0], [`Sample B On ~A`],
    [63], [0], [`Track B While A`],
    [64], [0], [`diode(a-b)`],
    [65], [0], [`diode(a-b+.5)`],
    [66], [0], [`diode(a-b-.5)`],
    [67], [0], [`diode(a-b+.25)`],
    [68], [0], [`diode(a-b-.25)`],
    [69], [0], [`Track B While ~A`],
  ),
  caption: [FUN objects in bank 0],
)

=== SETUP_2

#block(sticky: true)[DIR_BANK to query SETUP_2 type 137/\$0109 in bank 0:]
#nonum[
```
F0 07 00 78 0C 01 09 00 00 F7
```
]

Response: 99 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [1], [0], [`Tonal Raver`],
    [2], [0], [`Kickin' Wave`],
    [3], [0], [`Hunkster Funk`],
    [4], [0], [`Off Your Rocker`],
    [5], [0], [`Shimmer Hop`],
    [6], [0], [`Slow Hipmeister`],
    [7], [0], [`Slow Driver`],
    [8], [0], [`Acidic Dance`],
    [9], [0], [`JR's Dance`],
    [10], [0], [`Drum & Wave`],
    [11], [0], [`Jetzen's Trance`],
    [12], [0], [`Euro-Rave`],
    [13], [0], [`Acoustic Country`],
    [14], [0], [`Trip-Hopper`],
    [15], [0], [`Latinease`],
    [16], [0], [`Hop II`],
    [17], [0], [`Slider Grooves`],
    [18], [0], [`Wah Blues`],
    [19], [0], [`DrumsE-B1Fills#3`],
    [20], [0], [`Controlled FX`],
    [21], [0], [`Quantum Mechanix`],
    [22], [0], [`Driven`],
    [23], [0], [`Drewz Groove`],
    [24], [0], [`Jazz Session 101`],
    [25], [0], [`Piano Trio & Str`],
    [26], [0], [`Jazzy's Band`],
    [27], [0], [`SmoothEPiano`],
    [28], [0], [`Orchescape`],
    [29], [0], [`New Rock Band`],
    [30], [0], [`Harpsichord&Vox`],
    [31], [0], [`Analogued`],
    [32], [0], [`Elegant Grandeur`],
    [33], [0], [`Cathedral`],
    [34], [0], [`Church Organ`],
    [35], [0], [`Jazz & Pop Brass`],
    [36], [0], [`Glory Orchestra`],
    [37], [0], [`Super Lush`],
    [38], [0], [`Scoring Stage`],
    [39], [0], [`Happnin' Brass`],
    [40], [0], [`RbnSpltB3+MIDIPd`],
    [41], [0], [`SpltVASTB+MIDIPd`],
    [42], [0], [`Guitars Sent Her`],
    [43], [0], [`GB 6Keys&2Basses`],
    [44], [0], [`Comper Keys`],
    [45], [0], [`Bold Beauty`],
    [46], [0], [`Nubian Nightfall`],
    [47], [0], [`Polar Reverie`],
    [48], [0], [`Vapor Drawing`],
    [49], [0], [`Siamese Ribbon`],
    [50], [0], [`Spacegroove`],
    [51], [0], [`F1 Latino Groove`],
    [52], [0], [`Dream Pad RbnVel`],
    [53], [0], [`C2 Jam`],
    [54], [0], [`Nu C2 & Lead`],
    [55], [0], [`Percolator`],
    [56], [0], [`Acid Prog C2`],
    [57], [0], [`Krazed Keith`],
    [58], [0], [`Conn Trolled`],
    [59], [0], [`Ravin & Droolin`],
    [60], [0], [`Clanger`],
    [61], [0], [`Waverado Rbn SW`],
    [62], [0], [`Elephantitus`],
    [63], [0], [`Ether Dreams`],
    [64], [0], [`Rbn Slider Bells`],
    [65], [0], [`Munchkin Factory`],
    [66], [0], [`Clink Split`],
    [67], [0], [`Lush World`],
    [68], [0], [`IX Dancers`],
    [69], [0], [`MidEasternBells`],
    [70], [0], [`FM Morpher`],
    [71], [0], [`Slo n' Funky`],
    [72], [0], [`Liquid Guitars`],
    [73], [0], [`Big & Meaty`],
    [74], [0], [`Glazing`],
    [75], [0], [`Play With Balls`],
    [76], [0], [`Pepper Digital`],
    [77], [0], [`De Tant`],
    [78], [0], [`Old & New Pad`],
    [79], [0], [`Punch-n-Oinky`],
    [80], [0], [`Brazzine`],
    [81], [0], [`Pad W/ Rotor`],
    [82], [0], [`Tang Nebulae`],
    [83], [0], [`Boink Head`],
    [84], [0], [`Voyager`],
    [85], [0], [`Changling`],
    [86], [0], [`Wash Cycle`],
    [87], [0], [`NewTaleSpinnin'`],
    [88], [0], [`Electric Swirl`],
    [89], [0], [`Land Of Giants`],
    [90], [0], [`Desert Planet`],
    [91], [0], [`Terraformation`],
    [92], [0], [`Sparkle & Bass`],
    [93], [0], [`Area 51 1/2`],
    [94], [0], [`Slider Player`],
    [95], [0], [`Synthozilla`],
    [96], [0], [`Dr.Distorted`],
    [97], [0], [`ControlSetup`],
    [98], [0], [`Clear Setup`],
    [99], [0], [`Default Setup`],
  ),
  caption: [SETUP_2 objects in bank 0],
)

#block(sticky: true)[DIR_BANK to query SETUP_2 type 137/\$0109 in bank 8:]
#nonum[
```
F0 07 00 78 0C 01 09 08 00 F7
```
]

Response: 51 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [800], [8], [`HyperGroov<-C4->`],
    [801], [8], [`PianoPad w/Percs`],
    [802], [8], [`Slo Held Arper`],
    [803], [8], [`Don'tGetFooled`],
    [804], [8], [`Touch Game`],
    [805], [8], [`BeatBoy E1`],
    [806], [8], [`ZawiClav Split`],
    [807], [8], [`Dyn Piano Pad`],
    [808], [8], [`Pulsar Stack`],
    [809], [8], [`Mt Chicorora C2`],
    [810], [8], [`Hold Low 3sec Rb`],
    [811], [8], [`Mettlorfus Pad`],
    [812], [8], [`Black Keys xtra`],
    [813], [8], [`Jungle Jammer`],
    [814], [8], [`Huge Rock Band`],
    [815], [8], [`Rock Ballad`],
    [816], [8], [`Jazz Setup`],
    [817], [8], [`Two Touchers`],
    [818], [8], [`Frontier prs`],
    [819], [8], [`Eclectric Grand`],
    [820], [8], [`Bad Trip FtSw/MW`],
    [821], [8], [`WhirliToys`],
    [822], [8], [`PluckSynths Perc`],
    [823], [8], [`SusPed RhythmJam`],
    [824], [8], [`Ballad Piano Pad`],
    [825], [8], [`Big AnaLoveVibe`],
    [826], [8], [`ShockBreaks PSw1`],
    [827], [8], [`Four Pluckers`],
    [828], [8], [`WaterPiano Pad`],
    [829], [8], [`Padded Room`],
    [830], [8], [`AtmosPolySphere`],
    [831], [8], [`Breath Pad`],
    [832], [8], [`Trippy Jam`],
    [833], [8], [`MeditationGuits`],
    [834], [8], [`Cool Down Funk`],
    [835], [8], [#raw("Tek`Groov  C5->")],
    [836], [8], [`Big Fat Split`],
    [837], [8], [`The Pump C2`],
    [838], [8], [`Ana Basses`],
    [839], [8], [`Multi Followers`],
    [840], [8], [`Plucksynths`],
    [841], [8], [`10 Leagues Under`],
    [842], [8], [`Gremlin Arps`],
    [843], [8], [`Broken Toys`],
    [844], [8], [`Two Synth`],
    [845], [8], [`Machine Shop`],
    [846], [8], [`Farawaway Place`],
    [847], [8], [`BehindEnemyLines`],
    [848], [8], [`Tunnel Visionprs`],
    [849], [8], [`Seismic Trance`],
    [850], [8], [`Medal`],
  ),
  caption: [SETUP_2 objects in bank 8],
)

#block(sticky: true)[DIR_BANK to query SETUP_2 type 137/\$0109 in bank 9:]
#nonum[
```
F0 07 00 78 0C 01 09 09 00 F7
```
]

Response: 51 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [900], [9], [`Deep Piano Rbn`],
    [901], [9], [`Choir & Harp`],
    [902], [9], [`Orchestrator`],
    [903], [9], [`Piano Concerto`],
    [904], [9], [`Xmas Carols`],
    [905], [9], [`Sideline Perc`],
    [906], [9], [`TonalGroov  C5->`],
    [907], [9], [`Exotic Grooves`],
    [908], [9], [`Lunar Harp`],
    [909], [9], [`Themes`],
    [910], [9], [`Wet Piano`],
    [911], [9], [`enter the Jester`],
    [912], [9], [`Tap the Jester`],
    [913], [9], [`Hybrid Strings`],
    [914], [9], [`Wonderous Spaces`],
    [915], [9], [`Metal Orch Pad`],
    [916], [9], [`Toon prs`],
    [917], [9], [`Tranquil Sea`],
    [918], [9], [`Sick Clock Jam`],
    [919], [9], [`Orc Split`],
    [920], [9], [`Baroque Brass`],
    [921], [9], [`Unison Orchestra`],
    [922], [9], [`Unison w/Pizz`],
    [923], [9], [`Switch Orchestra`],
    [924], [9], [`Pizz/Str/Winds`],
    [925], [9], [`Harp Arps Cmaj`],
    [926], [9], [`Desert Bloom E1`],
    [927], [9], [`Exotic Charge`],
    [928], [9], [`ET Comes Home`],
    [929], [9], [`Fanfare Orch`],
    [930], [9], [`Switch Orch 2`],
    [931], [9], [`Orbiting Venus`],
    [932], [9], [`Glass Dulcimer`],
    [933], [9], [`Hybrid Reeds`],
    [934], [9], [`Two Hand Pizz`],
    [935], [9], [`Slo Str & Horn`],
    [936], [9], [`Pianist Band`],
    [937], [9], [`Prepared Pianos`],
    [938], [9], [`FSW1 solo winds`],
    [939], [9], [`Strings&Winds`],
    [940], [9], [`Str Ens Solo MW`],
    [941], [9], [`Pno&Vox&Pizz`],
    [942], [9], [`Down Wind SmRbn`],
    [943], [9], [`Guitar & Piano`],
    [944], [9], [`Cirrus 9`],
    [945], [9], [`Dry Plucks`],
    [946], [9], [`String Collage`],
    [947], [9], [`Esoterica`],
    [948], [9], [`Poseidon`],
    [949], [9], [`Stalkers`],
    [950], [9], [`Diabolic Trickle`],
  ),
  caption: [SETUP_2 objects in bank 9],
)

=== STUDIO

#block(sticky: true)[DIR_BANK to query STUDIO type 140/\$010C in bank 127:]
#nonum[
```
F0 07 00 78 0C 01 0C 7F 00 F7
```
]

Response: 290 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [1], [127], [`RoomChorDly Hall`],
    [2], [127], [`RmChorChRv Hall`],
    [3], [127], [`RoomChorCDR Hall`],
    [4], [127], [`RoomChor    Hall`],
    [5], [127], [`RoomChrCh4T Hall`],
    [6], [127], [`RoomFlngCDR Hall`],
    [7], [127], [`RoomFlgEcho Hall`],
    [8], [127], [`RmFlngStImg Garg`],
    [9], [127], [`RmFlgChDly Room`],
    [10], [127], [`ChmbFlgGtRv Hall`],
    [11], [127], [`RoomFlngCDR Hall`],
    [12], [127], [`RoomFlngLsr Echo`],
    [13], [127], [`RmFlgFXFlng Flng`],
    [14], [127], [`SpaceFlng   Hall`],
    [15], [127], [`ChmbFlngCDR Verb`],
    [16], [127], [`RoomPhsrCDR Hall`],
    [17], [127], [`RmPhsrQuFlg Hall`],
    [18], [127], [`RoomPhsr   Space`],
    [19], [127], [`RmEQmphEcho Comp`],
    [20], [127], [`RmEQmphEcho Hall`],
    [21], [127], [`RmEQmph4Tp  Room`],
    [22], [127], [`RmEQmph4Tap Hall`],
    [23], [127], [`RmSweepEcho Hall`],
    [24], [127], [`RoomResEcho Hall`],
    [25], [127], [`RmRotoFl4T CmpRv`],
    [26], [127], [`RoomSRSCDR  Hall`],
    [27], [127], [`RoomSRSRoom Room`],
    [28], [127], [`RoomSRSChDl Hall`],
    [29], [127], [`RoomSRSCDR   CDR`],
    [30], [127], [`RmStImgChDl Hall`],
    [31], [127], [`RoomSRSRoom Chmb`],
    [32], [127], [`RoomSRSRoom Hall`],
    [33], [127], [`ChmbCompCDR Hall`],
    [34], [127], [`RoomCmpChor Hall`],
    [35], [127], [`RoomComp    Hall`],
    [36], [127], [`RoomComp    Hall`],
    [37], [127], [`BthComp SRS Hall`],
    [38], [127], [`RoomCmpCh4T Hall`],
    [39], [127], [`RmDsRotFl4t RvCm`],
    [40], [127], [`RoomRmHall  Hall`],
    [41], [127], [`Room Room   SRS2`],
    [42], [127], [`RoomRmHall  Hall`],
    [43], [127], [`Room   Room Hall`],
    [44], [127], [`Room   Hall Hall`],
    [45], [127], [`Room  Room Hall2`],
    [46], [127], [`Room Room  Hall3`],
    [47], [127], [`Room  Room Hall4`],
    [48], [127], [`Room  Hall Hall2`],
    [49], [127], [`Sndbrd Room Hall`],
    [50], [127], [`Sndbrd Rm  Hall2`],
    [51], [127], [`Room  Room Hall3`],
    [52], [127], [`auxChrMDly  Room`],
    [53], [127], [`auxFlngChRv Room`],
    [54], [127], [`auxShp4MDly Hall`],
    [55], [127], [`auxDistLasr Room`],
    [56], [127], [`auxEnhSp4T Class`],
    [57], [127], [`auxDistLasr Acid`],
    [58], [127], [`EnhcManPhs  Room`],
    [59], [127], [`EnhcFlg8Tap Room`],
    [60], [127], [`EnhcCmpFlng Room`],
    [61], [127], [`CompEQmphCh Room`],
    [62], [127], [`BthQFlg4Tap Hall`],
    [63], [127], [`ChmbTremCDR Room`],
    [64], [127], [`ChmbCmpFlRv Hall`],
    [65], [127], [`ChamDstEcho Room`],
    [66], [127], [`ChamFlg4Tap Hall`],
    [67], [127], [`ChmbEnv4Tap GtRv`],
    [68], [127], [`CmbrShapLsr Hall`],
    [69], [127], [`auxPtchDst+ Chmb`],
    [70], [127], [`auxChorFlRv Cmbr`],
    [71], [127], [`auxChorFlRv Cmb2`],
    [72], [127], [`auxChorFlRv Cmb3`],
    [73], [127], [`auxChorFlRv Cmb4`],
    [74], [127], [`HallFlgChDl Room`],
    [75], [127], [`HallPtchLsr Hall`],
    [76], [127], [`HallGateFl4T Bth`],
    [77], [127], [`HallChorFDR Room`],
    [78], [127], [`HallPtchPtFl Lsr`],
    [79], [127], [`HallFlng8Tp Room`],
    [80], [127], [`HallChrEcho Room`],
    [81], [127], [`HallChorCDR Hall`],
    [82], [127], [`HallRsFltChDl Rm`],
    [83], [127], [`Hall  ChDly Hall`],
    [84], [127], [`HallFlgChDl Hall`],
    [85], [127], [`Hall    Room SRS`],
    [86], [127], [`Hall   Room Room`],
    [87], [127], [`Hall      CmpRvb`],
    [88], [127], [`HallFlng    Hall`],
    [89], [127], [`HallRoomChr Hall`],
    [90], [127], [`auxPhsrFDR  Hall`],
    [91], [127], [`auxChrDist+ Hall`],
    [92], [127], [`auxFlgDist+ Hall`],
    [93], [127], [`auxChrDist+ Hall`],
    [94], [127], [`auxChorMDly Hall`],
    [95], [127], [`auxChorSp6T Hall`],
    [96], [127], [`auxChorChDl Hall`],
    [97], [127], [`auxPhasStIm Hall`],
    [98], [127], [`auxFlngCDR  Hall`],
    [99], [127], [`auxPhsFlDbl Hall`],
    [100], [127], [`auxSRSRoom  Hall`],
    [101], [127], [`auxFlngLasr Hall`],
    [102], [127], [`auxEnh4Tap  Hall`],
    [103], [127], [`EnhcChorCDR Hall`],
    [104], [127], [`EnhChorChDl Hall`],
    [105], [127], [`EnhcChor   Plate`],
    [106], [127], [`CompFlgChor Hall`],
    [107], [127], [`ChorChorFlg Hall`],
    [108], [127], [`ChapelSRS   Hall`],
    [109], [127], [`ChapelSRS  Hall2`],
    [110], [127], [`Chapel Room Hall`],
    [111], [127], [`PltEnvFl4T  Room`],
    [112], [127], [`PlatEnvFl4T Filt`],
    [113], [127], [`PltEnvFl4T Plate`],
    [114], [127], [`PltTEnvFlg Plate`],
    [115], [127], [`PlateRngMd  Hall`],
    [116], [127], [`auxDist+Echo Plt`],
    [117], [127], [`auxEnvSp4T Plate`],
    [118], [127], [`auxShap4MD Plate`],
    [119], [127], [`auxChorDist+ Plt`],
    [120], [127], [`auxShFlgChDl Plt`],
    [121], [127], [`auxMPFlgLasr Plt`],
    [122], [127], [`auxShap4MD Plate`],
    [123], [127], [`FlgEnv4Tap Plate`],
    [124], [127], [`EnhrFlgCDR Plate`],
    [125], [127], [`auxRingPFD Plate`],
    [126], [127], [`GtRvShapMDl Room`],
    [127], [127], [`GtdEnhcStIm Room`],
    [128], [127], [`Gtd2ChrEcho 2Vrb`],
    [129], [127], [`GtdEnhcStIm Hall`],
    [130], [127], [`auxEnvSp4T GtVrb`],
    [131], [127], [`GtRbSwpFlt  Lasr`],
    [132], [127], [`GtRbSwpFlt FlDly`],
    [133], [127], [`ChRvStIEcho Hall`],
    [134], [127], [`ChorChorCDR Spac`],
    [135], [127], [`ChDlDstEQ   Hall`],
    [136], [127], [`auxDPanCDR ChPlt`],
    [137], [127], [`AuxChorFlng  CDR`],
    [138], [127], [`auxEnhcSp4T  CDR`],
    [139], [127], [`auxPtchDst+ ChRv`],
    [140], [127], [`EnhcChorChDl PCD`],
    [141], [127], [`auxPoly      FDR`],
    [142], [127], [`EnhcChorChDl FDR`],
    [143], [127], [`EnhcChrChDl FDR2`],
    [144], [127], [`auxRotoSp4T FlRv`],
    [145], [127], [`auxRotaryFDR Plt`],
    [146], [127], [`RotoOrgFX   Hall`],
    [147], [127], [`CmpRvbFlDl  Hall`],
    [148], [127], [`auxEnhSp4T  RvCm`],
    [149], [127], [`auxPtchRoom RvCm`],
    [150], [127], [`PhsrChorCDR Phsr`],
    [151], [127], [`ChDlSp4TFlDl Phs`],
    [152], [127], [`auxFlgDst+ ChLsD`],
    [153], [127], [`auxFlgDst+ ChLs2`],
    [154], [127], [`RoomRoomSRS CmRv`],
    [155], [127], [`RoomRoom    Room`],
    [156], [127], [`GtRvPlate   Hall`],
    [157], [127], [`RoomRoom     SRS`],
    [158], [127], [`EnhcSp4T    Hall`],
    [159], [127], [`Room RoomChr SRS`],
    [160], [127], [`KB3 V/C ->Rotary`],
    [161], [127], [`EQStImg  5BndEQ`],
    [162], [127], [`aux5BeqStIm Hall`],
    [163], [127], [`10Band StIm Hall`],
    [164], [127], [`3BndCmp     PtFl`],
    [165], [127], [`auxDst+Lsr   Plt`],
    [166], [127], [`auxRsFltEnvSwFlt`],
    [167], [127], [`auxChorEnvF Hall`],
    [168], [127], [`auxChRvEncr Chor`],
    [169], [127], [`auxChrDstEQ Room`],
    [170], [127], [`aux ChDlSRS Hall`],
    [171], [127], [`aux EQFlng DstEQ`],
    [172], [127], [`auxFlngPhsr Lasr`],
    [173], [127], [`auxFlShQFlg Hall`],
    [174], [127], [`auxFlgDist+ Room`],
    [175], [127], [`aux GtVbFl4T Bth`],
    [176], [127], [`auxRvRvQFlg Hall`],
    [177], [127], [`auxRvRbShapeChmb`],
    [178], [127], [`auxSpinMDly Room`],
    [179], [127], [`aux SweepEchoBth`],
    [180], [127], [`auxRoto&DsFDRPlt`],
    [181], [127], [`auxRot&Ds2FDRPlt`],
    [182], [127], [`auxFlgChDl Hall`],
    [183], [127], [`auxDstLsr  CDR`],
    [184], [127], [`CPDlEnFltCmpGtRv`],
    [185], [127], [`RotoOrgFX2  Hall`],
    [186], [127], [`ChDlFlPtLzVb Plt`],
    [187], [127], [`CDFlDlyPhRm Hall`],
    [188], [127], [`CDR FlgRvb  Hall`],
    [189], [127], [`DstPhsPnLzVb CDR`],
    [190], [127], [`DistRoom  GrphEQ`],
    [191], [127], [`Enh  Ch 4T Hall`],
    [192], [127], [`FiltCmpExpFl CDR`],
    [193], [127], [`LzVbFlDstEQ Room`],
    [194], [127], [`PhseDist    Room`],
    [195], [127], [`ChDlyRvFlRv Hall`],
    [196], [127], [`RmRotr&DstChrPlt`],
    [197], [127], [`Clear Studio`],
    [198], [127], [`Pre-KDFX Studio`],
    [199], [127], [`Default Studio`],
    [700], [127], [`Flanger Trio`],
    [701], [127], [`Thin Cloud Layer`],
    [702], [127], [`Fazetortion`],
    [703], [127], [`Ultraphasulate`],
    [704], [127], [`Chutney Squishy`],
    [705], [127], [`BuddMeetsTomita`],
    [706], [127], [`2 Buses 2 Chains`],
    [707], [127], [`RingMod Envelope`],
    [708], [127], [`Drum Megashaper`],
    [709], [127], [`Dist Lead Guitar`],
    [710], [127], [`Blues/R&R Guitar`],
    [711], [127], [`Clean Rhythm Gtr`],
    [712], [127], [`Sweeping Verb`],
    [713], [127], [`Pad Ambience`],
    [714], [127], [`Coming of Dawn`],
    [715], [127], [`Wide Raindrops`],
    [716], [127], [`Robot Voice`],
    [717], [127], [`Before the Crash`],
    [718], [127], [`Piano Multi-Verb`],
    [719], [127], [`Mastering Studio`],
    [800], [127], [`Sweet Hall`],
    [801], [127], [`Small Hall`],
    [802], [127], [`Medium Hall`],
    [803], [127], [`Large Hall`],
    [804], [127], [`Big Gym`],
    [805], [127], [`Bright Plate 1`],
    [806], [127], [`Opera House`],
    [807], [127], [`Live Chamber`],
    [808], [127], [`Bathroom`],
    [809], [127], [`Med Large Room`],
    [810], [127], [`Real Room`],
    [811], [127], [`Drum Room`],
    [812], [127], [`Small Dark Room`],
    [813], [127], [`Small Closet`],
    [814], [127], [`Add Ambience`],
    [815], [127], [`Gated Reverb`],
    [816], [127], [`Reverse Reverb`],
    [817], [127], [`Non-Linear`],
    [818], [127], [`Slapverb`],
    [819], [127], [`Full Bass`],
    [820], [127], [`Room + Delay`],
    [821], [127], [`Delay Big Hall`],
    [822], [127], [`Chorus Room`],
    [823], [127], [`Chorus Smallhall`],
    [824], [127], [`Chorus Med Hall`],
    [825], [127], [`Chorus Big Hall`],
    [826], [127], [`Chor-Delay Room`],
    [827], [127], [`Chor-Delay Hall`],
    [828], [127], [`Flange-Dly Room`],
    [829], [127], [`Flange-Dly Hall`],
    [830], [127], [`Stereo Chorus`],
    [831], [127], [`Stereo Flanger`],
    [832], [127], [`Stereo Delay`],
    [833], [127], [`4-Tap Delay`],
    [834], [127], [`Chorus Delay`],
    [835], [127], [`Flange Delay`],
    [836], [127], [`Chorus 4-Tap`],
    [837], [127], [`Flange 4 Tap`],
    [838], [127], [`Chorus Echo`],
    [839], [127], [`Chorus Echoverb`],
    [840], [127], [`Fast Flange`],
    [841], [127], [`Wash`],
    [842], [127], [`Into The Abyss`],
    [843], [127], [`Space Flanger`],
    [844], [127], [`Flange Room`],
    [845], [127], [`Predelay Hall`],
    [846], [127], [`Flange Echo`],
    [847], [127], [`Rotary Club`],
    [848], [127], [`Rotary Hall`],
    [849], [127], [`Chorus`],
    [850], [127], [`Soundbrd/rvb`],
    [851], [127], [`Percussive Room`],
    [852], [127], [`Brt Empty Room`],
    [853], [127], [`Mosque Room`],
    [854], [127], [`New Gated`],
    [855], [127], [`Chorus Slap Room`],
    [856], [127], [`Chorus Bass Room`],
    [857], [127], [`New Chorus Hall`],
    [858], [127], [`Spacious`],
    [859], [127], [`Wash Lead`],
    [860], [127], [`New Hall w/Delay`],
    [861], [127], [`Rich Delay`],
    [862], [127], [`Glass Delay`],
    [863], [127], [`Real Plate`],
    [864], [127], [`Real Niceverb`],
    [865], [127], [`ClassicalChamber`],
    [866], [127], [`Empty Stage`],
    [867], [127], [`Long & Narrow`],
    [868], [127], [`Far Bloom`],
    [869], [127], [`Floyd Hall`],
    [870], [127], [`With A Mic`],
  ),
  caption: [STUDIO objects in bank 127],
)

=== KDFX_PRESET

#block(sticky: true)[DIR_BANK to query KDFX_PRESET type 141/\$010D in bank 0:]
#nonum[
```
F0 07 00 78 0C 01 0D 00 00 F7
```
]

Response: 75 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [1], [0], [`NiceLittleBooth`],
    [2], [0], [`Small Wood Booth`],
    [3], [0], [`Natural Room`],
    [4], [0], [`PrettySmallPlace`],
    [5], [0], [`Sun Room`],
    [6], [0], [`Soundboard`],
    [7], [0], [`Add More Air`],
    [8], [0], [`Standard Booth`],
    [9], [0], [`A Distance Away`],
    [10], [0], [`Live Place`],
    [11], [0], [`Viewing Booth`],
    [15], [0], [`BrightSmallRoom`],
    [16], [0], [`Bassy Room`],
    [17], [0], [`Percussive Room`],
    [18], [0], [`SmallStudioRoom`],
    [19], [0], [`ClassRoom`],
    [20], [0], [`Utility Room`],
    [21], [0], [`Thick Room`],
    [22], [0], [`The Real Room`],
    [23], [0], [`Sizzly Drum Room`],
    [24], [0], [`Real Big Room`],
    [25], [0], [`The Comfy Club`],
    [26], [0], [`Spitty Drum Room`],
    [27], [0], [`Stall One`],
    [28], [0], [`Green Room`],
    [29], [0], [`Tabla Room`],
    [30], [0], [`Large Room`],
    [31], [0], [`Platey Room`],
    [40], [0], [`SmallDrumChamber`],
    [41], [0], [`Brass Chamber`],
    [42], [0], [`Sax Chamber`],
    [43], [0], [`Plebe Chamber`],
    [44], [0], [`In The Studio`],
    [45], [0], [`My Garage`],
    [46], [0], [`School Stairwell`],
    [47], [0], [`JudgeJudyChamber`],
    [48], [0], [`Bloom Chamber`],
    [55], [0], [`Grandiose Hall`],
    [56], [0], [`Elegant Hall`],
    [57], [0], [`Bright Hall`],
    [58], [0], [`Ballroom`],
    [59], [0], [`Spacious Hall`],
    [60], [0], [`Classic Chapel`],
    [61], [0], [`Semisweet Hall`],
    [62], [0], [`Pipes Hall`],
    [63], [0], [`Reflective Hall`],
    [64], [0], [`Smoooth Hall`],
    [65], [0], [`Splendid Palace`],
    [66], [0], [`Pad Space`],
    [67], [0], [`Bob'sDiffuseHall`],
    [68], [0], [`Abbey Piano Hall`],
    [69], [0], [`Short Hall`],
    [70], [0], [`The Long Haul`],
    [71], [0], [`Predelay Hall`],
    [72], [0], [`Sweeter Hall`],
    [73], [0], [`The Piano Hall`],
    [74], [0], [`Bloom Hall`],
    [75], [0], [`Recital Hall`],
    [76], [0], [`Generic Hall`],
    [77], [0], [`Burst Space`],
    [78], [0], [`Real Dense Hall`],
    [79], [0], [`Concert Hall`],
    [80], [0], [`Standing Ovation`],
    [81], [0], [`Flinty Hall`],
    [82], [0], [`HighSchool Gym`],
    [83], [0], [`My Dreamy 481!!`],
    [84], [0], [`Deep Hall`],
    [85], [0], [`Immense Mosque`],
    [86], [0], [`Dreamverb`],
    [87], [0], [`Huge Batcave`],
    [95], [0], [`Classic Plate`],
    [96], [0], [`Weighty Platey`],
    [97], [0], [`Medm Warm Plate`],
    [98], [0], [`Bloom Plate`],
    [99], [0], [`Clean Plate`],
  ),
  caption: [KDFX_PRESET objects in bank 0],
)

#block(sticky: true)[DIR_BANK to query KDFX_PRESET type 141/\$010D in bank 1:]
#nonum[
```
F0 07 00 78 0C 01 0D 01 00 F7
```
]

Response: 100 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [100], [1], [`Plate Mail`],
    [101], [1], [`RealSmoothPlate`],
    [102], [1], [`Huge Tight Plate`],
    [103], [1], [`BigPredelayPlate`],
    [104], [1], [`Cool Dark Place`],
    [105], [1], [`Gunshot Verb`],
    [106], [1], [`Rvrb Compression`],
    [107], [1], [`Snappy Drum Room`],
    [108], [1], [`Roomitizer`],
    [109], [1], [`Live To Tape`],
    [110], [1], [`LrgRm`],
    [111], [1], [`Hall`],
    [112], [1], [`Gated Reverb`],
    [113], [1], [`Gate Plate`],
    [114], [1], [`Exponent Booth`],
    [115], [1], [`Drum Latch1`],
    [116], [1], [`Drum Latch2`],
    [117], [1], [`Diffuse Gate`],
    [118], [1], [`Acid Trip Room`],
    [119], [1], [`Furbelows`],
    [120], [1], [`Festoons`],
    [121], [1], [`Reverse Reverb`],
    [122], [1], [`Reverse Reverb 2`],
    [123], [1], [`Rvrs Laserverb`],
    [124], [1], [`Growler`],
    [125], [1], [`Ringy Drum Plate`],
    [126], [1], [`Oil Tank`],
    [127], [1], [`Wobbly Plate`],
    [128], [1], [`Pitcher Hall`],
    [129], [1], [`DistantPitchRoom`],
    [130], [1], [`Guitar Echo`],
    [131], [1], [`Stereo Echoes1`],
    [132], [1], [`Stereo Echoes2`],
    [133], [1], [`4-Tap Delay`],
    [134], [1], [`OffbeatFlamDelay`],
    [135], [1], [`8-Tap Delay`],
    [136], [1], [`Spectral 4-Tap`],
    [137], [1], [`Astral Taps`],
    [138], [1], [`SpectraShapeTaps`],
    [139], [1], [`Fanfare In Gmaj`],
    [140], [1], [`Basic Delay 1/8`],
    [141], [1], [`Diffuse Slaps`],
    [142], [1], [`Multitaps ms`],
    [143], [1], [`Timbre Taps`],
    [144], [1], [`Ecko Plecks`],
    [145], [1], [`Degenerator`],
    [146], [1], [`Nanobot Feedback`],
    [147], [1], [`Takes a while...`],
    [148], [1], [`Wait for UFO`],
    [149], [1], [`News Update`],
    [150], [1], [`Basic Chorus`],
    [151], [1], [`Chorus Comeback`],
    [152], [1], [`Chorusier`],
    [153], [1], [`Ordinary Chorus`],
    [154], [1], [`SlowSpinChorus`],
    [155], [1], [`Chorus Morris`],
    [156], [1], [`Everyday Chorus`],
    [157], [1], [`Thick Chorus`],
    [158], [1], [`Soft Chorus`],
    [159], [1], [`Rock Chorus`],
    [160], [1], [`Sm Stereo Chorus`],
    [161], [1], [`Lg Stereo Chorus`],
    [162], [1], [`Full Chorus`],
    [163], [1], [`Dense Gtr Chorus`],
    [164], [1], [`Standrd Gtr Chor`],
    [165], [1], [`Bass Chorus`],
    [166], [1], [`StChorus+Delay`],
    [167], [1], [`StChor+3vs2Delay`],
    [168], [1], [`CDR for Lead Gtr`],
    [169], [1], [`PinchChorusDelay`],
    [170], [1], [`Big Slow Flange`],
    [171], [1], [`Wetlip Flange`],
    [172], [1], [`Sweet Flange`],
    [173], [1], [`Throaty Flange`],
    [174], [1], [`Delirium Tremens`],
    [175], [1], [`Flanger Double`],
    [176], [1], [`Squeeze Flange`],
    [177], [1], [`Simply Flange`],
    [178], [1], [`Analog Flanger`],
    [179], [1], [`Soft Edge Flange`],
    [180], [1], [`Ned Flangers`],
    [181], [1], [`Wispy Flange`],
    [182], [1], [`Crystal Flange`],
    [183], [1], [`NarrowResFlange`],
    [184], [1], [`TightSlapFlange`],
    [185], [1], [`Flanged Taps`],
    [186], [1], [`StFlange+Delay`],
    [187], [1], [`StFlng+3vs2Delay`],
    [188], [1], [`Singing Flanger`],
    [189], [1], [`DampedEchoFlange`],
    [190], [1], [`Circles`],
    [191], [1], [`Slow Deep Phaser`],
    [192], [1], [`Manual Phaser`],
    [193], [1], [`Vibrato Phaser`],
    [194], [1], [`ThunderPhaser`],
    [195], [1], [`Saucepan Phaser`],
    [196], [1], [`Static Phaser`],
    [197], [1], [`Slippery Slope`],
    [198], [1], [`Westward Waves`],
    [199], [1], [`No Effect`],
  ),
  caption: [KDFX_PRESET objects in bank 1],
)

#block(sticky: true)[DIR_BANK to query KDFX_PRESET type 141/\$010D in bank 7:]
#nonum[
```
F0 07 00 78 0C 01 0D 07 00 F7
```
]

Response: 100 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [700], [7], [`Chorus Delay`],
    [701], [7], [`Chorus PanDelay`],
    [702], [7], [`Doubler & Echo`],
    [703], [7], [`Chorus VryLngDly`],
    [704], [7], [`FastChorusDouble`],
    [705], [7], [`BasicChorusDelay`],
    [706], [7], [`MultiTap Chorus`],
    [707], [7], [`ThickChorus no4T`],
    [708], [7], [`Chorused Taps`],
    [709], [7], [`Chorus Slapbacks`],
    [710], [7], [`MultiEchoChorus`],
    [711], [7], [`ChorusDelayHall`],
    [712], [7], [`ChorDlyRvb Lead`],
    [713], [7], [`ChorDlyRvb Lead2`],
    [714], [7], [`Fluid ChorDlyRvb`],
    [715], [7], [`ChorLite DlyHall`],
    [716], [7], [`ChorusSmallRoom`],
    [717], [7], [`DeepChorDlyHall`],
    [718], [7], [`Chorus PercHall`],
    [719], [7], [`Chorus Booth`],
    [720], [7], [`ClassicEP ChorRm`],
    [721], [7], [`ChorusMedChamber`],
    [722], [7], [`Vanilla ChorRvb`],
    [723], [7], [`Chorus Slow Hall`],
    [724], [7], [`SoftChorus Hall`],
    [725], [7], [`ChorBigBrtPlate`],
    [726], [7], [`Chorus Air`],
    [727], [7], [`Chorus HiCeiling`],
    [728], [7], [`Chorus MiniHall`],
    [729], [7], [`CathedralChorus`],
    [730], [7], [`PsiloChorusHall`],
    [731], [7], [`GuitarChorLsrDly`],
    [732], [7], [`Flange + Delay`],
    [733], [7], [`ThroatyFlangeDly`],
    [734], [7], [`Flange + 4Tap`],
    [735], [7], [`Bap ba-da-dap`],
    [736], [7], [`Slapback Flange`],
    [737], [7], [`Quantize+Flange`],
    [738], [7], [`FlangeDelayHall`],
    [739], [7], [`FlangeDelayRoom`],
    [740], [7], [`SloFlangeDlyRoom`],
    [741], [7], [`FlangeDlyBigHall`],
    [742], [7], [`Flange Theatre`],
    [743], [7], [`FlangeVerb Clav`],
    [744], [7], [`FlangeVerb Gtr`],
    [745], [7], [`Flange Hall`],
    [746], [7], [`Flange Booth`],
    [747], [7], [`Flange->LaserDly`],
    [748], [7], [`FlangeTap Synth`],
    [749], [7], [`Lazertag Flange`],
    [750], [7], [`Flange->Pitcher`],
    [751], [7], [`Flange->Shaper`],
    [752], [7], [`Shaper->Flange`],
    [753], [7], [`Warped Echoes`],
    [754], [7], [`Delay`],
    [755], [7], [`StereoFlamDelay`],
    [756], [7], [`2Dlys Ch Fl Mono`],
    [757], [7], [`LaserDelay->Rvb`],
    [758], [7], [`Shaper->Reverb`],
    [759], [7], [`MnPitcher+Chorus`],
    [760], [7], [`MnPitcher+Flange`],
    [761], [7], [`Pitcher+Chor+Dly`],
    [762], [7], [`Pitcher+Flng+Dly`],
    [763], [7], [`SubtleDistortion`],
    [764], [7], [`Synth Distortion`],
    [765], [7], [`Dist Cab EPiano`],
    [766], [7], [`Distortion+EQ`],
    [767], [7], [`Burnt Transistor`],
    [768], [7], [`TubeAmp DlyChor`],
    [769], [7], [`TubeAmp DlyChor2`],
    [770], [7], [`TubeAmp DlyFlnge`],
    [771], [7], [`TubeAmp Flange`],
    [772], [7], [`PolyAmp Chorus`],
    [773], [7], [`PolyAmp DlyFlnge`],
    [774], [7], [`VibrChor Rotors`],
    [775], [7], [`SlightDistRotors`],
    [776], [7], [`Rotostort`],
    [777], [7], [`VibrChor Rotors2`],
    [778], [7], [`Full VbCh Rotors`],
    [779], [7], [`KB3 FXBus`],
    [780], [7], [`KB3 AuxFX`],
    [781], [7], [`Pitch Spinner`],
    [782], [7], [`VibrChrDstRotor1`],
    [783], [7], [`VibrChrDstRotor2`],
    [784], [7], [`VibChrDstRotor3`],
    [785], [7], [`FullVbChTubeRotr`],
    [786], [7], [`ChorDlyHall 2`],
    [787], [7], [`Flange Hall 2`],
    [788], [7], [`SpeeChorusDeep`],
    [789], [7], [`Fluid Wash`],
    [790], [7], [`VC+DistRotor`],
    [791], [7], [`Slow Res Rotor`],
    [792], [7], [`Smooth Rotors`],
    [793], [7], [`Very Nazty Rotor`],
    [794], [7], [`80's Funk Guitar`],
    [795], [7], [`Mean 70'sFunkGtr`],
    [796], [7], [`Crunch Guitar`],
    [797], [7], [`Classic Gtr Dist`],
    [798], [7], [`SaturatedGtrDist`],
    [799], [7], [`TubeDist DlyChor`],
  ),
  caption: [KDFX_PRESET objects in bank 7],
)

#block(sticky: true)[DIR_BANK to query KDFX_PRESET type 141/\$010D in bank 8:]
#nonum[
```
F0 07 00 78 0C 01 0D 08 00 F7
```
]

Response: 71 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [800], [8], [`Sweet Hall`],
    [801], [8], [`Small Hall`],
    [802], [8], [`Medium Hall`],
    [803], [8], [`Large Hall`],
    [804], [8], [`Big Gym`],
    [805], [8], [`Bright Plate 1`],
    [806], [8], [`Opera House`],
    [807], [8], [`Live Chamber`],
    [808], [8], [`Bathroom`],
    [809], [8], [`Med Large Room`],
    [810], [8], [`Real Room`],
    [811], [8], [`Drum Room`],
    [812], [8], [`Small Dark Room`],
    [813], [8], [`Small Closet`],
    [814], [8], [`Add Ambience`],
    [815], [8], [`Gated Reverb`],
    [816], [8], [`Reverse Reverb`],
    [817], [8], [`Non-Linear`],
    [818], [8], [`Slapverb`],
    [819], [8], [`Full Bass`],
    [820], [8], [`Room + Delay`],
    [821], [8], [`Delay Big Hall`],
    [822], [8], [`Chorus Room`],
    [823], [8], [`Chorus Smallhall`],
    [824], [8], [`Chorus Med Hall`],
    [825], [8], [`Chorus Big Hall`],
    [826], [8], [`Chor-Delay Room`],
    [827], [8], [`Chor-Delay Hall`],
    [828], [8], [`Flange-Dly Room`],
    [829], [8], [`Flange-Dly Hall`],
    [830], [8], [`Stereo Chorus`],
    [831], [8], [`Stereo Flanger`],
    [832], [8], [`Stereo Delay`],
    [833], [8], [`4-Tap Delay`],
    [834], [8], [`Chorus Delay`],
    [835], [8], [`Flange Delay`],
    [836], [8], [`Chorus 4-Tap`],
    [837], [8], [`Flange 4 Tap`],
    [838], [8], [`Chorus Echo`],
    [839], [8], [`Chorus Echoverb`],
    [840], [8], [`Fast Flange`],
    [841], [8], [`Wash`],
    [842], [8], [`Into The Abyss`],
    [843], [8], [`Space Flanger`],
    [844], [8], [`Flange Room`],
    [845], [8], [`Predelay Hall`],
    [846], [8], [`Flange Echo`],
    [847], [8], [`Rotary Club`],
    [848], [8], [`Rotary Hall`],
    [849], [8], [`Chorus`],
    [850], [8], [`Soundbrd/rvb`],
    [851], [8], [`Percussive Room`],
    [852], [8], [`Brt Empty Room`],
    [853], [8], [`Mosque Room`],
    [854], [8], [`New Gated`],
    [855], [8], [`Chorus Slap Room`],
    [856], [8], [`Chorus Bass Room`],
    [857], [8], [`New Chorus Hall`],
    [858], [8], [`Spacious`],
    [859], [8], [`Wash Lead`],
    [860], [8], [`New Hall w/Delay`],
    [861], [8], [`Rich Delay`],
    [862], [8], [`Glass Delay`],
    [863], [8], [`Real Plate`],
    [864], [8], [`Real Niceverb`],
    [865], [8], [`ClassicalChamber`],
    [866], [8], [`Empty Stage`],
    [867], [8], [`Long & Narrow`],
    [868], [8], [`Far Bloom`],
    [869], [8], [`Floyd Hall`],
    [870], [8], [`With A Mic`],
  ),
  caption: [KDFX_PRESET objects in bank 8],
)

#block(sticky: true)[DIR_BANK to query KDFX_PRESET type 141/\$010D in bank 9:]
#nonum[
```
F0 07 00 78 0C 01 0D 09 00 F7
```
]

Response: 100 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [900], [9], [`Basic Env Filter`],
    [901], [9], [`Phunk Env Filter`],
    [902], [9], [`Synth Env Filter`],
    [903], [9], [`Bass Env Filter`],
    [904], [9], [`EPno Env Filter`],
    [905], [9], [`Trig Env Filter`],
    [906], [9], [`LFO Sweep Filter`],
    [907], [9], [`DoubleRiseFilter`],
    [908], [9], [`Circle Bandsweep`],
    [909], [9], [`Resonant Filter`],
    [910], [9], [`Dual Res Filter`],
    [911], [9], [`EQ Morpher`],
    [912], [9], [`Mono EQ Morpher`],
    [913], [9], [`Ring Modulator`],
    [914], [9], [`PitcherA`],
    [915], [9], [`PitcherB`],
    [916], [9], [`SuperShaper`],
    [917], [9], [`SubtleDrumShape`],
    [918], [9], [`3 Band Shaper`],
    [919], [9], [`LaserVerb`],
    [920], [9], [`Laserwaves`],
    [921], [9], [`Crystallizer`],
    [922], [9], [`Spry Young Boy`],
    [923], [9], [`Cheap LaserVerb`],
    [924], [9], [`Drum Neurezonate`],
    [925], [9], [`LazerfazerEchoes`],
    [926], [9], [`Simple Lazerverb`],
    [927], [9], [`TripFilter`],
    [928], [9], [`Gated Laserverb`],
    [929], [9], [`Waterford`],
    [930], [9], [`A little dirty`],
    [931], [9], [`Slight Overload`],
    [932], [9], [`Blown Speaker`],
    [933], [9], [`Ring Linger`],
    [934], [9], [`Drum Shaper`],
    [935], [9], [`Aliaser`],
    [936], [9], [`Quantize+Alias1`],
    [937], [9], [`Quantize+Alias2`],
    [938], [9], [`Drum Mortar`],
    [939], [9], [`Superphasulate`],
    [940], [9], [`Rich Noodle`],
    [941], [9], [`Nickel Chorus`],
    [942], [9], [`HF Stimulator`],
    [943], [9], [`OddHarmSuppress`],
    [944], [9], [`AM Radio`],
    [945], [9], [`U-Shaped EQ`],
    [946], [9], [`Drum Crusher`],
    [947], [9], [`Vocal Room`],
    [948], [9], [`Vocal Stage`],
    [949], [9], [`Mid Compressor`],
    [950], [9], [`?`],
    [951], [9], [`?`],
    [952], [9], [`?`],
    [953], [9], [`?`],
    [954], [9], [`?`],
    [955], [9], [`Compress w/SC EQ`],
    [956], [9], [`Compress/Expand`],
    [957], [9], [`Comprs/Expnd +EQ`],
    [958], [9], [`Reverb>Compress`],
    [959], [9], [`Reverb>Compress2`],
    [960], [9], [`Drum Comprs>Rvb`],
    [961], [9], [`Expander`],
    [962], [9], [`3Band Compressor`],
    [963], [9], [`Simple Gate`],
    [964], [9], [`Gate w/ SC EQ`],
    [965], [9], [`Graphic EQ`],
    [966], [9], [`5 Band EQ`],
    [967], [9], [`ContourGraphicEQ`],
    [968], [9], [`Dance GraphicEQ`],
    [969], [9], [`OldPianoEnhancer`],
    [970], [9], [`3 Band Enhancer`],
    [971], [9], [`3 Band Enhancer2`],
    [972], [9], [`Extreem Enhancer`],
    [973], [9], [`Tremolo`],
    [974], [9], [`Dual Panner`],
    [975], [9], [`SRS`],
    [976], [9], [`Widespread`],
    [977], [9], [`Mono->Stereo`],
    [978], [9], [`3 Band Compress`],
    [979], [9], [`Simple Panner`],
    [980], [9], [`Big Bass EQ`],
    [981], [9], [`Poly Pitcher`],
    [982], [9], [`CheapVoxChanger`],
    [983], [9], [`Hip Hop Aura`],
    [984], [9], [`Woodenize`],
    [985], [9], [`Marimbafication`],
    [986], [9], [`Frequency Offset`],
    [987], [9], [`Drum Loosener`],
    [988], [9], [`Drum Tightener`],
    [989], [9], [`Vox Honker`],
    [990], [9], [`Glacial Canyon`],
    [991], [9], [`Spring Thing`],
    [992], [9], [`Contact`],
    [993], [9], [`Drum Frightener`],
    [994], [9], [`Mad Hatter`],
    [995], [9], [`Fallout`],
    [996], [9], [`Ascension`],
    [997], [9], [`60Hz Buzz Kill`],
    [998], [9], [`Stereo Analyze`],
    [999], [9], [`FX Mod Diag`],
  ),
  caption: [KDFX_PRESET objects in bank 9],
)

=== DITHER

#block(sticky: true)[DIR_BANK to query DITHER type 142/\$010E in bank 0:]
#nonum[
```
F0 07 00 78 0C 01 0E 00 00 F7
```
]

Response: 16 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [0], [0], [`SumOnly`],
    [1], [0], [`ClearBase`],
    [2], [0], [`FlatBase`],
    [3], [0], [`MinimumBase`],
    [4], [0], [`MediumBase`],
    [5], [0], [`MaximumBase`],
    [11], [0], [`Clear18`],
    [12], [0], [`Flat18`],
    [13], [0], [`Minimum18`],
    [14], [0], [`Medium18`],
    [15], [0], [`Maximum18`],
    [21], [0], [`Clear16`],
    [22], [0], [`Flat16`],
    [23], [0], [`Minimum16`],
    [24], [0], [`Medium16`],
    [25], [0], [`Maximum16`],
  ),
  caption: [DITHER objects in bank 0],
)

#block(sticky: true)[DIR_BANK to query DITHER type 142/\$010E in bank 1:]
#nonum[
```
F0 07 00 78 0C 01 0E 01 00 F7
```
]

Response: 1 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [100], [1], [`PatchDither16`],
  ),
  caption: [DITHER objects in bank 1],
)

=== KDFX_ALG

#block(sticky: true)[DIR_BANK to query KDFX_ALG type 143/\$010F in bank 0:]
#nonum[
```
F0 07 00 78 0C 01 0F 00 00 F7
```
]

Response: 15 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [1], [0], [`MiniVerb`],
    [2], [0], [`Dual MiniVerb`],
    [3], [0], [`Gated MiniVerb`],
    [4], [0], [`Classic Place`],
    [5], [0], [`Classic Verb`],
    [6], [0], [`TQ Place`],
    [7], [0], [`TQ Verb`],
    [8], [0], [`Diffuse Place`],
    [9], [0], [`Diffuse Verb`],
    [10], [0], [`OmniPlace`],
    [11], [0], [`OmniVerb`],
    [12], [0], [`Panaural Room`],
    [13], [0], [`Stereo Hall`],
    [14], [0], [`Grand Plate`],
    [15], [0], [`Finite Verb`],
  ),
  caption: [KDFX_ALG objects in bank 0],
)

#block(sticky: true)[DIR_BANK to query KDFX_ALG type 143/\$010F in bank 1:]
#nonum[
```
F0 07 00 78 0C 01 0F 01 00 F7
```
]

Response: 22 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [130], [1], [`Complex Echo`],
    [131], [1], [`4-Tap Delay`],
    [132], [1], [`4-Tap Delay BPM`],
    [133], [1], [`8-Tap Delay`],
    [134], [1], [`8-Tap Delay BPM`],
    [135], [1], [`Spectral 4-Tap`],
    [136], [1], [`Spectral 6-Tap`],
    [138], [1], [`Degen Regen BPM`],
    [139], [1], [`Switch Loops`],
    [140], [1], [`Moving Delay`],
    [150], [1], [`Chorus 1`],
    [151], [1], [`Chorus 2`],
    [152], [1], [`Dual Chorus 1`],
    [153], [1], [`Dual Chorus 2`],
    [154], [1], [`Flanger 1`],
    [155], [1], [`Flanger 2`],
    [156], [1], [`LFO Phaser`],
    [157], [1], [`LFO Phaser Twin`],
    [158], [1], [`Manual Phaser`],
    [159], [1], [`VibratoPhaser`],
    [160], [1], [`SingleLFO Phaser`],
    [161], [1], [`Allpass Phaser 3`],
  ),
  caption: [KDFX_ALG objects in bank 1],
)

#block(sticky: true)[DIR_BANK to query KDFX_ALG type 143/\$010F in bank 7:]
#nonum[
```
F0 07 00 78 0C 01 0F 07 00 F7
```
]

Response: 51 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [700], [7], [`Chorus+Delay`],
    [701], [7], [`Chorus+4Tap`],
    [702], [7], [`Chorus<>4Tap`],
    [703], [7], [`Chor+Dly+Reverb`],
    [704], [7], [`Chorus<>Reverb`],
    [705], [7], [`Chorus<>LasrDly`],
    [706], [7], [`Flange+Delay`],
    [707], [7], [`Flange+4Tap`],
    [708], [7], [`Flange<>4Tap`],
    [709], [7], [`Flan+Dly+Reverb`],
    [710], [7], [`Flange<>Reverb`],
    [711], [7], [`Flange<>LasrDly`],
    [712], [7], [`Flange<>Pitcher`],
    [713], [7], [`Flange<>Shaper`],
    [714], [7], [`Quantize+Flange`],
    [715], [7], [`Dual MovDelay`],
    [716], [7], [`Quad MovDelay`],
    [717], [7], [`LasrDly<>Reverb`],
    [718], [7], [`Shaper<>Reverb`],
    [719], [7], [`Reverb<>Compress`],
    [720], [7], [`MonoPitcher+Chor`],
    [721], [7], [`MonoPitcher+Flan`],
    [722], [7], [`Pitcher+Chor+Dly`],
    [723], [7], [`Pitcher+Flan+Dly`],
    [724], [7], [`Mono Distortion`],
    [725], [7], [`MonoDistort+Cab`],
    [726], [7], [`MonoDistort + EQ`],
    [727], [7], [`PolyDistort + EQ`],
    [728], [7], [`StereoDistort+EQ`],
    [729], [7], [`TubeAmp<>MD>Chor`],
    [730], [7], [`TubeAmp<>MD>Flan`],
    [731], [7], [`PolyAmp<>MD>Chor`],
    [732], [7], [`PolyAmp<>MD>Flan`],
    [733], [7], [`VibChor+Rotor 2`],
    [734], [7], [`Distort + Rotary`],
    [735], [7], [`KB3 FXBus`],
    [736], [7], [`KB3 AuxFX`],
    [737], [7], [`VibChor+Rotor 4`],
    [738], [7], [`VC+Dist+1Rotor 2`],
    [739], [7], [`VC+Dist+HiLoRotr`],
    [740], [7], [`VC+Tube+Rotor 4`],
    [741], [7], [`Rotor 1`],
    [742], [7], [`VC+Dist+HiLoRot2`],
    [743], [7], [`Subtle Distort`],
    [744], [7], [`Quantize+Alias`],
    [745], [7], [`Pitcher+Miniverb`],
    [746], [7], [`Reverb+Compress`],
    [781], [7], [`St Chorus+Delay`],
    [784], [7], [`St Flange+Delay`],
    [790], [7], [`Gate+Cmp[EQ]+Vrb`],
    [792], [7], [`Gate+TubeAmp`],
  ),
  caption: [KDFX_ALG objects in bank 7],
)

#block(sticky: true)[DIR_BANK to query KDFX_ALG type 143/\$010F in bank 9:]
#nonum[
```
F0 07 00 78 0C 01 0F 09 00 F7
```
]

Response: 49 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [900], [9], [`Env Follow Filt`],
    [901], [9], [`TrigEnvelopeFilt`],
    [902], [9], [`LFO Sweep Filter`],
    [903], [9], [`Resonant Filter`],
    [904], [9], [`Dual Res Filter`],
    [905], [9], [`EQ Morpher`],
    [906], [9], [`Mono EQ Morpher`],
    [907], [9], [`Ring Modulator`],
    [908], [9], [`Pitcher`],
    [909], [9], [`Super Shaper`],
    [910], [9], [`3 Band Shaper`],
    [911], [9], [`Mono LaserVerb`],
    [912], [9], [`LaserVerb Lite`],
    [913], [9], [`LaserVerb`],
    [914], [9], [`Reverse LaserVrb`],
    [915], [9], [`Gated LaserVerb`],
    [916], [9], [`Poly Pitcher`],
    [917], [9], [`Frequency Offset`],
    [918], [9], [`MutualFreqOffset`],
    [919], [9], [`WackedPitchLFO`],
    [920], [9], [`Chaos!`],
    [948], [9], [`Band Compress`],
    [949], [9], [`CompressDualTime`],
    [950], [9], [`HardKneeCompress`],
    [951], [9], [`SoftKneeCompress`],
    [952], [9], [`Expander`],
    [953], [9], [`Compress w/SC EQ`],
    [954], [9], [`Compress/Expand`],
    [955], [9], [`Comp/Exp + EQ`],
    [956], [9], [`Compress 3 Band`],
    [957], [9], [`Gate`],
    [958], [9], [`Super Gate`],
    [959], [9], [`2 Band Enhancer`],
    [960], [9], [`3 Band Enhancer`],
    [961], [9], [`Tremolo`],
    [962], [9], [`Tremolo BPM`],
    [963], [9], [`AutoPanner`],
    [964], [9], [`Dual AutoPanner`],
    [965], [9], [`SRS`],
    [966], [9], [`Stereo Image`],
    [967], [9], [`Mono -> Stereo`],
    [968], [9], [`Graphic EQ`],
    [969], [9], [`Dual Graphic EQ`],
    [970], [9], [`5 Band EQ`],
    [971], [9], [`3 Band EQ`],
    [972], [9], [`HF Stimulate 1`],
    [975], [9], [`HarmonicSuppress`],
    [998], [9], [`FXMod Diagnostic`],
    [999], [9], [`Stereo Analyze`],
  ),
  caption: [KDFX_ALG objects in bank 9],
)

=== STUDIO_PARAM

#block(sticky: true)[DIR_BANK to query STUDIO_PARAM type 144/\$0110 in bank 0:]
#nonum[
```
F0 07 00 78 0C 01 10 00 00 F7
```
]

Response: 14 INFO

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Bank*], [*Name*]),
    [1], [0], [`Bass Shelf`],
    [2], [0], [`Treble Shelf`],
    [3], [0], [`Lowpass1`],
    [4], [0], [`Lowpass2`],
    [5], [0], [`Hipass1`],
    [6], [0], [`Hipass2`],
    [7], [0], [`Mid Tone`],
    [8], [0], [`Mono Pan`],
    [9], [0], [`Stereo Pan`],
    [10], [0], [`Stereo Balance`],
    [11], [0], [`Mixer`],
    [12], [0], [`Auxer`],
    [13], [0], [`ClearoutOne`],
    [14], [0], [`ClearoutThree`],
  ),
  caption: [STUDIO_PARAM objects in bank 0],
)

== Updating Sound block

This section describes the sequence used by `KurzweilExplorer` to load a patch with samples given a SysEx file and a PCM file.

=== Sequence for one sound block

When the sysex contains a SOUND_BLOCK, multiple commands needs to be executed:

#figure(
  table(
    align: left,
    columns: (auto, 1fr, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Step*], [*Sent*], [*Expected answer*]),
    [1],
    [`DIR` of the sound block, type `01 06`],
    [`INFO`: size zero if the sample is not on the unit, otherwise the sample name is returned],

    [2], [If the size is zero, We send the PCM file using SDS, starting with the SDS header], [SDS `ACK` (`7F`)],
    [3], [SDS data packets, numbered from 0], [SDS `ACK` for each packet, or SDS `NAK` and a resend],
    [4], [`READ` of the unit sound block], [`WRITE` of the current sound block. contains the memory address],

    [5],
    [`WRITE` of the sysex sound block, with its headers relocated to the address found in step 4],
    [`DACK`, or `DNAK` with an error code],
  ),
  caption: [Uploading one sample and its sound block],
)

Some details of the implementation:

- The sample number sent in step 2 is `id - 1`. The K2600R stores the sample at index `id`.
- The base address is not chosen by us. After the dump, the unit places the sample at an address in sample RAM. The
  sound block returned in step 4 holds that address (`sampleStart`).
- If the sample is already in the unit with the same name, steps 2 and 3 are skipped.
- If the sample is on the unit with another name, the ID is already used.

=== Sequence for a patch

`KurzweilExplorer.sendPatch` reads the objects of a `.syx` file in order and sends them one by one:

- each sound block goes through the sequence above.
- every other object (keymaps, then the program) is sent as a single `WRITE`.
- After each one, the code waits for a `DACK` before going on, so that the unit is never flooded.

The sample blocks must therefore come first in the file. The `.syx` files produced by `KurzweilFileConverter` respect
this order: sample blocks, keymaps, then the program (see the converter chapter).

=== Errors to expect

- `DNAK` with code 5 means the sample RAM is full. Free some RAM on the unit, or delete the unused samples, then restart
  the sequence.
- `DNAK` with code 1 means the object is being edited on the front panel. Exit the editor on the unit first.
- A missing `ACK` after the header, or a `NAK` that does not clear after retries, aborts the dump. The code sends a
  CANCEL and raises an exception. The sound block is then never written, so the program that uses it will not find its
  sample.

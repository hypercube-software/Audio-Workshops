#import "settings.typ": *
#let hx(b, c) = box(fill: c, inset: (x: 2.5pt, y: 2pt), radius: 2pt, raw(b))
#let hexline(..c) = block(c.pos().join(h(2pt)))

#chapter("MIDI Format", image: image("./assets/hd3-2481-500.svg"), l: "chap1")
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

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [1], [`3-A`],
    [2], [`2-PA`],
    [3], [`2-DA`],
    [4], [`2-1-A`],
    [5], [`2-SA`],
    [6], [`2-1-MA`],
    [7], [`2-1-MA`],
    [8], [`1-1-1-A`],
    [9], [`1-1-SA`],
    [10], [`1-1-1-MA`],
    [11], [`1-1-1-MA`],
    [12], [`1-1-1-MA`],
    [13], [`1-1-PA`],
    [14], [`1-1-DA`],
    [15], [`1-1-DA`],
    [16], [`1-T-A`],
    [17], [`1-V-A`],
    [18], [`1-M-V-A`],
    [19], [`LP-V-A`],
    [20], [`1-M-1-A`],
    [21], [`1-M-SA`],
    [22], [`1-M-1-MA`],
    [23], [`1-M-1-MA`],
    [24], [`1-M-PA`],
    [25], [`1-M-DA`],
    [26], [`HS-PA`],
    [27], [`HS-1-A`],
    [28], [`HS-SA`],
    [29], [`HM-1-HS-DA`],
    [30], [`HM-1-HS-MA`],
    [31], [`HM-HS-DA`],
    [33], [`2-2`],
    [34], [`1-2-1`],
    [35], [`2-1-1`],
    [36], [`1-1-2`],
    [37], [`3-1`],
    [38], [`1-3`],
    [39], [`1-1-1-1`],
    [40], [`1-1-1-MA`],
    [41], [`1-1-1-MA`],
    [42], [`1-1-1-MA`],
    [43], [`1-1-MA-1`],
    [44], [`1-1-MA-1`],
    [45], [`1-MA-1-1`],
    [46], [`1-MA-2`],
    [47], [`2-1-MA`],
    [48], [`1-2-MA`],
    [49], [`2-1-MA`],
    [50], [`1-2-MA`],
    [51], [`3-MA`],
    [52], [`2-MA-1`],
    [53], [`1-MA-1-MA`],
    [54], [`1-MA-1-MA`],
    [55], [`1-MA-MA-1`],
    [56], [`1-1-MA-MA`],
    [57], [`1-1-MA-MA`],
    [58], [`2-MA-MA`],
    [59], [`1-MA-MA-MA`],
    [60], [`1-1-1-MA`],
    [61], [`1-1-MA-1`],
    [62], [`1-1-MA-MA`],
    [63], [`MA-1-1-1`],
    [64], [`MA-1-2`],
    [65], [`1-MA-1-1`],
    [66], [`1-MA-1-MA`],
    [67], [`1-MA-2`],
    [68], [`1-MA-1-1`],
    [69], [`1-MA-2`],
    [70], [`1-1-1-MA`],
    [71], [`1-1-1-MA`],
    [72], [`MA-1-1-MA`],
    [73], [`MA-1-1-MA`],
    [74], [`MA-1-1-MA`],
    [75], [`MA-1-1-MA`],
    [76], [`MA-1-1-MA`],
    [77], [`MA-1-1-MA`],
    [78], [`1-MA-1-1`],
    [79], [`MA-1-1-MA`],
    [80], [`MA-1-AMP`],
    [81], [`3-A`],
    [82], [`2-1-A`],
    [83], [`2-SA`],
    [84], [`2-1-MA`],
    [85], [`2-1-MA`],
    [86], [`1-1-1-A`],
    [87], [`1-1-SA`],
    [88], [`1-1-1-MA`],
    [89], [`1-1-1-MA`],
    [90], [`1-1-1-MA`],
    [91], [`1-T-A`],
    [92], [`1-V-A`],
    [93], [`LP-V-A`],
    [94], [`LOPAS2-SHMOD-AMP`],
    [95], [`1-M-1-A`],
    [96], [`1-M-SA`],
    [97], [`1-MA-1-MA`],
    [98], [`1-MA-1-MA`],
    [99], [`1-1-1-MA`],
  ),
  caption: [ALG_DESC objects in bank 0],
)

#block(sticky: true)[DIR_BANK to query ALG_DESC type 102/\$0066 in bank 1:]
#nonum[
```
F0 07 00 78 0C 00 66 01 00 F7
```
]

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [100], [`2-1-MA`],
    [101], [`2-1-AMP`],
    [102], [`1-1-1-AMP`],
    [103], [`1-2-AMP`],
    [104], [`1-1-2.AMP`],
    [105], [`2-2.AMP`],
    [106], [`2-1-AMP`],
    [107], [`2-1-AMP`],
    [108], [`2-1-AMP`],
    [109], [`2-1-AMP`],
    [110], [`2-1-AMP`],
    [111], [`2-1-AMP`],
    [112], [`2-1-AMP`],
    [113], [`2-1-AMP`],
    [114], [`2-1-AMP`],
    [115], [`2-1-AMP`],
    [116], [`2-1-AMP`],
    [117], [`2-1-AMP`],
    [118], [`2-1-AMP`],
    [119], [`2-1-AMP`],
    [120], [`2-1-AMP`],
    [121], [`2-1-AMP`],
    [122], [`2-1-AMP`],
    [123], [`2-1-AMP`],
    [124], [`2-1-AMP`],
    [125], [`2-1-AMP`],
    [126], [`2-1-AMP`],
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

Response: 75 INFO + 1 END_OF_BANK

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [1], [`ProgramMode`],
    [2], [`SetupMode`],
    [3], [`QuickAccessMode`],
    [4], [`EffectsMode`],
    [5], [`MIDIMode:TRANSMIT`],
    [6], [`MIDIMode:RECEIVE`],
    [7], [`MIDIMode:CHANNELS`],
    [8], [`MasterMode`],
    [9], [`Master2`],
    [10], [`SongMode:MAIN`],
    [11], [`DiskMode`],
    [12], [`SampleRecord`],
    [13], [`SampleMode`],
    [14], [`SongMode:MISC`],
    [15], [`SongMode:MIX`],
    [16], [`EditSong:EVENT`],
    [17], [`EditSong:TRACK`],
    [18], [`EditSong:ARRANGE`],
    [19], [`EditSong:STEP`],
    [20], [`EVENT:View`],
    [21], [`EditSetup`],
    [22], [`EditQuickAccess`],
    [23], [`EditIntonationTable`],
    [24], [`EditVelocityMap`],
    [25], [`EditPressureMap`],
    [26], [`EditSong:COMMON`],
    [27], [`EditKeyMap`],
    [28], [`Edit`],
    [29], [`F`],
    [30], [`S`],
    [31], [`KDFXMode:CTRL`],
    [32], [`EditStudio:INPUT`],
    [33], [`EditStudio:FXBUS`],
    [34], [`EditStudio:AUXFX`],
    [35], [`EditStudio:OUTPUT`],
    [36], [`EditStudio:FXSEND`],
    [38], [`Mixdown`],
    [39], [`MIDI Faders`],
    [40], [`GOTO`],
    [41], [`BEND`],
    [42], [`COMMON`],
    [43], [`ARPEG`],
    [44], [`RIBCFG`],
    [45], [`CH/PRG`],
    [46], [`KEY/VEL`],
    [47], [`PAN/VOL`],
    [48], [`RFU`],
    [49], [`SLIDER`],
    [50], [`SLID/2`],
    [51], [`FOOTSW`],
    [52], [`CPEDAL`],
    [53], [`RIBBON`],
    [54], [`WHEEL`],
    [55], [`SWITCH`],
    [56], [`PRESS`],
    [60], [`EditFXPreset:PARAM1`],
    [61], [`EditFXPreset:PARAM2`],
    [62], [`EditFXPreset:PARAM3`],
    [63], [`EditFXPreset:PARAM4`],
    [70], [`KDFX`],
    [71], [`FXMOD2`],
    [72], [`FXMOD3`],
    [73], [`FXMOD4`],
    [74], [`FXLFO`],
    [75], [`FXASR`],
  ),
  caption: [EDIT_MENU objects in bank 0],
)

#block(sticky: true)[DIR_BANK to query EDIT_MENU type 107/\$006B in bank 1:]
#nonum[
```
F0 07 00 78 0C 00 6B 01 00 F7
```
]

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [100], [`FXFUN`],
    [101], [`FX Import`],
    [104], [`GOTO`],
    [105], [`COMMON`],
    [106], [`LAYER`],
    [107], [`KEYMAP`],
    [108], [`OUTPUT`],
    [109], [`PITCH`],
    [110], [`AMPENV`],
    [111], [`ENV2`],
    [112], [`ENV3`],
    [113], [`ENVCTL`],
    [114], [`LFO`],
    [115], [`ASR`],
    [116], [`VTRIG`],
    [117], [`FUN`],
    [118], [`EFFECT`],
    [119], [`TONEWL`],
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

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [1], [`main`],
    [2], [`edprog`],
    [3], [`mngb`],
    [4], [`mng2a3`],
    [5], [`fx`],
    [6], [`mng2a2`],
    [7], [`mng2a2`],
    [8], [`mng1`],
    [9], [`mng2b`],
    [10], [`mng2c`],
    [11], [`mng2a2`],
    [12], [`studio`],
    [13], [`multifx`],
    [15], [`mngsb`],
    [16], [`kdfx`],
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

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [1], [`Keys`],
    [2], [`Analog`],
    [3], [`Hard Edge`],
    [4], [`Jazz`],
    [5], [`Rock/Blues`],
    [6], [`Classical`],
    [7], [`Film Score`],
    [8], [`Jukebox`],
    [9], [`Basic QA Bank`],
  ),
  caption: [B_LIST objects in bank 0],
)

#block(sticky: true)[DIR_BANK to query B_LIST type 111/\$006F in bank 127:]
#nonum[
```
F0 07 00 78 0C 00 6F 7F 00 F7
```
]

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [800], [`Bands`],
    [801], [`Grooves`],
    [802], [`World`],
    [803], [`Pop`],
    [804], [`More Keys`],
    [805], [`More Analog`],
    [806], [`Leads`],
    [807], [`Trio Parts`],
    [808], [`Techno`],
    [809], [`Texture`],
    [900], [`Piano Patch`],
    [901], [`Full Orch`],
    [902], [`Strings`],
    [903], [`Horns`],
    [904], [`Winds`],
    [905], [`Solo Orch`],
    [906], [`Perc Pit`],
    [907], [`Perc Ens`],
    [908], [`Moody`],
    [909], [`Exotic`],
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

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [1], [`Sine`],
    [2], [`+Sine`],
    [3], [`Square`],
    [4], [`+Square`],
    [5], [`Triangle`],
    [6], [`+Triangle`],
    [7], [`Rise Saw`],
    [8], [`+Rise Saw`],
    [9], [`Fall Saw`],
    [10], [`+Fall Saw`],
    [20], [`3 Step`],
    [21], [`+3 Step`],
    [22], [`4 Step`],
    [23], [`+4 Step`],
    [24], [`5 Step`],
    [25], [`+5 Step`],
    [26], [`6 Step`],
    [27], [`+6 Step`],
    [28], [`7 Step`],
    [29], [`+7 Step`],
    [30], [`8 Step`],
    [31], [`+8 Step`],
    [34], [`10 Step`],
    [35], [`+10 Step`],
    [38], [`12 Step`],
    [39], [`+12 Step`],
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

Response: 69 INFO + 1 END_OF_BANK

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [1], [`a+b`],
    [2], [`a-b`],
    [3], [`(a+b)/2`],
    [4], [`a/2+b`],
    [5], [`a/4+b/2`],
    [6], [`(a+2b)/3`],
    [9], [`a*b`],
    [10], [`-a*b`],
    [13], [`a*10^b`],
    [16], [`|a+b|`],
    [17], [`|a-b|`],
    [18], [`min(a,b)`],
    [19], [`max(a,b)`],
    [20], [`Quantize B To A`],
    [22], [`lowpass(f=a,b)`],
    [23], [`hipass(f=a,b)`],
    [25], [`b/(1-a)`],
    [27], [`a(b-y)`],
    [29], [`(a+b)^2`],
    [31], [`sin(a+b)`],
    [32], [`cos(a+b)`],
    [33], [`tri(a+b)`],
    [35], [`warp1(a,b)`],
    [36], [`warp2(a,b)`],
    [37], [`warp3(a,b)`],
    [38], [`warp4(a,b)`],
    [39], [`warp8(a,b)`],
    [41], [`a AND b`],
    [42], [`a OR b`],
    [43], [`b > a`],
    [47], [`ramp(f=a+b)`],
    [48], [`ramp(f=a-b)`],
    [49], [`ramp(f=(a+b)/2)`],
    [50], [`ramp(f=a*b)`],
    [51], [`ramp(f=-a*b)`],
    [52], [`ramp(f=a*10^b)`],
    [53], [`ramp(f=(a+b)/4)`],
    [54], [`a(y+b)`],
    [55], [`ay+b`],
    [56], [`(a+1)y+b`],
    [57], [`y+a(y+b)`],
    [58], [`a|y|+b`],
    [61], [`Sample B On A`],
    [62], [`Sample B On ~A`],
    [63], [`Track B While A`],
    [64], [`diode(a-b)`],
    [65], [`diode(a-b+.5)`],
    [66], [`diode(a-b-.5)`],
    [67], [`diode(a-b+.25)`],
    [68], [`diode(a-b-.25)`],
    [69], [`Track B While ~A`],
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

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [1], [`Tonal Raver`],
    [2], [`Kickin' Wave`],
    [3], [`Hunkster Funk`],
    [4], [`Off Your Rocker`],
    [5], [`Shimmer Hop`],
    [6], [`Slow Hipmeister`],
    [7], [`Slow Driver`],
    [8], [`Acidic Dance`],
    [9], [`JR's Dance`],
    [10], [`Drum & Wave`],
    [11], [`Jetzen's Trance`],
    [12], [`Euro-Rave`],
    [13], [`Acoustic Country`],
    [14], [`Trip-Hopper`],
    [15], [`Latinease`],
    [16], [`Hop II`],
    [17], [`Slider Grooves`],
    [18], [`Wah Blues`],
    [19], [`DrumsE-B1Fills#3`],
    [20], [`Controlled FX`],
    [21], [`Quantum Mechanix`],
    [22], [`Driven`],
    [23], [`Drewz Groove`],
    [24], [`Jazz Session 101`],
    [25], [`Piano Trio & Str`],
    [26], [`Jazzy's Band`],
    [27], [`SmoothEPiano`],
    [28], [`Orchescape`],
    [29], [`New Rock Band`],
    [30], [`Harpsichord&Vox`],
    [31], [`Analogued`],
    [32], [`Elegant Grandeur`],
    [33], [`Cathedral`],
    [34], [`Church Organ`],
    [35], [`Jazz & Pop Brass`],
    [36], [`Glory Orchestra`],
    [37], [`Super Lush`],
    [38], [`Scoring Stage`],
    [39], [`Happnin' Brass`],
    [40], [`RbnSpltB3+MIDIPd`],
    [41], [`SpltVASTB+MIDIPd`],
    [42], [`Guitars Sent Her`],
    [43], [`GB 6Keys&2Basses`],
    [44], [`Comper Keys`],
    [45], [`Bold Beauty`],
    [46], [`Nubian Nightfall`],
    [47], [`Polar Reverie`],
    [48], [`Vapor Drawing`],
    [49], [`Siamese Ribbon`],
    [50], [`Spacegroove`],
    [51], [`F1 Latino Groove`],
    [52], [`Dream Pad RbnVel`],
    [53], [`C2 Jam`],
    [54], [`Nu C2 & Lead`],
    [55], [`Percolator`],
    [56], [`Acid Prog C2`],
    [57], [`Krazed Keith`],
    [58], [`Conn Trolled`],
    [59], [`Ravin & Droolin`],
    [60], [`Clanger`],
    [61], [`Waverado Rbn SW`],
    [62], [`Elephantitus`],
    [63], [`Ether Dreams`],
    [64], [`Rbn Slider Bells`],
    [65], [`Munchkin Factory`],
    [66], [`Clink Split`],
    [67], [`Lush World`],
    [68], [`IX Dancers`],
    [69], [`MidEasternBells`],
    [70], [`FM Morpher`],
    [71], [`Slo n' Funky`],
    [72], [`Liquid Guitars`],
    [73], [`Big & Meaty`],
    [74], [`Glazing`],
    [75], [`Play With Balls`],
    [76], [`Pepper Digital`],
    [77], [`De Tant`],
    [78], [`Old & New Pad`],
    [79], [`Punch-n-Oinky`],
    [80], [`Brazzine`],
    [81], [`Pad W/ Rotor`],
    [82], [`Tang Nebulae`],
    [83], [`Boink Head`],
    [84], [`Voyager`],
    [85], [`Changling`],
    [86], [`Wash Cycle`],
    [87], [`NewTaleSpinnin'`],
    [88], [`Electric Swirl`],
    [89], [`Land Of Giants`],
    [90], [`Desert Planet`],
    [91], [`Terraformation`],
    [92], [`Sparkle & Bass`],
    [93], [`Area 51 1/2`],
    [94], [`Slider Player`],
    [95], [`Synthozilla`],
    [96], [`Dr.Distorted`],
    [97], [`ControlSetup`],
    [98], [`Clear Setup`],
    [99], [`Default Setup`],
  ),
  caption: [SETUP_2 objects in bank 0],
)

#block(sticky: true)[DIR_BANK to query SETUP_2 type 137/\$0109 in bank 8:]
#nonum[
```
F0 07 00 78 0C 01 09 08 00 F7
```
]

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [800], [`HyperGroov<-C4->`],
    [801], [`PianoPad w/Percs`],
    [802], [`Slo Held Arper`],
    [803], [`Don'tGetFooled`],
    [804], [`Touch Game`],
    [805], [`BeatBoy E1`],
    [806], [`ZawiClav Split`],
    [807], [`Dyn Piano Pad`],
    [808], [`Pulsar Stack`],
    [809], [`Mt Chicorora C2`],
    [810], [`Hold Low 3sec Rb`],
    [811], [`Mettlorfus Pad`],
    [812], [`Black Keys xtra`],
    [813], [`Jungle Jammer`],
    [814], [`Huge Rock Band`],
    [815], [`Rock Ballad`],
    [816], [`Jazz Setup`],
    [817], [`Two Touchers`],
    [818], [`Frontier prs`],
    [819], [`Eclectric Grand`],
    [820], [`Bad Trip FtSw/MW`],
    [821], [`WhirliToys`],
    [822], [`PluckSynths Perc`],
    [823], [`SusPed RhythmJam`],
    [824], [`Ballad Piano Pad`],
    [825], [`Big AnaLoveVibe`],
    [826], [`ShockBreaks PSw1`],
    [827], [`Four Pluckers`],
    [828], [`WaterPiano Pad`],
    [829], [`Padded Room`],
    [830], [`AtmosPolySphere`],
    [831], [`Breath Pad`],
    [832], [`Trippy Jam`],
    [833], [`MeditationGuits`],
    [834], [`Cool Down Funk`],
    [835], [#raw("Tek`Groov  C5->")],
    [836], [`Big Fat Split`],
    [837], [`The Pump C2`],
    [838], [`Ana Basses`],
    [839], [`Multi Followers`],
    [840], [`Plucksynths`],
    [841], [`10 Leagues Under`],
    [842], [`Gremlin Arps`],
    [843], [`Broken Toys`],
    [844], [`Two Synth`],
    [845], [`Machine Shop`],
    [846], [`Farawaway Place`],
    [847], [`BehindEnemyLines`],
    [848], [`Tunnel Visionprs`],
    [849], [`Seismic Trance`],
    [850], [`Medal`],
  ),
  caption: [SETUP_2 objects in bank 8],
)

#block(sticky: true)[DIR_BANK to query SETUP_2 type 137/\$0109 in bank 9:]
#nonum[
```
F0 07 00 78 0C 01 09 09 00 F7
```
]

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [900], [`Deep Piano Rbn`],
    [901], [`Choir & Harp`],
    [902], [`Orchestrator`],
    [903], [`Piano Concerto`],
    [904], [`Xmas Carols`],
    [905], [`Sideline Perc`],
    [906], [`TonalGroov  C5->`],
    [907], [`Exotic Grooves`],
    [908], [`Lunar Harp`],
    [909], [`Themes`],
    [910], [`Wet Piano`],
    [911], [`enter the Jester`],
    [912], [`Tap the Jester`],
    [913], [`Hybrid Strings`],
    [914], [`Wonderous Spaces`],
    [915], [`Metal Orch Pad`],
    [916], [`Toon prs`],
    [917], [`Tranquil Sea`],
    [918], [`Sick Clock Jam`],
    [919], [`Orc Split`],
    [920], [`Baroque Brass`],
    [921], [`Unison Orchestra`],
    [922], [`Unison w/Pizz`],
    [923], [`Switch Orchestra`],
    [924], [`Pizz/Str/Winds`],
    [925], [`Harp Arps Cmaj`],
    [926], [`Desert Bloom E1`],
    [927], [`Exotic Charge`],
    [928], [`ET Comes Home`],
    [929], [`Fanfare Orch`],
    [930], [`Switch Orch 2`],
    [931], [`Orbiting Venus`],
    [932], [`Glass Dulcimer`],
    [933], [`Hybrid Reeds`],
    [934], [`Two Hand Pizz`],
    [935], [`Slo Str & Horn`],
    [936], [`Pianist Band`],
    [937], [`Prepared Pianos`],
    [938], [`FSW1 solo winds`],
    [939], [`Strings&Winds`],
    [940], [`Str Ens Solo MW`],
    [941], [`Pno&Vox&Pizz`],
    [942], [`Down Wind SmRbn`],
    [943], [`Guitar & Piano`],
    [944], [`Cirrus 9`],
    [945], [`Dry Plucks`],
    [946], [`String Collage`],
    [947], [`Esoterica`],
    [948], [`Poseidon`],
    [949], [`Stalkers`],
    [950], [`Diabolic Trickle`],
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

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [1], [`RoomChorDly Hall`],
    [2], [`RmChorChRv Hall`],
    [3], [`RoomChorCDR Hall`],
    [4], [`RoomChor    Hall`],
    [5], [`RoomChrCh4T Hall`],
    [6], [`RoomFlngCDR Hall`],
    [7], [`RoomFlgEcho Hall`],
    [8], [`RmFlngStImg Garg`],
    [9], [`RmFlgChDly Room`],
    [10], [`ChmbFlgGtRv Hall`],
    [11], [`RoomFlngCDR Hall`],
    [12], [`RoomFlngLsr Echo`],
    [13], [`RmFlgFXFlng Flng`],
    [14], [`SpaceFlng   Hall`],
    [15], [`ChmbFlngCDR Verb`],
    [16], [`RoomPhsrCDR Hall`],
    [17], [`RmPhsrQuFlg Hall`],
    [18], [`RoomPhsr   Space`],
    [19], [`RmEQmphEcho Comp`],
    [20], [`RmEQmphEcho Hall`],
    [21], [`RmEQmph4Tp  Room`],
    [22], [`RmEQmph4Tap Hall`],
    [23], [`RmSweepEcho Hall`],
    [24], [`RoomResEcho Hall`],
    [25], [`RmRotoFl4T CmpRv`],
    [26], [`RoomSRSCDR  Hall`],
    [27], [`RoomSRSRoom Room`],
    [28], [`RoomSRSChDl Hall`],
    [29], [`RoomSRSCDR   CDR`],
    [30], [`RmStImgChDl Hall`],
    [31], [`RoomSRSRoom Chmb`],
    [32], [`RoomSRSRoom Hall`],
    [33], [`ChmbCompCDR Hall`],
    [34], [`RoomCmpChor Hall`],
    [35], [`RoomComp    Hall`],
    [36], [`RoomComp    Hall`],
    [37], [`BthComp SRS Hall`],
    [38], [`RoomCmpCh4T Hall`],
    [39], [`RmDsRotFl4t RvCm`],
    [40], [`RoomRmHall  Hall`],
    [41], [`Room Room   SRS2`],
    [42], [`RoomRmHall  Hall`],
    [43], [`Room   Room Hall`],
    [44], [`Room   Hall Hall`],
    [45], [`Room  Room Hall2`],
    [46], [`Room Room  Hall3`],
    [47], [`Room  Room Hall4`],
    [48], [`Room  Hall Hall2`],
    [49], [`Sndbrd Room Hall`],
    [50], [`Sndbrd Rm  Hall2`],
    [51], [`Room  Room Hall3`],
    [52], [`auxChrMDly  Room`],
    [53], [`auxFlngChRv Room`],
    [54], [`auxShp4MDly Hall`],
    [55], [`auxDistLasr Room`],
    [56], [`auxEnhSp4T Class`],
    [57], [`auxDistLasr Acid`],
    [58], [`EnhcManPhs  Room`],
    [59], [`EnhcFlg8Tap Room`],
    [60], [`EnhcCmpFlng Room`],
    [61], [`CompEQmphCh Room`],
    [62], [`BthQFlg4Tap Hall`],
    [63], [`ChmbTremCDR Room`],
    [64], [`ChmbCmpFlRv Hall`],
    [65], [`ChamDstEcho Room`],
    [66], [`ChamFlg4Tap Hall`],
    [67], [`ChmbEnv4Tap GtRv`],
    [68], [`CmbrShapLsr Hall`],
    [69], [`auxPtchDst+ Chmb`],
    [70], [`auxChorFlRv Cmbr`],
    [71], [`auxChorFlRv Cmb2`],
    [72], [`auxChorFlRv Cmb3`],
    [73], [`auxChorFlRv Cmb4`],
    [74], [`HallFlgChDl Room`],
    [75], [`HallPtchLsr Hall`],
    [76], [`HallGateFl4T Bth`],
    [77], [`HallChorFDR Room`],
    [78], [`HallPtchPtFl Lsr`],
    [79], [`HallFlng8Tp Room`],
    [80], [`HallChrEcho Room`],
    [81], [`HallChorCDR Hall`],
    [82], [`HallRsFltChDl Rm`],
    [83], [`Hall  ChDly Hall`],
    [84], [`HallFlgChDl Hall`],
    [85], [`Hall    Room SRS`],
    [86], [`Hall   Room Room`],
    [87], [`Hall      CmpRvb`],
    [88], [`HallFlng    Hall`],
    [89], [`HallRoomChr Hall`],
    [90], [`auxPhsrFDR  Hall`],
    [91], [`auxChrDist+ Hall`],
    [92], [`auxFlgDist+ Hall`],
    [93], [`auxChrDist+ Hall`],
    [94], [`auxChorMDly Hall`],
    [95], [`auxChorSp6T Hall`],
    [96], [`auxChorChDl Hall`],
    [97], [`auxPhasStIm Hall`],
    [98], [`auxFlngCDR  Hall`],
    [99], [`auxPhsFlDbl Hall`],
    [100], [`auxSRSRoom  Hall`],
    [101], [`auxFlngLasr Hall`],
    [102], [`auxEnh4Tap  Hall`],
    [103], [`EnhcChorCDR Hall`],
    [104], [`EnhChorChDl Hall`],
    [105], [`EnhcChor   Plate`],
    [106], [`CompFlgChor Hall`],
    [107], [`ChorChorFlg Hall`],
    [108], [`ChapelSRS   Hall`],
    [109], [`ChapelSRS  Hall2`],
    [110], [`Chapel Room Hall`],
    [111], [`PltEnvFl4T  Room`],
    [112], [`PlatEnvFl4T Filt`],
    [113], [`PltEnvFl4T Plate`],
    [114], [`PltTEnvFlg Plate`],
    [115], [`PlateRngMd  Hall`],
    [116], [`auxDist+Echo Plt`],
    [117], [`auxEnvSp4T Plate`],
    [118], [`auxShap4MD Plate`],
    [119], [`auxChorDist+ Plt`],
    [120], [`auxShFlgChDl Plt`],
    [121], [`auxMPFlgLasr Plt`],
    [122], [`auxShap4MD Plate`],
    [123], [`FlgEnv4Tap Plate`],
    [124], [`EnhrFlgCDR Plate`],
    [125], [`auxRingPFD Plate`],
    [126], [`GtRvShapMDl Room`],
    [127], [`GtdEnhcStIm Room`],
    [128], [`Gtd2ChrEcho 2Vrb`],
    [129], [`GtdEnhcStIm Hall`],
    [130], [`auxEnvSp4T GtVrb`],
    [131], [`GtRbSwpFlt  Lasr`],
    [132], [`GtRbSwpFlt FlDly`],
    [133], [`ChRvStIEcho Hall`],
    [134], [`ChorChorCDR Spac`],
    [135], [`ChDlDstEQ   Hall`],
    [136], [`auxDPanCDR ChPlt`],
    [137], [`AuxChorFlng  CDR`],
    [138], [`auxEnhcSp4T  CDR`],
    [139], [`auxPtchDst+ ChRv`],
    [140], [`EnhcChorChDl PCD`],
    [141], [`auxPoly      FDR`],
    [142], [`EnhcChorChDl FDR`],
    [143], [`EnhcChrChDl FDR2`],
    [144], [`auxRotoSp4T FlRv`],
    [145], [`auxRotaryFDR Plt`],
    [146], [`RotoOrgFX   Hall`],
    [147], [`CmpRvbFlDl  Hall`],
    [148], [`auxEnhSp4T  RvCm`],
    [149], [`auxPtchRoom RvCm`],
    [150], [`PhsrChorCDR Phsr`],
    [151], [`ChDlSp4TFlDl Phs`],
    [152], [`auxFlgDst+ ChLsD`],
    [153], [`auxFlgDst+ ChLs2`],
    [154], [`RoomRoomSRS CmRv`],
    [155], [`RoomRoom    Room`],
    [156], [`GtRvPlate   Hall`],
    [157], [`RoomRoom     SRS`],
    [158], [`EnhcSp4T    Hall`],
    [159], [`Room RoomChr SRS`],
    [160], [`KB3 V/C ->Rotary`],
    [161], [`EQStImg  5BndEQ`],
    [162], [`aux5BeqStIm Hall`],
    [163], [`10Band StIm Hall`],
    [164], [`3BndCmp     PtFl`],
    [165], [`auxDst+Lsr   Plt`],
    [166], [`auxRsFltEnvSwFlt`],
    [167], [`auxChorEnvF Hall`],
    [168], [`auxChRvEncr Chor`],
    [169], [`auxChrDstEQ Room`],
    [170], [`aux ChDlSRS Hall`],
    [171], [`aux EQFlng DstEQ`],
    [172], [`auxFlngPhsr Lasr`],
    [173], [`auxFlShQFlg Hall`],
    [174], [`auxFlgDist+ Room`],
    [175], [`aux GtVbFl4T Bth`],
    [176], [`auxRvRvQFlg Hall`],
    [177], [`auxRvRbShapeChmb`],
    [178], [`auxSpinMDly Room`],
    [179], [`aux SweepEchoBth`],
    [180], [`auxRoto&DsFDRPlt`],
    [181], [`auxRot&Ds2FDRPlt`],
    [182], [`auxFlgChDl Hall`],
    [183], [`auxDstLsr  CDR`],
    [184], [`CPDlEnFltCmpGtRv`],
    [185], [`RotoOrgFX2  Hall`],
    [186], [`ChDlFlPtLzVb Plt`],
    [187], [`CDFlDlyPhRm Hall`],
    [188], [`CDR FlgRvb  Hall`],
    [189], [`DstPhsPnLzVb CDR`],
    [190], [`DistRoom  GrphEQ`],
    [191], [`Enh  Ch 4T Hall`],
    [192], [`FiltCmpExpFl CDR`],
    [193], [`LzVbFlDstEQ Room`],
    [194], [`PhseDist    Room`],
    [195], [`ChDlyRvFlRv Hall`],
    [196], [`RmRotr&DstChrPlt`],
    [197], [`Clear Studio`],
    [198], [`Pre-KDFX Studio`],
    [199], [`Default Studio`],
    [700], [`Flanger Trio`],
    [701], [`Thin Cloud Layer`],
    [702], [`Fazetortion`],
    [703], [`Ultraphasulate`],
    [704], [`Chutney Squishy`],
    [705], [`BuddMeetsTomita`],
    [706], [`2 Buses 2 Chains`],
    [707], [`RingMod Envelope`],
    [708], [`Drum Megashaper`],
    [709], [`Dist Lead Guitar`],
    [710], [`Blues/R&R Guitar`],
    [711], [`Clean Rhythm Gtr`],
    [712], [`Sweeping Verb`],
    [713], [`Pad Ambience`],
    [714], [`Coming of Dawn`],
    [715], [`Wide Raindrops`],
    [716], [`Robot Voice`],
    [717], [`Before the Crash`],
    [718], [`Piano Multi-Verb`],
    [719], [`Mastering Studio`],
    [800], [`Sweet Hall`],
    [801], [`Small Hall`],
    [802], [`Medium Hall`],
    [803], [`Large Hall`],
    [804], [`Big Gym`],
    [805], [`Bright Plate 1`],
    [806], [`Opera House`],
    [807], [`Live Chamber`],
    [808], [`Bathroom`],
    [809], [`Med Large Room`],
    [810], [`Real Room`],
    [811], [`Drum Room`],
    [812], [`Small Dark Room`],
    [813], [`Small Closet`],
    [814], [`Add Ambience`],
    [815], [`Gated Reverb`],
    [816], [`Reverse Reverb`],
    [817], [`Non-Linear`],
    [818], [`Slapverb`],
    [819], [`Full Bass`],
    [820], [`Room + Delay`],
    [821], [`Delay Big Hall`],
    [822], [`Chorus Room`],
    [823], [`Chorus Smallhall`],
    [824], [`Chorus Med Hall`],
    [825], [`Chorus Big Hall`],
    [826], [`Chor-Delay Room`],
    [827], [`Chor-Delay Hall`],
    [828], [`Flange-Dly Room`],
    [829], [`Flange-Dly Hall`],
    [830], [`Stereo Chorus`],
    [831], [`Stereo Flanger`],
    [832], [`Stereo Delay`],
    [833], [`4-Tap Delay`],
    [834], [`Chorus Delay`],
    [835], [`Flange Delay`],
    [836], [`Chorus 4-Tap`],
    [837], [`Flange 4 Tap`],
    [838], [`Chorus Echo`],
    [839], [`Chorus Echoverb`],
    [840], [`Fast Flange`],
    [841], [`Wash`],
    [842], [`Into The Abyss`],
    [843], [`Space Flanger`],
    [844], [`Flange Room`],
    [845], [`Predelay Hall`],
    [846], [`Flange Echo`],
    [847], [`Rotary Club`],
    [848], [`Rotary Hall`],
    [849], [`Chorus`],
    [850], [`Soundbrd/rvb`],
    [851], [`Percussive Room`],
    [852], [`Brt Empty Room`],
    [853], [`Mosque Room`],
    [854], [`New Gated`],
    [855], [`Chorus Slap Room`],
    [856], [`Chorus Bass Room`],
    [857], [`New Chorus Hall`],
    [858], [`Spacious`],
    [859], [`Wash Lead`],
    [860], [`New Hall w/Delay`],
    [861], [`Rich Delay`],
    [862], [`Glass Delay`],
    [863], [`Real Plate`],
    [864], [`Real Niceverb`],
    [865], [`ClassicalChamber`],
    [866], [`Empty Stage`],
    [867], [`Long & Narrow`],
    [868], [`Far Bloom`],
    [869], [`Floyd Hall`],
    [870], [`With A Mic`],
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

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [1], [`NiceLittleBooth`],
    [2], [`Small Wood Booth`],
    [3], [`Natural Room`],
    [4], [`PrettySmallPlace`],
    [5], [`Sun Room`],
    [6], [`Soundboard`],
    [7], [`Add More Air`],
    [8], [`Standard Booth`],
    [9], [`A Distance Away`],
    [10], [`Live Place`],
    [11], [`Viewing Booth`],
    [15], [`BrightSmallRoom`],
    [16], [`Bassy Room`],
    [17], [`Percussive Room`],
    [18], [`SmallStudioRoom`],
    [19], [`ClassRoom`],
    [20], [`Utility Room`],
    [21], [`Thick Room`],
    [22], [`The Real Room`],
    [23], [`Sizzly Drum Room`],
    [24], [`Real Big Room`],
    [25], [`The Comfy Club`],
    [26], [`Spitty Drum Room`],
    [27], [`Stall One`],
    [28], [`Green Room`],
    [29], [`Tabla Room`],
    [30], [`Large Room`],
    [31], [`Platey Room`],
    [40], [`SmallDrumChamber`],
    [41], [`Brass Chamber`],
    [42], [`Sax Chamber`],
    [43], [`Plebe Chamber`],
    [44], [`In The Studio`],
    [45], [`My Garage`],
    [46], [`School Stairwell`],
    [47], [`JudgeJudyChamber`],
    [48], [`Bloom Chamber`],
    [55], [`Grandiose Hall`],
    [56], [`Elegant Hall`],
    [57], [`Bright Hall`],
    [58], [`Ballroom`],
    [59], [`Spacious Hall`],
    [60], [`Classic Chapel`],
    [61], [`Semisweet Hall`],
    [62], [`Pipes Hall`],
    [63], [`Reflective Hall`],
    [64], [`Smoooth Hall`],
    [65], [`Splendid Palace`],
    [66], [`Pad Space`],
    [67], [`Bob'sDiffuseHall`],
    [68], [`Abbey Piano Hall`],
    [69], [`Short Hall`],
    [70], [`The Long Haul`],
    [71], [`Predelay Hall`],
    [72], [`Sweeter Hall`],
    [73], [`The Piano Hall`],
    [74], [`Bloom Hall`],
    [75], [`Recital Hall`],
    [76], [`Generic Hall`],
    [77], [`Burst Space`],
    [78], [`Real Dense Hall`],
    [79], [`Concert Hall`],
    [80], [`Standing Ovation`],
    [81], [`Flinty Hall`],
    [82], [`HighSchool Gym`],
    [83], [`My Dreamy 481!!`],
    [84], [`Deep Hall`],
    [85], [`Immense Mosque`],
    [86], [`Dreamverb`],
    [87], [`Huge Batcave`],
    [95], [`Classic Plate`],
    [96], [`Weighty Platey`],
    [97], [`Medm Warm Plate`],
    [98], [`Bloom Plate`],
    [99], [`Clean Plate`],
  ),
  caption: [KDFX_PRESET objects in bank 0],
)

#block(sticky: true)[DIR_BANK to query KDFX_PRESET type 141/\$010D in bank 1:]
#nonum[
```
F0 07 00 78 0C 01 0D 01 00 F7
```
]

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [100], [`Plate Mail`],
    [101], [`RealSmoothPlate`],
    [102], [`Huge Tight Plate`],
    [103], [`BigPredelayPlate`],
    [104], [`Cool Dark Place`],
    [105], [`Gunshot Verb`],
    [106], [`Rvrb Compression`],
    [107], [`Snappy Drum Room`],
    [108], [`Roomitizer`],
    [109], [`Live To Tape`],
    [110], [`LrgRm`],
    [111], [`Hall`],
    [112], [`Gated Reverb`],
    [113], [`Gate Plate`],
    [114], [`Exponent Booth`],
    [115], [`Drum Latch1`],
    [116], [`Drum Latch2`],
    [117], [`Diffuse Gate`],
    [118], [`Acid Trip Room`],
    [119], [`Furbelows`],
    [120], [`Festoons`],
    [121], [`Reverse Reverb`],
    [122], [`Reverse Reverb 2`],
    [123], [`Rvrs Laserverb`],
    [124], [`Growler`],
    [125], [`Ringy Drum Plate`],
    [126], [`Oil Tank`],
    [127], [`Wobbly Plate`],
    [128], [`Pitcher Hall`],
    [129], [`DistantPitchRoom`],
    [130], [`Guitar Echo`],
    [131], [`Stereo Echoes1`],
    [132], [`Stereo Echoes2`],
    [133], [`4-Tap Delay`],
    [134], [`OffbeatFlamDelay`],
    [135], [`8-Tap Delay`],
    [136], [`Spectral 4-Tap`],
    [137], [`Astral Taps`],
    [138], [`SpectraShapeTaps`],
    [139], [`Fanfare In Gmaj`],
    [140], [`Basic Delay 1/8`],
    [141], [`Diffuse Slaps`],
    [142], [`Multitaps ms`],
    [143], [`Timbre Taps`],
    [144], [`Ecko Plecks`],
    [145], [`Degenerator`],
    [146], [`Nanobot Feedback`],
    [147], [`Takes a while...`],
    [148], [`Wait for UFO`],
    [149], [`News Update`],
    [150], [`Basic Chorus`],
    [151], [`Chorus Comeback`],
    [152], [`Chorusier`],
    [153], [`Ordinary Chorus`],
    [154], [`SlowSpinChorus`],
    [155], [`Chorus Morris`],
    [156], [`Everyday Chorus`],
    [157], [`Thick Chorus`],
    [158], [`Soft Chorus`],
    [159], [`Rock Chorus`],
    [160], [`Sm Stereo Chorus`],
    [161], [`Lg Stereo Chorus`],
    [162], [`Full Chorus`],
    [163], [`Dense Gtr Chorus`],
    [164], [`Standrd Gtr Chor`],
    [165], [`Bass Chorus`],
    [166], [`StChorus+Delay`],
    [167], [`StChor+3vs2Delay`],
    [168], [`CDR for Lead Gtr`],
    [169], [`PinchChorusDelay`],
    [170], [`Big Slow Flange`],
    [171], [`Wetlip Flange`],
    [172], [`Sweet Flange`],
    [173], [`Throaty Flange`],
    [174], [`Delirium Tremens`],
    [175], [`Flanger Double`],
    [176], [`Squeeze Flange`],
    [177], [`Simply Flange`],
    [178], [`Analog Flanger`],
    [179], [`Soft Edge Flange`],
    [180], [`Ned Flangers`],
    [181], [`Wispy Flange`],
    [182], [`Crystal Flange`],
    [183], [`NarrowResFlange`],
    [184], [`TightSlapFlange`],
    [185], [`Flanged Taps`],
    [186], [`StFlange+Delay`],
    [187], [`StFlng+3vs2Delay`],
    [188], [`Singing Flanger`],
    [189], [`DampedEchoFlange`],
    [190], [`Circles`],
    [191], [`Slow Deep Phaser`],
    [192], [`Manual Phaser`],
    [193], [`Vibrato Phaser`],
    [194], [`ThunderPhaser`],
    [195], [`Saucepan Phaser`],
    [196], [`Static Phaser`],
    [197], [`Slippery Slope`],
    [198], [`Westward Waves`],
    [199], [`No Effect`],
  ),
  caption: [KDFX_PRESET objects in bank 1],
)

#block(sticky: true)[DIR_BANK to query KDFX_PRESET type 141/\$010D in bank 7:]
#nonum[
```
F0 07 00 78 0C 01 0D 07 00 F7
```
]

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [700], [`Chorus Delay`],
    [701], [`Chorus PanDelay`],
    [702], [`Doubler & Echo`],
    [703], [`Chorus VryLngDly`],
    [704], [`FastChorusDouble`],
    [705], [`BasicChorusDelay`],
    [706], [`MultiTap Chorus`],
    [707], [`ThickChorus no4T`],
    [708], [`Chorused Taps`],
    [709], [`Chorus Slapbacks`],
    [710], [`MultiEchoChorus`],
    [711], [`ChorusDelayHall`],
    [712], [`ChorDlyRvb Lead`],
    [713], [`ChorDlyRvb Lead2`],
    [714], [`Fluid ChorDlyRvb`],
    [715], [`ChorLite DlyHall`],
    [716], [`ChorusSmallRoom`],
    [717], [`DeepChorDlyHall`],
    [718], [`Chorus PercHall`],
    [719], [`Chorus Booth`],
    [720], [`ClassicEP ChorRm`],
    [721], [`ChorusMedChamber`],
    [722], [`Vanilla ChorRvb`],
    [723], [`Chorus Slow Hall`],
    [724], [`SoftChorus Hall`],
    [725], [`ChorBigBrtPlate`],
    [726], [`Chorus Air`],
    [727], [`Chorus HiCeiling`],
    [728], [`Chorus MiniHall`],
    [729], [`CathedralChorus`],
    [730], [`PsiloChorusHall`],
    [731], [`GuitarChorLsrDly`],
    [732], [`Flange + Delay`],
    [733], [`ThroatyFlangeDly`],
    [734], [`Flange + 4Tap`],
    [735], [`Bap ba-da-dap`],
    [736], [`Slapback Flange`],
    [737], [`Quantize+Flange`],
    [738], [`FlangeDelayHall`],
    [739], [`FlangeDelayRoom`],
    [740], [`SloFlangeDlyRoom`],
    [741], [`FlangeDlyBigHall`],
    [742], [`Flange Theatre`],
    [743], [`FlangeVerb Clav`],
    [744], [`FlangeVerb Gtr`],
    [745], [`Flange Hall`],
    [746], [`Flange Booth`],
    [747], [`Flange->LaserDly`],
    [748], [`FlangeTap Synth`],
    [749], [`Lazertag Flange`],
    [750], [`Flange->Pitcher`],
    [751], [`Flange->Shaper`],
    [752], [`Shaper->Flange`],
    [753], [`Warped Echoes`],
    [754], [`Delay`],
    [755], [`StereoFlamDelay`],
    [756], [`2Dlys Ch Fl Mono`],
    [757], [`LaserDelay->Rvb`],
    [758], [`Shaper->Reverb`],
    [759], [`MnPitcher+Chorus`],
    [760], [`MnPitcher+Flange`],
    [761], [`Pitcher+Chor+Dly`],
    [762], [`Pitcher+Flng+Dly`],
    [763], [`SubtleDistortion`],
    [764], [`Synth Distortion`],
    [765], [`Dist Cab EPiano`],
    [766], [`Distortion+EQ`],
    [767], [`Burnt Transistor`],
    [768], [`TubeAmp DlyChor`],
    [769], [`TubeAmp DlyChor2`],
    [770], [`TubeAmp DlyFlnge`],
    [771], [`TubeAmp Flange`],
    [772], [`PolyAmp Chorus`],
    [773], [`PolyAmp DlyFlnge`],
    [774], [`VibrChor Rotors`],
    [775], [`SlightDistRotors`],
    [776], [`Rotostort`],
    [777], [`VibrChor Rotors2`],
    [778], [`Full VbCh Rotors`],
    [779], [`KB3 FXBus`],
    [780], [`KB3 AuxFX`],
    [781], [`Pitch Spinner`],
    [782], [`VibrChrDstRotor1`],
    [783], [`VibrChrDstRotor2`],
    [784], [`VibChrDstRotor3`],
    [785], [`FullVbChTubeRotr`],
    [786], [`ChorDlyHall 2`],
    [787], [`Flange Hall 2`],
    [788], [`SpeeChorusDeep`],
    [789], [`Fluid Wash`],
    [790], [`VC+DistRotor`],
    [791], [`Slow Res Rotor`],
    [792], [`Smooth Rotors`],
    [793], [`Very Nazty Rotor`],
    [794], [`80's Funk Guitar`],
    [795], [`Mean 70'sFunkGtr`],
    [796], [`Crunch Guitar`],
    [797], [`Classic Gtr Dist`],
    [798], [`SaturatedGtrDist`],
    [799], [`TubeDist DlyChor`],
  ),
  caption: [KDFX_PRESET objects in bank 7],
)

#block(sticky: true)[DIR_BANK to query KDFX_PRESET type 141/\$010D in bank 8:]
#nonum[
```
F0 07 00 78 0C 01 0D 08 00 F7
```
]

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [800], [`Sweet Hall`],
    [801], [`Small Hall`],
    [802], [`Medium Hall`],
    [803], [`Large Hall`],
    [804], [`Big Gym`],
    [805], [`Bright Plate 1`],
    [806], [`Opera House`],
    [807], [`Live Chamber`],
    [808], [`Bathroom`],
    [809], [`Med Large Room`],
    [810], [`Real Room`],
    [811], [`Drum Room`],
    [812], [`Small Dark Room`],
    [813], [`Small Closet`],
    [814], [`Add Ambience`],
    [815], [`Gated Reverb`],
    [816], [`Reverse Reverb`],
    [817], [`Non-Linear`],
    [818], [`Slapverb`],
    [819], [`Full Bass`],
    [820], [`Room + Delay`],
    [821], [`Delay Big Hall`],
    [822], [`Chorus Room`],
    [823], [`Chorus Smallhall`],
    [824], [`Chorus Med Hall`],
    [825], [`Chorus Big Hall`],
    [826], [`Chor-Delay Room`],
    [827], [`Chor-Delay Hall`],
    [828], [`Flange-Dly Room`],
    [829], [`Flange-Dly Hall`],
    [830], [`Stereo Chorus`],
    [831], [`Stereo Flanger`],
    [832], [`Stereo Delay`],
    [833], [`4-Tap Delay`],
    [834], [`Chorus Delay`],
    [835], [`Flange Delay`],
    [836], [`Chorus 4-Tap`],
    [837], [`Flange 4 Tap`],
    [838], [`Chorus Echo`],
    [839], [`Chorus Echoverb`],
    [840], [`Fast Flange`],
    [841], [`Wash`],
    [842], [`Into The Abyss`],
    [843], [`Space Flanger`],
    [844], [`Flange Room`],
    [845], [`Predelay Hall`],
    [846], [`Flange Echo`],
    [847], [`Rotary Club`],
    [848], [`Rotary Hall`],
    [849], [`Chorus`],
    [850], [`Soundbrd/rvb`],
    [851], [`Percussive Room`],
    [852], [`Brt Empty Room`],
    [853], [`Mosque Room`],
    [854], [`New Gated`],
    [855], [`Chorus Slap Room`],
    [856], [`Chorus Bass Room`],
    [857], [`New Chorus Hall`],
    [858], [`Spacious`],
    [859], [`Wash Lead`],
    [860], [`New Hall w/Delay`],
    [861], [`Rich Delay`],
    [862], [`Glass Delay`],
    [863], [`Real Plate`],
    [864], [`Real Niceverb`],
    [865], [`ClassicalChamber`],
    [866], [`Empty Stage`],
    [867], [`Long & Narrow`],
    [868], [`Far Bloom`],
    [869], [`Floyd Hall`],
    [870], [`With A Mic`],
  ),
  caption: [KDFX_PRESET objects in bank 8],
)

#block(sticky: true)[DIR_BANK to query KDFX_PRESET type 141/\$010D in bank 9:]
#nonum[
```
F0 07 00 78 0C 01 0D 09 00 F7
```
]

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [900], [`Basic Env Filter`],
    [901], [`Phunk Env Filter`],
    [902], [`Synth Env Filter`],
    [903], [`Bass Env Filter`],
    [904], [`EPno Env Filter`],
    [905], [`Trig Env Filter`],
    [906], [`LFO Sweep Filter`],
    [907], [`DoubleRiseFilter`],
    [908], [`Circle Bandsweep`],
    [909], [`Resonant Filter`],
    [910], [`Dual Res Filter`],
    [911], [`EQ Morpher`],
    [912], [`Mono EQ Morpher`],
    [913], [`Ring Modulator`],
    [914], [`PitcherA`],
    [915], [`PitcherB`],
    [916], [`SuperShaper`],
    [917], [`SubtleDrumShape`],
    [918], [`3 Band Shaper`],
    [919], [`LaserVerb`],
    [920], [`Laserwaves`],
    [921], [`Crystallizer`],
    [922], [`Spry Young Boy`],
    [923], [`Cheap LaserVerb`],
    [924], [`Drum Neurezonate`],
    [925], [`LazerfazerEchoes`],
    [926], [`Simple Lazerverb`],
    [927], [`TripFilter`],
    [928], [`Gated Laserverb`],
    [929], [`Waterford`],
    [930], [`A little dirty`],
    [931], [`Slight Overload`],
    [932], [`Blown Speaker`],
    [933], [`Ring Linger`],
    [934], [`Drum Shaper`],
    [935], [`Aliaser`],
    [936], [`Quantize+Alias1`],
    [937], [`Quantize+Alias2`],
    [938], [`Drum Mortar`],
    [939], [`Superphasulate`],
    [940], [`Rich Noodle`],
    [941], [`Nickel Chorus`],
    [942], [`HF Stimulator`],
    [943], [`OddHarmSuppress`],
    [944], [`AM Radio`],
    [945], [`U-Shaped EQ`],
    [946], [`Drum Crusher`],
    [947], [`Vocal Room`],
    [948], [`Vocal Stage`],
    [949], [`Mid Compressor`],
    [950], [`?`],
    [951], [`?`],
    [952], [`?`],
    [953], [`?`],
    [954], [`?`],
    [955], [`Compress w/SC EQ`],
    [956], [`Compress/Expand`],
    [957], [`Comprs/Expnd +EQ`],
    [958], [`Reverb>Compress`],
    [959], [`Reverb>Compress2`],
    [960], [`Drum Comprs>Rvb`],
    [961], [`Expander`],
    [962], [`3Band Compressor`],
    [963], [`Simple Gate`],
    [964], [`Gate w/ SC EQ`],
    [965], [`Graphic EQ`],
    [966], [`5 Band EQ`],
    [967], [`ContourGraphicEQ`],
    [968], [`Dance GraphicEQ`],
    [969], [`OldPianoEnhancer`],
    [970], [`3 Band Enhancer`],
    [971], [`3 Band Enhancer2`],
    [972], [`Extreem Enhancer`],
    [973], [`Tremolo`],
    [974], [`Dual Panner`],
    [975], [`SRS`],
    [976], [`Widespread`],
    [977], [`Mono->Stereo`],
    [978], [`3 Band Compress`],
    [979], [`Simple Panner`],
    [980], [`Big Bass EQ`],
    [981], [`Poly Pitcher`],
    [982], [`CheapVoxChanger`],
    [983], [`Hip Hop Aura`],
    [984], [`Woodenize`],
    [985], [`Marimbafication`],
    [986], [`Frequency Offset`],
    [987], [`Drum Loosener`],
    [988], [`Drum Tightener`],
    [989], [`Vox Honker`],
    [990], [`Glacial Canyon`],
    [991], [`Spring Thing`],
    [992], [`Contact`],
    [993], [`Drum Frightener`],
    [994], [`Mad Hatter`],
    [995], [`Fallout`],
    [996], [`Ascension`],
    [997], [`60Hz Buzz Kill`],
    [998], [`Stereo Analyze`],
    [999], [`FX Mod Diag`],
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

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [0], [`SumOnly`],
    [1], [`ClearBase`],
    [2], [`FlatBase`],
    [3], [`MinimumBase`],
    [4], [`MediumBase`],
    [5], [`MaximumBase`],
    [11], [`Clear18`],
    [12], [`Flat18`],
    [13], [`Minimum18`],
    [14], [`Medium18`],
    [15], [`Maximum18`],
    [21], [`Clear16`],
    [22], [`Flat16`],
    [23], [`Minimum16`],
    [24], [`Medium16`],
    [25], [`Maximum16`],
  ),
  caption: [DITHER objects in bank 0],
)

#block(sticky: true)[DIR_BANK to query DITHER type 142/\$010E in bank 1:]
#nonum[
```
F0 07 00 78 0C 01 0E 01 00 F7
```
]

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [100], [`PatchDither16`],
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

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [1], [`MiniVerb`],
    [2], [`Dual MiniVerb`],
    [3], [`Gated MiniVerb`],
    [4], [`Classic Place`],
    [5], [`Classic Verb`],
    [6], [`TQ Place`],
    [7], [`TQ Verb`],
    [8], [`Diffuse Place`],
    [9], [`Diffuse Verb`],
    [10], [`OmniPlace`],
    [11], [`OmniVerb`],
    [12], [`Panaural Room`],
    [13], [`Stereo Hall`],
    [14], [`Grand Plate`],
    [15], [`Finite Verb`],
  ),
  caption: [KDFX_ALG objects in bank 0],
)

#block(sticky: true)[DIR_BANK to query KDFX_ALG type 143/\$010F in bank 1:]
#nonum[
```
F0 07 00 78 0C 01 0F 01 00 F7
```
]

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [130], [`Complex Echo`],
    [131], [`4-Tap Delay`],
    [132], [`4-Tap Delay BPM`],
    [133], [`8-Tap Delay`],
    [134], [`8-Tap Delay BPM`],
    [135], [`Spectral 4-Tap`],
    [136], [`Spectral 6-Tap`],
    [138], [`Degen Regen BPM`],
    [139], [`Switch Loops`],
    [140], [`Moving Delay`],
    [150], [`Chorus 1`],
    [151], [`Chorus 2`],
    [152], [`Dual Chorus 1`],
    [153], [`Dual Chorus 2`],
    [154], [`Flanger 1`],
    [155], [`Flanger 2`],
    [156], [`LFO Phaser`],
    [157], [`LFO Phaser Twin`],
    [158], [`Manual Phaser`],
    [159], [`VibratoPhaser`],
    [160], [`SingleLFO Phaser`],
    [161], [`Allpass Phaser 3`],
  ),
  caption: [KDFX_ALG objects in bank 1],
)

#block(sticky: true)[DIR_BANK to query KDFX_ALG type 143/\$010F in bank 7:]
#nonum[
```
F0 07 00 78 0C 01 0F 07 00 F7
```
]

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [700], [`Chorus+Delay`],
    [701], [`Chorus+4Tap`],
    [702], [`Chorus<>4Tap`],
    [703], [`Chor+Dly+Reverb`],
    [704], [`Chorus<>Reverb`],
    [705], [`Chorus<>LasrDly`],
    [706], [`Flange+Delay`],
    [707], [`Flange+4Tap`],
    [708], [`Flange<>4Tap`],
    [709], [`Flan+Dly+Reverb`],
    [710], [`Flange<>Reverb`],
    [711], [`Flange<>LasrDly`],
    [712], [`Flange<>Pitcher`],
    [713], [`Flange<>Shaper`],
    [714], [`Quantize+Flange`],
    [715], [`Dual MovDelay`],
    [716], [`Quad MovDelay`],
    [717], [`LasrDly<>Reverb`],
    [718], [`Shaper<>Reverb`],
    [719], [`Reverb<>Compress`],
    [720], [`MonoPitcher+Chor`],
    [721], [`MonoPitcher+Flan`],
    [722], [`Pitcher+Chor+Dly`],
    [723], [`Pitcher+Flan+Dly`],
    [724], [`Mono Distortion`],
    [725], [`MonoDistort+Cab`],
    [726], [`MonoDistort + EQ`],
    [727], [`PolyDistort + EQ`],
    [728], [`StereoDistort+EQ`],
    [729], [`TubeAmp<>MD>Chor`],
    [730], [`TubeAmp<>MD>Flan`],
    [731], [`PolyAmp<>MD>Chor`],
    [732], [`PolyAmp<>MD>Flan`],
    [733], [`VibChor+Rotor 2`],
    [734], [`Distort + Rotary`],
    [735], [`KB3 FXBus`],
    [736], [`KB3 AuxFX`],
    [737], [`VibChor+Rotor 4`],
    [738], [`VC+Dist+1Rotor 2`],
    [739], [`VC+Dist+HiLoRotr`],
    [740], [`VC+Tube+Rotor 4`],
    [741], [`Rotor 1`],
    [742], [`VC+Dist+HiLoRot2`],
    [743], [`Subtle Distort`],
    [744], [`Quantize+Alias`],
    [745], [`Pitcher+Miniverb`],
    [746], [`Reverb+Compress`],
    [781], [`St Chorus+Delay`],
    [784], [`St Flange+Delay`],
    [790], [`Gate+Cmp[EQ]+Vrb`],
    [792], [`Gate+TubeAmp`],
  ),
  caption: [KDFX_ALG objects in bank 7],
)

#block(sticky: true)[DIR_BANK to query KDFX_ALG type 143/\$010F in bank 9:]
#nonum[
```
F0 07 00 78 0C 01 0F 09 00 F7
```
]

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [900], [`Env Follow Filt`],
    [901], [`TrigEnvelopeFilt`],
    [902], [`LFO Sweep Filter`],
    [903], [`Resonant Filter`],
    [904], [`Dual Res Filter`],
    [905], [`EQ Morpher`],
    [906], [`Mono EQ Morpher`],
    [907], [`Ring Modulator`],
    [908], [`Pitcher`],
    [909], [`Super Shaper`],
    [910], [`3 Band Shaper`],
    [911], [`Mono LaserVerb`],
    [912], [`LaserVerb Lite`],
    [913], [`LaserVerb`],
    [914], [`Reverse LaserVrb`],
    [915], [`Gated LaserVerb`],
    [916], [`Poly Pitcher`],
    [917], [`Frequency Offset`],
    [918], [`MutualFreqOffset`],
    [919], [`WackedPitchLFO`],
    [920], [`Chaos!`],
    [948], [`Band Compress`],
    [949], [`CompressDualTime`],
    [950], [`HardKneeCompress`],
    [951], [`SoftKneeCompress`],
    [952], [`Expander`],
    [953], [`Compress w/SC EQ`],
    [954], [`Compress/Expand`],
    [955], [`Comp/Exp + EQ`],
    [956], [`Compress 3 Band`],
    [957], [`Gate`],
    [958], [`Super Gate`],
    [959], [`2 Band Enhancer`],
    [960], [`3 Band Enhancer`],
    [961], [`Tremolo`],
    [962], [`Tremolo BPM`],
    [963], [`AutoPanner`],
    [964], [`Dual AutoPanner`],
    [965], [`SRS`],
    [966], [`Stereo Image`],
    [967], [`Mono -> Stereo`],
    [968], [`Graphic EQ`],
    [969], [`Dual Graphic EQ`],
    [970], [`5 Band EQ`],
    [971], [`3 Band EQ`],
    [972], [`HF Stimulate 1`],
    [975], [`HarmonicSuppress`],
    [998], [`FXMod Diagnostic`],
    [999], [`Stereo Analyze`],
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

#figure(
  table(
    align: left,
    columns: (auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Object ID*], [*Name*]),
    [1], [`Bass Shelf`],
    [2], [`Treble Shelf`],
    [3], [`Lowpass1`],
    [4], [`Lowpass2`],
    [5], [`Hipass1`],
    [6], [`Hipass2`],
    [7], [`Mid Tone`],
    [8], [`Mono Pan`],
    [9], [`Stereo Pan`],
    [10], [`Stereo Balance`],
    [11], [`Mixer`],
    [12], [`Auxer`],
    [13], [`ClearoutOne`],
    [14], [`ClearoutThree`],
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

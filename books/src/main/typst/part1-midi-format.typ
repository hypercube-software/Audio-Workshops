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
    [1568], [`Bands`],
    [1569], [`Grooves`],
    [1570], [`World`],
    [1571], [`Pop`],
    [1572], [`More Keys`],
    [1573], [`More Analog`],
    [1574], [`Leads`],
    [1575], [`Trio Parts`],
    [1576], [`Techno`],
    [1577], [`Texture`],
    [1796], [`Piano Patch`],
    [1797], [`Full Orch`],
    [1798], [`Strings`],
    [1799], [`Horns`],
    [1800], [`Winds`],
    [1801], [`Solo Orch`],
    [1802], [`Perc Pit`],
    [1803], [`Perc Ens`],
    [1804], [`Moody`],
    [1805], [`Exotic`],
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
    [256], [`Gtd2ChrEcho 2Vrb`],
    [257], [`GtdEnhcStIm Hall`],
    [258], [`auxEnvSp4T GtVrb`],
    [259], [`GtRbSwpFlt  Lasr`],
    [260], [`GtRbSwpFlt FlDly`],
    [261], [`ChRvStIEcho Hall`],
    [262], [`ChorChorCDR Spac`],
    [263], [`ChDlDstEQ   Hall`],
    [264], [`auxDPanCDR ChPlt`],
    [265], [`AuxChorFlng  CDR`],
    [266], [`auxEnhcSp4T  CDR`],
    [267], [`auxPtchDst+ ChRv`],
    [268], [`EnhcChorChDl PCD`],
    [269], [`auxPoly      FDR`],
    [270], [`EnhcChorChDl FDR`],
    [271], [`EnhcChrChDl FDR2`],
    [272], [`auxRotoSp4T FlRv`],
    [273], [`auxRotaryFDR Plt`],
    [274], [`RotoOrgFX   Hall`],
    [275], [`CmpRvbFlDl  Hall`],
    [276], [`auxEnhSp4T  RvCm`],
    [277], [`auxPtchRoom RvCm`],
    [278], [`PhsrChorCDR Phsr`],
    [279], [`ChDlSp4TFlDl Phs`],
    [280], [`auxFlgDst+ ChLsD`],
    [281], [`auxFlgDst+ ChLs2`],
    [282], [`RoomRoomSRS CmRv`],
    [283], [`RoomRoom    Room`],
    [284], [`GtRvPlate   Hall`],
    [285], [`RoomRoom     SRS`],
    [286], [`EnhcSp4T    Hall`],
    [287], [`Room RoomChr SRS`],
    [288], [`KB3 V/C ->Rotary`],
    [289], [`EQStImg  5BndEQ`],
    [290], [`aux5BeqStIm Hall`],
    [291], [`10Band StIm Hall`],
    [292], [`3BndCmp     PtFl`],
    [293], [`auxDst+Lsr   Plt`],
    [294], [`auxRsFltEnvSwFlt`],
    [295], [`auxChorEnvF Hall`],
    [296], [`auxChRvEncr Chor`],
    [297], [`auxChrDstEQ Room`],
    [298], [`aux ChDlSRS Hall`],
    [299], [`aux EQFlng DstEQ`],
    [300], [`auxFlngPhsr Lasr`],
    [301], [`auxFlShQFlg Hall`],
    [302], [`auxFlgDist+ Room`],
    [303], [`aux GtVbFl4T Bth`],
    [304], [`auxRvRvQFlg Hall`],
    [305], [`auxRvRbShapeChmb`],
    [306], [`auxSpinMDly Room`],
    [307], [`aux SweepEchoBth`],
    [308], [`auxRoto&DsFDRPlt`],
    [309], [`auxRot&Ds2FDRPlt`],
    [310], [`auxFlgChDl Hall`],
    [311], [`auxDstLsr  CDR`],
    [312], [`CPDlEnFltCmpGtRv`],
    [313], [`RotoOrgFX2  Hall`],
    [314], [`ChDlFlPtLzVb Plt`],
    [315], [`CDFlDlyPhRm Hall`],
    [316], [`CDR FlgRvb  Hall`],
    [317], [`DstPhsPnLzVb CDR`],
    [318], [`DistRoom  GrphEQ`],
    [319], [`Enh  Ch 4T Hall`],
    [320], [`FiltCmpExpFl CDR`],
    [321], [`LzVbFlDstEQ Room`],
    [322], [`PhseDist    Room`],
    [323], [`ChDlyRvFlRv Hall`],
    [324], [`RmRotr&DstChrPlt`],
    [325], [`Clear Studio`],
    [326], [`Pre-KDFX Studio`],
    [327], [`Default Studio`],
    [1340], [`Flanger Trio`],
    [1341], [`Thin Cloud Layer`],
    [1342], [`Fazetortion`],
    [1343], [`Ultraphasulate`],
    [1344], [`Chutney Squishy`],
    [1345], [`BuddMeetsTomita`],
    [1346], [`2 Buses 2 Chains`],
    [1347], [`RingMod Envelope`],
    [1348], [`Drum Megashaper`],
    [1349], [`Dist Lead Guitar`],
    [1350], [`Blues/R&R Guitar`],
    [1351], [`Clean Rhythm Gtr`],
    [1352], [`Sweeping Verb`],
    [1353], [`Pad Ambience`],
    [1354], [`Coming of Dawn`],
    [1355], [`Wide Raindrops`],
    [1356], [`Robot Voice`],
    [1357], [`Before the Crash`],
    [1358], [`Piano Multi-Verb`],
    [1359], [`Mastering Studio`],
    [1568], [`Sweet Hall`],
    [1569], [`Small Hall`],
    [1570], [`Medium Hall`],
    [1571], [`Large Hall`],
    [1572], [`Big Gym`],
    [1573], [`Bright Plate 1`],
    [1574], [`Opera House`],
    [1575], [`Live Chamber`],
    [1576], [`Bathroom`],
    [1577], [`Med Large Room`],
    [1578], [`Real Room`],
    [1579], [`Drum Room`],
    [1580], [`Small Dark Room`],
    [1581], [`Small Closet`],
    [1582], [`Add Ambience`],
    [1583], [`Gated Reverb`],
    [1584], [`Reverse Reverb`],
    [1585], [`Non-Linear`],
    [1586], [`Slapverb`],
    [1587], [`Full Bass`],
    [1588], [`Room + Delay`],
    [1589], [`Delay Big Hall`],
    [1590], [`Chorus Room`],
    [1591], [`Chorus Smallhall`],
    [1592], [`Chorus Med Hall`],
    [1593], [`Chorus Big Hall`],
    [1594], [`Chor-Delay Room`],
    [1595], [`Chor-Delay Hall`],
    [1596], [`Flange-Dly Room`],
    [1597], [`Flange-Dly Hall`],
    [1598], [`Stereo Chorus`],
    [1599], [`Stereo Flanger`],
    [1600], [`Stereo Delay`],
    [1601], [`4-Tap Delay`],
    [1602], [`Chorus Delay`],
    [1603], [`Flange Delay`],
    [1604], [`Chorus 4-Tap`],
    [1605], [`Flange 4 Tap`],
    [1606], [`Chorus Echo`],
    [1607], [`Chorus Echoverb`],
    [1608], [`Fast Flange`],
    [1609], [`Wash`],
    [1610], [`Into The Abyss`],
    [1611], [`Space Flanger`],
    [1612], [`Flange Room`],
    [1613], [`Predelay Hall`],
    [1614], [`Flange Echo`],
    [1615], [`Rotary Club`],
    [1616], [`Rotary Hall`],
    [1617], [`Chorus`],
    [1618], [`Soundbrd/rvb`],
    [1619], [`Percussive Room`],
    [1620], [`Brt Empty Room`],
    [1621], [`Mosque Room`],
    [1622], [`New Gated`],
    [1623], [`Chorus Slap Room`],
    [1624], [`Chorus Bass Room`],
    [1625], [`New Chorus Hall`],
    [1626], [`Spacious`],
    [1627], [`Wash Lead`],
    [1628], [`New Hall w/Delay`],
    [1629], [`Rich Delay`],
    [1630], [`Glass Delay`],
    [1631], [`Real Plate`],
    [1632], [`Real Niceverb`],
    [1633], [`ClassicalChamber`],
    [1634], [`Empty Stage`],
    [1635], [`Long & Narrow`],
    [1636], [`Far Bloom`],
    [1637], [`Floyd Hall`],
    [1638], [`With A Mic`],
  ),
  caption: [STUDIO objects in bank 127],
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

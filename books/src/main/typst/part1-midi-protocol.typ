#import "settings.typ": *
#let hx(b, c) = box(fill: c, inset: (x: 2.5pt, y: 2pt), radius: 2pt, raw(b))
#let hexline(..c) = block(c.pos().join(h(2pt)))

#import "@preview/rivet:0.3.1": schema

#chapter("MIDI protocol", image: image("./assets/hd3-2481-500.svg"), l: "chap1")
#index("Sectioning")

The Musician's Reference (chapter 7) describes the SysEx protocol used to read and write objects in the unit's memory.
This chapter explains the messages, their format, and how they are used by `KurzweilExplorer`. It is based on the
Musician's Reference and on the behaviour observed with the unit, unless stated otherwise.

== Message structure

Every Kurzweil SysEx message has the same header:

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Bytes*], [*Value*], [*Meaning*]),
    [`F0`], [`F0`], [Start of SysEx],
    [`07`], [`07`], [Kurzweil manufacturer ID],
    [dev-id], [`00`-`7F`], [Device ID, set by the SysEx ID parameter (MIDI mode, RECEIVE page). `7F` accepts any value],
    [product], [`78`], [K2600 and later (`78` = 120)],
    [command], [`00`-`19`], [Message type, see the command table below],
    [payload], [variable], [Command arguments],
    [`F7`], [`F7`], [End of SysEx],
  ),
  caption: [Header of a Kurzweil SysEx message],
)

The payload is made only of MIDI data bytes, so every value is split into groups of 7 bits, most significant group
first. A 14-bit value is two bytes, a 21-bit value is three bytes.

== Objects: Type and ID

Each object is identified by a *type* and an *ID*. Therefore, Program 1 is "type program, ID 1".
- The *type* says what kind of object it is (program, keymap, sound block...).
- The *ID* is an instance of that type: which program, which keymap.

The Musician's Reference describes those fields as `type(2)` and `idno(2)`. Sounds simple, right? Unfortunately, they
start to explain that a type in itself has an ID! This is very confusing. Instead, they should simply explain that, what
they call "type's ID", is nothing more than the encoding of the type in MIDI. It is a packed version of a 7-bit value
into 2 MIDI bytes. On MIDI, a data byte only holds 7 bits. A field that needs 8 or more bits is therefore sent as
several bytes, most significant group first. In our case, the *type* is always sent as 14 bits (two bytes)

For instance, the value `0x84` (132) is the type of a program, and it is the only thing the type field carries:
- *Type* `0x84` (`1000 0100`) is split into `0000001` and `0000100`, sent as `01 04`
- Guess what ? `$0104` is called "Type's ID" in the documentation !
- *ID* `1` is sent as `00 01`. Is the ID of an object of a given type.

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

- `00`: nibble format. Each byte of the object becomes two MIDI bytes, the high nibble first. `4F D8 01 29` becomes
  `04 0F 0D 08 00 01 02 09`. This is simple to read and to write, but it doubles the size.
- `01`: bit-stream format. The bits of the object are concatenated and cut in groups of 7 bits, the last group being
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
  hx("F0", gray.lighten(60%)),
  hx("07", gray.lighten(60%)),
  hx("00", blue.lighten(60%)),
  hx("78", gray.lighten(60%)),
  hx("00", orange.lighten(50%)),
  hx("01", green.lighten(50%)),
  hx("04", green.lighten(50%)),
  hx("00", green.lighten(60%)),
  hx("01", green.lighten(60%)),
  hx("00", purple.lighten(60%)),
  hx("00", purple.lighten(60%)),
  hx("00", purple.lighten(60%)),
  hx("00", purple.lighten(60%)),
  hx("03", purple.lighten(60%)),
  hx("0A", purple.lighten(60%)),
  hx("00", purple.lighten(60%)),
  hx("F7", gray.lighten(60%)),
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
      bytes(1, fill: gray.lighten(60%))[`F0`],
      bytes(1, fill: gray.lighten(60%))[`07`],
      bytes(1, fill: blue.lighten(60%))[device],
      bytes(1, fill: gray.lighten(60%))[`78`],
      bytes(1, fill: orange.lighten(50%))[`00`],
      bytes(2, fill: green.lighten(50%))[type],
      bytes(2, fill: green.lighten(60%))[ID],
      bytes(1, fill: purple.lighten(60%))[`00`],
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
      bytes(3, fill: purple.lighten(60%))[offset],
      bytes(3, fill: purple.lighten(60%))[size],
      bytes(1, fill: gray.lighten(60%))[`F7`],
      bytes(3, fill: white)[],
    ),
  ),
  caption: [`DUMP` for program 1, nibble format (17 bytes)],
)
==== DIR

`DIR` for program type (01 04) id 1 (00 01):

#hexline(
  hx("F0", gray.lighten(60%)), hx("07", gray.lighten(60%)), hx("00", blue.lighten(60%)), hx("78", gray.lighten(60%)),
  hx("04", orange.lighten(50%)), hx("01", green.lighten(50%)), hx("04", green.lighten(50%)),
  hx("00", green.lighten(60%)), hx("01", green.lighten(60%)), hx("F7", gray.lighten(60%)),
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
    bytes(1, fill: gray.lighten(60%))[`F0`],
    bytes(1, fill: gray.lighten(60%))[`07`],
    bytes(1, fill: blue.lighten(60%))[device],
    bytes(1, fill: gray.lighten(60%))[`78`],
    bytes(1, fill: orange.lighten(50%))[`04`],
    bytes(2, fill: green.lighten(50%))[type],
    bytes(2, fill: green.lighten(60%))[ID],
    bytes(1, fill: gray.lighten(60%))[`F7`],
  ),
  caption: [`DIR` for program 1 (10 bytes)],
)

==== READ

`READ` of program type (01 04) id 1 (00 01) in packed format:

#hexline(
  hx("F0", gray.lighten(60%)), hx("07", gray.lighten(60%)), hx("00", blue.lighten(60%)), hx("78", gray.lighten(60%)),
  hx("0A", orange.lighten(50%)), hx("01", green.lighten(50%)), hx("04", green.lighten(50%)),
  hx("00", green.lighten(60%)), hx("01", green.lighten(60%)), hx("01", purple.lighten(60%)), hx("F7", gray.lighten(60%)),
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
      bytes(1, fill: gray.lighten(60%))[`F0`],
      bytes(1, fill: gray.lighten(60%))[`07`],
      bytes(1, fill: blue.lighten(60%))[device],
      bytes(1, fill: gray.lighten(60%))[`78`],
      bytes(1, fill: orange.lighten(50%))[`0A`],
      bytes(2, fill: green.lighten(50%))[type],
      bytes(2, fill: green.lighten(60%))[ID],
      bytes(1, fill: purple.lighten(60%))[format],
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
      bytes(1, fill: gray.lighten(60%))[`F7`],
      bytes(9, fill: white)[],
    ),
  ),
  caption: [`READ` of program 1 in packed format (11 bytes)],
)

==== DIRBANK

`DIRBANK` for all programs (01 04) of all banks (7F):

#hexline(
  hx("F0", gray.lighten(60%)), hx("07", gray.lighten(60%)), hx("00", blue.lighten(60%)), hx("78", gray.lighten(60%)),
  hx("0C", orange.lighten(50%)), hx("01", green.lighten(50%)), hx("04", green.lighten(50%)),
  hx("7F", purple.lighten(60%)), hx("00", purple.lighten(40%)), hx("F7", gray.lighten(60%)),
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
    bytes(1, fill: gray.lighten(60%))[`F0`],
    bytes(1, fill: gray.lighten(60%))[`07`],
    bytes(1, fill: blue.lighten(60%))[device],
    bytes(1, fill: gray.lighten(60%))[`78`],
    bytes(1, fill: orange.lighten(50%))[`0C`],
    bytes(2, fill: green.lighten(50%))[type],
    bytes(1, fill: purple.lighten(60%))[bank],
    bytes(1, fill: purple.lighten(40%))[ramonly],
    bytes(1, fill: gray.lighten(60%))[`F7`],
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
      bytes(1, fill: gray.lighten(60%))[`F0`],
      bytes(1, fill: gray.lighten(60%))[`07`],
      bytes(1, fill: blue.lighten(60%))[device],
      bytes(1, fill: gray.lighten(60%))[`78`],
      bytes(1, fill: orange.lighten(50%))[`14`],
      bytes(1, fill: green.lighten(50%))[action],
      bytes(1, fill: purple.lighten(60%))[key],
      bytes(1, fill: purple.lighten(40%))[repeat],
      bytes(1, fill: gray.lighten(60%))[`F7`],
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

== Sample Dump Standard

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
      bytes(1, fill: gray.lighten(60%))[`F0`],
      bytes(1, fill: gray.lighten(60%))[`7E`],
      bytes(1, fill: blue.lighten(60%))[channel],
      bytes(1, fill: gray.lighten(60%))[`01`],
      bytes(2, fill: orange.lighten(50%))[sample number],
      bytes(1, fill: blue.lighten(60%))[bit depth],
      bytes(3, fill: green.lighten(50%))[sample period],
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
      bytes(3, fill: purple.lighten(60%))[sample length],
      bytes(3, fill: yellow.lighten(40%))[loop start],
      bytes(3, fill: yellow.lighten(60%))[loop end],
      bytes(1, fill: blue.lighten(60%))[loop type],
    ),
    bytefield(
      bpr: 80,
      bitheader(marker: false, angle: 0deg, numbers: none, 0, [20]),
      bytes(1, fill: gray.lighten(60%))[`F7`],
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
      bytes(1, fill: gray.lighten(60%))[`F0`],
      bytes(1, fill: gray.lighten(60%))[`7E`],
      bytes(1, fill: blue.lighten(60%))[channel],
      bytes(1, fill: gray.lighten(60%))[`02`],
      bytes(1, fill: orange.lighten(50%))[packet number],
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
      bytes(3, fill: green.lighten(60%))[sample 1],
      bytes(3, fill: green.lighten(60%))[sample 2],
      bytes(3, fill: green.lighten(60%))[...],
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
      bytes(3, fill: green.lighten(60%))[...],
      bytes(3, fill: green.lighten(60%))[sample 39],
      bytes(3, fill: green.lighten(60%))[sample 40],
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
      bytes(1, fill: orange.lighten(50%))[CHK],
      bytes(1, fill: gray.lighten(60%))[`F7`],
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

== Conversation with the K2600

This section describes the sequence used by `KurzweilExplorer` to load a patch with samples.

=== Sequence for one sound block

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

    [2], [If the size is zero, SDS header with sample block `id - 1` is sent], [`ACK` (`7F`)],
    [3], [SDS data packets, numbered from 0], [`ACK` for each packet, or `NAK` and a resend],
    [4], [`READ` of the sound block], [`WRITE` of the current sound block state],

    [5],
    [`WRITE` of the updated sound block, with its headers relocated to the address found in step 4],
    [`DACK`, or `DNAK` with an error code],
  ),
  caption: [Uploading one sample and its sound block],
)

Some details of the implementation:

- The sample number sent in step 2 is `id - 1`. The K2600R stores the sample one number above the one in the header.
  Some PC sample editors show the same off-by-one: the sample is stored at the requested number, but a `DIR` or a `READ`
  must use the number minus one. The conversion is applied in both directions in `KurzweilExplorer` and
  `SampleDumpStandard.requestSample`. Check this on your unit before relying on it for other tools.
- The base address is not chosen by us. After the dump, the unit places the sample at an address in sample RAM. The
  first header of the sound block that the unit returns in step 4 holds that address (`sampleStart`).
- If the sample is already on the unit with the same name, steps 2 and 3 are skipped.
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

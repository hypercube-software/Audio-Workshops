#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Edit buffer commands", l: "chap-edit-cmds")

The 2 edit buffers live in RAM: the global one and the preset one (see #cref(<chap-memory>)). Their commands share
the same frame: a query carries the count and the packed address, a write carries the count, the address and the data,
and the write answers with a status byte. The address designates any byte of the buffer, as #cref(<chap-sysex>)
explains.

The figures colour the fields: blue is the command, pink the payload size, orange the count, green the address, red the
status. A field left white is the data that the frame carries.

== Global settings

`QGLOB` reads a block of the global edit buffer. Its payload is the count and the packed address; the frame below reads
the current preset, 1 byte at `0x0001`:

#figure(
  frame(
    bytefield(
      bpr: 88,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[20],
      bytes(2, fill: l_purple)[00 03],
      bytes(1, fill: l_orange)[01],
      bytes(2, fill: l_green)[00 01],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
    (l_orange, [count]),
    (l_green, [address]),
  ),
  caption: [QGLOB: Query 1 byte at 0x0001],
)

The answer reuses the `WGLOB` frame: it repeats the count and the address, then carries the data.

#figure(
  frame(
    bytefield(
      bpr: 96,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[30],
      bytes(2, fill: l_purple)[00 04],
      bytes(1, fill: l_orange)[01],
      bytes(2, fill: l_green)[00 01],
      bytes(1)[07],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
    (l_orange, [count]),
    (l_green, [address]),
  ),
  caption: [WGLOB: Write 1 byte at 0x0001, and the answer to QGLOB],
)

`WGLOB` writes with the same frame: the count, the address, then the data. The unit answers with `ACKG`, which repeats
the count and the address, then gives a status byte:

#figure(
  frame(
    bytefield(
      bpr: 96,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[38],
      bytes(2, fill: l_purple)[00 04],
      bytes(1, fill: l_orange)[01],
      bytes(2, fill: l_green)[00 01],
      bytes(1, fill: l_red)[00],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
    (l_orange, [count]),
    (l_green, [address]),
    (l_red, [status]),
  ),
  caption: [ACKG: the answer to a write of 1 byte at 0x0001],
)

A status byte other than `00` is a failure, and it is the only diagnostic the unit gives.

`CONTRAST` writes the contrast of the display alone, at address `0x0002`, without carrying an address:

#figure(
  frame(
    bytefield(
      bpr: 72,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[7A],
      bytes(2, fill: l_purple)[00 01],
      bytes(1)[32],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
  ),
  caption: [CONTRAST: Set the contrast],
)

== Current preset

`QEDIT` reads a block of the preset edit buffer. Its payload is the count and the packed address; the frame below reads
the colour of a pad, 1 byte at `0x0B3C` (see #cref(<chap-controls>)):

#figure(
  frame(
    bytefield(
      bpr: 88,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[21],
      bytes(2, fill: l_purple)[00 03],
      bytes(1, fill: l_orange)[01],
      bytes(2, fill: l_green)[0B 3C],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
    (l_orange, [count]),
    (l_green, [address]),
  ),
  caption: [QEDIT: Query 1 byte at 0x0B3C],
)

The answer reuses the `WEDIT` frame, with the data after the address. A name is a read of 8 bytes at `0x0000`, so its
answer carries the 8 characters:

#figure(
  frame(
    bytefield(
      bpr: 152,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[31],
      bytes(2, fill: l_purple)[00 0B],
      bytes(1, fill: l_orange)[08],
      bytes(2, fill: l_green)[00 00],
      bytes(8)[StudOne5],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
    (l_orange, [count]),
    (l_green, [address]),
  ),
  caption: [WEDIT: Write 8 bytes at 0x0000, and the answer to QEDIT],
)

`WEDIT` writes with the same frame: the count, the address, then the data. The unit answers with `ACKE`, which repeats
the count and the address, then gives a status byte:

#figure(
  frame(
    bytefield(
      bpr: 96,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[39],
      bytes(2, fill: l_purple)[00 04],
      bytes(1, fill: l_orange)[01],
      bytes(2, fill: l_green)[00 13],
      bytes(1, fill: l_red)[00],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
    (l_orange, [count]),
    (l_green, [address]),
    (l_red, [status]),
  ),
  caption: [ACKE: the answer to a write of 1 byte at 0x0013],
)

2 reads give the settings of the current preset, in the 2 memory layouts of #cref(<chap-preset>):

- `QEDIT` reads them in the memory layout of the edit buffer, 127 bytes at most per request: a full preset takes 12
    requests of 127 bytes and 1 of 20 bytes;
- `QPRESET`, with the preset number `00`, reads them in the memory layout of a user preset, in a single request; the
    bytes that the edit buffer does not hold come back as `00` (see #cref(<chap-flash-cmds>)).

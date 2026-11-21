#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Edit buffer commands", l: "chap-edit-cmds")

The 2 edit buffers live in RAM: the global one and the preset one (see #cref(<chap-overview>)). Their commands share
the same frame: a query carries the count and the packed address, a write carries the count, the address and the data,
and the write answers with a status byte.

== Global settings

`QGLOB` reads a block of the global edit buffer. Its payload is the count and the packed address:

#figure(
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
  caption: [QGLOB: reading 1 byte of the global edit buffer],
)

The answer reuses the `WGLOB` frame: it repeats the count and the address, then carries the data.

#figure(
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
    bytes(1, fill: l_blue)[07],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [WGLOB: a write to the global edit buffer, and the answer to QGLOB],
)

`WGLOB` writes with the same frame: the count, the address, then the data. The unit answers with `ACKG`, which repeats
the count and the address, then gives a status byte:

#figure(
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
    bytes(1, fill: l_blue)[00],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [ACKG: the answer to a write to the global edit buffer],
)

A status byte other than `00` is a failure, and it is the only diagnostic the unit gives.

`CONTRAST` writes the contrast of the display alone, at address `0x0002`, without carrying an address:

#figure(
  bytefield(
    bpr: 72,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[7A],
    bytes(2, fill: l_purple)[00 01],
    bytes(1, fill: l_orange)[32],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [CONTRAST: setting the contrast in the global edit buffer],
)

== Preset

`QEDIT` reads a block of the preset edit buffer. Its payload is the count and the packed address; the frame below reads
the colour of a pad, 1 byte at `0x0B3C` (see #cref(<chap-pads>)):

#figure(
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
  caption: [QEDIT: reading 1 byte of the preset edit buffer],
)

The answer reuses the `WEDIT` frame, with the data after the address. A name is a read of 8 bytes at `0x0000`, so its
answer carries the 8 characters:

#figure(
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
    bytes(8, fill: l_blue)[StudOne5],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [WEDIT: a write to the preset edit buffer, and the answer to QEDIT],
)

`WEDIT` writes with the same frame: the count, the address, then the data. The unit answers with `ACKE`, which repeats
the count and the address, then gives a status byte:

#figure(
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
    bytes(1, fill: l_blue)[00],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [ACKE: the answer to a write to the preset edit buffer],
)

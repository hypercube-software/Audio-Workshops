#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Flash memory commands", l: "chap-flash-cmds")

The flash memory holds the 30 user presets and the stored settings. The preset commands carry the whole record or a
block of it; the setting commands carry a fixed payload.

== User presets

`QPRESET` asks for 1 stored preset. Its payload is 1 byte, the number of the slot, from 1 to 30; `00` designates the
preset that is playing:

#figure(
  bytefield(
    bpr: 72,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[12],
    bytes(2, fill: l_purple)[00 01],
    bytes(1, fill: l_orange)[08],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [QPRESET: reading a user preset in flash memory],
)

The answer reuses the `WPRESET` frame: the slot, the name and the whole record, in a frame of 1555 bytes. `WPRESET`
writes with the same frame (see #cref(<chap-preset>)).

#figure(
  bytefield(
    bpr: 184,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[10],
    bytes(2, fill: l_purple)[0C 0B],
    bytes(1, fill: l_orange)[08],
    bytes(8, fill: l_green)[name],
    bytes(6, fill: l_gray)[1538 data bytes],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [WPRESET: a write in flash memory, and the answer to QPRESET],
)

`PPRESET` writes a block of bytes at an address inside the stored record, without carrying the whole preset. Its
payload is the count, the packed address, the slot, then the data; the frame below writes 2 bytes at `0x0575`, in the
record of slot 8:

#figure(
  bytefield(
    bpr: 112,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[32],
    bytes(2, fill: l_purple)[00 06],
    bytes(1, fill: l_orange)[02],
    bytes(2, fill: l_green)[05 75],
    bytes(1, fill: l_orange)[08],
    bytes(2, fill: l_blue)[00 01],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [PPRESET: writing 2 bytes inside a user preset in flash memory],
)

The unit answers with `ACKP`, which repeats the count, the address and the slot, then gives a status byte:

#figure(
  bytefield(
    bpr: 104,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[3A],
    bytes(2, fill: l_purple)[00 05],
    bytes(1, fill: l_orange)[02],
    bytes(2, fill: l_green)[05 75],
    bytes(1, fill: l_orange)[08],
    bytes(1, fill: l_blue)[00],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [ACKP: the answer to a poke in flash memory],
)

== Stored settings

3 commands reach the settings saved in flash memory, and none of them answers.

`SGLOB` sets the contrast to its 1 payload byte, then saves the 18 settings of the global edit buffer to flash memory
(see #cref(<chap-store>)):

#figure(
  bytefield(
    bpr: 72,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[7B],
    bytes(2, fill: l_purple)[00 01],
    bytes(1, fill: l_orange)[32],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [SGLOB: saving the global edit buffer to flash memory],
)

`FIELD4` writes 4 bytes in a stored field whose role is not established:

#figure(
  bytefield(
    bpr: 96,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[78],
    bytes(2, fill: l_purple)[00 04],
    bytes(4, fill: l_orange)[data],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [FIELD4: writing 4 bytes in a stored field],
)

`SERIAL` writes the 16 bytes of the serial number that the device inquiry reports (see #cref(<chap-inquiry>)):

#figure(
  bytefield(
    bpr: 192,
    bytes(1, fill: l_gray)[F0],
    bytes(1, fill: l_gray)[47],
    bytes(1, fill: l_gray)[00],
    bytes(1, fill: l_gray)[25],
    bytes(1, fill: l_blue)[79],
    bytes(2, fill: l_purple)[00 10],
    bytes(16, fill: l_orange)[serial, ASCII],
    bytes(1, fill: l_gray)[F7],
  ),
  caption: [SERIAL: writing the serial number],
)

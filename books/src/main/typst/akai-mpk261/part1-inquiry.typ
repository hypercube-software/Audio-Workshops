#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Device inquiry", l: "chap-inquiry")

The device inquiry is a standard MIDI request, outside the AKAI protocol: it asks the units connected to a port to
identify themselves. The MPK-261 answers with its model, the version of its OS and its serial number.

== The request

#figure(
  frame(
    bytefield(
      bpr: 48,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[7E],
      bytes(1, fill: l_blue)[7F],
      bytes(2, fill: l_gray)[06 01],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [broadcast device ID]),
  ),
  caption: [Device inquiry: the request],
)

The request is 6 bytes long: the start of SysEx, `7E` for the universal non-real-time message, the broadcast device ID
`7F`, the identity request `06 01`, and the end of SysEx. Every unit on the port answers, and each answer comes back on
the port that carried the request, so the 4 ports of the MPK-261 can be interrogated 1 by 1.

== The answer

The answer of the MPK-261 is 35 bytes: 14 bytes that the MIDI specification defines, then 21 bytes of AKAI extension.
On the reference unit the standard part reads:

#figure(
  frame(
    bytefield(
      bpr: 112,
      bitheader(
        marker: false,
        angle: 0deg,
        numbers: none,
        0, [0],
        8, [1],
        16, [2],
        24, [3],
        32, [4],
        40, [5],
        48, [6],
        56, [7],
        64, [8],
        72, [9],
        80, [10],
        88, [11],
        96, [12],
        104, [13],
      ),
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[7E],
      bytes(1, fill: l_blue)[00],
      bytes(2, fill: l_gray)[06 02],
      bytes(1, fill: l_gray)[47],
      bytes(2, fill: l_green)[25 00],
      bytes(2, fill: l_green)[19 00],
      bytes(1, fill: l_orange)[01],
      bytes(1, fill: l_orange)[00],
      bytes(1, fill: l_orange)[02],
      bytes(1, fill: l_blue)[00],
    ),
    (l_blue, [device ID]),
    (l_green, [family and member]),
    (l_orange, [version]),
  ),
  caption: [Device inquiry: the standard part of the answer],
)

and the AKAI extension follows:

#figure(
  frame(
    bytefield(
      bpr: 168,
      bitheader(
        marker: false,
        angle: 0deg,
        numbers: none,
        0, [14],
        8, [15],
        16, [16],
        24, [17],
        32, [18],
        40, [19],
        48, [20],
        56, [21],
        64, [22],
        72, [23],
        80, [24],
        88, [25],
        96, [26],
        104, [27],
        112, [28],
        120, [29],
        128, [30],
        136, [31],
        144, [32],
        152, [33],
        160, [34],
      ),
      bytes(1, fill: l_blue)[03],
      bytes(1, fill: l_gray)[12],
      bytes(1, fill: l_purple)[7F],
      bytes(1, fill: l_purple)[7F],
      bytes(8, fill: l_purple)[A1210115],
      bytes(7, fill: l_purple)[5725077],
      bytes(1, fill: l_purple)[00],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [port]),
    (l_purple, [stored field and serial number]),
  ),
  caption: [Device inquiry: the AKAI extension of the answer],
)

#figure(
  ptable(
    columns: (auto, auto, auto, 1fr),
    head([*Offset*], [*Field*], [*Value*], [*Meaning*]),
    [`0x00`], [start], [`F0`], [Start of SysEx],
    [`0x01`], [universal], [`7E`], [Universal non-real-time message],
    [`0x02`], [channel], [`00`], [Common channel of the controls, counted from zero. *Not* the device ID],
    [`0x03`], [information], [`06`], [General information],
    [`0x04`], [reply], [`02`], [Identity reply],
    [`0x05`], [manufacturer], [`47`], [AKAI],
    [`0x06`], [family], [`25 00`], [The model, little-endian: `0x23` MPK-225, `0x24` MPK-249, `0x25` MPK-261],
    [`0x08`], [member], [`19 00`], [The member of the family: `0x0019` for the whole MPK2 series],
    [`0x0A`], [version], [`01 00 02`], [The version of the OS, `1.02`],
    [`0x0D`], [device ID], [`00`], [The device ID, the byte at `0x0030` of the global settings],
    [`0x0E`], [port], [`03`], [The USB port that carries the answer, from 0 for port 1],
    [`0x0F`], [fixed], [`12`], [Constant],
    [`0x10`], [field], [`7F 7F`], [The low half of a stored field; role not established],
    [`0x12`], [serial], [ASCII], [The 15 characters of the serial number, then a `00`: `A12101155725077`],
    [`0x22`], [end], [`F7`], [End of SysEx],
  ),
  caption: [Fields of the identity reply],
)

The reply follows the MIDI specification up to the version field: manufacturer, family, member and version. The last
20 bytes are an AKAI extension: the USB port, a constant, 2 stored bytes and the serial number. Every byte is masked to
7 bits.

== The device ID

The unit has 1 device ID, the byte at `0x0030` of the global settings. It travels after the manufacturer ID of every AKAI
frame (see #cref(<chap-sysex>)), and the unit repeats it at `0x0D` of the identity reply.

A device ID is the address that SysEx uses to designate the receiver of a message: it tells several machines apart on
the same bus. The unit writes its own ID in every frame that it sends, and it does not filter an incoming frame on that
ID: a frame reaches the unit whatever ID it carries.

Changing the ID changes the header of the frames that the unit sends, and the unit still answers frames
that carry `00`, the factory value.

== The field at 0x02

The MIDI specification gives the identity reply a field that the responder fills with a device ID. The MPK-261 fills
that field with the common channel of the controls, at `0x000A`, counted from zero, so the answer of a unit on the
factory settings carries `00` there.

That byte carries the common channel, and the device ID sits at `0x0D`. The 2 values are independent: copying one into
the other sets the common channel of the presets.

== What the identity reply gives

The reply describes the unit in 4 fields:

- The family field gives the model, so the replies of the 3 models of the series differ there;
- The version field gives the version of the OS, without having to look at the *OS* page of the front panel;
- The serial number is unique to the unit;
- The device ID at `0x0D` designates the unit on a bus that carries several of them.

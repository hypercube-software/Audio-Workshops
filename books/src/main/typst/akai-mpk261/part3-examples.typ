#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Examples", l: "chap-examples")

Each example writes 1 value of the current preset, then says what the unit does with it. The frames are the ones of
#cref(<chap-edit-cmds>): a `WEDIT` frame with the count, the address and the data.

== Writing 1 control

The controller number of the first knob is 1 byte at address `0x0514` of the edit buffer. Sending `2D` instead of `0B`
is a `WEDIT` of 1 byte at that address:

#figure(
  frame(
    bytefield(
      bpr: 96,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[31],
      bytes(2, fill: l_purple)[00 04],
      bytes(1, fill: l_orange)[01],
      bytes(2, fill: l_green)[05 14],
      bytes(1)[0B],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
    (l_orange, [count]),
    (l_green, [address]),
  ),
  caption: [WEDIT: Write 1 byte at 0x0514],
)

The unit takes the new value immediately: the knob sends the other controller from that point, until another preset is
loaded. The user preset keeps its content, and the other 2 banks keep their settings.

== Writing 1 colour

The hit colour of the first pad of bank A is 1 byte at address `0x0B3C` of the edit buffer. Sending `03` turns the pad
amber:

#figure(
  frame(
    bytefield(
      bpr: 96,
      bytes(1, fill: l_gray)[F0],
      bytes(1, fill: l_gray)[47],
      bytes(1, fill: l_gray)[00],
      bytes(1, fill: l_gray)[25],
      bytes(1, fill: l_blue)[31],
      bytes(2, fill: l_purple)[00 04],
      bytes(1, fill: l_orange)[01],
      bytes(2, fill: l_green)[0B 3C],
      bytes(1)[03],
      bytes(1, fill: l_gray)[F7],
    ),
    (l_blue, [command]),
    (l_purple, [payload size]),
    (l_orange, [count]),
    (l_green, [address]),
  ),
  caption: [WEDIT: Write 1 byte at 0x0B3C],
)

The pad lights in amber as soon as the `ACKE` command arrives. The first pad of bank B is 16 bytes further, at
`0x0B4C`: the 16 pads of a bank are contiguous, and a whole bank of colours is a single write of 16 bytes.

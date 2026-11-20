#import "../common/settings.typ": *
#import "lib.typ": *

#chapter("Firmware update", l: "chap-firmware")

#caveat[
    *Do not send the frames of this chapter.* They drive the update mode of the unit: the first one erases the OS and
    leaves the controller waiting for a transfer. The frames are documented here so that a tool recognises them and
    never sends them by accident.
]

== The update mode

The `UPDATE` command carries 2 payload bytes, `00 00`:

#figure(
    bytefield(
        bpr: 64,
        bytes(1, fill: l_gray)[F0],
        bytes(1, fill: l_gray)[47],
        bytes(1, fill: l_gray)[00],
        bytes(1, fill: l_blue)[25],
        bytes(1, fill: l_red)[72],
        bytes(2, fill: l_purple)[00 00],
        bytes(1, fill: l_gray)[F7],
    ),
    caption: [Entering the update mode],
)

The unit erases its OS and shows *Automated Update*, *Do not remove USB cable* and *WAITING FOR TRANSFER*. It answers
no memory command in that state.

The 3 models of the series share the frame: the model byte selects the unit, `23` for the MPK-225, `24` for the
MPK-249 and `25` for the MPK-261.

== The transfer

The `XFER` command carries the OS in 1 frame, in the text form of Intel HEX
(#link("https://en.wikipedia.org/wiki/Intel_HEX")[Wikipedia: Intel HEX]). The frame has no size field: the text starts
after the command byte and runs to `F7`.

#figure(
    bytefield(
        bpr: 72,
        bytes(1, fill: l_gray)[F0],
        bytes(1, fill: l_gray)[47],
        bytes(1, fill: l_gray)[00],
        bytes(1, fill: l_blue)[24],
        bytes(1, fill: l_red)[70],
        bytes(3, fill: l_purple)[Intel HEX text],
        bytes(1, fill: l_gray)[F7],
    ),
    caption: [Transferring the OS],
)

The frame is 512 010 bytes long, of which 512 004 are text. The text describes 181 988 bytes of code, which the unit
writes from address `0x08006000` to `0x08033FFF`.

The frame carries the model byte `24`, the MPK-249, because the OS is common to the MPK-249 and the MPK-261. The
transfer uses port 1, the port that carries the notes, and not port 4.

== The update file

The official updater does not send the frame directly: it reads a container, with the extension `.upd`, which holds the
text of the OS and the 3 update-mode frames, 1 per model.

== The recovery

#caveat[
    Do not send requests at random to this unit, as I did: you could start an update. Fortunately, I was able to
    recover the OS easily.
]

Let's say you send the infamous update command by mistake. If the unit shows *WAITING FOR TRANSFER*, do not panic,
and *do not switch it off*. The update mode waits for a transfer, and the official updater completes it: run it, and
it sends the same command again, which is harmless.

1. Download the update of the MPK2 series from
    #link("https://www.akaipro.com/downloads-and-support/downloads/?product=MPK261")[the MPK261 downloads page of AKAI]. The
    archive holds the *MIDI Updater* and the `.upd` file of the OS.
2. Run the updater, drop the `.upd` file on it, and confirm.
3. Keep the USB cable plugged during the transfer: it takes a few minutes.
4. Press Enter when the updater asks for it.

// =============================================================================
// Entry point of the book "AKAI MPK-261 Sysex and Memory Reference".
//
// Same structure as the other books: shared code in ../common, everything which is
// specific to this book (chapters, pictures) next to this file:
//
//   akai-mpk261/
//     main.typ                this file
//     lib.typ                 helpers of the chapters
//     part1-overview.typ      what the unit is, why this document, disclaimer, scope, conventions
//     part1-protocol.typ      the 4 MIDI ports, the commands of the 2 memories and of the OS
//     part1-memory.typ        the 3 memories, RAM and flash memory, address spaces, control matrices
//     part1-sysex.typ         MIDI frames, 14-bit fields, read and write
//     part1-inquiry.typ       device inquiry and identity reply
//     part1-edit-cmds.typ     commands of the 2 edit buffers (RAM)
//     part1-flash-cmds.typ    commands of the flash memory
//     part1-firmware.typ      update mode, the frames that must never be sent
//     part2-global.typ        the global settings (parameters, device ID, live states)
//     part2-store.typ         saving the global settings
//     part3-layout.typ        the preset: edit buffer and user presets, their 2 memory layouts, the arpeggiator pattern
//     part3-controls.typ      DAW buttons, switches, faders, knobs, pads, keystroke codes, channel encoding
//     part3-examples.typ      writing 1 control, writing 1 colour
//     assets/                 screenshots used by the chapters (create it if needed)
//
// Compile from the books/src/main/typst directory:
//   typst compile --root . akai-mpk261/main.typ akai-mpk-261-sysex-guide.pdf
// =============================================================================
#import "../common/settings.typ": *

#show: init.with(
  title: "AKAI MPK-261",
  subtitle: "Sysex and Memory Reference",
  author: "2026",
  lang: "en",
  // Green artwork for this book; the Kurzweil book is blue and the SE-02 red.
  cover: green-book-cover,
  part-background: green-part-background,
  banners: green-banners,
  image-index: green-outline-banner,
  // Colours of this book. Any key not set here falls back to `default-theme`,
  // which keeps the running page header transparent (`header-background: none`).
  theme: (
    main: rgb("#0f7a52"),
    cover-background: rgb("#0f7a52").lighten(76%),
    heading-background: rgb("#0f7a52").lighten(84%),
  ),
  copyright: copyright-notice(edition: "First draft, November 2026"),
  list-of-figure-title: "List of Figures",
  list-of-table-title: "List of Tables",
  supplement-chapter: "Chapter",
  supplement-part: "Part",
  part-style: 0,
  outline-depth: 4,
)

#include "part1-overview.typ"
#include "part1-protocol.typ"
#include "part1-memory.typ"
#include "part1-sysex.typ"
#include "part1-inquiry.typ"
#include "part1-edit-cmds.typ"
#include "part1-flash-cmds.typ"
#include "part1-firmware.typ"
#include "part2-global.typ"
#include "part2-store.typ"
#include "part3-layout.typ"
#include "part3-controls.typ"
#include "part3-examples.typ"

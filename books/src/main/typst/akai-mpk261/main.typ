// =============================================================================
// Entry point of the book "AKAI MPK-261 Sysex and Memory Reference".
//
// Same layout as the other books: shared code in ../common, everything which is
// specific to this book (chapters, pictures) next to this file:
//
//   akai-mpk261/
//     main.typ                this file
//     lib.typ                 helpers of the chapters
//     part1-overview.typ      what the unit is, what this book documents
//     part1-sysex.typ         MIDI frames, commands, 14-bit fields
//     part2-global.typ        the global memory (system settings, live states)
//     part3-preset.typ        the preset block (name, tempo, transport, arp)
//     part3-keyboard.typ      keyboard, split, wheels and pedals
//     part3-controls.typ      DAW buttons, switches, faders and knobs
//     part3-pads.typ          the pads and their colours
//     part4-dump.typ          the preset dump
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
#include "part1-sysex.typ"
#include "part2-global.typ"
#include "part3-preset.typ"
#include "part3-keyboard.typ"
#include "part3-controls.typ"
#include "part3-pads.typ"
#include "part4-dump.typ"

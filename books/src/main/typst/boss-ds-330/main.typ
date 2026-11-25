// =============================================================================
// Entry point of the book "Boss DS-330 Sysex Format".
//
// Same layout as the other books: shared code in ../common, everything which is
// specific to this book (chapters, pictures) next to this file:
//
//   boss-ds-330/
//     main.typ                this file
//     lib.typ                 helpers of the chapters
//     part1-overview.typ
//     part1-sysex.typ
//     part1-encodings.typ
//     part2-system.typ
//     part3-patch-common.typ
//     part3-patch-params.typ
//     part3-patch-sends.typ
//     part4-drum-maps.typ
//     part4-bulk-dump.typ
//     assets/                 screenshots used by the chapters (create it if needed)
//
// Compile from the books/src/main/typst directory:
//   typst compile --root . boss-ds-330/main.typ boss-ds-330-sysex-guide.pdf
// =============================================================================
#import "../common/settings.typ": *

#show: init.with(
  title: "Boss DS-330",
  subtitle: "Sysex Format Reference",
  author: "2026",
  lang: "en",
  // Purple artwork for this book: a distinct colour from the SE-02 book, which
  // shares the red variant. Both are Roland machines.
  cover: purple-book-cover,
  part-background: purple-part-background,
  banners: purple-banners,
  image-index: purple-outline-banner,
  // Colours of this book. Any key not set here falls back to `default-theme`.
  theme: (
    main: rgb("#7a3bd6"),
    cover-background: rgb("#7a3bd6").lighten(78%),
    heading-background: rgb("#7a3bd6").lighten(84%),
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
#include "part1-encodings.typ"
#include "part2-system.typ"
#include "part3-patch-common.typ"
#include "part3-patch-params.typ"
#include "part3-patch-sends.typ"
#include "part4-drum-maps.typ"
#include "part4-bulk-dump.typ"
#include "part5-single-mode.typ"

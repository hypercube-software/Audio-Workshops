// =============================================================================
// Entry point of the book "Roland SE-02 Sysex format".
//
// Same layout as the other books: shared code in ../common, everything which is
// specific to this book (chapters, pictures) next to this file:
//
//   se-02/
//     main.typ                this file
//     part1-overview.typ
//     part1-sysex.typ
//     part2-system.typ
//     part3-patch-block.typ
//     part3-patch-params.typ
//     part4-seq-params.typ
//     part4-seq-patterns.typ
//     part4-seq-songs.typ
//     assets/                 screenshots used by the chapters (create it if needed)
//
// Compile from the books/src/main/typst directory:
//   typst compile --root . se-02/main.typ roland-se02-sysex-guide.pdf
// =============================================================================
#import "../common/settings.typ": *

#show: init.with(
  title: "Roland SE-02",
  subtitle: "Sysex Format Reference",
  author: "2026",
  lang: "en",
  // Red artwork for this book; the Kurzweil book uses the blue variant.
  cover: red-book-cover,
  banners: red-banners,
  image-index: red-outline-banner,
  // Colours of this book. Any key not set here falls back to `default-theme`.
  theme: (
    main: rgb("#c8102e"),
    cover-background: rgb("#c8102e").lighten(78%),
    heading-background: rgb("#c8102e").lighten(84%),
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
#include "part2-addresses.typ"
#include "part2-system.typ"
#include "part3-patch-block.typ"
#include "part3-patch-params.typ"
#include "part4-seq-params.typ"
#include "part4-seq-patterns.typ"
#include "part4-seq-songs.typ"

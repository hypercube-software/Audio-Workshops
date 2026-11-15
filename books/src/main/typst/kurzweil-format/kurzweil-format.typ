// =============================================================================
// Entry point of the book "Inside V.A.S.T data model" (Kurzweil K2600).
//
// The shared code (template, settings, artwork) lives in ../common, the
// chapters and the screenshots of this book live next to this file:
//
//   kurzweil-format/
//     kurzweil-format.typ   this file
//     part1*.typ, part2.typ, history.typ
//     assets/               screenshots used by the chapters
//     scripts/              generators of part1-memory_map.typ
//
// Compile from the books/src/main/typst directory:
//   typst compile --root . kurzweil-format/kurzweil-format.typ
// =============================================================================
#import "../common/settings.typ": *

#show: init.with(
  title: "Inside V.A.S.T Data model",
  subtitle: "MIDI Development Guide",
  author: "2026",
  lang: "en",
  // Shared artwork, see common/settings.typ
  cover: book-cover,
  image-index: outline-banner,
  // Colours of this book. Only the accent colour differs from the shared
  // defaults (`default-theme`): cover band, chapter titles and running header
  // keep their values from common/settings.typ.
  theme: (
    main: rgb("#19a3f3"),
  ),
  copyright: copyright-notice(edition: "First printing, July 2026"),
  list-of-figure-title: "List of Figures",
  list-of-table-title: "List of Tables",
  supplement-chapter: "Chapter",
  supplement-part: "Part",
  part-style: 0,
  outline-depth: 4,
)

#include "part1.typ"
#include "part2.typ"
#include "part3-history.typ"

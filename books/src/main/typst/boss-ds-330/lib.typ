// =============================================================================
// Helpers shared by the chapters of the DS-330 book.
//
// The layout of every table of this book is the same as in the other books:
// a plain `table` with a grey header row, wrapped in a `figure` so that it gets
// a number, a caption and an entry in the list of tables.
//
// Parameter tables are dense, so they are set in a smaller font: `ptable` is a
// `table` whose body is set at 8.5pt (captions keep the size of the book).
// =============================================================================
#import "../common/settings.typ": *

// A parameter table. Same arguments as `table`.
#let ptable(..args) = text(size: 8.5pt, table(
  align: left,
  fill: (x, y) => if y == 0 { luma(230) },
  stroke: 0.5pt,
  ..args,
))

// Header row of a `ptable`.
#let head(..cells) = table.header(..cells)

// Figures are breakable by default, which is what lets a long table run over
// two pages. `kfigure` is for the small tables whose caption must stay with the
// table: the block around the figure is not breakable, so both go to the next
// page together.
#let kfigure(..args) = align(center, block(breakable: false, figure(..args)))

// A reference to a chapter of *another part*.
//
// Chapter numbers restart in every part, so a plain `@chap-...` prints "Chapter
// 2" and says nothing about which part it is in. This helper prints the part
// and the chapter, and stays clickable in the PDF.
#let cref(target) = context {
  let first-of(v) = if type(v) == array { v.first() } else { v }
  let chap = first-of(counter(heading).at(target))
  let pt = first-of(counter("part-counter").at(target))
  link(target, [Part #numbering("I", pt), chapter #chap])
}

// An address of the DS-330 address space, as three 7-bit bytes.
#let addr(a) = raw(a)

// A value of a parameter, with its hex form when it is useful.
#let val(v, hex: none) = if hex == none { raw(str(v)) } else { raw(str(v) + " / " + hex) }

// Callout used for a warning about a field whose semantic is not fully covered.
#let caveat(body) = block(
  width: 100%,
  inset: 8pt,
  radius: 3pt,
  fill: luma(245),
  stroke: (left: 2.5pt + rgb("#c8102e")),
  body,
)

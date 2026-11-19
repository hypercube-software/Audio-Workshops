// =============================================================================
// Helpers shared by the chapters of the SE-02 book.
//
// The layout of every table of this book is the same as in the Kurzweil book:
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

// Figures are breakable by default, which is what lets a long table (the list of
// the parameter IDs, for instance) run over two pages. `kfigure` is for the
// small tables whose caption must stay with the table: the block around the
// figure is not breakable, so both go to the next page together.
#let kfigure(..args) = block(breakable: false, figure(..args))

// A reference to a chapter of *another part*.
//
// Chapter numbers restart in every part, so a plain `@chap-...` prints "Chapter
// 2" and says nothing about which part it is in. This helper prints the part
// and the chapter, and stays clickable in the PDF. Both numbers are read from
// the counters, so the text follows the structure of the book.
#let cref(target) = context {
  // `counter.at` returns the raw counter value: an array when the counter has
  // several levels (the heading counter), an integer or a one-element array
  // depending on how the counter was stepped.
  let first-of(v) = if type(v) == array { v.first() } else { v }
  let chap = first-of(counter(heading).at(target))
  let pt = first-of(counter("part-counter").at(target))
  link(target, [Part #numbering("I", pt), chapter #chap])
}

// An address of the SE-02 address space, as four 7-bit bytes.
#let addr(a) = raw(a)

// A value of a parameter, with its hex form when it is useful.
#let val(v, hex: none) = if hex == none { raw(str(v)) } else { raw(str(v) + " / " + hex) }

// One song part, drawn as eight coloured bytes. The colours group the fields:
// pattern (green), bank (blue), patch (orange), repeats (purple), reserved
// (grey). `payload` is the eight-byte string, for example
// "04 0A 02 5C 07 00 00 00".
#let song-part(payload) = {
  let b = payload.split(" ")
  bytefield(
    bpr: 64,
    bytes(1, fill: l_green)[#b.at(0)],
    bytes(1, fill: l_green)[#b.at(1)],
    bytes(1, fill: l_blue)[#b.at(2)],
    bytes(1, fill: l_orange)[#b.at(3)],
    bytes(1, fill: l_purple)[#b.at(4)],
    bytes(3, fill: l_gray)[#b.slice(5).join(" ")],
  )
}

// One pattern step, drawn as eight coloured bytes. The colours group the
// fields: note switch and note number (green), gate (blue), glide (purple),
// PRM switch, parameter ID and value (orange). `payload` is the eight-byte
// string, for example "01 24 54 00 00 19 06 0D".
#let pattern-step(payload) = {
  let b = payload.split(" ")
  bytefield(
    bpr: 64,
    bytes(2, fill: l_green)[#b.slice(0, 2).join(" ")],
    bytes(1, fill: l_blue)[#b.at(2)],
    bytes(1, fill: l_purple)[#b.at(3)],
    bytes(1, fill: l_orange)[#b.at(4)],
    bytes(1, fill: l_orange)[#b.at(5)],
    bytes(2, fill: l_orange)[#b.slice(6, 8).join(" ")],
  )
}

// Callout used for a warning about a field whose semantic is not fully covered.
#let caveat(body) = block(
  width: 100%,
  inset: 8pt,
  radius: 3pt,
  fill: luma(245),
  stroke: (left: 2.5pt + rgb("#c8102e")),
  body,
)

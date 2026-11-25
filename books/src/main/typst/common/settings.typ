// =============================================================================
// Shared settings for every book of the Audio-Workshops documentation.
//
// The Typst project root is books/src/main/typst. Each book lives in its own
// sub-directory and only keeps what is specific to it:
//
//   books/src/main/typst/
//     common/                 this file, the template and the shared artwork
//     kurzweil/               book "Inside V.A.S.T data model", entry point main.typ
//     roland-se-02/           book "Roland SE-02 Sysex format", entry point main.typ
//     akai-mpk261/            book "AKAI MPK-261 Sysex and Memory Reference", entry point main.typ
//
// A book entry point reuses this file and only overrides what differs:
//
//   #import "../common/settings.typ": *
//
//   #show: init.with(
//     title: "My book",
//     theme: (
//       main: rgb("#19a3f3"),                // accent colour
//       cover-background: rgb("#e1f5fe"),    // title band of the cover page
//       heading-background: rgb("#e1f5fe"),  // title band of the chapters
//       header-background: rgb("#f5f5f5"),   // running page header
//     ),
//   )
//   #include "part1.typ"
//
// Always compile with an explicit root, otherwise Typst refuses to read the
// files of "common" (it never reads outside of the project root):
//
//   typst compile --root books/src/main/typst \
//     books/src/main/typst/kurzweil/main.typ kurzweil-sysex-guide.pdf
// =============================================================================
#import "@preview/bytefield:0.0.8": *
#import "@preview/codly:1.3.0": *
#import "@preview/codly-languages:0.1.1": *
#import "@preview/rivet:0.3.1": *

#import "template/lib.typ": (
  appendices, book, chapter, chapter-banners, corollary, definition, example, exercise, index, make-index, my-bibliography, notation,
  part, part-background-state, part-change, problem, proposition, remark, scr, theorem, update-heading-image, vocabulary,
)

// -----------------------------------------------------------------------------
// Artwork shared by the books, in three colour variants. Files are produced by
// books/src/main/generator/GenerateSvgBackgrounds.java during the initialize phase of
// books/pom.xml, and sized to the real print area: A4 for the cover, 21 x 9.4 cm
// for the banners. `svg.<theme>.seed` in books/pom.xml gives each variant its own
// grid, `svg.<theme>.*` its colours.
//
// Those paths are resolved relative to THIS file, so a book can reuse them whatever
// its own sub-directory is. A book which needs its own picture simply passes another
// `image(..)` to `init` (cover:) or to `chapter` (image:).
#let blue-book-cover = image("assets/generated/blue/cover.svg")
#let blue-part-background = image("assets/generated/blue/part.svg")
#let blue-chapter-banner = image("assets/generated/blue/banner-hexagon.svg")
#let blue-outline-banner = image("assets/generated/blue/banner-wave.svg")
#let blue-bands-banner = image("assets/generated/blue/banner-bands.svg")

#let red-book-cover = image("assets/generated/red/cover.svg")
#let red-part-background = image("assets/generated/red/part.svg")
#let red-chapter-banner = image("assets/generated/red/banner-hexagon.svg")
#let red-outline-banner = image("assets/generated/red/banner-wave.svg")
#let red-bands-banner = image("assets/generated/red/banner-bands.svg")

#let green-book-cover = image("assets/generated/green/cover.svg")
#let green-part-background = image("assets/generated/green/part.svg")
#let green-chapter-banner = image("assets/generated/green/banner-hexagon.svg")
#let green-outline-banner = image("assets/generated/green/banner-wave.svg")
#let green-bands-banner = image("assets/generated/green/banner-bands.svg")

// Backward-compatible names used by the Kurzweil book.
#let book-cover = blue-book-cover
#let chapter-banner = blue-chapter-banner
#let outline-banner = blue-outline-banner
#let bands-banner = blue-bands-banner

// Chapter banners cycled by `chapter` when no `image:` is given, one set per colour.
#let blue-banners = (blue-chapter-banner, blue-outline-banner, blue-bands-banner)
#let red-banners = (red-chapter-banner, red-outline-banner, red-bands-banner)
#let green-banners = (green-chapter-banner, green-outline-banner, green-bands-banner)

// -----------------------------------------------------------------------------
// Default colours, overridable per book with `init.with(theme: (..))`.
// A book only overrides the keys it cares about, the others keep the values
// below because the given dictionary is merged on top of this one.
//
//   main                accent colour (links, heading numbers, part pages...)
//   cover-background    background of the title band of the cover page,
//                       `auto` derives it from `main` (lighten 70%)
//   heading-background  background of the chapter title band
//   header-background   background of the running page header,
//                       `none` keeps the header transparent
// -----------------------------------------------------------------------------
#let default-theme = (
  main: rgb("#19a3f3"),
  cover-background: auto,
  heading-background: rgb("#FFFFFFAA"),
  header-background: none,
)

// Standard copyright block shared by the books.
// `edition` is the optional printing note at the end of the block (none = hidden).
#let copyright-notice(edition: none) = [
  #link("https://github.com/hypercube-software", "HYPERCUBE-SOFTWARE")

  Licensed under the Apache 2.0 License (the “License”).
  You may not use this file except in compliance with the License. You may obtain a copy of
  the License at https://www.apache.org/licenses/LICENSE-2.0. Unless required by
  applicable law or agreed to in writing, software distributed under the License is distributed on an
  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and limitations under the License.

  #if edition != none { emph(edition) }
]

// Light colours for bytefield cells. One shade per hue.
#let l_gray = gray.lighten(55%)
#let l_blue = blue.lighten(55%)
#let l_orange = orange.lighten(55%)
#let l_green = green.lighten(55%)
#let l_purple = purple.lighten(55%)
#let l_red = red.lighten(55%)
#let l_yellow = yellow.lighten(55%)

// Code block without line numbers: #nonum[` ... `]. Resets the codly number format afterwards.
#let nonum(body) = {
  codly(number-format: none)
  body
  codly(number-format: numbering.with("1"))
}

// Block that keeps the next paragraph indented (Typst only indents paragraphs that follow a paragraph).
#let kblock(body) = {
  block(breakable: false, above: 1.2em, below: 1.2em, body)
  box(width: 0pt)
}

// -----------------------------------------------------------------------------
// Document entry point of a book: `#show: init.with(title: ..)`.
// Everything which is not listed here keeps the defaults of `template/book`,
// the only mandatory argument is the document itself, given by the show rule.
// -----------------------------------------------------------------------------
#let init(
  doc,
  title: "Untitled",
  subtitle: "",
  author: (),
  date: datetime.today,
  lang: "en",
  theme: (:),
  cover: book-cover,
  image-index: outline-banner,
  banners: (),
  part-background: none,
  copyright: none,
  list-of-figure-title: "List of Figures",
  list-of-table-title: "List of Tables",
  supplement-chapter: "Chapter",
  supplement-part: "Part",
  part-style: 0,
  outline-depth: 4,
) = {
  // Book specific colours on top of the shared defaults
  let palette = default-theme + theme

  // Banners cycled by `chapter`: the chapter number picks one, so a book only
  // declares its palette (`blue-banners`, `red-banners`, `green-banners`).
  chapter-banners.update(x => banners)
  part-background-state.update(x => part-background)

  show: codly-init.with()
  codly(
    enabled: true,
    fill: luma(230),
    zebra-fill: none,
    stroke: 1pt + luma(200),
  )

  show: book.with(
    title: title,
    subtitle: subtitle,
    date: date,
    author: author,
    main-color: palette.main,
    cover-background: palette.cover-background,
    heading-background: palette.heading-background,
    header-background: palette.header-background,
    lang: lang,
    cover: cover,
    image-index: image-index,
    list-of-figure-title: list-of-figure-title,
    list-of-table-title: list-of-table-title,
    supplement-chapter: supplement-chapter,
    supplement-part: supplement-part,
    part-style: part-style,
    outline-depth: outline-depth,
    copyright: copyright,
    lowercase-references: false,
  )

  // Force footer init afeter template init
  set page(
    footer: context {
      if here().page() > 1 {
        // Pour éviter le numéro sur la page de garde
        // Part pages paint a dark background: their page number goes white.
        let on-part = part-change.at(here()) and part-background-state.at(here()) != none
        align(center, text(fill: if on-part { white } else { black }, counter(page).display()))
      }
    },
  )

  // Custom thmbox
  let solution(name: none, body) = {
    context {
      thmbox(
        "solution",
        "Solution",
        stroke: (left: 4pt + green),
        radius: 0em,
        inset: 0.65em,
        namefmt: x => [*--- #x.*],
        separator: h(0.2em),
        titlefmt: x => text(fill: green, weight: "bold", x),
        fill: green.lighten(90%),
        base_level: 1,
      )(name: name, body)
    }
  }

  doc
}

#let binary_block(title, size, fill_color) = {
  rect(
    width: 100%,
    height: 40pt,
    fill: fill_color,
    stroke: 1pt + black,
    align(center + horizon)[*#title* \ #size],
  )
}
#let binary_spacer(title, fill_color) = {
  rect(
    width: 100%,
    height: 70pt,
    fill: fill_color,
    stroke: 1pt + black,
    align(center + horizon)[*#title*],
  )
}

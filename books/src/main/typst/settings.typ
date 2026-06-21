#import "@preview/bytefield:0.0.8": *
#import "@preview/codly:1.3.0": *
#import "@preview/codly-languages:0.1.1": *
#import "@preview/rivet:0.3.1": *

#import "template/lib.typ": (
    book,
    part,
    chapter,
    my-bibliography,
    appendices,
    make-index,
    index,
    theorem,
    definition,
    notation,
    remark,
    corollary,
    proposition,
    example,
    exercise,
    problem,
    vocabulary,
    scr,
    update-heading-image,
)

#let init(doc) = {
    show: codly-init.with()
    codly(
        enabled: true,
        fill: luma(230),  
        zebra-fill: none,
        stroke: 1pt + luma(200) 
    )

    //#set text(font: "Linux Libertine")
    //#set text(font: "TeX Gyre Pagella")
    //#set text(font: "Lato")
    //#show math.equation: set text(font: "Fira Math")
    //#show math.equation: set text(font: "Lato Math")
    //#show raw: set text(font: "Fira Code")

    show: book.with(
        title: "Inside V.A.S.T Data model",
        subtitle: "MIDI Development Guide",
        date: datetime.today,
        author: "2026",
        main-color: rgb("#19a3f3"),
        lang: "en",
        cover: image("./assets/bg-2481-3507.svg"),
        image-index: image("./assets/hd-2481-500.svg"),
        list-of-figure-title: "List of Figures",
        list-of-table-title: "List of Tables",
        supplement-chapter: "Chapter",
        supplement-part: "Part",
        part-style: 0,
        copyright: [
            Copyright © 2023 Flavio Barisi

            PUBLISHED BY PUBLISHER

            #link("https://github.com/flavio20002/typst-orange-template", "TEMPLATE-WEBSITE")

            Licensed under the Apache 2.0 License (the “License”).
            You may not use this file except in compliance with the License. You may obtain a copy of
            the License at https://www.apache.org/licenses/LICENSE-2.0. Unless required by
            applicable law or agreed to in writing, software distributed under the License is distributed on an
            “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
            See the License for the specific language governing permissions and limitations under the License.

            _First printing, July 2023_
        ],
        lowercase-references: false,
    )

    // Forcez le pied de page APRES l'initialisation du template :
    set page(
        footer: context {
            if here().page() > 1 {
                // Pour éviter le numéro sur la page de garde
                align(center, counter(page).display())
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

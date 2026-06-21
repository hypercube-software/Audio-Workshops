#import "settings.typ": *
#part("The K2600 format") 

#let binary_block(title, size, fill_color) = {
  rect(
    width: 100%,
    height: 40pt,
    fill: fill_color,
    stroke: 1pt + black,
    align(center + horizon)[*#title* \ #size]
  )
}
#let binary_spacer(title, fill_color) = {
  rect(
    width: 100%,
    height: 70pt,
    fill: fill_color,
    stroke: 1pt + black,
    align(center + horizon)[*#title*]
  )
}


#chapter("Overview", image: image("./assets/hd3-2481-500.svg"), l: "chap1")
#index("Sectioning")

== Why this document?

I had the oportunity to acquire a K2600R in 2026 without knowing pretty much anything about V.A.S.T. To my suprise, I didn't found a single software able to send patches directly via MIDI (with or without samples). Instead, I was required to use the infamous floppy drive or an emulator like the Gotek. 

After a little bit of research I finally found the official specification of the format in the form of .H files, provided by Kurzweil. Unfortunately they are far from complete so I decided to dig into the subject to find my way in all of this.

Long story short, I discover the disk format (PRAM#footnote[PRAM stands for "Program RAM" apparently]) files) use a "compact" version of the MIDI format: some fields are optionals whereas in MIDI they are required. So it is really possible to convert K26 patches and send them to the unit.

My main goal is to be able to send a Kurzweil patch in one single click, inside my software MPM (Midi Preset Manager#footnote[MPM is part of an Audio Workshop in Java which can be found here: https://hypercube-software.github.io/Audio-Workshops/])

== PRAM format

== Endianness

The PRAM format is Big Endian

=== Header

The `samples offset` point to the footer. The precise sample position in the file is given by *samples offset + (sample start \* 2)*, since the bit depth is always 16 bits and sample start is expressed in sample instead of bytes.

#stack(
  dir: ttb,
  spacing: 2pt,
  
  binary_block("Magic: 'PRAM'", "4 bytes", rgb("#e1f5fe")),
  binary_block("samples offset", "4 bytes", rgb("#fcb3b3")),
)

== Block content

The PRAM file content is made of multiple blocks. For some unknown reason the block size is *NEGATIVE* and include itself. So to get the block size from \$FFFFFFA4 you have to do Abs(\$FFFFFFA4)-4 = Abs(-92)-4= 88

#stack(
  dir: ttb,
  spacing: 2pt,
  
  binary_block("block 1 size", "4 bytes", rgb("#b3fcb9")),
  binary_block("block 1 content", "... bytes", rgb("#61be69")),
  binary_block("block 2 size", "4 bytes", rgb("#b3fcb9")),
  binary_block("block 2 content", "... bytes", rgb("#61be69")),
  binary_spacer("...", rgb("#ffffff")),
)

Each block contains multiple kurzweil objects.

#stack(
  dir: ttb,
  spacing: 2pt,
  
 grid(
    columns: (50%, 50%),
    binary_block("Object 1 identifier (compacted)", "2 bytes", rgb("#ffffff")),
    binary_block("Object 1 size", "2 bytes", rgb("#ffffff")),
  ),
  binary_block("Object 1 content", "... bytes", rgb("#8cddd0")),
 grid(
    columns: (50%, 50%),
    binary_block("Object 2 identifier (compacted)", "2 bytes", rgb("#ffffff")),
    binary_block("Object 2 size", "2 bytes", rgb("#ffffff")),
  ),
  binary_block("Object 2 content", "... bytes", rgb("#8cddd0")),
  binary_spacer("...", rgb("#ffffff")),
)

=== Object types

Kurzweil uses an object oriented architecture in their devices. So, things are very familar for any OOP developers:
- They have types
- They have objects which are instances of a given type
- An object have a unique identifier inside a given type
So don't think a second that an identifier *200* points to a unique object. You need the type identifier to know what you are pointing to. This is why type identifiers and object identifiers work together.

#block(breakable: false)[
The official specification *`objtypes.h`* state that the number of possible objects depends on the type:
- Types from 96 to 115 use 8 bits object identifiers, so you can have 256 objects
- Types from 132 to 159 use 10 bits objects identifiers, so you can have 1024 objects
- Some types identifiers are simply not used
]

Each type correspond to various .H files provided by Kurzweil. unfortunately, some parts are not clear. We are going to dig into that in next chapters...

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => 
    if y == 0 { luma(68.49%) } 
    else if y >= 19 { rgb("#ccd9ee")}
    else  { rgb("#cceee1")},
    stroke: 0.5pt,
    
    table.header([*Type*], [*Enum*], [*Description*]),
    
    [97], [INDEX], [Index type],
    [98], [X_CODE], [Executable code type],
    [100], [TABLE], [Miscellaneous table type],
    [101], [LFO_SHAPE], [LFO shape table (unused)],
    [102], [ALG_DESC], [C&H wiring diagram type],
    [103], [INTONATION_TABLE], [Intonation table type],
    [104], [VELOCITY_MAP], [Velocity map type],
    [105], [PRESSURE_MAP], [Pressure map type],
    [106], [EDIT], [Editor table type],
    [107], [EDIT_MENU], [Edit menu type],
    [108], [MENU_GROUP], [Edit menu group type],
    [109], [MENU_ENTRY_LIST], [Menu entry list type],
    [110], [P_LIST], [PList type],
    [111], [B_LIST], [BList type],
    [112], [SONG], [Song type],
    [113], [EFFECT], [Effect type],
    [114], [SHAPE], [New LFO shape function type],
    [115], [F_PROC], [Function proc type],
    [132], [PROGRAM], [Program type],
    [133], [KEYMAP], [Keymap type],
    [134], [SOUND_BLOCK], [Sound block type],
    [135], [SETUP], [Setup type],
    [136], [MARGE], [Marge type],
    [140], [STUDIO], [Studio type ⚠️Not explicitly defined in objtypes.h],
  ),
  caption: [List of well known object types],
)

⚠️ The K2600R Musician's reference states that type *113* is a studio: this is wrong, it is *140* (chapter 7-7).

=== PRAM Objects

The object identifiers used in PRAM Files are made of two fields packed in 16 bits:
- The *Object Type*. Its unpacked size should be 8 bits
- The *Object ID*.

#block(breakable: false)[
*When the highest bit is 1*:
- The Object Type is unpacked from 5 bits to 8 bits. Its highest bit is 1
- The Object ID is made of 10 bits

#bytefield(
    bpr: 16,
    bits(1,fill: red.lighten(30%))[1],
    bits(5,fill: red.lighten(30%))[5 bits Type],
    bits(10,fill: blue.lighten(30%))[10 bits Object ID],
)
]

#block(breakable: false)[
*When the highest bit is 0*:
- The Object Type is unpacked from 7 bits to 8 bits. Its highest bit is 0
- The Object ID is made of 8 bits

#bytefield(
    bpr: 16,
    bits(1,fill: red.lighten(30%))[0],
    bits(7,fill: red.lighten(30%))[7 bits Type],
    bits(8,fill: blue.lighten(30%))[8 bits Object ID],
)
]

Here two examples to illustrate what's going on. Pay attention to *134*, how it is forged. We read 00110 and set the bit 7 to get the unpacked value 10000110.

#figure(
table(
    columns: 5,
    align: left + horizon,  
    [Input Hexa],
    [Input Binary],
    [Object ID Size],
    [Object Type],
    [Object ID],
    [\$98C8],
    [10011000 11001000],
    [10 bits],
    [134 = 10000110],
    [200 = 0011001000],
    [\$7130],
    [01110001 00110000],
    [8 bits],
    [113 = 01110001],
    [48 = 00110000],
    )
    ,
caption: [Examples of packed identifiers],  
) <figure>



== Footer

The Footer contains 16 bits PCM samples (in big endian as usual). Sample rate may vary.

#stack(
  dir: ttb,
  spacing: 2pt,
  
  binary_block("PCM samples", "... bytes", rgb("#44a8fa")),

)


#chapter("Objects", image: image("./assets/hd3-2481-500.svg"), l: "chap1")
#index("Sectioning")

== Intonnation tables

They are defined in *`itbl.h`*, it's just an array of 12 words of 16 bits. 
Intonnation tables are not really interesting per say, BUT, some of them are used to provide very usefull information. The intonation tables 18 to 22 indicate which version of objects are installed.

#figure(
  table(
    align: left,
    columns: (auto, auto, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    
    table.header([*ID*], [*Name*], [*Description*]),
    
    [18], [`Obj B1.00`], [Base ROM objects version],
    [19], [`Obj O1.00`], [Contemporary objects version],
    [20], [`Obj C1.00`], [Orchestral objects version],
    [21], [`Obj P1.00`], [Stereo Grand objects version],
    [22], [`J`], [OS version (J = Janis, C = Calvin)\
    K2000 models use OS 1.x and 2.x based on ASIC chip called "Janis"\
    K2500 models use OS 3.x and 4.x based on ASIC chip called "Calvin"],
  ),
  caption: [Kurzweil System Object Versions],
)

== Velocity Maps
== Pressure Maps
== Key Maps
== Tables
== Program

A program is made of multiple segments identified by a tag.

=== Program Common segment
=== Program segment
=== Zone segment
=== ARP segment
=== ASR segment
=== Calvin segment
=== Channel segment
=== EFX segment
=== ENC segment
=== ENV segment
=== FCN segment
=== FCN segment
=== LFO segment
=== MASTER segment
=== FX Root segment
=== FX Part segment
=== FCN segment
=== Hobbes segment
=== Layer segment
=== Hamm segment 1
=== Hamm segment 2
=== Hamm segment 3


#chapter("MIDI protocol", image: image("./assets/hd3-2481-500.svg"), l: "chap1")
#index("Sectioning")

#import "settings.typ": *

#chapter("PRAM Format", image: image("./assets/hd3-2481-500.svg"), l: "chap1")
#index("Sectioning")


This is the format used in K26 files. It contains both settings and samples for the unit.

== Endianness

The PRAM format is Big Endian.

== Header

The `samples offset` points to the footer. The precise sample position in the file is given by *samples offset + (sample
start \* 2)*, since the bit depth is always 16 bits and the sample start is expressed in samples instead of bytes.

#stack(
  dir: ttb,
  spacing: 2pt,

  binary_block("Magic: 'PRAM'", "4 bytes", rgb("#e1f5fe")),
  binary_block("samples offset", "4 bytes", rgb("#fcb3b3")),
)
== Body

The PRAM file content is made of multiple blocks. For some unknown reason, the block size is *NEGATIVE* and includes
itself. So to get the block size from \$FFFFFFA4, you have to compute Abs(\$FFFFFFA4)-4 = Abs(-92)-4 = 88

#stack(
  dir: ttb,
  spacing: 2pt,

  binary_block("block 1 size", "4 bytes", rgb("#b3fcb9")),
  binary_block("block 1 content", "... bytes", rgb("#61be69")),
  binary_block("block 2 size", "4 bytes", rgb("#b3fcb9")),
  binary_block("block 2 content", "... bytes", rgb("#61be69")),
  binary_spacer("...", rgb("#ffffff")),
)

== Block content

=== Overview

Each block contains multiple Kurzweil objects.

#stack(
  dir: ttb,
  spacing: 2pt,

  binary_block("Object 1", "... bytes", rgb("#8cddd0")),
  binary_block("Object 2 ", "... bytes", rgb("#8cddd0")),
  binary_block("Object 3", "... bytes", rgb("#8cddd0")),
  binary_spacer("...", rgb("#ffffff")),
)

=== Generic object structure

Every object has the same header, followed by its data. The header is common to all object types. The data part is
specific to each type.
- `type/ID` is described in @pram-identifiers[chapter]
- `size` includes the header itself
- `offset to data` includes its size
- `name` is described in @object-names[chapter]

#figure(
  bytefield(
    bpr: 80,
    bitheader(
      marker: false,
      angle: 0deg,
      numbers: none,
      0,
      [0],
      8,
      [1],
      16,
      [2],
      24,
      [3],
      32,
      [4],
      40,
      [5],
      48,
      [6],
      56,
      [7],
      64,
      [8],
      72,
      [9],
    ),
    bytes(2, fill: l_gray)[type/ID],
    bytes(2, fill: l_orange)[size],
    bytes(2, fill: l_blue)[offset to data],
    bytes(4, fill: l_green)[name (variable, see below)],
    bytes(10, fill: l_purple)[object data (variable)],
  ),
  caption: [Generic object structure: header, then data],
) <generic-object>

=== Identifiers <pram-identifiers>

The object identifiers used in PRAM files are made of two fields packed into 16 bits:
- The *Object Type*. Its unpacked size should be 8 bits
- The *Object ID*. Its unpacked size should be 10 or 8 bits.
This is very common in PRAM files: they try to compact as much as possible, because the main target was floppy disks

#kblock[
  *When the highest bit is 1*:
  - The Object Type is unpacked from 5 bits to 8 bits. Its highest bit is 1
  - The Object ID is made of 10 bits

  #bytefield(
    bpr: 16,
    bits(1, fill: l_red)[1],
    bits(5, fill: l_red)[5 bits Type],
    bits(10, fill: l_blue)[10 bits Object ID],
  )
]

#kblock[
  *When the highest bit is 0*:
  - The Object Type is unpacked from 7 bits to 8 bits. Its highest bit is 0
  - The Object ID is made of 8 bits

  #bytefield(
    bpr: 16,
    bits(1, fill: l_red)[0],
    bits(7, fill: l_red)[7 bits Type],
    bits(8, fill: l_blue)[8 bits Object ID],
  )
]

Here are two examples to illustrate what is going on. Pay attention to *134* and how it is built. We read 00110 and set
bit 7 to get the unpacked value 10000110.

#figure(
  table(
    columns: 5,
    align: left + horizon,
    [Input Hexa], [Input Binary], [Object ID Size], [Object Type], [Object ID],
    [\$98C8], [10011000 11001000], [10 bits], [134 = 10000110], [200 = 0011001000],
    [\$7130], [01110001 00110000], [8 bits], [113 = 01110001], [48 = 00110000],
  ),
  caption: [Examples of packed identifiers],
) <figure>

=== Object Names <object-names>

The name is a null-terminated C string, padded to an even number of bytes (the word pad):
- max 18 bytes: 16 characters, the null terminator and one pad byte,
- min 2 bytes: one character and the null terminator.

The pad byte is only added when the characters and the null make an odd count. The name `Bass` has four characters, a
null and one pad byte, so it takes 6 bytes:

#figure(
  bytefield(
    bpr: 48,
    bitheader(
      marker: false,
      angle: 0deg,
      numbers: none,
      0,
      [0],
      8,
      [1],
      16,
      [2],
      24,
      [3],
      32,
      [4],
      40,
      [5],
    ),
    bytes(1, fill: l_green)[char 1],
    bytes(1, fill: l_green)[char 2],
    bytes(1, fill: l_green)[char 3],
    bytes(1, fill: l_green)[char 4],
    bytes(1, fill: l_gray)[null],
    bytes(1, fill: white)[pad],
  ),
  caption: [Object name `Bass` encoded into 6 bytes],
) <name-word-pad>

The field `offset to data` is the length of the name + null terminating byte + pad size + 2 (its own size).
- For `Bass` it is 4 + 2 + 2 = 8 (1 byte of pad).
- For `Synth` it is 5 + 1 + 2 = 8 (no pad).

== Footer

The footer contains 16-bit PCM samples (in Big Endian, as usual). The sample rate may vary.

#stack(
  dir: ttb,
  spacing: 2pt,

  binary_block("PCM samples", "... bytes", rgb("#44a8fa")),
)

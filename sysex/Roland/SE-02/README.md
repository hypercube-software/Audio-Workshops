# Roland SE-02

The reference for the layout and the protocol is the book at
`books/src/main/typst/roland-se-02/`.

- `SE-02.mmap`: memory map of the instrument. The SE-02 uses packed 4-byte
  addresses (28 bits), device ID `0x10` and model ID `00 00 00 44`.
- The system settings live at `0x00000000`, the patch edit buffer at
  `0x05000000`, the stored user presets at `0x06nn0000`, the pattern edit buffer
  at `0x10000000`, the stored patterns at `0x11nn0000`, the song edit buffer at
  `0x20000000` and the stored songs at `0x21nn0000`.
- The stored blocks are arrays of `Preset`, `Pattern` and `Song`. These structs
  are padded to 0x4000 bytes, so `preset[128]`, `pattern[128]` and `song[16]`
  place each entry at the book addresses.
- A preset is one `Preset` struct padded to 0x4000: a `NIBBLES`
  `PatchParameters` part (208 wire bytes for 104 logical values), then the
  16-byte `PatchName`, the 16-byte `Reserved16`, then padding. The edit buffers
  and the stored arrays use the same structs.
- The mmap is registered in `Manufacturer.ROLAND` as the `SE-02` device, model
  code `0x44`, with a 4-byte model ID and device ID `0x10`.
- The regions use disjoint addresses, so they all live in the implicit
  `Default` memory space. There is no `-r` option to pass for this device.

```text
read-memory -m "SE-02" -a 0500006E -s 2 -i "<in>" -o "<out>"
read-memory -m "SE-02" -a 06050000 -s 2 -i "<in>" -o "<out>"
```

The `User Presets`, `Stored Patterns` and `Stored Songs` zones are arrays of
padded structs: `Preset preset[128]`, `Pattern pattern[128]` and `Song song[16]`.
Each struct is padded to 0x4000 bytes: the 240 bytes of a preset (144 for a
pattern or a song), then padding. The array stride is therefore 0x4000, which is
exactly the second packed byte (`06 nn 00 00`, `11 nn 00 00`, `21 nn 00 00`).

`read-memory` addresses any stored preset, pattern or song directly. Dumping all
stored entries is the next step: it must iterate the array and request the 240 or
144 bytes, not the whole 0x200000 zone.


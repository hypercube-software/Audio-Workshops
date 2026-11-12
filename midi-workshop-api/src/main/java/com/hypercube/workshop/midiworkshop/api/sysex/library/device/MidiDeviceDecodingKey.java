package com.hypercube.workshop.midiworkshop.api.sysex.library.device;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

/**
 * This class tells how to interpret incoming bits from MIDI and where to place them in the target memory
 * <ul>
 *     <li>We read 8 bytes of packed memory from MIDI</li>
 *     <li>We decode 7 bytes of memory</li>
 * </ul>
 * <p>
 * The target memory is described like this:
 * byte      : AAAAAAAA BBBBBBBB CCCCCCCC ...
 * bit       : 76543210 76543210 76543210 ...
 * <p>
 * Bits use the natural numbering: 7 is the MSB (leftmost), 0 is the LSB (rightmost).
 * So "B2" is bit 2 of the third byte, i.e. the third bit from the right.
 */
@Getter
@Setter
@NoArgsConstructor
public class MidiDeviceDecodingKey {
    public static final int UNMAPPED_BIT = -1;
    private int start;
    private int end;
    private List<String> key;
    private List<Integer> decodingKey;

    public MidiDeviceDecodingKey(int start, int end, String key) {
        this.key = Arrays.stream(key.split("\n"))
                .map(String::trim)
                .toList();
        this.start = start;
        this.end = end;
    }

    /**
     * Given a block of 64 input MIDI bits, the mapping return the target bit index given the input bit index.
     * <ul>
     *     <li>The returned list contains always 64 entries (input bit index)</li>
     *     <li>Each value is in the range [0-55] since we target a block of 56 unpacked bits</li>
     *     <li>Value -1 ({@link #UNMAPPED_BIT}) indicate the input bit is not used, this is because MIDI use only 7 bits
     * </ul>
     */
    public List<Integer> getMapping() {
        if (decodingKey == null) {
            List<Integer> _key = key.stream()
                    .flatMap(l -> Arrays.stream(l.split("\\s")))
                    .map(String::trim)
                    .map(v -> {
                        if (v.equals("0")) {
                            return UNMAPPED_BIT;
                        } else {
                            int byteOffset = v.charAt(0) - 'A';
                            int bit = v.charAt(1) - '0'; // natural bit number: 0 = LSB, 7 = MSB
                            return bit + byteOffset * 8;
                        }
                    })
                    .toList();
            long usedBits = _key.stream()
                    .filter(b -> b != UNMAPPED_BIT)
                    .count();
            if (usedBits != 56) {
                throw new MidiError("Invalid decoding key, should have 56 bits used");
            }
            decodingKey = _key;
        }
        return decodingKey;
    }
}

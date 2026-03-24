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
 * bit index : 01234567 89111111 11112222 ...
 * bit index :            012345 67890123
 * <p>
 * So "B2" is bit index "13". Do not mistake the bit index and the bit number
 */
@Getter
@Setter
@NoArgsConstructor
public class MidiDeviceDecodingKey {
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
     *     <li>Each value are in the range [0-56] since we target a block of 56 unpacked bits</li>
     *     <li>Value -1 indicate the input bit is not used, this is because MIDI use only 7 bits
     * </ul>
     */
    public List<Integer> getMapping() {
        if (decodingKey == null) {
            List<Integer> _key = key.stream()
                    .flatMap(l -> Arrays.stream(l.split("\\s")))
                    .map(String::trim)
                    .map(v -> {
                        if (v.equals("0")) {
                            return -1;
                        } else {
                            int byteOffset = v.charAt(0) - 'A';
                            int bit = v.charAt(1) - '0';   // bit 0 is on the right
                            int bitOffset = (8 - bit) - 1; // bitOffset 0 is on the left
                            return bitOffset + byteOffset * 8;
                        }
                    })
                    .toList();
            if (_key.size() != 64) {
                throw new MidiError("Invalid decoding key, should have 64 bits");
            }
            decodingKey = _key;
        }
        return decodingKey;
    }
}

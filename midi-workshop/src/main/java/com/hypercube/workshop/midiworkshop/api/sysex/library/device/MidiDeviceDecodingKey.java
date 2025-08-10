package com.hypercube.workshop.midiworkshop.api.sysex.library.device;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

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

    public List<Integer> getMapping() {
        if (decodingKey == null) {
            List<Integer> _key = key.stream()
                    .flatMap(l -> Arrays.stream(l.split("\\s")))
                    .map(String::trim)
                    .map(v -> {
                        if (v.equals("0")) {
                            return -1;
                        } else {
                            int offset = v.charAt(0) - 'A';
                            int bit = v.charAt(1) - '0';
                            return bit + offset * 8;
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

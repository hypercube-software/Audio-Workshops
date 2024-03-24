package com.hypercube.workshop.midiworkshop.sysex.device.memory;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives.MemoryInt24;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.map.MemoryMapFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public class DeviceMemory {
    private final String name;
    private final List<MemoryMap> memorySpaces;
    private final Set<Integer> written = new HashSet<>();

    public MemoryMap getMemoryMap(String name) {
        return memorySpaces.stream()
                .filter(m -> m.getName()
                        .equals(name))
                .findFirst()
                .orElseThrow();
    }

    public void writeByte(MemoryInt24 address, int value) {
        var space = memorySpaces.stream()
                .filter(s -> s.contains(address))
                .findFirst()
                .orElseThrow(() -> new MidiError("Memory address not mapped: %s".formatted(address)));
        //Log.info("Write 0x%02X at %s in space %s".formatted(value, address, space));
        if (written.contains(address.value())) {
            throw new MidiError("Already written: %s".formatted(address));
        } else {
            space.writeByte(address, value);
            written.add(address.value());
        }
    }

    public int readByte(MemoryInt24 address) {
        return memorySpaces.stream()
                .filter(s -> s.contains(address))
                .findFirst()
                .map(space -> space.readByte(address))
                .orElseThrow(() -> new MidiError("Memory Address not defined: %s".formatted(address)));
    }

    public String readString(MemoryMapFormat format, MemoryInt24 address, int size) {
        byte[] data;
        if (format == MemoryMapFormat.NIBBLES) {
            data = new byte[size / 2];
            for (int i = 0; i < size; i += 2) {
                int v1 = readByte(address.add(i));
                int v2 = readByte(address.add(i + 1));
                int v = v1 << 4 | v2;
                data[i / 2] = (byte) v;
            }
        } else {
            data = new byte[size];
            for (int i = 0; i < size; i++) {
                data[i] = (byte) readByte(address.add(i));
            }
        }
        if (data[0] == 0) {
            return "";
        } else {
            return new String(data, StandardCharsets.US_ASCII);
        }
    }

}

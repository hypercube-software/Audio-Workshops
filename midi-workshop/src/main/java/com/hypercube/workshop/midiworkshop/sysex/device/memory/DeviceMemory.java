package com.hypercube.workshop.midiworkshop.sysex.device.memory;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives.MemoryInt24;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public class DeviceMemory {
    private final String name;
    private final List<MemoryMap> memoryMaps;
    private final Set<Integer> written = new HashSet<>();

    public MemoryMap getMemoryMap(String name) {
        return memoryMaps.stream()
                .filter(m -> m.getName()
                        .equals(name))
                .findFirst()
                .orElseThrow();
    }

    public void writeByte(MemoryInt24 address, int value) {
        var space = memoryMaps.stream()
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
        return memoryMaps.stream()
                .filter(s -> s.contains(address))
                .findFirst()
                .map(space -> space.readByte(address))
                .orElseThrow(() -> new MidiError("[%s] Memory Address not defined: %s (this happen when the content is bigger than the container. The memory space size is wrong)".formatted(name, address)));
    }
}

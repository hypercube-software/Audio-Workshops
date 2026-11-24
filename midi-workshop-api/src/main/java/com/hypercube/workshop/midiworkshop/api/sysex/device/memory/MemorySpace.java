package com.hypercube.workshop.midiworkshop.api.sysex.device.memory;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * A named group of {@link MemoryMap} zones.
 *
 * <p>A device can expose several memory spaces that share the same addresses:
 * the SysEx command, not the address, selects the space. This is why the
 * space name is the contract between the device parser and the memory map.
 *
 * <p>A memory space is only a list of zones. The zones carry their own address
 * and size, so a space has no range of its own. The list order is the
 * declaration order, which keeps dumps deterministic.
 */
@Getter
@RequiredArgsConstructor
public class MemorySpace {
    /**
     * Name of the implicit memory space used when the mmap declares zones
     * outside any named memory space.
     */
    public static final String DEFAULT_NAME = "Default";

    private final String name;
    private final List<MemoryMap> memoryMaps;

    public MemoryMap getMemoryMap(String name) {
        return memoryMaps.stream()
                .filter(m -> m.getName()
                        .equals(name))
                .findFirst()
                .orElseThrow(() -> new MidiError("Memory zone not found in space %s: %s".formatted(this.name, name)));
    }

    public Optional<MemoryMap> findMemoryMap(MemoryInt28 address) {
        return memoryMaps.stream()
                .filter(m -> m.contains(address))
                .findFirst();
    }

    public boolean contains(MemoryInt28 address) {
        return findMemoryMap(address).isPresent();
    }

    public void writeByte(MemoryInt28 address, int value) {
        MemoryMap memoryMap = findMemoryMap(address)
                .orElseThrow(() -> new MidiError("Memory address not mapped in space %s: %s".formatted(name, address)));
        memoryMap.writeByte(address, value);
    }

    public int readByte(MemoryInt28 address) {
        return findMemoryMap(address)
                .map(m -> m.readByte(address))
                .orElseThrow(() -> new MidiError("Memory address not mapped in space %s: %s".formatted(name, address)));
    }

    /**
     * Read one logical value from the space, taking the zone format into
     * account (a NIBBLES value is rebuilt from two consecutive nibbles).
     */
    public int readValueByte(MemoryInt28 address) {
        return findMemoryMap(address)
                .map(m -> m.readValueByte(address))
                .orElseThrow(() -> new MidiError("Memory address not mapped in space %s: %s".formatted(name, address)));
    }

    @Override
    public String toString() {
        return name;
    }
}

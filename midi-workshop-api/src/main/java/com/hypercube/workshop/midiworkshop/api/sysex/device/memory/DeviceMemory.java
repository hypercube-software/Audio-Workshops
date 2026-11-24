package com.hypercube.workshop.midiworkshop.api.sysex.device.memory;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Getter
@Slf4j
public class DeviceMemory {
    private final String name;
    private final List<MemorySpace> memorySpaces;
    private final Set<Integer> written = new HashSet<>();

    /**
     * @return all the zones of all the memory spaces, in declaration order
     */
    public List<MemoryMap> getMemoryMaps() {
        return memorySpaces.stream()
                .flatMap(memorySpace -> memorySpace.getMemoryMaps()
                        .stream())
                .toList();
    }

    public MemorySpace getMemorySpace(String name) {
        return memorySpaces.stream()
                .filter(memorySpace -> memorySpace.getName()
                        .equals(name))
                .findFirst()
                .orElseThrow(() -> new MidiError("Memory space not defined: %s".formatted(name)));
    }

    public boolean hasMemorySpace(String name) {
        return memorySpaces.stream()
                .anyMatch(memorySpace -> memorySpace.getName()
                        .equals(name));
    }

    public List<MemoryMap> getMemoryMaps(String memorySpaceName) {
        return getMemorySpace(memorySpaceName).getMemoryMaps();
    }

    /**
     * Resolve the memory space selected on the command line.
     *
     * <p>When no name is given, the implicit {@link MemorySpace#DEFAULT_NAME}
     * space is accepted if it is the only one. As soon as the mmap declares
     * named spaces, the caller must choose one explicitly: the address alone
     * cannot distinguish overlapping spaces.
     *
     * @param name requested space name, or {@code null}/blank to use the default
     */
    public MemorySpace resolveMemorySpace(String name) {
        if (name != null && !name.isBlank()) {
            return getMemorySpace(name);
        }
        if (memorySpaces.isEmpty()) {
            throw new MidiError("[%s] No memory space defined".formatted(this.name));
        }
        if (memorySpaces.size() == 1 && MemorySpace.DEFAULT_NAME.equals(memorySpaces.get(0)
                .getName())) {
            return memorySpaces.get(0);
        }
        throw new MidiError("[%s] Memory space required, available: %s".formatted(this.name, getMemorySpaceNames()));
    }

    public List<String> getMemorySpaceNames() {
        return memorySpaces.stream()
                .map(MemorySpace::getName)
                .toList();
    }

    public MemoryMap getMemoryMap(String name) {
        return getMemoryMaps().stream()
                .filter(m -> m.getName()
                        .equals(name))
                .findFirst()
                .orElseThrow(() -> new MidiError("Memory zone not defined: %s".formatted(name)));
    }

    public MemoryMap getMemoryMap(String memorySpaceName, String name) {
        return getMemorySpace(memorySpaceName).getMemoryMap(name);
    }

    public void writeByte(MemoryInt28 address, int value) {
        var memorySpace = memorySpaces.stream()
                .filter(candidate -> candidate.contains(address))
                .findFirst()
                .orElseThrow(() -> new MidiError("Memory address not mapped: %s".formatted(address)));
        //log.info("Write {} at {} in memory space {}", "0x%02X".formatted(value), address, memorySpace);
        memorySpace.writeByte(address, value);
        written.add(address.value());
    }

    public void writeByte(String memorySpaceName, MemoryInt28 address, int value) {
        getMemorySpace(memorySpaceName).writeByte(address, value);
        written.add(address.value());
    }

    public int readByte(MemoryInt28 address) {
        return memorySpaces.stream()
                .filter(memorySpace -> memorySpace.contains(address))
                .findFirst()
                .map(memorySpace -> memorySpace.readByte(address))
                .orElseThrow(() -> new MidiError("[%s] Memory Address not defined: %s (this happen when the content is bigger than the container. The memory space size is wrong)".formatted(name, address)));
    }

    public int readByte(String memorySpaceName, MemoryInt28 address) {
        return getMemorySpace(memorySpaceName).readByte(address);
    }

    public Optional<MemorySpace> findMemorySpace(MemoryInt28 address) {
        return memorySpaces.stream()
                .filter(memorySpace -> memorySpace.contains(address))
                .findFirst();
    }
}

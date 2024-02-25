package com.hypercube.workshop.midiworkshop.sysex.model.memory.map;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.model.memory.primitives.MemoryInt24;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent a part of a memory device, consider this as a C Struct
 *
 * <p>This class represent two types of memory location:
 * <ul>
 *     <li>A top level memory location with a real base address
 *     <li>A relative memory location with a base address set to 0x000000*
 * </ul>
 * <p>A relative memory location can be used many times in various fields (it's like a type or a C struct)
 * , {@link MemoryMap#referenceCount} reflect this
 */
@Getter
@Slf4j
public class MemoryMap {
    private final String name;
    private final MemoryInt24 baseAddress;
    private MemoryInt24 size;
    private MemoryInt24 endAddress;
    private int referenceCount;
    private byte[] memory;
    private final List<MemoryField> fields = new ArrayList<>();

    public MemoryMap(String name, MemoryInt24 baseAddress, MemoryInt24 size) {
        this.baseAddress = baseAddress;
        this.name = name;
        setSize(size.value());
    }

    public MemoryMap(String name, MemoryInt24 baseAddress) {
        this.baseAddress = baseAddress;
        this.name = name;
    }

    public void allocateMemory() {
        if (!isTopLevel()) {
            throw new MidiError("Memory allocation is only for top level Memory maps");
        }
        memory = new byte[size.value()];
    }

    public void add(MemoryField memoryField) {
        fields.add(memoryField);
    }

    public boolean isTopLevel() {
        return referenceCount == 0;
    }

    public void incReferenceCount() {
        referenceCount++;
    }

    @Override
    public String toString() {
        return name + " " + baseAddress + " " + size;
    }

    public void setSize(int size) {
        if (this.size != null) {
            throw new MidiError("Size already set in %s: %s".formatted(name, size));
        }
        if (size == 0) {
            throw new MidiError("Size can't be 0");
        }
        this.size = MemoryInt24.from(size);
        this.endAddress = MemoryInt24.from(baseAddress.value() + size - 1);
    }

    public boolean contains(MemoryInt24 address) {
        return endAddress != null && address.value() >= baseAddress.value() && address.value() <= endAddress.value();
    }

    public void writeByte(MemoryInt24 address, int value) {
        int relativeAddress = address.value() - baseAddress.value();
        memory[relativeAddress] = (byte) value;
    }

    public int readByte(MemoryInt24 address) {
        int relativeAddress = address.value() - baseAddress.value();
        return memory[relativeAddress];
    }
}

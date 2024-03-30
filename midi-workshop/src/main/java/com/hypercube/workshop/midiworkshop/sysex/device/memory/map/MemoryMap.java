package com.hypercube.workshop.midiworkshop.sysex.device.memory.map;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives.MemoryInt24;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent a part of a memory device, consider this as a C Struct
 *
 * <p>This class represent two types of memory location:
 * <ul>
 *     <li>A top level memory location with a real base address
 *     <li>A relative memory location with a base address set to 0x000000
 * </ul>
 * <p>A relative memory location can be used many times in various fields (it's like a type or a C struct)
 * , {@link MemoryMap#referenceCount} reflect this
 */
@Getter
@Slf4j
public class MemoryMap {
    /**
     * Nme of the memory
     */
    private final String name;
    /**
     * Start address of the memory
     */
    private final MemoryInt24 baseAddress;
    /**
     * Is it struct or top level memory zone
     */
    private final MemoryMapType type;
    /**
     * Content of the memory
     */
    private final List<MemoryField> fields = new ArrayList<>();
    /**
     * Size of the memory in bytes (not in nibbles!)
     * <p>Use {@link #getEffectiveSize()} to get the real size
     */
    private MemoryInt24 size;
    /**
     * End address based on the {@link #getEffectiveSize()} (take into account the {@link #format}
     */
    private MemoryInt24 endAddress;
    /**
     * How many child struct are used by this memory
     */
    private int referenceCount;
    /**
     * Allocated virtual memory
     */
    private byte[] memory;
    /**
     * If format is {@link MemoryMapFormat#NIBBLES} then {@link #getEffectiveSize()} is {@link #size} * 2
     */
    @Setter
    private MemoryMapFormat format;

    public MemoryMap(MemoryMapType type, String name, MemoryInt24 baseAddress, MemoryInt24 size, MemoryMapFormat format) {
        this.baseAddress = baseAddress;
        this.name = name;
        this.type = type;
        this.format = format;
        setSize(size.value());
    }

    public MemoryMap(MemoryMapType type, String name, MemoryInt24 baseAddress, MemoryMapFormat format) {
        this.baseAddress = baseAddress;
        this.name = name;
        this.type = type;
        this.format = format;
    }

    /**
     * @return Take into account the {@link #format} to get the real size in bytes
     */
    public MemoryInt24 getEffectiveSize() {
        return MemoryInt24.from(switch (format) {
            case BYTES -> size.value();
            case NIBBLES -> size.value() * 2;
        });
    }

    /**
     * Set the field {@link #memory} according to {@link #getEffectiveSize()}
     */
    public void allocateMemory() {
        if (!isTopLevel()) {
            throw new MidiError("Memory allocation is only for top level Memory maps");
        }
        memory = new byte[getEffectiveSize().value()];
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
        return name + " " + baseAddress + " " + size + " " + format;
    }

    /**
     * Set the size of the memory in bytes (not in nibbles!)
     *
     * @param size in {@link MemoryMapFormat#BYTES}
     */
    public void setSize(int size) {
        if (this.size != null) {
            throw new MidiError("Size already set in %s: %s".formatted(name, size));
        }
        if (size == 0) {
            throw new MidiError("Size can't be 0");
        }
        this.size = MemoryInt24.from(size);
        updateEndAddress();
    }

    /**
     * Update the end address is {@link #size} and {@link #format} are set
     */
    public void updateEndAddress() {
        if (size != null && format != null) {
            this.endAddress = MemoryInt24.from(baseAddress.value() + getEffectiveSize().value() - 1);
        }
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
        return memory[relativeAddress] & 0xFF;
    }
}

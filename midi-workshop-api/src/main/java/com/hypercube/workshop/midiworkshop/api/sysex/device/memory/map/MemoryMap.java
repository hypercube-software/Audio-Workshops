package com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
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
    private final MemoryInt28 baseAddress;
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
    private MemoryInt28 size;
    /**
     * End address based on the {@link #getEffectiveSize()} (take into account the {@link #format}
     */
    private MemoryInt28 endAddress;
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
    /**
     * True when the device only answers a request for the full zone.
     *
     * <p>Some regions, such as the Roland bulk dump, have no addressable
     * sub-part: the request must carry the exact zone size or the device
     * rejects it. The {@code MONOLITH} keyword of the mmap declares this.
     */
    @Setter
    private boolean monolithic;

    public MemoryMap(MemoryMapType type, String name, MemoryInt28 baseAddress, MemoryInt28 size, MemoryMapFormat format) {
        this.baseAddress = baseAddress;
        this.name = name;
        this.type = type;
        this.format = format;
        setSize(size);
    }

    public MemoryMap(MemoryMapType type, String name, MemoryInt28 baseAddress, MemoryMapFormat format) {
        this.baseAddress = baseAddress;
        this.name = name;
        this.type = type;
        this.format = format;
    }

    /**
     * @return Take into account the {@link #format} to get the real size in bytes
     */
    public MemoryInt28 getEffectiveSize() {
        return MemoryInt28.from(switch (format) {
            case BYTES -> size.value();
            case NIBBLES -> size.value() * 2;
        }, size.byteCount());
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
        return name + " " + baseAddress + " " + size + " " + format + (monolithic ? " MONOLITH" : "");
    }

    /**
     * Set the size of the memory in bytes (not in nibbles!)
     *
     * <p>The size is encoded with the same number of MIDI bytes as the base address,
     * which is the convention used by the SysEx requests.
     *
     * @param size in {@link MemoryMapFormat#BYTES}
     */
    public void setSize(int size) {
        setSize(MemoryInt28.from(size, baseAddress.byteCount()));
    }

    /**
     * Set the size of the memory, keeping its declared MIDI width.
     *
     * @param size in {@link MemoryMapFormat#BYTES}
     */
    public void setSize(MemoryInt28 size) {
        if (this.size != null) {
            throw new MidiError("Size already set in %s: %s".formatted(name, size));
        }
        if (size.value() == 0) {
            throw new MidiError("Size can't be 0");
        }
        this.size = size;
        updateEndAddress();
    }

    /**
     * Update the end address is {@link #size} and {@link #format} are set
     */
    public void updateEndAddress() {
        if (size != null && format != null) {
            this.endAddress = MemoryInt28.from(baseAddress.value() + getEffectiveSize().value() - 1,
                    baseAddress.byteCount());
        }
    }

    public boolean contains(MemoryInt28 address) {
        return endAddress != null && address.value() >= baseAddress.value() && address.value() <= endAddress.value();
    }

    public void writeByte(MemoryInt28 address, int value) {
        int relativeAddress = address.value() - baseAddress.value();
        memory[relativeAddress] = (byte) value;
    }

    public int readByte(MemoryInt28 address) {
        int relativeAddress = address.value() - baseAddress.value();
        return memory[relativeAddress] & 0xFF;
    }

    /**
     * Read one logical value from the map, taking the field format into
     * account.
     *
     * <p>For {@link MemoryMapFormat#BYTES} this is a plain byte. For
     * {@link MemoryMapFormat#NIBBLES} the value is rebuilt from the two
     * consecutive nibbles stored at the address. The format is resolved from
     * the deepest field containing the address, so a composite struct can mix
     * NIBBLES and BYTES children.
     */
    public int readValueByte(MemoryInt28 address) {
        MemoryField field = findField(address);
        MemoryMapFormat valueFormat = field == null ? format : field.getParent()
                .getFormat();
        return switch (valueFormat) {
            case BYTES -> readByte(address);
            case NIBBLES -> (readByte(address) << 4) | readByte(address.add(1));
        };
    }

    private MemoryField findField(MemoryInt28 address) {
        for (MemoryField field : fields) {
            int start = field.getAddress()
                    .value();
            int fieldSize = field.getEffectiveTotalSize()
                    .value();
            if (address.value() < start || address.value() >= start + fieldSize) {
                continue;
            }
            if (!field.isReference()) {
                return field;
            }
            int offset = address.value() - start;
            if (field.isArray()) {
                int elementSize = field.getReference()
                        .getEffectiveSize()
                        .value();
                offset = offset % elementSize;
            }
            return field.getReference()
                    .findField(MemoryInt28.from(offset));
        }
        return null;
    }
}

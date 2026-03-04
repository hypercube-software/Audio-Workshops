package com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map;

import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryArray;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryEnum;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt24;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Describe part of a {@link MemoryMap}
 * <ul>
 *     <li>A MemoryField have a type (String, Byte or a custom Enum)
 *     <li>If it is an array, in this case the {@link MemoryField#array} is not null
 *     <li>If it is enum, in this case the {@link MemoryField#enumReference} is not null
 *     <li>If it is set of bytes from another {@link MemoryMap}, in this case the {@link MemoryField#reference} is not null
 * </ul>
 * <p>Note: {@link MemoryField#getSize()} != {@link MemoryField#getTotalSize()} in case of arrays
 */
@Getter
@RequiredArgsConstructor
public class MemoryField {
    private final MemoryMap parent;
    private final int line;
    private final String name;
    private final String type;
    private final MemoryArray array;
    @Setter
    private MemoryInt24 size;
    @Setter
    private MemoryMap reference;
    @Setter
    private MemoryInt24 address;
    @Setter
    private MemoryEnum enumReference;

    public boolean isArray() {
        return array != null;
    }

    public boolean isReference() {
        return reference != null;
    }

    public boolean isString() {
        return type.equals("String");
    }

    public boolean isEnum() {
        return enumReference != null;
    }

    /**
     * @return Take into account the {@link MemoryMapFormat} to get the real size in bytes
     */
    public MemoryInt24 getEffectiveSize() {
        return MemoryInt24.from(switch (parent.getFormat()) {
            case BYTES -> size.value();
            case NIBBLES -> size.value() * 2;
        });
    }

    /**
     * @return {@link #getSize()} including the array size if available
     */
    public MemoryInt24 getTotalSize() {
        if (array != null) {
            return MemoryInt24.from(array.size() * size.value());
        }
        return size;
    }

    /**
     * @return {@link #getEffectiveSize()} including the array size if available
     */
    public MemoryInt24 getEffectiveTotalSize() {
        if (array != null) {
            return MemoryInt24.from(array.size() * getEffectiveSize().value());
        }
        return getEffectiveSize();
    }

    @Override
    public String toString() {
        String typeName = name != null ? type + " " + name : type;
        if (array != null) {
            return typeName + array;
        }
        return typeName;
    }


}

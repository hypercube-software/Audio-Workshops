package com.hypercube.workshop.midiworkshop.sysex.model.memory.map;

import com.hypercube.workshop.midiworkshop.sysex.model.memory.primitives.MemoryArray;
import com.hypercube.workshop.midiworkshop.sysex.model.memory.primitives.MemoryEnum;
import com.hypercube.workshop.midiworkshop.sysex.model.memory.primitives.MemoryInt24;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Describe part of a {@link MemoryMap}
 * <ul>
 *     <li>A MemoryField have a type (String, Byte or a custom Enum)
 *     <li>If it is an array, in this case the {@link MemoryField#array} is not null
 *     <li>If it is enum, in this case the {@link MemoryField#memoryEnum} is not null
 *     <li>If it is set of bytes from another {@link MemoryMap}, in this case the {@link MemoryField#reference} is not null
 * </ul>
 * <p>Note: {@link MemoryField#getSize()} != {@link MemoryField#getTotalSize()} in case of arrays
 */
@Getter
@RequiredArgsConstructor
public class MemoryField {
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
    private MemoryEnum memoryEnum;

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
        return memoryEnum != null;
    }

    public MemoryInt24 getTotalSize() {
        if (array != null) {
            return MemoryInt24.from(array.size() * size.value());
        } else {
            return size;
        }
    }

    @Override
    public String toString() {
        String typeName = isReference() ? name : type + " " + name;
        if (array != null) {
            return typeName + array;
        } else {
            return typeName;
        }
    }


}

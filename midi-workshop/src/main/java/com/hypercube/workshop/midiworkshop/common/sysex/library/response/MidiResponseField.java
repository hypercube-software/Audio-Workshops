package com.hypercube.workshop.midiworkshop.common.sysex.library.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MidiResponseField {
    /**
     * Name of the field. Will appear in {@link MidiResponse}
     */
    private String name;
    /**
     * Which kind of data need to be extracted
     */
    private MidiResponseFieldType type = MidiResponseFieldType.STRING;
    /**
     * What is the unit of the offset
     */
    private MidiResponseUnit unit = MidiResponseUnit.BYTE;
    /**
     * Offset in BYTE or BIT where to extract the value
     */
    private int offset;
    /**
     * For strings, number of characters. Otherwise, number of bits or bytes to read
     */
    private int size;
}

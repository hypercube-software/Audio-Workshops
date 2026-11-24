package com.hypercube.workshop.midiworkshop.api.sysex.device.memory;

/**
 * How the payload of a memory space is addressed on the wire.
 *
 * <p>Most spaces are addressed by byte offset. Some persistent spaces, such as
 * the AKAI user presets, are addressed by preset number instead: the whole
 * record is requested at once.
 */
public enum MemorySpaceAddressing {
    /**
     * The request carries a byte address and a size.
     */
    BYTE,
    /**
     * The request carries a preset number and returns a whole record.
     */
    PRESET
}

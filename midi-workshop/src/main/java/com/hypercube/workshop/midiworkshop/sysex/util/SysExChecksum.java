package com.hypercube.workshop.midiworkshop.sysex.util;

/**
 * Interface used to implement various Checksum algorithms used in SysEx
 */
public interface SysExChecksum {
    /**
     * Add a new byte to the current checksum
     *
     * @param value 8 bit value
     */
    void update(int value);

    /**
     * Return the checksum
     *
     * @return the checksum on 8 bits
     */
    int getValue();
}

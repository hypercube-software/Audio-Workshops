package com.hypercube.workshop.midiworkshop.sysex.checksum;

import com.hypercube.workshop.midiworkshop.sysex.util.SysExChecksum;

/**
 * This is a simple checksum where the sum must be zero (used by Roland)
 */
public class DefaultChecksum implements SysExChecksum {
    private int checksum = 0;

    @Override
    public void update(int value) {
        checksum += value;
    }

    @Override
    public int getValue() {
        return -checksum & 0x7F;
    }
}

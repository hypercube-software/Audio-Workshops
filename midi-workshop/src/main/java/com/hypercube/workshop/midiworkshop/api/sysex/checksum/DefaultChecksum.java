package com.hypercube.workshop.midiworkshop.api.sysex.checksum;

import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExChecksum;

/**
 * This is a simple checksum where the sum must be zero (used by Roland and Yamaha)
 */
public class DefaultChecksum implements SysExChecksum {
    protected int checksum = 0;

    @Override
    public void reset() {
        checksum = 0;
    }

    @Override
    public void update(int value) {
        checksum += value;
    }

    @Override
    public int getValue() {
        return -checksum & 0x7F;
    }
}

package com.hypercube.workshop.midiworkshop.sysex.cheksum;

import com.hypercube.workshop.midiworkshop.sysex.util.SysExChecksum;

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

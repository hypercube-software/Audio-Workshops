package com.hypercube.workshop.midiworkshop.api.sysex.checksum;

public class XORChecksum extends DefaultChecksum {
    @Override
    public void update(int value) {
        checksum = checksum ^ value;
    }

    @Override
    public int getValue() {
        return checksum & 0x7F;
    }
}

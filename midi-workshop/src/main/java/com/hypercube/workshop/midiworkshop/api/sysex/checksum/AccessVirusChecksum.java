package com.hypercube.workshop.midiworkshop.api.sysex.checksum;

public class AccessVirusChecksum extends DefaultChecksum {
    @Override
    public int getValue() {
        return checksum & 0x7F;
    }
}

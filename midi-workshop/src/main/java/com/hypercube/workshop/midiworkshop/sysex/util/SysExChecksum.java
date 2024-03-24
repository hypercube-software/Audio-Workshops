package com.hypercube.workshop.midiworkshop.sysex.util;

public interface SysExChecksum {
    void update(int value);

    int getValue();
}

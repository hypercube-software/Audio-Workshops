package com.hypercube.workshop.midiworkshop.sysex.manufacturer.roland.command;

import com.hypercube.workshop.midiworkshop.sysex.manufacturer.roland.RolandDevice;
import com.hypercube.workshop.midiworkshop.sysex.util.CustomByteBuffer;

public interface RolandCommandParser {
    void parse(int deviceId, RolandDevice model, CustomByteBuffer buffer);
}

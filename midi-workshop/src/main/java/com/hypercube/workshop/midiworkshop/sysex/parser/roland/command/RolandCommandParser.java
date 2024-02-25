package com.hypercube.workshop.midiworkshop.sysex.parser.roland.command;

import com.hypercube.workshop.midiworkshop.sysex.parser.roland.RolandDeviceModel;
import com.hypercube.workshop.midiworkshop.sysex.util.CustomByteBuffer;

public interface RolandCommandParser {
    void parse(int deviceId, RolandDeviceModel model, CustomByteBuffer buffer);
}

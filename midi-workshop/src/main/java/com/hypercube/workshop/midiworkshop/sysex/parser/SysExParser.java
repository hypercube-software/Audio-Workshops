package com.hypercube.workshop.midiworkshop.sysex.parser;

import com.hypercube.workshop.midiworkshop.sysex.model.DeviceModel;
import com.hypercube.workshop.midiworkshop.sysex.model.Manufacturer;
import com.hypercube.workshop.midiworkshop.sysex.util.CustomByteBuffer;

public interface SysExParser {
    int SYSEX_START = 0xF0;
    int SYSEX_END = 0xF7;

    DeviceModel parse(Manufacturer manufacturer, CustomByteBuffer buffer);
}

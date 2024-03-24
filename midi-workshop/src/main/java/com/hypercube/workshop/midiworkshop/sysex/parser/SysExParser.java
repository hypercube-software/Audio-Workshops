package com.hypercube.workshop.midiworkshop.sysex.parser;

import com.hypercube.workshop.midiworkshop.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.sysex.util.CustomByteBuffer;

public interface SysExParser {
    Device parse(Manufacturer manufacturer, CustomByteBuffer buffer);
}

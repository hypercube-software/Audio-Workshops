package com.hypercube.workshop.midiworkshop.sysex.parser;

import com.hypercube.workshop.midiworkshop.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.sysex.util.SysExReader;

public interface SysExParser {
    Device parse(Manufacturer manufacturer, SysExReader buffer);
}

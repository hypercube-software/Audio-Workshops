package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.roland.command;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.roland.RolandDevice;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExReader;

public interface RolandCommandParser {
    void parse(int deviceId, RolandDevice model, SysExReader buffer);
}

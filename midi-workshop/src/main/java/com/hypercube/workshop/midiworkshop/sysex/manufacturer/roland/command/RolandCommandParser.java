package com.hypercube.workshop.midiworkshop.sysex.manufacturer.roland.command;

import com.hypercube.workshop.midiworkshop.sysex.manufacturer.roland.RolandDevice;
import com.hypercube.workshop.midiworkshop.sysex.util.SysExReader;

public interface RolandCommandParser {
    void parse(int deviceId, RolandDevice model, SysExReader buffer);
}

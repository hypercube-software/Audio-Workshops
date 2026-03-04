package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.behringer;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.parser.ManufacturerSysExParser;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExReader;

public class BehringerSysExParser extends ManufacturerSysExParser {
    @Override
    public Device parse(Manufacturer manufacturer, SysExReader buffer) {
        throw new MidiError("Not Implemented yet");
    }
}

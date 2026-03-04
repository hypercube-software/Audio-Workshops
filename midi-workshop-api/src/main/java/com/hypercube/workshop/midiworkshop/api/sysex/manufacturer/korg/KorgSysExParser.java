package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.korg;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.parser.ManufacturerSysExParser;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExReader;

public class KorgSysExParser extends ManufacturerSysExParser {
    /**
     * This key is the right one to use. I found it after hours of reverse engineering
     */
    private static final String officialKey = """
            0 G7 F7 E7 D7 C7 B7 A7
            0 A6 A5 A4 A3 A2 A1 A0
            0 B6 B5 B4 B3 B2 B1 B0
            0 C6 C5 C4 C3 C2 C1 C0
            0 D6 D5 D4 D3 D2 D1 D0
            0 E6 E5 E4 E3 E2 E1 E0
            0 F6 F5 F4 F3 F2 F1 F0
            0 G6 G5 G4 G3 G2 G1 G0
            """;

    @Override
    public Device parse(Manufacturer manufacturer, SysExReader buffer) {
        throw new MidiError("Not Implemented yet");
    }
}

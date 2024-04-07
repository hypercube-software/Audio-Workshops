package com.hypercube.workshop.midiworkshop.common.sysex.parser;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.common.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExReader;
import lombok.extern.slf4j.Slf4j;

import static com.hypercube.workshop.midiworkshop.common.sysex.util.SysExConstants.SYSEX_END;

@Slf4j
public class ManufacturerSysExParser {
    public Device parse(Manufacturer manufacturer, SysExReader buffer) {
        throw new MidiError("Not Implemented");
    }

    public void parseIdentityResponse(Manufacturer manufacturer, int deviceId, SysExReader sysExReader) {
        StringBuilder sb = new StringBuilder();
        for (; ; ) {
            int value = sysExReader.getByte();
            if (value == SYSEX_END) {
                break;
            }
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append("%02X".formatted(value));
        }
        log.info("Identity Response for {} (device id: {}): {}", manufacturer.name(), deviceId, sb);
    }
}

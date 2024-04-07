package com.hypercube.workshop.midiworkshop.common.sysex.manufacturer.roland.command;

import com.hypercube.workshop.midiworkshop.common.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.common.sysex.device.memory.primitives.MemoryInt24;
import com.hypercube.workshop.midiworkshop.common.sysex.manufacturer.roland.RolandDevice;
import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExReader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataSetCommandParser implements RolandCommandParser {
    @Override
    public void parse(int deviceId, RolandDevice model, SysExReader buffer) {
        MemoryInt24 address = MemoryInt24.fromPacked(buffer.getInt24());
        int size = buffer.remaining() - 1;
        DeviceMemory memory = model.getMemory();
        for (int i = 0; i < size; i++) {
            int v = buffer.getByte();
            memory.writeByte(address, v);
            address = address.add(1);
        }
        // https://github.com/shingo45endo/sysex-checksum/blob/main/sysex_parser.js
        int checksum = buffer.getByte();
    }


}

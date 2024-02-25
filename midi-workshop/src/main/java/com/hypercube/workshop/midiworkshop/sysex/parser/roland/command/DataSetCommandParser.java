package com.hypercube.workshop.midiworkshop.sysex.parser.roland.command;

import com.hypercube.workshop.midiworkshop.sysex.model.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.sysex.model.memory.primitives.MemoryInt24;
import com.hypercube.workshop.midiworkshop.sysex.parser.roland.RolandDeviceModel;
import com.hypercube.workshop.midiworkshop.sysex.util.CustomByteBuffer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataSetCommandParser implements RolandCommandParser {

    @Override
    public void parse(int deviceId, RolandDeviceModel model, CustomByteBuffer buffer) {
        MemoryInt24 address = MemoryInt24.fromPacked(buffer.getInt24());
        int size = buffer.remaining() - 1;
        log.info("Write %d bytes at %s".formatted(size, address));
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

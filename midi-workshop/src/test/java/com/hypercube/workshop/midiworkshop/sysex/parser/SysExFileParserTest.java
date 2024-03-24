package com.hypercube.workshop.midiworkshop.sysex.parser;

import com.hypercube.workshop.midiworkshop.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.dump.DeviceMemoryDumper;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.map.MemoryMapFormat;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives.MemoryInt24;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SysExFileParserTest {
    @Test
    void loadD70Map() {
        SysExFileParser p = new SysExFileParser();
        Device device = p.parse(new File("sysex/Roland/D-70 reset.syx"));
        var memory = device.getMemory();
        var dumper = new DeviceMemoryDumper(memory);
        dumper.dumpMemory(new File("target/D-70.memory"));
        assertEquals(4, memory
                .getMemorySpaces()
                .size());
        String channel1Name = memory
                .readString(MemoryMapFormat.BYTES, MemoryInt24.fromPacked(0x000008), 10);
        assertEquals("Ch 1      ", channel1Name);
        String performance47Name = memory
                .readString(MemoryMapFormat.BYTES, MemoryInt24.fromPacked(0x007E00), 10);
        assertEquals("SpaceDream", performance47Name);
    }

    @Test
    void loadDS330Bulk() {
        SysExFileParser p = new SysExFileParser();
        Device device = p.parse(new File("sysex/Roland/DS-330 - MultiMode.syx"));
        DeviceMemory memory = device.getMemory();
        var dumper = new DeviceMemoryDumper(memory);
        dumper.dumpMemory(new File("target/DS-330.memory"));
        dumper.dumpMemoryMap(new File("target/DS-330.map"));
        assertEquals(8, memory
                .getMemorySpaces()
                .size());
    }
}

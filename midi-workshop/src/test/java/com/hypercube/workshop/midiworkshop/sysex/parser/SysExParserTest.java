package com.hypercube.workshop.midiworkshop.sysex.parser;

import com.hypercube.workshop.midiworkshop.common.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.common.sysex.device.Devices;
import com.hypercube.workshop.midiworkshop.common.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.common.sysex.device.memory.dump.DeviceMemoryDumper;
import com.hypercube.workshop.midiworkshop.common.sysex.device.memory.map.MemoryMapFormat;
import com.hypercube.workshop.midiworkshop.common.sysex.device.memory.primitives.MemoryInt24;
import com.hypercube.workshop.midiworkshop.common.sysex.parser.SysExParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SysExParserTest {

    @AfterEach
    void reset() {
        System.clearProperty(Devices.SYSTEM_PROPERTY_FORCE_DEVICE);
    }

    @Test
    void loadD70Map() {
        SysExParser p = new SysExParser();
        Device device = p.parse(new File("../sysex/Roland/D-70/D-70 reset.syx"));
        var memory = device.getMemory();
        var dumper = new DeviceMemoryDumper(memory);
        dumper.dumpMemory(new File("target/D-70.memory"));
        assertEquals(4, memory
                .getMemoryMaps()
                .size());
        String channel1Name = dumper
                .readString(MemoryInt24.fromPacked(0x000008), MemoryMapFormat.BYTES, 10);
        assertEquals("Ch 1      ", channel1Name);
        String performance47Name = dumper
                .readString(MemoryInt24.fromPacked(0x007E00), MemoryMapFormat.BYTES, 10);
        assertEquals("SpaceDream", performance47Name);
    }

    @Test
    void loadDS330Bulk() {
        System.setProperty(Devices.SYSTEM_PROPERTY_FORCE_DEVICE, "DS-330");
        SysExParser p = new SysExParser();
        Device device = p.parse(new File("../sysex/Roland/DS-330/Boss-DS-330-MultiMode.syx"));
        DeviceMemory memory = device.getMemory();
        var dumper = new DeviceMemoryDumper(memory);
        dumper.dumpMemory(new File("target/DS-330.memory"));
        dumper.dumpMemoryMap(new File("target/DS-330.map"));
        assertEquals(24, memory
                .getMemoryMaps()
                .size());
    }
}

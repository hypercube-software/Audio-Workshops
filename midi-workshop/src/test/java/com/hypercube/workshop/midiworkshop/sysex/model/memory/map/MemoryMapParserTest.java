package com.hypercube.workshop.midiworkshop.sysex.model.memory.map;

import com.hypercube.workshop.midiworkshop.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.map.MemoryMapParser;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryMapParserTest {

    @Test
    void loadD70Map() {
        DeviceMemory dm = MemoryMapParser.load(new File("sysex/Roland/D-70/D-70.mmap"));
        assertEquals(4, dm.getMemorySpaces()
                .size());
        assertMemoryMapSize(0x0000A8, "Internal Memory Core", dm);
        assertMemoryMapSize(0x000781, "Temporary Memory", dm);
        assertMemoryMapSize(0x009158, "Internal Memory", dm);
        assertMemoryMapSize(0x009158, "Card Memory", dm);
    }

    @Test
    void loadDS330Map() {
        DeviceMemory dm = MemoryMapParser.load(new File("sysex/Roland/DS-330/DS-330.mmap"));
        assertEquals(9, dm.getMemorySpaces()
                .size());
        assertMemoryMapSize(0x000080, "Zone 1", dm);
        assertMemoryMapSize(0x000800, "Zone 2", dm);
        assertMemoryMapSize(0x001000, "Zone 3", dm);
        assertMemoryMapSize(0x001000, "Zone 4", dm);
        assertMemoryMapSize(0x000020, "Information", dm);
        assertMemoryMapSize(0x00000C, "Drum Map Name", dm);
        assertMemoryMapSize(0x000400, "Drum Map", dm);
        assertMemoryMapSize(0x000E90, "Bulk Dump", dm);
        assertMemoryMapSize(0x000E90, "Bulk Dump Drum", dm);
    }

    void assertMemoryMapSize(int size, String name, DeviceMemory dm) {
        assertEquals(size, dm.getMemoryMap(name)
                .getSize()
                .value());
    }
}

package com.hypercube.workshop.midiworkshop.sysex.model.memory.map;

import com.hypercube.workshop.midiworkshop.common.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.common.sysex.device.memory.map.MemoryMapParser;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryMapParserTest {

    @Test
    void loadD70Map() {
        DeviceMemory dm = MemoryMapParser.load(new File("../sysex/Roland/D-70/D-70.mmap"));
        assertEquals(4, dm.getMemoryMaps()
                .size());
        assertMemoryMapSize(0x0000A8, "Internal Memory Core", dm);
        assertMemoryMapSize(0x000781, "Temporary Memory", dm);
        assertMemoryMapSize(0x009158, "Internal Memory", dm);
        assertMemoryMapSize(0x009158, "Card Memory", dm);
    }

    @Test
    void loadDS330Map() {
        DeviceMemory dm = MemoryMapParser.load(new File("../sysex/Roland/DS-330/DS-330.mmap"));
        assertEquals(24, dm.getMemoryMaps()
                .size());
        assertMemoryMapSize(0x000080, "SystemParams Zone", dm);
        assertMemoryMapSize(0x000800, "CommonPatchParams Zone", dm);
        assertMemoryMapSize(0x001000, "PatchParams Zone", dm);
        assertMemoryMapSize(0x001000, "PatchSends Zone", dm);
        assertMemoryMapSize(0x000020, "Information Zone", dm);
        assertMemoryMapSize(0x00000C, "Drum Map 1 Name", dm);
        assertMemoryMapSize(0x000400, "Drum Map 1", dm);
        assertMemoryMapSize(0x00000C, "Drum Map 2 Name", dm);
        assertMemoryMapSize(0x000400, "Drum Map 2", dm);
        assertMemoryMapSize(0x000748, "Bulk Dump", dm);

        assertMemoryMapSize(0x000080, "Bulk DrumMap1 Key", dm);
        assertMemoryMapSize(0x000080, "Bulk DrumMap1 Level", dm);
        assertMemoryMapSize(0x000080, "Bulk DrumMap1 Pan", dm);
        assertMemoryMapSize(0x000080, "Bulk DrumMap1 Reverb", dm);
        assertMemoryMapSize(0x000080, "Bulk DrumMap1 Chorus", dm);
        assertMemoryMapSize(0x00000C, "Bulk DrumMap1 Name", dm);

        assertMemoryMapSize(0x000080, "Bulk DrumMap2 Key", dm);
        assertMemoryMapSize(0x000080, "Bulk DrumMap2 Level", dm);
        assertMemoryMapSize(0x000080, "Bulk DrumMap2 Pan", dm);
        assertMemoryMapSize(0x000080, "Bulk DrumMap2 Reverb", dm);
        assertMemoryMapSize(0x000080, "Bulk DrumMap2 Chorus", dm);
        assertMemoryMapSize(0x00000C, "Bulk DrumMap2 Name", dm);
    }

    void assertMemoryMapSize(int size, String name, DeviceMemory dm) {
        assertEquals(size, dm.getMemoryMap(name)
                .getSize()
                .value());
    }
}

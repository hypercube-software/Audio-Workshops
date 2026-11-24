package com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.DeviceMemory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void loadShortAddresses(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("short-addresses.mmap");
        Files.writeString(file, """
                @Model Short Addresses
                @Packed

                @0128-0100 Two Byte Zone
                {
                    byte Level
                }

                @00000128-00000100 Four Byte Zone
                {
                    byte Level
                }
                """);

        DeviceMemory dm = MemoryMapParser.load(file.toFile());

        var twoBytes = dm.getMemoryMap("Two Byte Zone");
        assertEquals(0x00A8, twoBytes.getBaseAddress()
                .value());
        assertEquals(2, twoBytes.getBaseAddress()
                .byteCount());
        assertEquals(0x0080, twoBytes.getSize()
                .value());
        assertEquals(2, twoBytes.getSize()
                .byteCount());
        assertArrayEquals(new int[]{0x01, 0x28}, twoBytes.getBaseAddress()
                .getPackedBytes());

        var fourBytes = dm.getMemoryMap("Four Byte Zone");
        assertEquals(0x00A8, fourBytes.getBaseAddress()
                .value());
        assertEquals(4, fourBytes.getBaseAddress()
                .byteCount());
        assertEquals(0x0080, fourBytes.getSize()
                .value());
        assertEquals(4, fourBytes.getSize()
                .byteCount());
    }

    @Test
    void reportsSyntaxErrorsWithLocation(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("broken.mmap");
        Files.writeString(file, """
                @Model Broken
                @Packed

                @400000 Broken Zone
                {
                    byte Level
                    enum Nope
                }
                """);

        MidiError error = assertThrows(MidiError.class, () -> MemoryMapParser.load(file.toFile()));

        assertTrue(error.getMessage()
                .contains("broken.mmap:"), error.getMessage());
    }

    void assertMemoryMapSize(int size, String name, DeviceMemory dm) {
        assertEquals(size, dm.getMemoryMap(name)
                .getSize()
                .value());
    }
}

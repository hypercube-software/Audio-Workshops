package com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.MemorySpace;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals(26, dm.getMemoryMaps()
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
        assertMemoryMapSize(0x000080, "Bulk DrumMap1 Rx Note", dm);
        assertMemoryMapSize(0x00000C, "Bulk DrumMap1 Name", dm);

        assertMemoryMapSize(0x000080, "Bulk DrumMap2 Key", dm);
        assertMemoryMapSize(0x000080, "Bulk DrumMap2 Level", dm);
        assertMemoryMapSize(0x000080, "Bulk DrumMap2 Pan", dm);
        assertMemoryMapSize(0x000080, "Bulk DrumMap2 Reverb", dm);
        assertMemoryMapSize(0x000080, "Bulk DrumMap2 Chorus", dm);
        assertMemoryMapSize(0x000080, "Bulk DrumMap2 Rx Note", dm);
        assertMemoryMapSize(0x00000C, "Bulk DrumMap2 Name", dm);

        // The bulk zones only answer a request for their full size.
        assertTrue(dm.getMemoryMap("Bulk Dump")
                .isMonolithic());
        assertTrue(dm.getMemoryMap("Bulk DrumMap1 Key")
                .isMonolithic());
        assertTrue(dm.getMemoryMap("Bulk DrumMap2 Name")
                .isMonolithic());
        assertFalse(dm.getMemoryMap("SystemParams Zone")
                .isMonolithic());
        assertFalse(dm.getMemoryMap("Drum Map 1")
                .isMonolithic());
    }

    @Test
    void loadMonolithModifiersInAnyOrder(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("monolith-zone.mmap");
        Files.writeString(file, """
                @Model Monolith Zone
                @Packed

                @0000-0020 NIBBLES MONOLITH First Zone
                {
                    byte Level[16]
                }

                @0100-0020 MONOLITH NIBBLES Second Zone
                {
                    byte Level[16]
                }
                """);

        DeviceMemory dm = MemoryMapParser.load(file.toFile());

        MemoryMap first = dm.getMemoryMap("First Zone");
        assertTrue(first.isMonolithic());
        assertEquals(MemoryMapFormat.NIBBLES, first.getFormat());
        assertMemoryMapSize(0x10, "First Zone", dm);

        MemoryMap second = dm.getMemoryMap("Second Zone");
        assertTrue(second.isMonolithic());
        assertEquals(MemoryMapFormat.NIBBLES, second.getFormat());
    }

    @Test
    void rejectMonolithOnStruct(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("monolith-struct.mmap");
        Files.writeString(file, """
                @Model Monolith Struct
                @Packed

                @0000-0010 Zone
                {
                    Record
                }

                struct MONOLITH Record
                {
                    byte Level
                }
                """);

        MidiError error = assertThrows(MidiError.class, () -> MemoryMapParser.load(file.toFile()));

        assertTrue(error.getMessage()
                .contains("MONOLITH"), error.getMessage());
    }

    @Test
    void rejectDuplicateMonolith(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("duplicate-monolith.mmap");
        Files.writeString(file, """
                @Model Duplicate Monolith
                @Packed

                @0000-0010 MONOLITH MONOLITH Zone
                {
                    byte Level[16]
                }
                """);

        MidiError error = assertThrows(MidiError.class, () -> MemoryMapParser.load(file.toFile()));

        assertTrue(error.getMessage()
                .contains("twice"), error.getMessage());
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
    void loadImplicitDefaultMemorySpace(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("default-space.mmap");
        Files.writeString(file, """
                @Model Default Space
                @Packed

                @0000-0010 Legacy Zone
                {
                    byte Level
                }

                CurrentPreset
                {
                    @0000-0020 Current Zone
                    {
                        byte Level
                    }
                }

                @0100-0010 Outside Zone
                {
                    byte Level
                }
                """);

        DeviceMemory dm = MemoryMapParser.load(file.toFile());

        assertEquals(List.of("Default", "CurrentPreset"), dm.getMemorySpaces()
                .stream()
                .map(MemorySpace::getName)
                .toList());
        assertMemorySpaceZones(dm, "Default", "Legacy Zone", "Outside Zone");
        assertMemorySpaceZones(dm, "CurrentPreset", "Current Zone");
    }

    @Test
    void rejectDuplicateMemorySpace(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("duplicate-space.mmap");
        Files.writeString(file, """
                @Model Duplicate Space

                CurrentPreset
                {
                    @0000-0005 First Zone
                    {
                        byte Level
                    }
                }

                CurrentPreset
                {
                    @0000-0005 Second Zone
                    {
                        byte Level
                    }
                }
                """);

        MidiError error = assertThrows(MidiError.class, () -> MemoryMapParser.load(file.toFile()));

        assertTrue(error.getMessage()
                .contains("already declared"), error.getMessage());
    }

    @Test
    void resolveImplicitDefaultMemorySpace() {
        DeviceMemory dm = MemoryMapParser.load(new File("../sysex/Roland/D-70/D-70.mmap"));

        assertEquals(MemorySpace.DEFAULT_NAME, dm.resolveMemorySpace(null)
                .getName());
        assertEquals(MemorySpace.DEFAULT_NAME, dm.resolveMemorySpace("  ")
                .getName());
    }

    @Test
    void requireMemorySpaceWhenSeveralAreDeclared() {
        DeviceMemory dm = MemoryMapParser.load(new File("../sysex/AKAI/MPK-261/MPK-261.mmap"));

        MidiError error = assertThrows(MidiError.class, () -> dm.resolveMemorySpace(null));
        assertTrue(error.getMessage()
                .contains("CurrentPreset"), error.getMessage());
        assertTrue(error.getMessage()
                .contains("Global"), error.getMessage());
        assertTrue(error.getMessage()
                .contains("UserPresets"), error.getMessage());

        assertEquals("Global", dm.resolveMemorySpace("Global")
                .getName());
        assertThrows(MidiError.class, () -> dm.resolveMemorySpace("Unknown"));
    }

    @Test
    void loadAkaiMpk261Map() {
        DeviceMemory dm = MemoryMapParser.load(new File("../sysex/AKAI/MPK-261/MPK-261.mmap"));
        assertEquals(3, dm.getMemorySpaces()
                .size());
        assertMemorySpaceZones(dm, "CurrentPreset", "Preset Edit Buffer");
        assertMemorySpaceZones(dm, "Global", "Global Settings");
        assertMemorySpaceZones(dm, "UserPresets", "User Presets");
        assertMemoryMapSize(0x179, "Global Settings", dm);
        assertMemoryMapSize(0x608, "Preset Edit Buffer", dm);
        assertMemoryMapSize(0x60A * 30, "User Presets", dm);

        MemoryMap global = referencedStruct(dm, "Global Settings");
        assertEquals(0x179, global.getSize()
                .value());
        assertEquals(0x13, field(global, "Live States").getAddress()
                .value());
        assertEquals(0x30, field(global, "SysEx Device ID").getAddress()
                .value());

        MemoryMap buffer = referencedStruct(dm, "Preset Edit Buffer");
        assertEquals(0x608, buffer.getSize()
                .value());
        assertEquals(0x5B, field(buffer, "DAW Buttons").getAddress()
                .value());
        assertEquals(0x9C, field(buffer, "Switches").getAddress()
                .value());
        assertEquals(0x1D4, field(buffer, "Faders").getAddress()
                .value());
        assertEquals(0x264, field(buffer, "Knobs").getAddress()
                .value());
        assertEquals(0x33C, field(buffer, "Pads").getAddress()
                .value());
        assertEquals(704, field(buffer, "Pads")
                .getTotalSize()
                .value());

        MemoryMap padArray = field(buffer, "Pads").getReference();
        assertEquals(704, padArray.getSize()
                .value());
        assertEquals(2 * 64, field(padArray, "Note").getAddress()
                .value());
        assertEquals(8 * 64, field(padArray, "MIDI to DIN").getAddress()
                .value());

        MemoryMap switchArray = field(buffer, "Switches").getReference();
        assertEquals(312, switchArray.getSize()
                .value());
        assertEquals(7 * 24, field(switchArray, "Note").getAddress()
                .value());
        assertEquals(8 * 24, field(switchArray, "MIDI to DIN").getAddress()
                .value());

        MemoryMap knobArray = field(buffer, "Knobs").getReference();
        assertEquals(216, knobArray.getSize()
                .value());
        assertEquals(5 * 24, field(knobArray, "MSB").getAddress()
                .value());
        assertEquals(8 * 24, field(knobArray, "MIDI to DIN").getAddress()
                .value());

        MemoryMap userPreset = referencedStruct(dm, "User Presets");
        assertEquals(0x60A, userPreset.getSize()
                .value());
        assertEquals(30, dm.getMemoryMap("User Presets")
                .getFields()
                .get(0)
                .getArray()
                .size());        assertEquals(0x35, field(userPreset, "Pads").getAddress()
                .value());
        assertEquals(0x2F5, field(userPreset, "Knobs").getAddress()
                .value());
        assertEquals(0x3CD, field(userPreset, "Faders").getAddress()
                .value());
        assertEquals(0x45D, field(userPreset, "Switches").getAddress()
                .value());
        assertEquals(0x595, field(userPreset, "DAW Buttons").getAddress()
                .value());
        assertEquals(0x5D6, field(userPreset, "Arp Type").getAddress()
                .value());

        MemoryMap padRecord = field(userPreset, "Pads").getReference();
        assertEquals(11, padRecord.getSize()
                .value());
        assertEquals(3, field(padRecord, "MIDI to DIN").getAddress()
                .value());
        assertEquals(9, field(padRecord, "Off Colour").getAddress()
                .value());

        MemoryMap switchRecord = field(userPreset, "Switches").getReference();
        assertEquals(13, switchRecord.getSize()
                .value());
        assertEquals(7, field(switchRecord, "MIDI to DIN").getAddress()
                .value());
        assertEquals(8, field(switchRecord, "Note").getAddress()
                .value());

        MemoryMap knobRecord = field(userPreset, "Knobs").getReference();
        assertEquals(9, knobRecord.getSize()
                .value());
        assertEquals(5, field(knobRecord, "MIDI to DIN").getAddress()
                .value());
        assertEquals(8, field(knobRecord, "Value").getAddress()
                .value());
    }

    private void assertMemorySpaceZones(DeviceMemory dm, String spaceName, String... zoneNames) {
        assertEquals(List.of(zoneNames), dm.getMemoryMaps(spaceName)
                .stream()
                .map(MemoryMap::getName)
                .toList());
    }

    @Test
    void loadSe02Map() {
        DeviceMemory dm = MemoryMapParser.load(new File("../sysex/Roland/SE-02/SE-02.mmap"));

        assertEquals(10, dm.getMemoryMaps()
                .size());
        assertEquals(1, dm.getMemorySpaces()
                .size());
        assertEquals(List.of("Default"), dm.getMemorySpaceNames());
        assertEquals("Default", dm.resolveMemorySpace(null)
                .getName());
        assertEquals("Default", dm.resolveMemorySpace("Default")
                .getName());
        assertMemoryMapSize(0x0A, "System Settings", dm);
        assertMemoryMapSize(0x02, "CV Calibration", dm);
        assertEquals(0x04, dm.getMemoryMap("CV Calibration")
                .getEffectiveSize()
                .value());
        assertMemoryMapSize(0x02, "System Reserved", dm);
        assertMemoryMapSize(0x10, "Reserved Internal Storage", dm);
        assertMemoryMapSize(0x4000, "Patch Edit Buffer", dm);
        assertMemoryMapSize(0x200000, "User Presets", dm);
        assertMemoryMapSize(0x4000, "Pattern Edit Buffer", dm);
        assertMemoryMapSize(0x200000, "Stored Patterns", dm);
        assertMemoryMapSize(0x4000, "Song Edit Buffer", dm);
        assertMemoryMapSize(0x40000, "Stored Songs", dm);

        assertArrayEquals(new int[]{0x05, 0x00, 0x00, 0x00}, dm.getMemoryMap("Patch Edit Buffer")
                .getBaseAddress()
                .getPackedBytes());
        assertArrayEquals(new int[]{0x06, 0x00, 0x00, 0x00}, dm.getMemoryMap("User Presets")
                .getBaseAddress()
                .getPackedBytes());

        MemoryMap system = referencedStruct(dm, "System Settings");
        assertEquals(0x0A, system.getSize()
                .value());
        MemoryMap cv = referencedStruct(dm, "CV Calibration");
        assertEquals(0x02, cv.getSize()
                .value());
        assertEquals(0x00, field(cv, "CV Fine Tune").getAddress()
                .value());
        assertEquals(0x02, field(cv, "CV Key Follow").getAddress()
                .value());

        MemoryMap preset = referencedStruct(dm, "Patch Edit Buffer");
        assertEquals(0x4000, preset.getSize()
                .value());
        MemoryMap parameters = field(preset, "Parameters").getReference();
        assertEquals(0x68, parameters.getSize()
                .value());
        assertEquals(0xD0, parameters.getEffectiveSize()
                .value());
        assertEquals(0x00, field(preset, "Parameters").getAddress()
                .value());
        assertEquals(0xD0, field(preset, "Name").getAddress()
                .value());
        assertEquals(0xE0, field(preset, "Reserved").getAddress()
                .value());
        assertEquals(0x00, field(parameters, "COM_BENDRANGE").getAddress()
                .value());
        assertEquals(0x22, field(parameters, "COM_OCT").getAddress()
                .value());
        assertEquals(0x28, field(parameters, "CTRL_GLIDE").getAddress()
                .value());
        assertEquals(0x32, field(parameters, "OSC_RANGE1").getAddress()
                .value());
        assertEquals(0x6E, field(parameters, "FLT_CUTOFF").getAddress()
                .value());
        assertEquals(0x96, field(parameters, "LFO_RATE").getAddress()
                .value());
        assertEquals(0xAA, field(parameters, "DLY_TIME").getAddress()
                .value());
        assertEquals(0xB0, field(parameters, "Unknown B0").getAddress()
                .value());

        MemoryMap storedPreset = referencedStruct(dm, "User Presets");
        assertEquals(0x4000, storedPreset.getSize()
                .value());
        assertEquals(128, dm.getMemoryMap("User Presets")
                .getFields()
                .get(0)
                .getArray()
                .size());

        MemoryMap pattern = referencedStruct(dm, "Pattern Edit Buffer");
        assertEquals(0x4000, pattern.getSize()
                .value());
        assertEquals(0x10, field(pattern, "Steps").getAddress()
                .value());
        assertEquals(0x08, field(pattern, "Steps").getReference()
                .getSize()
                .value());

        MemoryMap storedPattern = referencedStruct(dm, "Stored Patterns");
        assertEquals(0x4000, storedPattern.getSize()
                .value());
        assertEquals(128, dm.getMemoryMap("Stored Patterns")
                .getFields()
                .get(0)
                .getArray()
                .size());

        MemoryMap song = referencedStruct(dm, "Song Edit Buffer");
        assertEquals(0x4000, song.getSize()
                .value());
        assertEquals(0x10, field(song, "Parts").getAddress()
                .value());
        assertEquals(0x08, field(song, "Parts").getReference()
                .getSize()
                .value());

        MemoryMap storedSong = referencedStruct(dm, "Stored Songs");
        assertEquals(0x4000, storedSong.getSize()
                .value());
        assertEquals(16, dm.getMemoryMap("Stored Songs")
                .getFields()
                .get(0)
                .getArray()
                .size());
    }

    @Test
    void readSe02NibbleValue() {
        DeviceMemory dm = MemoryMapParser.load(new File("../sysex/Roland/SE-02/SE-02.mmap"));
        MemoryInt28 address = MemoryInt28.from("05000000", true);

        dm.writeByte("Default", address, 0x0C);
        dm.writeByte("Default", MemoryInt28.from("05000001", true), 0x08);

        assertEquals(0xC8, dm.readValueByte("Default", address));
    }

    @Test
    void readSe02StoredPresetValue() {
        DeviceMemory dm = MemoryMapParser.load(new File("../sysex/Roland/SE-02/SE-02.mmap"));
        MemoryInt28 address = MemoryInt28.from("06050000", true);

        dm.writeByte("Default", address, 0x0C);
        dm.writeByte("Default", address.add(1), 0x08);

        assertEquals(0xC8, dm.readValueByte("Default", address));
    }

    private MemoryMap referencedStruct(DeviceMemory dm, String zoneName) {
        MemoryField field = dm.getMemoryMap(zoneName)
                .getFields()
                .get(0);
        assertTrue(field.isReference(), zoneName + " should reference a struct");
        return field.getReference();
    }

    private MemoryField field(MemoryMap memoryMap, String name) {
        return memoryMap.getFields()
                .stream()
                .filter(f -> name.equals(f.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Field not found: " + name));
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

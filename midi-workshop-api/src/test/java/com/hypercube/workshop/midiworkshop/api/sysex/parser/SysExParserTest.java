package com.hypercube.workshop.midiworkshop.api.sysex.parser;

import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Devices;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.dump.DeviceMemoryDumper;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMapFormat;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
                .readString(MemoryInt28.fromPacked(0x000008), MemoryMapFormat.BYTES, 10);
        assertEquals("Ch 1      ", channel1Name);
        String performance47Name = dumper
                .readString(MemoryInt28.fromPacked(0x007E00), MemoryMapFormat.BYTES, 10);
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

    @Test
    void parseAkaiReferencePreset() {
        SysExParser parser = new SysExParser();
        Device device = parser.parse(new File("../sysex/AKAI/MPK-261/preset_08_StudOne5.syx"));

        assertNotNull(device);
        assertEquals("MPK-261", device.getName());
        DeviceMemory memory = device.getMemory();
        assertEquals(3, memory.getMemoryMaps()
                .size());
        assertEquals(3, memory.getMemorySpaces()
                .size());

        MemoryMap userPreset = memory.getMemoryMap("UserPresets", "User Presets");
        int presetBase = 7 * 0x60A; // WPRESET 8 -> Preset[7]
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            name.append((char) userPreset.readByte(MemoryInt28.from(presetBase + i, 4)));
        }
        assertEquals("StudOne5", name.toString());
        assertEquals(0x00, userPreset.readByte(MemoryInt28.from(presetBase + 9, 4)));
        assertEquals(0x64, userPreset.readByte(MemoryInt28.from(presetBase + 10, 4)));
        assertEquals(0x04, userPreset.readByte(MemoryInt28.from(presetBase + 12, 4)));
        assertEquals(0x19, userPreset.readByte(MemoryInt28.from(presetBase + 14, 4)));
        assertEquals(0x32, userPreset.readByte(MemoryInt28.from(presetBase + 15, 4)));
    }

    @Test
    void parseAkaiEditBufferBlock() {
        Device device = new SysExParser().parse(akaiFrame(0x31, 0x0008, 0x42));

        assertNotNull(device);
        assertEquals(0x42, device.getMemory()
                .readByte("CurrentPreset", MemoryInt28.from(8, 2)));
    }

    @Test
    void parseAkaiGlobalBlock() {
        Device device = new SysExParser().parse(akaiFrame(0x30, 0x0008, 0x42));

        assertNotNull(device);
        assertEquals(0x42, device.getMemory()
                .readByte("Global", MemoryInt28.from(8, 2)));
    }

    @Test
    void ignoreNonSysExMessage() {
        assertNull(new SysExParser().parse(new byte[]{(byte) 0xF8}));
        assertNull(new SysExParser().parse(new byte[]{(byte) 0xFE}));
    }

    @Test
    void ignoreAkaiEditBufferPreset() {
        Device device = Manufacturer.AKAI.getDevice("MPK-261");
        device.loadMemoryMap();

        new SysExParser().parse(akaiPresetFrame(0, 0x42));

        MemoryMap presets = device.getMemory()
                .getMemoryMap("UserPresets", "User Presets");
        assertEquals(0, presets.readByte(MemoryInt28.from(0, 4)));
    }

    /**
     * Build a {@code WPRESET} answer carrying a 1546-byte preset record.
     */
    private byte[] akaiPresetFrame(int presetNumber, int firstValue) {
        int payloadSize = 1 + 0x60A;
        byte[] frame = new byte[5 + 2 + payloadSize + 1];
        frame[0] = (byte) 0xF0;
        frame[1] = 0x47;
        frame[2] = 0x00;
        frame[3] = 0x25;
        frame[4] = 0x10;
        frame[5] = (byte) (payloadSize >> 7);
        frame[6] = (byte) (payloadSize & 0x7F);
        frame[7] = (byte) presetNumber;
        frame[8] = (byte) firstValue;
        frame[frame.length - 1] = (byte) 0xF7;
        return frame;
    }

    /**
     * Build a one-byte {@code WEDIT} or {@code WGLOB} answer.
     */
    private byte[] akaiFrame(int command, int address, int value) {
        return new byte[]{(byte) 0xF0, 0x47, 0x00, 0x25, (byte) command,
                0x00, 0x04, 0x01, (byte) (address >> 7), (byte) (address & 0x7F), (byte) value, (byte) 0xF7};
    }
}

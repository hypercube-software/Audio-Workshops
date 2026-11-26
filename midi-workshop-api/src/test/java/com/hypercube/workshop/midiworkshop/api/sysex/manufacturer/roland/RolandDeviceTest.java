package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.roland;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Devices;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.parser.SysExParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolandDeviceTest {

    @BeforeEach
    void setUp() {
        System.clearProperty(Devices.SYSTEM_PROPERTY_FORCE_DEVICE);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(Devices.SYSTEM_PROPERTY_FORCE_DEVICE);
    }

    @Test
    void se02UsesFourByteModelAndFourByteAddress() {
        RolandDevice device = Manufacturer.ROLAND.getDevice("SE-02");

        assertEquals("SE-02", device.getName());
        assertEquals(0x10, device.getSysexDeviceId());
        assertEquals(4, device.getAddressByteCount());
        assertArrayEquals(new int[]{0x00, 0x00, 0x00, 0x44}, device.getModelId());

        RecordingMidiOutPort out = new RecordingMidiOutPort();
        device.requestData(out, MemoryInt28.from("05000000", true), MemoryInt28.from("00000150", true));

        assertEquals("F04110000000441105000000000001502AF7", out.lastHex());
    }

    @Test
    void parseSe02DataDump() {
        byte[] frame = new byte[]{
                (byte) 0xF0, 0x41, 0x10, 0x00, 0x00, 0x00, 0x44, 0x12,
                0x00, 0x00, 0x00, 0x00, 0x01, 0x7F, (byte) 0xF7};

        Device device = new SysExParser().parse(frame);

        assertNotNull(device);
        assertEquals("SE-02", device.getName());
        assertEquals(0x01, device.getMemory()
                .readByte(MemoryInt28.from("00000000", true)));
    }

    @Test
    void ds330MonolithZoneIsRequestedAtFullSize() {
        RolandDevice device = Manufacturer.ROLAND.getDevice("DS-330");
        device.loadMemoryMap();
        var memorySpace = device.getMemory()
                .resolveMemorySpace(null);

        // 16 bytes in the middle of the bulk dump are not addressable on their
        // own: the request must cover the whole zone.
        Device.ReadRequest request = device.resolveReadRequest(memorySpace,
                MemoryInt28.from("480048", true), MemoryInt28.from(16, 3));

        assertTrue(request.monolithic());
        assertEquals("Bulk Dump", request.zone()
                .getName());
        assertEquals(0x120000, request.address()
                .value());
        assertEquals(3728, request.size()
                .value());

        RecordingMidiOutPort out = new RecordingMidiOutPort();
        device.requestMemorySpace(out, memorySpace.getName(), request.address(), request.size());
        assertEquals("F041004211480000001D100BF7", out.lastHex());
    }

    @Test
    void ds330RegularZoneKeepsTheRequestedRange() {
        RolandDevice device = Manufacturer.ROLAND.getDevice("DS-330");
        device.loadMemoryMap();
        var memorySpace = device.getMemory()
                .resolveMemorySpace(null);

        Device.ReadRequest request = device.resolveReadRequest(memorySpace,
                MemoryInt28.from("400004", true), MemoryInt28.from(1, 3));

        assertFalse(request.monolithic());
        assertNull(request.zone());
        assertEquals(0x100004, request.address()
                .value());
        assertEquals(1, request.size()
                .value());
    }

    @Test
    void ds330RejectsARangeOutsideTheMonolithZone() {
        RolandDevice device = Manufacturer.ROLAND.getDevice("DS-330");
        device.loadMemoryMap();
        var memorySpace = device.getMemory()
                .resolveMemorySpace(null);

        assertThrows(MidiError.class, () -> device.resolveReadRequest(memorySpace,
                MemoryInt28.from("480000", true), MemoryInt28.from(3729, 3)));
    }

    private static final class RecordingMidiOutPort extends MidiOutPort {
        private static final AtomicInteger COUNTER = new AtomicInteger();
        private final List<MidiEvent> events = new ArrayList<>();

        private RecordingMidiOutPort() {
            super("roland-recording-" + COUNTER.incrementAndGet());
        }

        @Override
        public void send(MidiMessage msg, long timestamp) {
            events.add(new CustomMidiEvent(msg));
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        protected void effectiveOpen() {
            // nothing to do
        }

        @Override
        protected void effectiveClose() {
            // nothing to do
        }

        private String lastHex() {
            return ((CustomMidiEvent) events.getLast()).getHexValues();
        }
    }
}

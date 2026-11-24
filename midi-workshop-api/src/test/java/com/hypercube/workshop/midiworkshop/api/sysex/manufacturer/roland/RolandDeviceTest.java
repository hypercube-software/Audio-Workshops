package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.roland;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

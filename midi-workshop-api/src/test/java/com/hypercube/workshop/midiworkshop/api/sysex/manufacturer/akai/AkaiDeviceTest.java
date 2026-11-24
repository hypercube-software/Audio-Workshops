package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.akai;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.MemorySpaceAddressing;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AkaiDeviceTest {

    private AkaiDevice device;

    @BeforeEach
    void setUp() {
        device = Manufacturer.AKAI.getDevice("MPK-261");
        device.loadMemoryMap();
    }

    @Test
    void byteAddressedSpacesUseTheirOwnReadCommand() {
        RecordingMidiOutPort currentPreset = new RecordingMidiOutPort();
        device.requestMemorySpace(currentPreset, "CurrentPreset", MemoryInt28.from(0x0128, 2), MemoryInt28.from(3, 2));
        assertEquals("F0470025210003030228F7", currentPreset.lastHex());

        RecordingMidiOutPort global = new RecordingMidiOutPort();
        device.requestMemorySpace(global, "Global", MemoryInt28.from(0x0000, 2), MemoryInt28.from(1, 2));
        assertEquals("F0470025200003010000F7", global.lastHex());
    }

    @Test
    void userPresetsUseQPreset() {
        RecordingMidiOutPort out = new RecordingMidiOutPort();
        device.requestPreset(out, "UserPresets", 8);
        assertEquals("F047002512000108F7", out.lastHex());
    }

    @Test
    void presetCommandIsRejectedForByteAddressedSpace() {
        assertThrows(MidiError.class,
                () -> device.requestPreset(new RecordingMidiOutPort(), "Global", 8));
    }

    @Test
    void spaceDescriptorsComeFromTheProtocol() {
        assertEquals(MemorySpaceAddressing.BYTE, device.getMemorySpaceAddressing("CurrentPreset"));
        assertEquals(MemorySpaceAddressing.BYTE, device.getMemorySpaceAddressing("Global"));
        assertEquals(MemorySpaceAddressing.PRESET, device.getMemorySpaceAddressing("UserPresets"));
        assertEquals(127, device.getMaxBlockSize("CurrentPreset"));
        assertEquals(127, device.getMaxBlockSize("Global"));
        assertEquals(1546, device.getMaxBlockSize("UserPresets"));
        assertEquals(30, device.getPresetCount("UserPresets"));
        assertTrue(AkaiMemorySpace.USER_PRESETS.isPresetAddressed());
    }

    private static final class RecordingMidiOutPort extends MidiOutPort {
        private static final AtomicInteger COUNTER = new AtomicInteger();
        private final List<MidiEvent> events = new ArrayList<>();

        private RecordingMidiOutPort() {
            super("recording-" + COUNTER.incrementAndGet());
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

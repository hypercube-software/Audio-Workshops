package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.akai;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.sysex.checksum.DefaultChecksum;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.MemorySpaceAddressing;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;

import static com.hypercube.workshop.midiworkshop.api.sysex.util.SysExConstants.SYSEX_END;
import static com.hypercube.workshop.midiworkshop.api.sysex.util.SysExConstants.SYSEX_START;

/**
 * AKAI MPK-261.
 *
 * <p>The generic {@link Device} API targets the preset edit buffer
 * ({@code QEDIT}/{@code WEDIT}). The global edit buffer and the user presets
 * use their own commands, exposed by {@link #requestGlobalData},
 * {@link #sendGlobalData} and {@link #requestUserPreset}.
 *
 * <p>The 2 RAM spaces both start at address {@code 0x0000}; the command selects
 * the space. The link between commands and mmap space names lives in
 * {@link AkaiMemorySpace}.
 */
@Slf4j
public final class AkaiDevice extends Device {
    public static final int MODEL_MPK261 = 0x25;

    public static final int WPRESET = 0x10;
    public static final int ACKW = 0x11;
    public static final int QPRESET = 0x12;
    public static final int QGLOB = 0x20;
    public static final int QEDIT = 0x21;
    public static final int WGLOB = 0x30;
    public static final int WEDIT = 0x31;
    public static final int PPRESET = 0x32;
    public static final int ACKG = 0x38;
    public static final int ACKE = 0x39;
    public static final int ACKP = 0x3A;

    /**
     * The unit ignores the device ID on input and copies its own value in its
     * answers. The factory value is 0.
     */
    private static final int SYSEX_DEVICE_ID = 0x00;
    private static final int MAX_COUNT = 127;

    public AkaiDevice(Manufacturer manufacturer, String name, int code) {
        super(manufacturer, name, code);
    }

    @Override
    public void loadMemoryMap() {
        super.loadMemoryMap();
        AkaiMemorySpace.validate(getMemory());
    }

    @Override
    public void requestData(MidiOutPort midiOutPort, MemoryInt28 address, MemoryInt28 size) {
        requestMemorySpace(midiOutPort, AkaiMemorySpace.CURRENT_PRESET.getMmapName(), address, size);
    }

    @Override
    public void sendData(MidiOutPort midiOutPort, MemoryInt28 address, int value) {
        checkData(value);
        writeMessage(midiOutPort, WEDIT, 1, address, value);
    }

    @Override
    public MemorySpaceAddressing getMemorySpaceAddressing(String memorySpaceName) {
        return AkaiMemorySpace.fromMmapName(memorySpaceName)
                .isPresetAddressed() ? MemorySpaceAddressing.PRESET : MemorySpaceAddressing.BYTE;
    }

    @Override
    public int getMaxBlockSize(String memorySpaceName) {
        return AkaiMemorySpace.fromMmapName(memorySpaceName)
                .getMaxBlockSize();
    }

    @Override
    public void requestMemorySpace(MidiOutPort midiOutPort, String memorySpaceName, MemoryInt28 address, MemoryInt28 size) {
        AkaiMemorySpace memorySpace = AkaiMemorySpace.fromMmapName(memorySpaceName);
        if (memorySpace.isPresetAddressed()) {
            throw new MidiError("MPK-261 memory space %s is preset-addressed, use requestPreset".formatted(memorySpaceName));
        }
        int count = checkCount(size.value());
        writeMessage(midiOutPort, memorySpace.getReadCommand(), count, address);
    }

    @Override
    public void requestPreset(MidiOutPort midiOutPort, String memorySpaceName, int presetNumber) {
        AkaiMemorySpace memorySpace = AkaiMemorySpace.fromMmapName(memorySpaceName);
        if (!memorySpace.isPresetAddressed()) {
            throw new MidiError("MPK-261 memory space %s is not preset-addressed".formatted(memorySpaceName));
        }
        requestUserPreset(midiOutPort, presetNumber);
    }

    /**
     * Read a block of the global edit buffer ({@code QGLOB}).
     */
    public void requestGlobalData(MidiOutPort midiOutPort, MemoryInt28 address, MemoryInt28 size) {
        requestMemorySpace(midiOutPort, AkaiMemorySpace.GLOBAL.getMmapName(), address, size);
    }

    /**
     * Write one byte of the global edit buffer ({@code WGLOB}).
     */
    public void sendGlobalData(MidiOutPort midiOutPort, MemoryInt28 address, int value) {
        checkData(value);
        writeMessage(midiOutPort, WGLOB, 1, address, value);
    }

    /**
     * Ask for a user preset dump ({@code QPRESET}). The unit answers with a
     * {@code WPRESET} frame.
     *
     * @param presetNumber 1 to 30, or 0 for the current preset
     */
    public void requestUserPreset(MidiOutPort midiOutPort, int presetNumber) {
        if (presetNumber < 0 || presetNumber > 30) {
            throw new MidiError("Invalid MPK-261 preset number: " + presetNumber);
        }
        MidiEventBuilder builder = new MidiEventBuilder(new DefaultChecksum());
        builder.write(SYSEX_START, manufacturer.getCode(), SYSEX_DEVICE_ID, code, QPRESET);
        builder.write(0x00, 0x01); // payload size: one byte
        builder.write(presetNumber);
        builder.write(SYSEX_END);
        send(midiOutPort, builder);
    }

    private void writeMessage(MidiOutPort midiOutPort, int command, int count, MemoryInt28 address, int... data) {
        int payloadSize = 3 + data.length; // count + address + data
        MidiEventBuilder builder = new MidiEventBuilder(new DefaultChecksum());
        builder.write(SYSEX_START, manufacturer.getCode(), SYSEX_DEVICE_ID, code, command);
        builder.write(payloadSize >> 7, payloadSize & 0x7F);
        builder.write(count);
        builder.write(MemoryInt28.from(address.value(), 2)
                .getPackedBytes());
        if (data.length > 0) {
            builder.write(data);
        }
        builder.write(SYSEX_END);
        send(midiOutPort, builder);
    }

    private void send(MidiOutPort midiOutPort, MidiEventBuilder builder) {
        try {
            CustomMidiEvent event = builder.buildMidiEvent();
            log.info("Send: {}", event.getHexValues());
            midiOutPort.send(event);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    private int checkCount(int count) {
        if (count < 1 || count > MAX_COUNT) {
            throw new MidiError("MPK-261 block count must be between 1 and %d, got %d"
                    .formatted(MAX_COUNT, count));
        }
        return count;
    }

    private void checkData(int value) {
        if (value < 0 || value > 0x7F) {
            throw new MidiError("MPK-261 data byte must be between 0 and 127, got " + value);
        }
    }
}

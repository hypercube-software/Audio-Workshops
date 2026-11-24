package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.roland;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.sysex.checksum.DefaultChecksum;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;

import static com.hypercube.workshop.midiworkshop.api.sysex.util.SysExConstants.SYSEX_END;
import static com.hypercube.workshop.midiworkshop.api.sysex.util.SysExConstants.SYSEX_START;

/**
 * A Roland device using the One-Way RQ1/DT1 protocol.
 *
 * <p>Most Roland machines use a 1-byte model ID, device ID {@code 0x00} and
 * 3-byte packed addresses. The SE-02 uses a 4-byte model ID,
 * device ID {@code 0x10} and 4-byte packed addresses, hence the configurable
 * frame layout.
 */
@SuppressWarnings("java:S2160")
@Slf4j
public class RolandDevice extends Device {
    private static final int DEFAULT_DEVICE_ID = 0x00;
    private static final int DEFAULT_ADDRESS_BYTE_COUNT = 3;
    private static final int DEFAULT_VALUE_BYTE_COUNT = 1;

    private final int sysexDeviceId;
    private final int[] modelId;
    private final int addressByteCount;
    private final int valueByteCount;

    public RolandDevice(Manufacturer manufacturer, String name, int code) {
        this(manufacturer, name, code, DEFAULT_DEVICE_ID, new int[]{code},
                DEFAULT_ADDRESS_BYTE_COUNT, DEFAULT_VALUE_BYTE_COUNT);
    }

    public RolandDevice(Manufacturer manufacturer, String name, int code,
                        int sysexDeviceId, int[] modelId, int addressByteCount, int valueByteCount) {
        super(manufacturer, name, code);
        this.sysexDeviceId = sysexDeviceId;
        this.modelId = modelId;
        this.addressByteCount = addressByteCount;
        this.valueByteCount = valueByteCount;
    }

    public int getSysexDeviceId() {
        return sysexDeviceId;
    }

    public int[] getModelId() {
        return modelId;
    }

    public int getAddressByteCount() {
        return addressByteCount;
    }

    public int getValueByteCount() {
        return valueByteCount;
    }

    @Override
    public void requestData(MidiOutPort midiOutPort, MemoryInt28 address, MemoryInt28 size) {
        // This is what Roland call a One-Way request Data: RQ1
        MidiEventBuilder sb = new MidiEventBuilder(new DefaultChecksum());
        sb.write(SYSEX_START, Manufacturer.ROLAND.getCode(), sysexDeviceId);
        sb.write(modelId);
        sb.write(0x11);
        sb.beginChecksum();
        sb.write(address.getPackedBytes());
        sb.write(size.getPackedBytes());
        sb.writeChecksum();
        sb.write(SYSEX_END);

        try {
            CustomMidiEvent evt = sb.buildMidiEvent();
            logHex(evt);
            midiOutPort.send(evt);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }

    }

    @Override
    public void sendData(MidiOutPort midiOutPort, MemoryInt28 address, int value) {
        // This is what Roland call a One-Way Data Set: DT1
        MidiEventBuilder sb = new MidiEventBuilder(new DefaultChecksum());
        sb.write(SYSEX_START, Manufacturer.ROLAND.getCode(), sysexDeviceId);
        sb.write(modelId);
        sb.write(0x12);
        sb.beginChecksum();
        sb.write(address.getPackedBytes());
        if (valueByteCount == 2) {
            sb.write((value >> 4) & 0x0F, value & 0x0F);
        } else {
            sb.write(value);
        }
        sb.writeChecksum();
        sb.write(SYSEX_END);

        try {
            CustomMidiEvent evt = sb.buildMidiEvent();
            logHex(evt);
            midiOutPort.send(evt);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }

    }

    private void logHex(CustomMidiEvent evt) {
        log.info("Send: {}", evt.getHexValues());
    }
}

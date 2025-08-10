package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.roland;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.checksum.DefaultChecksum;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt24;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;

import static com.hypercube.workshop.midiworkshop.api.sysex.util.SysExConstants.SYSEX_END;
import static com.hypercube.workshop.midiworkshop.api.sysex.util.SysExConstants.SYSEX_START;

@SuppressWarnings("java:S2160")
@Slf4j
public final class RolandDevice extends Device {
    public RolandDevice(Manufacturer manufacturer, String name, int code) {
        super(manufacturer, name, code);
    }

    @Override
    public void requestData(MidiOutDevice midiOutDevice, MemoryInt24 address, MemoryInt24 size) {
        // This is what Roland call a One-Way request Data: RQ1
        SysExBuilder sb = new SysExBuilder(new DefaultChecksum());
        sb.write(SYSEX_START, Manufacturer.ROLAND.getCode(), 0x00, code, 0x11);
        sb.beginChecksum();
        sb.write(address.getPackedBytes());
        sb.write(size.getPackedBytes());
        sb.writeChecksum();
        sb.write(SYSEX_END);

        try {
            CustomMidiEvent evt = sb.buildMidiEvent();
            log.info("Send: " + evt.getHexValues());
            midiOutDevice.send(evt);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }

    }

    @Override
    public void sendData(MidiOutDevice midiOutDevice, MemoryInt24 address, int value) {
        // This is what Roland call a One-Way Data Set: DT1
        SysExBuilder sb = new SysExBuilder(new DefaultChecksum());
        sb.write(SYSEX_START, Manufacturer.ROLAND.getCode(), 0x00, code, 0x12);
        sb.beginChecksum();
        sb.write(address.getPackedBytes());
        sb.write(value);
        sb.writeChecksum();
        sb.write(SYSEX_END);

        try {
            CustomMidiEvent evt = sb.buildMidiEvent();
            log.info("Send: " + evt.getHexValues());
            midiOutDevice.send(evt);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }

    }
}

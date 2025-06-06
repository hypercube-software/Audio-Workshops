package com.hypercube.workshop.midiworkshop.common.sysex.manufacturer.behringer;

import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.sysex.checksum.DefaultChecksum;
import com.hypercube.workshop.midiworkshop.common.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.common.sysex.device.memory.primitives.MemoryInt24;
import com.hypercube.workshop.midiworkshop.common.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;

import static com.hypercube.workshop.midiworkshop.common.sysex.util.SysExConstants.SYSEX_END;
import static com.hypercube.workshop.midiworkshop.common.sysex.util.SysExConstants.SYSEX_START;

@Slf4j
public class BehringerDevice extends Device {

    public BehringerDevice(Manufacturer manufacturer, String name, int code) {
        super(manufacturer, name, code);
    }

    public void requestData(MidiOutDevice midiOutDevice, MemoryInt24 address, MemoryInt24 size) {
        // REQUEST
        // F0 00 20 32 28 7F 05 F7

        SysExBuilder sb = new SysExBuilder(new DefaultChecksum());
        sb.write(SYSEX_START, 0x00, 0x20, 0x32, 0x28, 0x7F, 0x05, SYSEX_END);
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
        throw new UnsupportedOperationException();
    }
}

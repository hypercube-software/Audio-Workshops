package com.hypercube.workshop.midiworkshop.sysex.manufacturer.roland;

import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.cheksum.DefaultChecksum;
import com.hypercube.workshop.midiworkshop.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives.MemoryInt24;
import com.hypercube.workshop.midiworkshop.sysex.util.SysExBuilder;
import org.jline.utils.Log;

import javax.sound.midi.InvalidMidiDataException;

import static com.hypercube.workshop.midiworkshop.sysex.util.SystemExclusiveConstants.SYSEX_END;
import static com.hypercube.workshop.midiworkshop.sysex.util.SystemExclusiveConstants.SYSEX_START;

@SuppressWarnings("java:S2160")
public final class RolandDevice extends Device {
    public RolandDevice(Manufacturer manufacturer, String name, int code) {
        super(manufacturer, name, code);
    }

    public void requestData(MidiOutDevice midiOutDevice, MemoryInt24 address, MemoryInt24 size) {
        // REQUEST
        // F0 41 00 55 11 20 01 00 00 01 00 5E F7
        // F0 BEGIN
        // 41 = Roland
        // 00 = Device
        // 55 = Model DS-330
        // 11 = Request Data RQ1
        // 20 01 00 = addr
        // 00 01 00 = size
        // 5E = checksum

        // RESPONSE
        // F0 41 00 55 12 20 01 00 00 04 00 00 40 01 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 19 F7
        // F0 BEGIN
        // 41 = Roland
        // 00 = Device
        // 55 = Model DS-330
        // 12 = Data Set DT1
        // 20 01 00 = address
        // 04 00 00 40 01 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00  = payload
        // 19 = checksum
        // F7 END

        // (checksum + payload + address) & 0x7F = 0

        SysExBuilder sb = new SysExBuilder(new DefaultChecksum());
        sb.write(SYSEX_START, Manufacturer.ROLAND.getCode(), 0x00, 0x42, 0x11);
        sb.beginChecksum();
        sb.write(address.getPackedBytes());
        sb.write(size.getPackedBytes());
        sb.writeChecksum();
        sb.write(SYSEX_END);

        try {
            CustomMidiEvent evt = sb.buildMidiEvent();
            Log.info("Send: " + evt.getHexValues());
            midiOutDevice.send(evt);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }

    }
}

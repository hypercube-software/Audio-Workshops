package com.hypercube.workshop.midiworkshop.sysex.manufacturer.roland;

import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.cheksum.DefaultChecksum;
import com.hypercube.workshop.midiworkshop.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives.MemoryInt24;
import com.hypercube.workshop.midiworkshop.sysex.manufacturer.Manufacturer;
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
            Log.info("Send: " + evt.getHexValues());
            midiOutDevice.send(evt);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }

    }
}

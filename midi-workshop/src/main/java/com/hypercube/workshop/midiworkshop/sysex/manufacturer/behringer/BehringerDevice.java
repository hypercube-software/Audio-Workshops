package com.hypercube.workshop.midiworkshop.sysex.manufacturer.behringer;

import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives.MemoryInt24;
import org.jline.utils.Log;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.hypercube.workshop.midiworkshop.sysex.util.SystemExclusiveConstants.SYSEX_END;
import static com.hypercube.workshop.midiworkshop.sysex.util.SystemExclusiveConstants.SYSEX_START;

public class BehringerDevice extends Device {

    public BehringerDevice(Manufacturer manufacturer, String name, int code) {
        super(manufacturer, name, code);
    }

    public void requestData(MidiOutDevice midiOutDevice, MemoryInt24 address, MemoryInt24 size) {
        // REQUEST
        // F0 00 20 32 28 7F 05 F7

        List<Integer> bytes = new ArrayList<>();
        bytes.addAll(List.of(SYSEX_START, 0x00, 0x20, 0x32, 0x28, 0x7F, 0x05, SYSEX_END));
        byte[] data = new byte[bytes.size()];
        IntStream.range(0, data.length)
                .forEach(idx -> data[idx] = bytes.get(idx)
                        .byteValue());

        try {
            CustomMidiEvent evt = new CustomMidiEvent(new SysexMessage(data, data.length), -1);
            Log.info("Send: " + evt.getHexValues());
            midiOutDevice.send(evt);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }

    }
}

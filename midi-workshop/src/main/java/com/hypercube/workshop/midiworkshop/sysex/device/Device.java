package com.hypercube.workshop.midiworkshop.sysex.device;

import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.map.MemoryMapParser;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives.MemoryInt24;
import com.hypercube.workshop.midiworkshop.sysex.manufacturer.Manufacturer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jline.utils.Log;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.io.File;
import java.util.List;
import java.util.stream.IntStream;

import static com.hypercube.workshop.midiworkshop.sysex.util.SystemExclusiveConstants.*;

/**
 * {@link Device} cannot be enum like {@link Manufacturer} because it is mutable via {@link Device#memory}
 */

@Getter
@EqualsAndHashCode
public abstract class Device {
    @EqualsAndHashCode.Include
    protected final Manufacturer manufacturer;
    @EqualsAndHashCode.Exclude
    protected final String name;
    @EqualsAndHashCode.Include
    protected final int code;

    @Setter
    @EqualsAndHashCode.Exclude
    protected DeviceMemory memory;

    protected Device(Manufacturer manufacturer, String name, int code) {
        this.manufacturer = manufacturer;
        this.name = name;
        this.code = code;
    }

    @Override
    public String toString() {
        return "%s [0x%02X %s]".formatted(getClass().getSimpleName(), code, name);
    }

    /**
     * @param midiOutDevice where to send the request
     * @param sysExChannel  between [0-15] or 127 for broadcast
     */
    public void sendIdentityRequest(MidiOutDevice midiOutDevice, int sysExChannel) {
        List<Integer> bytes = List.of(SYSEX_START, SYSEX_NON_REALTIME, sysExChannel, SYSEX_GENERAL_INFORMATION, SYSEX_IDENTITY_REQUEST, SYSEX_END);
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

    public abstract void requestData(MidiOutDevice midiOutDevice, MemoryInt24 address, MemoryInt24 size);

    public void loadMemoryMap() {
        String manufacturerName = manufacturer.getTitle();
        var memoryMap = new File("midi-workshop/sysex/%s/%s/%s.mmap".formatted(manufacturerName, name, name));
        if (!memoryMap.exists()) {
            memoryMap = new File("sysex/%s/%s/%s.mmap".formatted(manufacturerName, name, name));
            if (!memoryMap.exists()) {
                throw new MidiError("Memory map is missing: " + memoryMap.getAbsolutePath());
            }
        }
        var deviceMemory = MemoryMapParser.load(memoryMap);
        setMemory(deviceMemory);
    }
}

package com.hypercube.workshop.midiworkshop.api.sysex.device;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMapParser;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt24;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.parser.SysExParser;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.hypercube.workshop.midiworkshop.api.sysex.util.SysExConstants.*;

/**
 * This class represent a MIDI Device
 * <ul>
 *     <li>We can read and write in its virtual memory stored in a {@link DeviceMemory}</li>
 *     <li>Its memory layout is stored in a {@link MemoryMap}</li>
 * </ul>
 * Note: {@link Device} cannot be enum like {@link Manufacturer} because it is mutable via {@link Device#memory}
 */

@Getter
@EqualsAndHashCode
@Slf4j
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

    /**
     * @param midiOutDevice where to send the request
     * @param sysExChannel  between [0-15] or 127 for broadcast
     */
    public static void sendIdentityRequest(MidiOutDevice midiOutDevice, int sysExChannel) {
        List<Integer> bytes = List.of(SYSEX_START, SYSEX_NON_REALTIME, sysExChannel, SYSEX_GENERAL_INFORMATION, SYSEX_IDENTITY_REQUEST, SYSEX_END);
        byte[] data = new byte[bytes.size()];
        IntStream.range(0, data.length)
                .forEach(idx -> data[idx] = bytes.get(idx)
                        .byteValue());

        try {
            CustomMidiEvent evt = new CustomMidiEvent(new SysexMessage(data, data.length));
            log.info("Send: " + evt.getHexValues());
            midiOutDevice.send(evt);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    @Override
    public String toString() {
        return "%s [0x%02X %s]".formatted(getClass().getSimpleName(), code, name);
    }

    /**
     * Send an appropriate SysEx message to query the memory content of a MIDI device
     *
     * @param midiOutDevice MIDI port to a real MIDI hardware
     * @param address       Memory address to query
     * @param size          Size of the data to retrieve
     */
    public abstract void requestData(MidiOutDevice midiOutDevice, MemoryInt24 address, MemoryInt24 size);

    /**
     * Send an appropriate SysEx message to write the memory content of a MIDI device
     *
     * @param midiOutDevice MIDI port to a real MIDI hardware
     * @param address       Memory address to write
     * @param value         value to send
     */
    public abstract void sendData(MidiOutDevice midiOutDevice, MemoryInt24 address, int value);

    /**
     * We store device memory maps in predefined folders
     */
    public void loadMemoryMap() {
        String manufacturerName = manufacturer.getTitle();
        String sysexFolder = getSysExFolder().getAbsolutePath();
        var memoryMap = new File("%s/%s/%s/%s.mmap".formatted(sysexFolder, manufacturerName, name, name));
        var deviceMemory = MemoryMapParser.load(memoryMap);
        setMemory(deviceMemory);
    }

    public int requestMemory(MidiInDevice in, MidiOutDevice out, MemoryInt24 address, MemoryInt24 size) {
        try {
            if (in == null)
                return 0;
            final CountDownLatch sysExReceived = new CountDownLatch(1);
            final MidiListener listener = (device, event) -> {
                log.info("Receive SYSEX:" + event.toString());
                SysExParser sysExParser = new SysExParser();
                Device d = sysExParser.parse(event.getMessage());
                assert (d == this);
                sysExReceived.countDown();
            };
            in.addSysExListener(listener);
            requestData(out, address, size);
            boolean timedOut = !sysExReceived.await(500, TimeUnit.MILLISECONDS);
            in.removeListener(listener);
            if (timedOut) {
                log.error("Timeout waiting SysEx...");
                return 0;
            }
            return memory.readByte(address);
        } catch (InterruptedException e) {
            throw new MidiError(e);
        }
    }

    private File getSysExFolder() {
        String[] folders = new String[]{System.getProperty("SYSEX_FOLDER"), "./sysex", "../sysex"};
        return Arrays.stream(folders)
                .map(Optional::ofNullable)
                .flatMap(Optional::stream)
                .map(File::new)
                .filter(File::exists)
                .findFirst()
                .orElseThrow(() -> new MidiError("SysEx folder not found"));
    }
}

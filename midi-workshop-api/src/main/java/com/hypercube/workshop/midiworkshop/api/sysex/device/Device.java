package com.hypercube.workshop.midiworkshop.api.sysex.device;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.MemorySpace;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.MemorySpaceAddressing;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryField;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMapParser;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
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
    private static final long SYSEX_TIMEOUT_MS = 2_000;
    private static final long SYSEX_TIMEOUT_PER_FRAME_MS = 1_000;

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
     * @param midiOutPort  where to send the request
     * @param sysExChannel between [0-15] or 127 for broadcast
     */
    public static void sendIdentityRequest(MidiOutPort midiOutPort, int sysExChannel) {
        List<Integer> bytes = List.of(SYSEX_START, SYSEX_NON_REALTIME, sysExChannel, SYSEX_GENERAL_INFORMATION, SYSEX_IDENTITY_REQUEST, SYSEX_END);
        byte[] data = new byte[bytes.size()];
        IntStream.range(0, data.length)
                .forEach(idx -> data[idx] = bytes.get(idx)
                        .byteValue());

        try {
            CustomMidiEvent evt = new CustomMidiEvent(new SysexMessage(data, data.length));
            log.info("Send: {}", evt.getHexValues());
            midiOutPort.send(evt);
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
     * @param midiOutPort MIDI port to a real MIDI hardware
     * @param address     Memory address to query
     * @param size        Size of the data to retrieve
     */
    public abstract void requestData(MidiOutPort midiOutPort, MemoryInt28 address, MemoryInt28 size);

    /**
     * Send an appropriate SysEx message to write the memory content of a MIDI device
     *
     * @param midiOutPort MIDI port to a real MIDI hardware
     * @param address     Memory address to write
     * @param value       value to send
     */
    public abstract void sendData(MidiOutPort midiOutPort, MemoryInt28 address, int value);

    /**
     * @return how the space is addressed on the wire
     */
    public MemorySpaceAddressing getMemorySpaceAddressing(String memorySpaceName) {
        return MemorySpaceAddressing.BYTE;
    }

    /**
     * Maximum number of bytes the device accepts in one memory request.
     * The default is unlimited: the whole zone is requested at once.
     */
    public int getMaxBlockSize(String memorySpaceName) {
        return Integer.MAX_VALUE;
    }

    /**
     * Send a read request for a byte-addressed memory space.
     *
     * <p>The device implementation selects the SysEx command from the space
     * name; the caller never builds a MIDI frame.
     */
    public void requestMemorySpace(MidiOutPort midiOutPort, String memorySpaceName, MemoryInt28 address, MemoryInt28 size) {
        requestData(midiOutPort, address, size);
    }

    /**
     * Send a read request for a preset-addressed memory space.
     *
     * @param presetNumber preset number, or 0 for the current preset
     */
    public void requestPreset(MidiOutPort midiOutPort, String memorySpaceName, int presetNumber) {
        throw new MidiError("Memory space %s is not addressed by preset number".formatted(memorySpaceName));
    }

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

    /**
     * Read a value from the default memory space.
     *
     * @deprecated prefer {@link #requestMemory(MidiInPort, MidiOutPort, String, MemoryInt28, MemoryInt28)}
     * so the space name is explicit.
     */
    @Deprecated
    public int requestMemory(MidiInPort midiInPort, MidiOutPort midiOutPort, MemoryInt28 address, MemoryInt28 size) {
        return requestMemory(midiInPort, midiOutPort, null, address, size);
    }

    /**
     * Read a value from a memory space, splitting the request when the device
     * imposes a maximum block size.
     *
     * @param memorySpaceName space name, or {@code null}/blank for the default
     */
    public int requestMemory(MidiInPort midiInPort, MidiOutPort midiOutPort, String memorySpaceName,
                             MemoryInt28 address, MemoryInt28 size) {
        if (midiInPort == null) {
            return 0;
        }
        MemorySpace memorySpace = memory.resolveMemorySpace(memorySpaceName);
        if (getMemorySpaceAddressing(memorySpace.getName()) == MemorySpaceAddressing.PRESET) {
            throw new MidiError("Memory space %s is preset-addressed, use requestPresetMemory".formatted(memorySpace.getName()));
        }
        int total = size.value();
        if (total < 1) {
            throw new MidiError("Invalid memory size: " + total);
        }
        int maxBlockSize = getMaxBlockSize(memorySpace.getName());
        if (maxBlockSize < 1) {
            throw new MidiError("Invalid max block size for memory space %s: %d".formatted(memorySpace.getName(), maxBlockSize));
        }
        int blocks = (int) (((long) total + maxBlockSize - 1) / maxBlockSize);
        boolean received = receiveFrames(midiInPort, blocks, () -> {
            int offset = 0;
            while (offset < total) {
                int blockSize = Math.min(maxBlockSize, total - offset);
                MemoryInt28 blockAddress = MemoryInt28.from(address.value() + offset, address.byteCount());
                MemoryInt28 blockSizeValue = MemoryInt28.from(blockSize, address.byteCount());
                requestMemorySpace(midiOutPort, memorySpace.getName(), blockAddress, blockSizeValue);
                offset += blockSize;
            }
        });
        if (!received) {
            log.error("Timeout waiting SysEx...");
            return 0;
        }
        return memory.readValueByte(memorySpace.getName(), address);
    }

    /**
     * Number of presets declared by the mmap for a preset-addressed memory
     * space. A preset-addressed space is a single zone whose first array field
     * references the preset structure.
     */
    public int getPresetCount(String memorySpaceName) {
        MemorySpace memorySpace = memory.resolveMemorySpace(memorySpaceName);
        return memorySpace.getMemoryMaps()
                .stream()
                .flatMap(zone -> zone.getFields()
                        .stream())
                .filter(MemoryField::isArray)
                .filter(MemoryField::isReference)
                .findFirst()
                .map(field -> field.getArray()
                        .size())
                .orElseThrow(() -> new MidiError("No preset array in memory space %s".formatted(memorySpace.getName())));
    }

    /**
     * Read a value from a preset-addressed memory space.
     *
     * <p>The device has no command for a whole-space dump: every stored preset
     * is requested in turn (preset 1 to N), then the value is read from the
     * parsed memory. The address is relative to the start of the space, so it
     * can span two consecutive presets.
     */
    public int requestPresetMemory(MidiInPort midiInPort, MidiOutPort midiOutPort, String memorySpaceName,
                                   MemoryInt28 address) {
        if (midiInPort == null) {
            return 0;
        }
        MemorySpace memorySpace = memory.resolveMemorySpace(memorySpaceName);
        if (getMemorySpaceAddressing(memorySpace.getName()) != MemorySpaceAddressing.PRESET) {
            throw new MidiError("Memory space %s is not preset-addressed".formatted(memorySpace.getName()));
        }
        int presetCount = getPresetCount(memorySpace.getName());
        for (int i = 1; i <= presetCount; i++) {
            final int presetNumber = i;
            boolean received = receiveFrames(midiInPort, 1,
                    () -> requestPreset(midiOutPort, memorySpace.getName(), presetNumber));
            if (!received) {
                log.error("Timeout waiting SysEx for memory space {} preset {}", memorySpace.getName(), presetNumber);
                return 0;
            }
        }
        return memory.readValueByte(memorySpace.getName(), address);
    }

    /**
     * Send the requests and wait for the matching answers, which are parsed
     * into {@link #memory} by the manufacturer parser.
     */
    private boolean receiveFrames(MidiInPort midiInPort, int expectedFrames, Runnable sender) {
        final CountDownLatch sysExReceived = new CountDownLatch(expectedFrames);
        final MidiListener listener = (device, event) -> {
            if (!(event.getMessage() instanceof SysexMessage)) {
                // MIDI clock (F8), active sensing and other real-time messages
                // reach the same listener; they are not SysEx and must not be
                // parsed as such.
                return;
            }
            log.info("Receive SYSEX:{}", event.toString());
            try {
                Device parsed = new SysExParser().parse(event.getMessage());
                if (equals(parsed)) {
                    sysExReceived.countDown();
                }
            } catch (RuntimeException e) {
                // The raw frame is still available on disk; do not block the
                // caller if a device sends an unrelated frame.
                log.warn("Unable to parse SysEx: {}", e.getMessage());
            }
        };
        midiInPort.addSysExListener(listener);
        try {
            sender.run();
            long timeout = SYSEX_TIMEOUT_MS + (long) expectedFrames * SYSEX_TIMEOUT_PER_FRAME_MS;
            return sysExReceived.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MidiError(e);
        } finally {
            midiInPort.removeListener(listener);
        }
    }

    private File getSysExFolder() {
        String[] folders = new String[]{System.getProperty("SYSEX_FOLDER"), "./sysex", "../sysex", "../../sysex"};
        return Arrays.stream(folders)
                .map(Optional::ofNullable)
                .flatMap(Optional::stream)
                .map(File::new)
                .filter(File::exists)
                .findFirst()
                .orElseThrow(() -> new MidiError("SysEx folder not found"));
    }
}

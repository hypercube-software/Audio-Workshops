package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.akai;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.DeviceMemory;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * Link between the AKAI SysEx commands and the memory spaces of the mmap.
 *
 * <p>The mmap only describes the layout. This enum owns the protocol
 * knowledge: which command reads or writes a space, the maximum block size and
 * whether the space is addressed by byte offset or by preset number.
 *
 * <p>The {@link #mmapName} values are the contract with
 * {@code MPK-261.mmap}; {@link #validate(DeviceMemory)} fails at load time if
 * the file and the enum diverge.
 */
@Getter
public enum AkaiMemorySpace {
    CURRENT_PRESET("CurrentPreset", "Preset Edit Buffer", AkaiDevice.QEDIT, AkaiDevice.WEDIT, AkaiDevice.WEDIT, 127, false),
    GLOBAL("Global", "Global Settings", AkaiDevice.QGLOB, AkaiDevice.WGLOB, AkaiDevice.WGLOB, 127, false),
    USER_PRESETS("UserPresets", "User Preset", AkaiDevice.QPRESET, AkaiDevice.WPRESET, AkaiDevice.WPRESET, 1546, true);

    private final String mmapName;
    private final String zoneName;
    private final int readCommand;
    private final int writeCommand;
    private final int answerCommand;
    private final int maxBlockSize;
    private final boolean presetAddressed;

    AkaiMemorySpace(String mmapName, String zoneName, int readCommand, int writeCommand, int answerCommand,
                    int maxBlockSize, boolean presetAddressed) {
        this.mmapName = mmapName;
        this.zoneName = zoneName;
        this.readCommand = readCommand;
        this.writeCommand = writeCommand;
        this.answerCommand = answerCommand;
        this.maxBlockSize = maxBlockSize;
        this.presetAddressed = presetAddressed;
    }

    public static Optional<AkaiMemorySpace> fromAnswer(int command) {
        return Arrays.stream(values())
                .filter(memorySpace -> memorySpace.answerCommand == command)
                .findFirst();
    }

    public static AkaiMemorySpace fromMmapName(String mmapName) {
        return Arrays.stream(values())
                .filter(memorySpace -> memorySpace.mmapName.equals(mmapName))
                .findFirst()
                .orElseThrow(() -> new MidiError("Unknown AKAI memory space: " + mmapName));
    }

    /**
     * Check that the mmap declares exactly the memory spaces known by the
     * protocol.
     */
    public static void validate(DeviceMemory memory) {
        for (AkaiMemorySpace memorySpace : values()) {
            if (!memory.hasMemorySpace(memorySpace.mmapName)) {
                throw new MidiError("AKAI memory space missing from the mmap: " + memorySpace.mmapName);
            }
            // Fail at load time if the mmap renames the zone.
            memory.getMemoryMap(memorySpace.mmapName, memorySpace.zoneName);
        }
        memory.getMemorySpaces()
                .forEach(memorySpace -> fromMmapName(memorySpace.getName()));
    }
}

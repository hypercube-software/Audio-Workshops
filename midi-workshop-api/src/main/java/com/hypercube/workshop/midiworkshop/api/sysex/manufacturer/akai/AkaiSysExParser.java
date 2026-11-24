package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.akai;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryField;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.parser.ManufacturerSysExParser;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExReader;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

import static com.hypercube.workshop.midiworkshop.api.sysex.util.SysExConstants.SYSEX_END;

/**
 * Parse the AKAI MPK-261 memory frames.
 *
 * <p>The parser handles the answers and writes that carry memory data:
 * <ul>
 *     <li>{@code WPRESET}: a user preset record;</li>
 *     <li>{@code WEDIT}: a block of the preset edit buffer;</li>
 *     <li>{@code WGLOB}: a block of the global edit buffer;</li>
 *     <li>{@code ACKW}, {@code ACKG}, {@code ACKE}, {@code ACKP}: no data.</li>
 * </ul>
 *
 * <p>The 2 RAM spaces both start at {@code 0x0000}; the memory space is
 * selected by the command through {@link AkaiMemorySpace}.
 */
@Slf4j
public class AkaiSysExParser extends ManufacturerSysExParser {
    @Override
    public Device parse(Manufacturer manufacturer, SysExReader buffer) {
        int deviceId = buffer.getByte();
        int model = buffer.getByte();
        int command = buffer.getByte();
        int payloadSize = buffer.getPackedInt(2);
        byte[] payload = buffer.getBytes(payloadSize);
        int end = buffer.getByte();
        if (end != SYSEX_END) {
            throw new MidiError("Unexpected AKAI frame end: 0x%02X".formatted(end));
        }

        AkaiDevice device = manufacturer.getDevice(model);
        if (device.getMemory() == null) {
            device.loadMemoryMap();
        }

        switch (command) {
            case AkaiDevice.WPRESET -> writeUserPreset(device, payload);
            case AkaiDevice.WEDIT, AkaiDevice.WGLOB -> {
                AkaiMemorySpace memorySpace = AkaiMemorySpace.fromAnswer(command)
                        .orElseThrow(() -> new MidiError("No AKAI memory space for command 0x%02X".formatted(command)));
                writeBlock(device, memorySpace, payload);
            }
            case AkaiDevice.ACKW, AkaiDevice.ACKG, AkaiDevice.ACKE, AkaiDevice.ACKP -> {
                // Acknowledges carry no memory data
            }
            default -> log.warn("Unsupported AKAI command 0x%02X (device id 0x%02X)".formatted(command, deviceId));
        }
        return device;
    }

    private void writeBlock(AkaiDevice device, AkaiMemorySpace memorySpace, byte[] payload) {
        SysExReader reader = new SysExReader(ByteBuffer.wrap(payload));
        int count = reader.getByte();
        int address = reader.getPackedInt(2);
        DeviceMemory memory = device.getMemory();
        MemoryMap map = memory.getMemoryMap(memorySpace.getMmapName(), memorySpace.getZoneName());
        if (address + count > map.getSize()
                .value()) {
            throw new MidiError("AKAI block out of range for %s/%s: 0x%04X + %d"
                    .formatted(memorySpace.getMmapName(), memorySpace.getZoneName(), address, count));
        }
        for (int i = 0; i < count; i++) {
            map.writeByte(MemoryInt28.from(address + i, 2), reader.getByte());
        }
    }

    private void writeUserPreset(AkaiDevice device, byte[] payload) {
        SysExReader reader = new SysExReader(ByteBuffer.wrap(payload));
        int presetNumber = reader.getByte();
        MemoryMap map = device.getMemory()
                .getMemoryMap(AkaiMemorySpace.USER_PRESETS.getMmapName(), AkaiMemorySpace.USER_PRESETS.getZoneName());
        MemoryField presetArray = findPresetArray(map);
        int presetCount = presetArray.getArray()
                .size();
        int presetSize = presetArray.getEffectiveSize()
                .value();
        if (reader.remaining() != presetSize) {
            throw new MidiError("AKAI preset payload has %d bytes, expected %d".formatted(reader.remaining(), presetSize));
        }
        // WPRESET 0 is the edit buffer, not a stored preset. Never write it
        // into Preset[0].
        if (presetNumber < 1 || presetNumber > presetCount) {
            log.warn("Ignore AKAI user preset {}: stored presets are 1 to {}", presetNumber, presetCount);
            return;
        }
        int base = (presetNumber - 1) * presetSize;
        int byteCount = map.getBaseAddress()
                .byteCount();
        for (int i = 0; i < presetSize; i++) {
            map.writeByte(MemoryInt28.from(base + i, byteCount), reader.getByte());
        }
        log.debug("Parsed AKAI user preset {}", presetNumber);
    }

    private MemoryField findPresetArray(MemoryMap map) {
        return map.getFields()
                .stream()
                .filter(MemoryField::isArray)
                .filter(MemoryField::isReference)
                .findFirst()
                .orElseThrow(() -> new MidiError("AKAI mmap zone %s has no preset array".formatted(map.getName())));
    }
}

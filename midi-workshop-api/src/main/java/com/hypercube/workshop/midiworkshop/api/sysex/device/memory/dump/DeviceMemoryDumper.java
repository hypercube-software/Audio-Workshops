package com.hypercube.workshop.midiworkshop.api.sysex.device.memory.dump;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.MemorySpace;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryField;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMapFormat;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
public class DeviceMemoryDumper {
    public static final String SEPARATOR = "-------------------------------------";
    private final DeviceMemory memory;

    public void visitMemory(DeviceMemoryVisitor visitor) {
        MemoryInt28 addr = MemoryInt28.from(0);
        memory.getMemorySpaces()
                .forEach(memorySpace -> memorySpace.getMemoryMaps()
                        .forEach(memoryMap -> {
                            visitor.onNewTopLevelMemoryMap(memoryMap);
                            dumpMemory(memorySpace.getName(), memoryMap.getName(), addr, memoryMap, visitor);
                        }));
    }

    public void dumpMemory(File dump) {
        log.info("Dump memory to " + dump.getAbsolutePath());
        if (!dump.getParentFile()
                .exists() && !dump.getParentFile()
                .mkdirs()) {
            throw new MidiError("Unable to create " + dump.getAbsolutePath());
        }
        try (PrintWriter out = new PrintWriter(new FileOutputStream(dump))) {
            out.println(SEPARATOR);
            out.println(memory.getName() + " Memory dump");

            DeviceMemoryVisitor visitor = new DeviceMemoryVisitor() {
                private String memorySpaceName = MemorySpace.DEFAULT_NAME;

                @Override
                public void onNewTopLevelMemoryMap(MemoryMap memoryMap) {
                    memorySpaceName = findMemorySpaceName(memoryMap);
                    out.println(SEPARATOR);
                    out.println("[%s] %s".formatted(memorySpaceName, memoryMap));
                    out.println(SEPARATOR);
                }

                @Override
                public void onNewEntry(String path, MemoryField field, MemoryInt28 addr) {
                    var value = readFieldValue(memorySpaceName, field, addr);
                    out.println("%s=%-15s %s".formatted(addr, value, path));
                }
            };
            visitMemory(visitor);
        } catch (FileNotFoundException e) {
            throw new MidiError(e);
        }
    }

    private String findMemorySpaceName(MemoryMap memoryMap) {
        return memory.getMemorySpaces()
                .stream()
                .filter(memorySpace -> memorySpace.getMemoryMaps()
                        .contains(memoryMap))
                .map(MemorySpace::getName)
                .findFirst()
                .orElse(MemorySpace.DEFAULT_NAME);
    }

    private void dumpMemory(String memorySpaceName, String prefix, MemoryInt28 addr, MemoryMap memoryMap, DeviceMemoryVisitor visitor) {
        addr = addr.add(memoryMap.getBaseAddress());
        for (var field : memoryMap.getFields()) {
            if (field.isArray()) {
                addr = dumpMemoryArray(memorySpaceName, prefix, addr, field, visitor);
            } else {
                addr = dumpMemoryField(memorySpaceName, prefix, addr, field, visitor);
            }
        }
    }

    private MemoryInt28 dumpMemoryField(String memorySpaceName, String prefix, MemoryInt28 addr, MemoryField field, DeviceMemoryVisitor visitor) {
        String name = Optional.ofNullable(field.getName())
                .orElse(field.getType());
        String valueName = prefix + "/" + name;
        if (field.isReference()) {
            dumpMemory(memorySpaceName, valueName, addr, field.getReference(), visitor);
        } else {
            visitor.onNewEntry(valueName, field, addr);
        }
        return addr.add(field.getEffectiveTotalSize());
    }

    private MemoryInt28 dumpMemoryArray(String memorySpaceName, String prefix, MemoryInt28 addr, MemoryField field, DeviceMemoryVisitor out) {
        var array = field.getArray();
        var allEntries = array.getAllEntries();
        for (var idx = 0; idx < array.size(); idx++) {
            var name = Optional.ofNullable(field.getName())
                    .orElse(field.getType());
            var valueName = prefix + "/" + allEntries.get(idx) + "/" + name;
            if (field.isReference()) {
                dumpMemory(memorySpaceName, valueName, addr, field.getReference(), out);
            } else {
                out.onNewEntry(valueName, field, addr);
            }
            addr = addr.add(field.getEffectiveSize());
        }
        return addr;
    }

    private String readFieldValue(String memorySpaceName, MemoryField field, MemoryInt28 addr) {
        try {
            MemoryMapFormat format = field.getParent()
                    .getFormat();
            if (field.isString()) {
                return readStringValue(memorySpaceName, field, addr, format);
            } else if (field.isEnum()) {
                return readEnumValue(memorySpaceName, field, addr, format);
            } else {
                if (field.getSize()
                        .value() == 1) {
                    return readByteValue(memorySpaceName, addr, format);
                } else if (field.getSize()
                        .value() == 2) {
                    return readShortValue(memorySpaceName, addr, format);
                } else if (field.getSize()
                        .value() == 4) {
                    return readIntValue(memorySpaceName, addr, format);
                } else if (field.getSize()
                        .value() == 8) {
                    return readLongValue(memorySpaceName, addr, format);
                } else {
                    return readVariableLengthValue(memorySpaceName, field, addr, format);
                }
            }
        } catch (MidiError e) {
            throw new MidiError("Unexpected error reading field %s in MemoryMap %s".formatted(field.getName(), field.getParent()
                    .getName()), e);
        }
    }

    private String readVariableLengthValue(String memorySpaceName, MemoryField field, MemoryInt28 addr, MemoryMapFormat format) {
        int size = field.getSize()
                .value();
        int effectiveSize = switch (format) {
            case BYTES -> 1;
            case NIBBLES -> 2;
        };
        int[] values = new int[size];
        for (int i = 0; i < values.length; i++) {
            values[i] = readByte(memorySpaceName, addr, format);
            addr = addr.add(effectiveSize);
        }
        StringBuilder sb = new StringBuilder();
        for (int value : values) {
            sb.append("%02X".formatted(value));
        }
        return sb.toString();
    }

    private int readByte(String memorySpaceName, MemoryInt28 addr, MemoryMapFormat format) {
        return switch (format) {
            case BYTES -> {
                yield memory.readByte(memorySpaceName, addr);
            }
            case NIBBLES -> {
                int v1 = memory.readByte(memorySpaceName, addr);
                int v2 = memory.readByte(memorySpaceName, addr.add(1));
                yield v1 << 4 | v2;
            }
        };
    }

    public String readString(MemoryInt28 addr, MemoryMapFormat format, int size) {
        return readString(MemorySpace.DEFAULT_NAME, addr, format, size);
    }

    public String readString(String memorySpaceName, MemoryInt28 addr, MemoryMapFormat format, int size) {
        byte[] data = new byte[size];
        int effectiveSize = switch (format) {
            case BYTES -> 1;
            case NIBBLES -> 2;
        };
        for (int i = 0; i < size; i++) {
            data[i] = (byte) readByte(memorySpaceName, addr, format);
            addr = addr.add(effectiveSize);
        }
        if (data[0] == 0) {
            return "";
        } else {
            return new String(data, StandardCharsets.US_ASCII);
        }
    }

    private long readNumericalValue(String memorySpaceName, MemoryInt28 addr, MemoryMapFormat format, int size) {
        long result = 0;
        int effectiveSize = switch (format) {
            case BYTES -> 1;
            case NIBBLES -> 2;
        };
        for (int i = 0; i < size; i++) {
            long value = readByte(memorySpaceName, addr, format);
            result = result << 8 | value;
            addr = addr.add(effectiveSize);
        }
        return result;
    }

    private String readShortValue(String memorySpaceName, MemoryInt28 addr, MemoryMapFormat format) {
        short v = (short) readNumericalValue(memorySpaceName, addr, format, 2);
        return "%04X".formatted(v);
    }

    private String readIntValue(String memorySpaceName, MemoryInt28 addr, MemoryMapFormat format) {
        int v = (int) readNumericalValue(memorySpaceName, addr, format, 4);
        return "%08X".formatted(v);
    }

    private String readLongValue(String memorySpaceName, MemoryInt28 addr, MemoryMapFormat format) {
        long v = readNumericalValue(memorySpaceName, addr, format, 8);
        return "%016X".formatted(v);
    }

    private String readByteValue(String memorySpaceName, MemoryInt28 addr, MemoryMapFormat format) {
        int v = readByte(memorySpaceName, addr, format);
        return "%02X".formatted(v);
    }

    private String readStringValue(String memorySpaceName, MemoryField field, MemoryInt28 addr, MemoryMapFormat format) {
        return '"' + readString(memorySpaceName, addr, format, field.getSize()
                .value()) + '"';
    }

    private String readEnumValue(String memorySpaceName, MemoryField field, MemoryInt28 addr, MemoryMapFormat format) {
        var values = field.getEnumReference()
                .getValues();
        int ordinal = readByte(memorySpaceName, addr, format);
        String strValue = "???";
        if (ordinal >= 0 && ordinal < values.size()) {
            strValue = values.get(ordinal);
        }
        return "%02X %s".formatted(ordinal, strValue);
    }

    private List<MemoryMap> collectZones(List<MemoryMap> memoryMaps) {
        return Stream.concat(memoryMaps.stream(),
                        memoryMaps.stream()
                                .flatMap(m -> {
                                            var ms = m.getFields()
                                                    .stream()
                                                    .filter(MemoryField::isReference)
                                                    .map(MemoryField::getReference)
                                                    .toList();
                                            return collectZones(ms).stream();
                                        }

                                )
                                .distinct())
                .toList();
    }

    public void dumpMemoryMap(File dump) {
        log.info("Dump memory map to " + dump.getAbsolutePath());
        if (!dump.getParentFile()
                .exists() && !dump.getParentFile()
                .mkdirs()) {
            throw new MidiError("Unable to create " + dump.getAbsolutePath());
        }
        try (PrintWriter out = new PrintWriter(new FileOutputStream(dump))) {
            out.println(SEPARATOR);
            out.println(memory.getName());
            collectZones(memory.getMemoryMaps()).forEach(memoryMap -> {
                out.println(SEPARATOR);
                out.println(memoryMap.getName());
                out.println("Format        : " + memoryMap.getFormat());
                out.println("Start Address : " + memoryMap.getBaseAddress());
                out.println("End   Address : " + memoryMap.getEndAddress());
                out.println("Size          : " + memoryMap.getSize() + " in bytes");
                out.println("Effective Size: " + memoryMap.getEffectiveSize() + " in " + memoryMap.getFormat());
                int total = memoryMap.getFields()
                        .stream()
                        .map(MemoryField::getEffectiveTotalSize)
                        .mapToInt(MemoryInt28::value)
                        .sum();
                out.println("Computed Size : " + MemoryInt28.from(total));
                for (var field : memoryMap.getFields()) {
                    out.println("\t%-40s %s at %s".formatted(field, field.getTotalSize(), field.getAddress()));
                }
            });
        } catch (FileNotFoundException e) {
            throw new MidiError(e);
        }
    }
}

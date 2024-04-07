package com.hypercube.workshop.midiworkshop.common.sysex.device.memory.dump;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.common.sysex.device.memory.map.MemoryField;
import com.hypercube.workshop.midiworkshop.common.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.common.sysex.device.memory.map.MemoryMapFormat;
import com.hypercube.workshop.midiworkshop.common.sysex.device.memory.primitives.MemoryInt24;
import lombok.RequiredArgsConstructor;
import org.jline.utils.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class DeviceMemoryDumper {
    public static final String SEPARATOR = "-------------------------------------";
    private final DeviceMemory memory;

    public void visitMemory(DeviceMemoryVisitor visitor) {
        MemoryInt24 addr = MemoryInt24.from(0);
        memory.getMemoryMaps()
                .forEach(memoryMap -> {
                    visitor.onNewTopLevelMemoryMap(memoryMap);
                    dumpMemory(memoryMap.getName(), addr, memoryMap, visitor);
                });
    }

    public void dumpMemory(File dump) {
        Log.info("Dump memory to " + dump.getAbsolutePath());
        if (!dump.getParentFile()
                .exists() && !dump.getParentFile()
                .mkdirs()) {
            throw new MidiError("Unable to create " + dump.getAbsolutePath());
        }
        try (PrintWriter out = new PrintWriter(new FileOutputStream(dump))) {
            out.println(SEPARATOR);
            out.println(memory.getName() + " Memory dump");

            DeviceMemoryVisitor visitor = new DeviceMemoryVisitor() {
                @Override
                public void onNewTopLevelMemoryMap(MemoryMap memoryMap) {
                    out.println(SEPARATOR);
                    out.println(memoryMap.toString());
                    out.println(SEPARATOR);
                }

                @Override
                public void onNewEntry(String path, MemoryField field, MemoryInt24 addr) {
                    var value = readFieldValue(field, addr);
                    out.println("%s=%-15s %s".formatted(addr, value, path));
                }
            };
            visitMemory(visitor);
        } catch (FileNotFoundException e) {
            throw new MidiError(e);
        }
    }

    private void dumpMemory(String prefix, MemoryInt24 addr, MemoryMap memoryMap, DeviceMemoryVisitor visitor) {
        addr = addr.add(memoryMap.getBaseAddress());
        for (var field : memoryMap.getFields()) {
            if (field.isArray()) {
                addr = dumpMemoryArray(prefix, addr, field, visitor);
            } else {
                addr = dumpMemoryField(prefix, addr, field, visitor);
            }
        }
    }

    private MemoryInt24 dumpMemoryField(String prefix, MemoryInt24 addr, MemoryField field, DeviceMemoryVisitor visitor) {
        String name = Optional.ofNullable(field.getName())
                .orElse(field.getType());
        String valueName = prefix + "/" + name;
        if (field.isReference()) {
            dumpMemory(valueName, addr, field.getReference(), visitor);
        } else {
            visitor.onNewEntry(valueName, field, addr);
        }
        return addr.add(field.getEffectiveTotalSize());
    }

    private MemoryInt24 dumpMemoryArray(String prefix, MemoryInt24 addr, MemoryField field, DeviceMemoryVisitor out) {
        var array = field.getArray();
        var allEntries = array.getAllEntries();
        for (var idx = 0; idx < array.size(); idx++) {
            var name = Optional.ofNullable(field.getName())
                    .orElse(field.getType());
            var valueName = prefix + "/" + allEntries.get(idx) + "/" + name;
            if (field.isReference()) {
                dumpMemory(valueName, addr, field.getReference(), out);
            } else {
                out.onNewEntry(valueName, field, addr);
            }
            addr = addr.add(field.getEffectiveSize());
        }
        return addr;
    }

    private String readFieldValue(MemoryField field, MemoryInt24 addr) {
        try {
            MemoryMapFormat format = field.getParent()
                    .getFormat();
            if (field.isString()) {
                return readStringValue(field, addr, format);
            } else if (field.isEnum()) {
                return readEnumValue(field, addr, format);
            } else {
                if (field.getSize()
                        .value() == 1) {
                    return readByteValue(addr, format);
                } else if (field.getSize()
                        .value() == 2) {
                    return readShortValue(addr, format);
                } else if (field.getSize()
                        .value() == 4) {
                    return readIntValue(addr, format);
                } else if (field.getSize()
                        .value() == 8) {
                    return readLongValue(addr, format);
                } else {
                    return readVariableLengthValue(field, addr, format);
                }
            }
        } catch (MidiError e) {
            throw new MidiError("Unexpected error reading field %s in MemoryMap %s".formatted(field.getName(), field.getParent()
                    .getName()), e);
        }
    }

    private String readVariableLengthValue(MemoryField field, MemoryInt24 addr, MemoryMapFormat format) {
        int size = field.getSize()
                .value();
        int effectiveSize = switch (format) {
            case BYTES -> 1;
            case NIBBLES -> 2;
        };
        int[] values = new int[size];
        for (int i = 0; i < values.length; i++) {
            values[i] = readByte(addr, format);
            addr = addr.add(effectiveSize);
        }
        StringBuilder sb = new StringBuilder();
        for (int value : values) {
            sb.append("%02X".formatted(value));
        }
        return sb.toString();
    }

    private int readByte(MemoryInt24 addr, MemoryMapFormat format) {
        return switch (format) {
            case BYTES -> {
                yield memory.readByte(addr);
            }
            case NIBBLES -> {
                int v1 = memory.readByte(addr);
                int v2 = memory.readByte(addr.add(1));
                yield v1 << 4 | v2;
            }
        };
    }

    public String readString(MemoryInt24 addr, MemoryMapFormat format, int size) {
        byte[] data = new byte[size];
        int effectiveSize = switch (format) {
            case BYTES -> 1;
            case NIBBLES -> 2;
        };
        for (int i = 0; i < size; i++) {
            data[i] = (byte) readByte(addr, format);
            addr = addr.add(effectiveSize);
        }
        if (data[0] == 0) {
            return "";
        } else {
            return new String(data, StandardCharsets.US_ASCII);
        }
    }

    private long readNumericalValue(MemoryInt24 addr, MemoryMapFormat format, int size) {
        long result = 0;
        int effectiveSize = switch (format) {
            case BYTES -> 1;
            case NIBBLES -> 2;
        };
        for (int i = 0; i < size; i++) {
            long value = readByte(addr, format);
            result = result << 8 | value;
            addr = addr.add(effectiveSize);
        }
        return result;
    }

    private String readShortValue(MemoryInt24 addr, MemoryMapFormat format) {
        short v = (short) readNumericalValue(addr, format, 2);
        return "%04X".formatted(v);
    }

    private String readIntValue(MemoryInt24 addr, MemoryMapFormat format) {
        int v = (int) readNumericalValue(addr, format, 4);
        return "%08X".formatted(v);
    }

    private String readLongValue(MemoryInt24 addr, MemoryMapFormat format) {
        long v = readNumericalValue(addr, format, 8);
        return "%016X".formatted(v);
    }

    private String readByteValue(MemoryInt24 addr, MemoryMapFormat format) {
        int v = readByte(addr, format);
        return "%02X".formatted(v);
    }

    private String readStringValue(MemoryField field, MemoryInt24 addr, MemoryMapFormat format) {
        return '"' + readString(addr, format, field.getSize()
                .value()) + '"';
    }

    private String readEnumValue(MemoryField field, MemoryInt24 addr, MemoryMapFormat format) {
        var values = field.getEnumReference()
                .getValues();
        int ordinal = readByte(addr, format);
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
        Log.info("Dump memory map to " + dump.getAbsolutePath());
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
                        .mapToInt(MemoryInt24::value)
                        .sum();
                out.println("Computed Size : " + MemoryInt24.from(total));
                for (var field : memoryMap.getFields()) {
                    out.println("\t%-40s %s at %s".formatted(field, field.getTotalSize(), field.getAddress()));
                }
            });
        } catch (FileNotFoundException e) {
            throw new MidiError(e);
        }
    }
}

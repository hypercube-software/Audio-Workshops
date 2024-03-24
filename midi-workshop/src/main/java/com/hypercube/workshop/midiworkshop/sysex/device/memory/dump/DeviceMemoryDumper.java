package com.hypercube.workshop.midiworkshop.sysex.device.memory.dump;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.map.MemoryField;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives.MemoryInt24;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.map.MemoryMapFormat;
import lombok.RequiredArgsConstructor;
import org.jline.utils.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class DeviceMemoryDumper {
    public static final String SEPARATOR = "-------------------------------------";
    private final DeviceMemory memory;

    public void visitMemory(DeviceMemoryVisitor visitor) {
        MemoryInt24 addr = MemoryInt24.from(0);
        memory.getMemorySpaces()
                .forEach(memoryMap -> dumpMemory(memoryMap.getName(), addr, memoryMap, visitor));
    }

    public void dumpMemory(File dump) {
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
        String valueName = prefix + "/" + field.getName();
        if (field.isReference()) {
            dumpMemory(valueName, addr, field.getReference(), visitor);
        } else {
            visitor.onNewEntry(valueName, field, addr);
        }
        return addr.add(field.getTotalSize());
    }

    private MemoryInt24 dumpMemoryArray(String prefix, MemoryInt24 addr, MemoryField field, DeviceMemoryVisitor out) {
        var array = field.getArray();
        var allEntries = array.getAllEntries();
        for (var idx = 0; idx < array.size(); idx++) {
            var valueName = prefix + "/" + allEntries.get(idx) + "/" + field.getName();
            if (field.isReference()) {
                dumpMemory(valueName, addr, field.getReference(), out);
            } else {
                out.onNewEntry(valueName, field, addr);
            }
            addr = addr.add(field.getSize());
        }
        return addr;
    }

    private String readFieldValue(MemoryField field, MemoryInt24 addr) {
        MemoryMapFormat format = field.getParent()
                .getFormat();
        if (field.isString()) {
            return readStringValue(field, addr, format);
        } else if (field.isEnum()) {
            return readEnumValue(field, addr, format);
        } else {
            if (field.getSize()
                    .value() == 1) {
                return readByteValue(addr);
            } else if (field.getSize()
                    .value() == 2) {
                return readShortValue(addr, format);
            } else if (field.getSize()
                    .value() == 4) {
                return readLongValue(addr, format);
            } else {
                return readVariableLengthValue(field, addr, format);
            }
        }
    }

    private String readVariableLengthValue(MemoryField field, MemoryInt24 addr, MemoryMapFormat format) {
        int[] values = new int[field.getSize()
                .value()];
        for (int i = 0; i < values.length; i++) {
            values[i] = memory.readByte(addr.add(i));
        }
        StringBuilder sb = new StringBuilder();
        return switch (format) {
            case BYTES -> {
                for (int value : values) {
                    sb.append("%02X".formatted(value));
                }
                yield sb.toString();
            }
            case NIBBLES -> {
                for (int i = 0; i < values.length; ) {
                    int v = values[i] << 4 | values[i + 1];
                    sb.append("%02X".formatted(v));
                    i += 2;
                }
                yield sb.toString();
            }
        };
    }

    private String readShortValue(MemoryInt24 addr, MemoryMapFormat format) {
        int v1 = memory.readByte(addr);
        int v2 = memory.readByte(addr.add(1));
        int v = switch (format) {
            case BYTES -> v1 << 8 | v2;
            case NIBBLES -> v1 << 4 | v2;
        };
        return "%04X".formatted(v);
    }

    private String readLongValue(MemoryInt24 addr, MemoryMapFormat format) {
        int v1 = memory.readByte(addr);
        int v2 = memory.readByte(addr.add(1));
        int v3 = memory.readByte(addr.add(2));
        int v4 = memory.readByte(addr.add(3));
        int v = switch (format) {
            case BYTES -> v1 << 24 | v2 << 16 | v3 << 8 | v4;
            case NIBBLES -> v1 << 12 | v2 << 8 | v3 << 4 | v4;
        };
        return "%08X".formatted(v);
    }

    private String readByteValue(MemoryInt24 addr) {
        int v = memory.readByte(addr);
        return "%02X".formatted(v);
    }

    private String readStringValue(MemoryField field, MemoryInt24 addr, MemoryMapFormat format) {
        return '"' + memory.readString(format, addr, field.getSize()
                .value()) + '"';
    }

    private String readEnumValue(MemoryField field, MemoryInt24 addr, MemoryMapFormat format) {
        var values = field.getEnumReference()
                .getValues();
        if (field.getSize()
                .value() == 1) {
            int v = memory.readByte(addr);
            return "%02X".formatted(v);
        } else if (field.getSize()
                .value() == 2) {
            int v1 = memory.readByte(addr);
            int v2 = memory.readByte(addr.add(1));
            int ordinal = switch (format) {
                case BYTES -> v1 << 8 | v2;
                case NIBBLES -> v1 << 4 | v2;
            };
            String strValue = "???";
            if (ordinal >= 0 && ordinal < values.size()) {
                strValue = values.get(ordinal);
            }

            return "%04X %s".formatted(ordinal, strValue);
        } else {
            Log.error("Can't read field of size " + field.getSize()
                    .value());
            return "????";
        }
    }

    private List<MemoryMap> collectZones(List<MemoryMap> memorySpaces) {
        return Stream.concat(memorySpaces.stream(),
                        memorySpaces.stream()
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
        if (!dump.getParentFile()
                .exists() && !dump.getParentFile()
                .mkdirs()) {
            throw new MidiError("Unable to create " + dump.getAbsolutePath());
        }
        try (PrintWriter out = new PrintWriter(new FileOutputStream(dump))) {
            out.println(SEPARATOR);
            out.println(memory.getName());
            collectZones(memory.getMemorySpaces()).forEach(memoryMap -> {
                out.println(SEPARATOR);
                out.println(memoryMap.getName());
                out.println("Format       : " + memoryMap.getFormat());
                out.println("Start Address: " + memoryMap.getBaseAddress());
                out.println("End   Address: " + memoryMap.getEndAddress());
                out.println("Size         : " + memoryMap.getSize());
                int total = memoryMap.getFields()
                        .stream()
                        .map(MemoryField::getTotalSize)
                        .mapToInt(MemoryInt24::value)
                        .sum();
                out.println("Computed Size: " + MemoryInt24.from(total));
                for (var field : memoryMap.getFields()) {
                    out.println("\t%-40s %s at %s".formatted(field, field.getTotalSize(), field.getAddress()));
                }
            });
        } catch (FileNotFoundException e) {
            throw new MidiError(e);
        }
    }
}

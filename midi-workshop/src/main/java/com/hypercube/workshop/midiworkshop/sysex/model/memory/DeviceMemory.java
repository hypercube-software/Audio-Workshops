package com.hypercube.workshop.midiworkshop.sysex.model.memory;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.model.memory.map.MemoryField;
import com.hypercube.workshop.midiworkshop.sysex.model.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.sysex.model.memory.primitives.MemoryInt24;
import lombok.RequiredArgsConstructor;
import org.jline.utils.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class DeviceMemory {
    private final String name;
    private final List<MemoryMap> memorySpaces;
    private final Set<Integer> written = new HashSet<>();

    public void writeByte(MemoryInt24 address, int value) {
        var space = memorySpaces.stream()
                .filter(s -> s.contains(address))
                .findFirst()
                .orElseThrow(() -> new MidiError("Memory address not mapped: %s".formatted(address)));
        //Log.info("Write 0x%02X at %s in space %s".formatted(value, address, space));
        if (written.contains(address.value())) {
            throw new MidiError("Already written: %s".formatted(address));
        } else {
            space.writeByte(address, value);
            written.add(address.value());
        }
    }

    public int readByte(MemoryInt24 address) {
        var space = memorySpaces.stream()
                .filter(s -> s.contains(address))
                .findFirst()
                .orElseThrow();
        return space.readByte(address);
    }

    public void dumpMemory(File dump) {
        if (!dump.getParentFile()
                .exists() && !dump.getParentFile()
                .mkdirs()) {
            throw new MidiError("Unable to create " + dump.getAbsolutePath());
        }
        try (PrintWriter out = new PrintWriter(new FileOutputStream(dump))) {
            out.println("-------------------------------------");
            out.println(name + " Memory dump");
            MemoryInt24 addr = MemoryInt24.from(0);
            memorySpaces.forEach(dms -> dumpMemory(dms.getName(), addr, dms, out));
        } catch (FileNotFoundException e) {
            throw new MidiError(e);
        }
    }

    private void dumpMemory(String prefix, MemoryInt24 addr, MemoryMap dms, PrintWriter out) {
        addr = addr.add(dms.getBaseAddress());
        for (var field : dms.getFields()) {
            if (field.isArray()) {
                addr = dumpMemoryArray(prefix, addr, field, out);
            } else {
                addr = dumpMemoryField(prefix, addr, field, out);
            }
        }
    }

    private MemoryInt24 dumpMemoryField(String prefix, MemoryInt24 addr, MemoryField field, PrintWriter out) {
        String valueName = prefix + "/" + field.getName();
        if (!field.isReference()) {
            var value = readFieldValue(field, addr);
            out.println("%s=%-15s %s".formatted(addr, value, valueName));

        } else {
            dumpMemory(valueName, addr, field.getReference(), out);
        }
        return addr.add(field.getTotalSize());
    }

    private MemoryInt24 dumpMemoryArray(String prefix, MemoryInt24 addr, MemoryField field, PrintWriter out) {
        var array = field.getArray();
        var allEntries = array.getAllEntries();
        for (var idx = 0; idx < array.size(); idx++) {
            var valueName = prefix + "/" + allEntries.get(idx) + "/" + field.getName();
            if (!field.isReference()) {
                var value = readFieldValue(field, addr);
                out.println("%s=%-15s %s".formatted(addr, value, valueName));
            } else {
                dumpMemory(valueName, addr, field.getReference(), out);
            }
            addr = addr.add(field.getSize());
        }
        return addr;
    }

    private String readFieldValue(MemoryField field, MemoryInt24 addr) {
        if (field.isString()) {
            return '"' + readString(addr, field.getSize()
                    .value()) + '"';
        } else if (field.isEnum()) {
            var values = field.getMemoryEnum()
                    .getValues();
            int v = readByte(addr);
            if (v >= values.size()) {
                Log.error("Enum index %d for %s out of bound at %s".formatted(v, field.getType(), addr));
                return "" + v;
            } else {
                return field.getMemoryEnum()
                        .getValues()
                        .get(v);
            }
        } else {
            return "" + readByte(addr);
        }
    }

    public void dumpMemoryMap(File dump) {
        if (!dump.getParentFile()
                .exists() && !dump.getParentFile()
                .mkdirs()) {
            throw new MidiError("Unable to create " + dump.getAbsolutePath());
        }
        try (PrintWriter out = new PrintWriter(new FileOutputStream(dump))) {
            out.println("-------------------------------------");
            out.println(name);
            collectZones(memorySpaces).forEach(dms -> {
                out.println("-------------------------------------");
                out.println(dms.getName());
                out.println("Start Address: " + dms.getBaseAddress());
                out.println("End   Address: " + dms.getEndAddress());
                out.println("Size         : " + dms.getSize());
                int total = dms.getFields()
                        .stream()
                        .map(MemoryField::getTotalSize)
                        .mapToInt(MemoryInt24::value)
                        .sum();
                out.println("Computed Size: " + MemoryInt24.from(total));
                for (var field : dms.getFields()) {
                    out.println("\t%-40s %s at %s".formatted(field, field.getTotalSize(), field.getAddress()));
                }
            });
        } catch (FileNotFoundException e) {
            throw new MidiError(e);
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

    public String readString(MemoryInt24 address, int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) readByte(address.add(i));
        }
        if (data[0] == 0) {
            return "";
        } else {
            return new String(data, StandardCharsets.US_ASCII);
        }
    }

}

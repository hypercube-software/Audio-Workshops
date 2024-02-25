package com.hypercube.workshop.midiworkshop.sysex.model.memory.map;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.model.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.sysex.model.memory.primitives.MemoryArray;
import com.hypercube.workshop.midiworkshop.sysex.model.memory.primitives.MemoryArrayIndex;
import com.hypercube.workshop.midiworkshop.sysex.model.memory.primitives.MemoryEnum;
import com.hypercube.workshop.midiworkshop.sysex.model.memory.primitives.MemoryInt24;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Parse a memory map file "*.mmap" describing the memory layout of a MIDI device
 * <p>It is important to understand that the device memory is not contiguous
 * <p>So a device memory contains multiple MemoryMap
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MemoryMapParser {
    private final File file;
    private final State state;
    private final Pattern MEMORY_DEF = Pattern.compile("@(?<addr>[0-9A-F]+)(-(?<size>[0-9A-F]+))?\\s(?<name>[a-zA-Z0-9 ]+)");
    private final Pattern FIELD_DEF = Pattern.compile("^((?<size>[0-9A-F]+)\\s)?(?<name>[a-zA-Z0-9 ]+)(\\[(?<array>[^\\]]+)\\])?(\\((?<type>[a-zA-Z$]+)\\))?");

    private final Pattern ARRAY_DEF = Pattern.compile("((?<size>[0-9]+)(\\s(?<name>[a-zA-Z]+))?),?");
    private List<String> lines;

    private final List<MemoryMap> memoryMaps = new ArrayList<>();
    private final List<MemoryEnum> memoryEnums = new ArrayList<>();

    private final List<MemoryField> fields = new ArrayList<>();

    private static class State {
        String currentModel;
        int currentLineNumber;
        int currentLineIndex;
        String currentLine;

        boolean packedAddress;

        MemoryMap currentMemorySpace;
        public MemoryEnum currentEnum;

    }

    public static DeviceMemory load(File file) {
        MemoryMapParser memoryMapParser = new MemoryMapParser(file, new State());
        return memoryMapParser.parse();
    }

    private DeviceMemory parse() {
        try {
            lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MidiError("Unable to parse " + file.getAbsolutePath(), e);
        }
        lines = lines.stream()
                .map(this::removeComments)
                .toList();
        IntStream.range(0, lines.size())
                .forEach(this::parseLine);
        resolveReferences();
        resolveAddresses();
        var topLevelMemorySpaces = memoryMaps.stream()
                .filter(MemoryMap::isTopLevel)
                .toList();
        topLevelMemorySpaces.forEach(MemoryMap::allocateMemory);
        return new DeviceMemory(state.currentModel, topLevelMemorySpaces);
    }

    private void resolveAddresses() {
        memoryMaps.forEach(dms -> {
            MemoryInt24 fieldAddress = dms.getBaseAddress();
            for (var field : dms.getFields()) {
                field.setAddress(fieldAddress);
                fieldAddress = fieldAddress.add(field.getTotalSize());
            }
        });

    }

    private void resolveReferences() {
        memoryMaps.forEach(this::resolveReferences);
    }

    private void resolveReferences(MemoryMap ms) {
        ms.getFields()
                .forEach(f -> {
                    if (f.getSize() == null) {
                        resolveFieldReference(ms, f);
                    }
                });
        updateDeviceMemorySpaceSize(ms);
        resolveEnums();
    }

    public void updateDeviceMemorySpaceSize(MemoryMap ms) {
        if (ms.getSize() != null)
            return;

        log.info("Update size of " + ms.getName());
        int size = 0;
        for (MemoryField f : ms.getFields()) {
            if (f.getSize() == null) {
                throw new MidiError("Field without size: %s at line %d".formatted(f.getName(), f.getLine()));
            }
            log.info(f.toString());
            size += f.getTotalSize()
                    .value();
        }
        ms.setSize(size);
        log.info("Size of " + ms.getName() + " = " + ms.getSize());
    }

    private void resolveFieldReference(MemoryMap ms, MemoryField f) {
        if (f.getSize() != null)
            return;
        log.info("Resolve %s in %s".formatted(f.getName(), ms.getName()));
        var memorySpace = memoryMaps.stream()
                .filter(dms -> dms.getName()
                        .equals(f.getName()))
                .findFirst()
                .orElseThrow(() -> new MidiError("Field reference %s not found at line %d".formatted(f.getName(), f.getLine())));
        memorySpace.incReferenceCount();
        resolveReferences(memorySpace);
        f.setReference(memorySpace);
        f.setSize(memorySpace.getSize());
    }

    private void resolveEnums() {
        memoryEnums.forEach(e -> fields.stream()
                .filter(f -> e.getName()
                        .equals(f.getType()))
                .forEach(f -> f.setMemoryEnum(e)));
    }

    private void parseLine(int lineIndex) {
        state.currentLineIndex = lineIndex;
        state.currentLineNumber = lineIndex + 1;
        state.currentLine = lines.get(lineIndex);
        if (state.currentLine.isEmpty()) {
            state.currentMemorySpace = null;
            state.currentEnum = null;
        } else if (state.currentLine.startsWith("@")) {
            if (state.currentLine.startsWith("@Model")) {
                state.currentModel = state.currentLine.substring(6)
                        .trim();
            } else if (state.currentLine.equals("@Packed")) {
                state.packedAddress = true;
            } else if (state.currentLine.equals("@Unpacked")) {
                state.packedAddress = false;
            } else {
                parseMemoryZone(state.currentLine);
            }
        } else if (state.currentLine.startsWith("$")) {
            parseEnum(state.currentLine);
        } else {
            parseField(state.currentLine);
        }
    }

    private void parseEnum(String currentLine) {
        String name = currentLine.substring(1)
                .trim();
        state.currentEnum = new MemoryEnum(name);
        memoryEnums.add(state.currentEnum);
    }

    private String getType(String name, MemoryInt24 size, String type) {
        if (type != null) {
            return type;
        }
        if (size == null) {
            return name;
        } else {
            return "Byte";
        }
    }

    private void parseField(String currentLine) {
        Matcher m = FIELD_DEF.matcher(currentLine);
        if (m.find()) {
            MemoryInt24 size = MemoryInt24.from(m.group("size"), state.packedAddress);
            String name = m.group("name");
            String type = m.group("type");
            String array = m.group("array");
            if (state.currentEnum != null) {
                state.currentEnum.add(name);
            } else {
                var field = new MemoryField(state.currentLineNumber, name, getType(name, size, type), parseMemoryArray(array));
                field.setSize(size);
                fields.add(field);
                state.currentMemorySpace.add(field);
            }
        } else {
            throw new MidiError("Unexpected Field Definition at line " + state.currentLineNumber + ": " + currentLine);
        }
    }

    private MemoryArray parseMemoryArray(String array) {
        if (array == null) {
            return null;
        }
        Matcher m = ARRAY_DEF.matcher(array);
        List<MemoryArrayIndex> indexes = new ArrayList<>();
        while (m.find()) {
            String size = m.group("size");
            String name = m.group("name");
            indexes.add(new MemoryArrayIndex(Integer.parseInt(size), name == null ? "" : name));
        }
        return new MemoryArray(indexes);
    }

    private void parseMemoryZone(String currentLine) {
        Matcher m = MEMORY_DEF.matcher(currentLine);
        if (m.find()) {
            MemoryInt24 addr = MemoryInt24.from(m.group("addr"), state.packedAddress);
            MemoryInt24 size = MemoryInt24.from(m.group("size"), state.packedAddress);
            String name = m.group("name");
            MemoryMap memorySpace = size == null ? new MemoryMap(name, addr) : new MemoryMap(name, addr, size);
            memoryMaps.add(memorySpace);
            state.currentMemorySpace = memorySpace;
        } else {
            throw new MidiError("Unexpected Zone Definition at line " + state.currentLineNumber + ": " + currentLine);
        }
    }

    private String removeComments(String line) {
        int idx = line.indexOf('#');
        if (idx != -1) {
            return line.substring(0, idx)
                    .trim();
        } else {
            return line;
        }
    }
}

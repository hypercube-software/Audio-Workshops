package com.hypercube.workshop.midiworkshop.sysex.device.memory.map;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives.MemoryArray;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives.MemoryInt24;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives.MemoryArrayIndex;
import com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives.MemoryEnum;
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
    private static final Pattern MEMORY_DEF = Pattern.compile("@(?<addr>[0-9A-F]+)(-(?<size>[0-9A-F]+))?(\\s(?<format>[A-Z]+))?\\s(?<name>[a-zA-Z0-9 ]+)");
    private static final Pattern STRUCT_DEF = Pattern.compile("struct\\s((?<size>[0-9A-F]+)\\s)?((?<format>[A-Z]+)\\s)?(?<name>[a-zA-Z0-9 ]+)");
    private static final Pattern FIELD_DEF = Pattern.compile("^((?<size>[0-9A-F]+)\\s)?(?<name>[a-zA-Z0-9 ]+[a-zA-Z0-9]+)(\\[(?<array>[^\\]]+)\\])?(\\s*\\((?<type>[a-zA-Z$]+)\\))?");
    private static final Pattern ARRAY_DEF = Pattern.compile("((?<size>[0-9]+)(\\s(?<name>[a-zA-Z]+))?),?");

    private final File file;
    private final State state;
    private final List<MemoryMap> memoryMaps = new ArrayList<>();
    private final List<MemoryEnum> memoryEnums = new ArrayList<>();

    private final List<MemoryField> fields = new ArrayList<>();
    private List<String> lines;

    private static class State {
        String currentModel;
        int currentLineNumber;
        int currentLineIndex;
        String currentLine;
        boolean inDefinitionBlock;
        boolean packedAddress;
        MemoryMap currentMemoryMap;
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
        resolveTypeReferences();
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

    /**
     * Since the definition of a memory map contains things defined later in the file,
     * we need to resolve those references in a second pass
     * <ul>
     *     <li>Update the field {@link MemoryField#reference} in all memory map fields</li>
     *     <li>Do the same for enums {@link MemoryField#memoryEnum} </li>
     * </ul>
     */
    private void resolveTypeReferences() {
        memoryMaps.forEach(this::resolveTypeReferences);
    }

    private void resolveTypeReferences(MemoryMap memoryMap) {
        memoryMap.getFields()
                .forEach(f -> {
                    if (f.getSize() == null) {
                        resolveTypeReference(memoryMap, f);
                    }
                });
        updateDeviceMemorySpaceSize(memoryMap);
        resolveEnumReferences();
    }

    private void resolveTypeReference(MemoryMap memoryMap, MemoryField field) {
        if (field.getSize() != null)
            return;
        log.info("Resolve %s in %s".formatted(field.getName(), memoryMap.getName()));
        var type = field.getType() == null ? field.getName() : field.getType();
        var referencedMemoryMap = memoryMaps.stream()
                .filter(dms -> dms.getName()
                        .equals(type))
                .findFirst()
                .orElseThrow(() -> new MidiError("Field reference %s not found at line %d".formatted(field.getName(), field.getLine())));
        if (referencedMemoryMap.getFormat() == null) {
            referencedMemoryMap.setFormat(memoryMap.getFormat());
        }
        referencedMemoryMap.incReferenceCount();
        resolveTypeReferences(referencedMemoryMap);
        field.setReference(referencedMemoryMap);
        field.setSize(referencedMemoryMap.getSize());
    }

    private void resolveEnumReferences() {
        memoryEnums.forEach(e -> fields.stream()
                .filter(f -> e.getName()
                        .equals(f.getType()))
                .forEach(f -> f.setEnumReference(e)));
    }

    /**
     * Knowing the size of each field, we can compute the size of the memory map
     *
     * @param memoryMap A memoryMap without size
     */
    public void updateDeviceMemorySpaceSize(MemoryMap memoryMap) {
        if (memoryMap.getSize() != null)
            return;

        log.info("Update size of " + memoryMap.getName());
        int size = 0;
        for (MemoryField f : memoryMap.getFields()) {
            if (f.getSize() == null) {
                throw new MidiError("Field without size: %s at line %d".formatted(f.getName(), f.getLine()));
            }
            log.info(f.toString());
            size += f.getTotalSize()
                    .value();
        }
        memoryMap.setSize(size);
        log.info("Size of " + memoryMap.getName() + " = " + memoryMap.getSize());
    }

    /**
     * Scan a line of the file and update the current state of the parser
     *
     * @param lineIndex Line number for [0,n]
     */
    private void parseLine(int lineIndex) {
        state.currentLineIndex = lineIndex;
        state.currentLineNumber = lineIndex + 1;
        state.currentLine = removeIndentation(lines.get(lineIndex));
        if (state.currentLine.equals("{")) {
            if (state.currentModel == null) {
                throw new MidiError("Unexpected block at line " + state.currentLineNumber + ": " + state.currentLine);
            }
            state.inDefinitionBlock = true;
        } else if (state.currentLine.equals("}")) {
            state.currentMemoryMap = null;
            state.currentEnum = null;
            state.inDefinitionBlock = false;
        } else if (state.currentLine.startsWith("@")) {
            if (state.inDefinitionBlock) {
                throw new MidiError("Unexpected directive in block at line " + state.currentLineNumber + ": " + state.currentLine);
            }
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
        } else if (state.currentLine.startsWith("struct ")) {
            if (state.inDefinitionBlock) {
                throw new MidiError("Unexpected struct in block at line " + state.currentLineNumber + ": " + state.currentLine);
            }
            parseStruct(state.currentLine);
        } else if (state.currentLine.startsWith("enum ")) {
            if (state.inDefinitionBlock) {
                throw new MidiError("Unexpected enum declaration in block at line " + state.currentLineNumber + ": " + state.currentLine);
            }
            parseEnum(state.currentLine);
        } else if (!state.currentLine.isEmpty()) {
            if (!state.inDefinitionBlock) {
                throw new MidiError("Unexpected field declaration outside any block at line " + state.currentLineNumber + ": " + state.currentLine);
            }
            parseField(state.currentLine);
        }
    }

    private String removeIndentation(String line) {
        return line.replaceAll("^\\s+", "");
    }

    private void parseEnum(String currentLine) {
        String name = currentLine.substring(currentLine.indexOf(' ') + 1)
                .trim();
        state.currentEnum = new MemoryEnum(name);
        memoryEnums.add(state.currentEnum);
    }

    private String getFieldType(String fieldName, MemoryInt24 fieldSize, String fieldTypeName) {
        if (fieldTypeName != null) {
            return fieldTypeName;
        }
        if (fieldSize == null) {
            return fieldName;
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
                var field = new MemoryField(state.currentMemoryMap, state.currentLineNumber, name, getFieldType(name, size, type), parseMemoryArray(array));
                field.setSize(size);
                fields.add(field);
                state.currentMemoryMap.add(field);
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
            String format = m.group("format");
            MemoryMapFormat type = format == null ? MemoryMapFormat.BYTES : MemoryMapFormat.valueOf(format);
            MemoryMap memorySpace = size == null ? new MemoryMap(MemoryMapType.MEMORY, name, addr, type) : new MemoryMap(MemoryMapType.MEMORY, name, addr, size, type);
            memoryMaps.add(memorySpace);
            state.currentMemoryMap = memorySpace;
        } else {
            throw new MidiError("Unexpected Zone Definition at line " + state.currentLineNumber + ": " + currentLine);
        }
    }

    private void parseStruct(String currentLine) {
        Matcher m = STRUCT_DEF.matcher(currentLine);
        if (m.find()) {
            MemoryInt24 addr = MemoryInt24.from("0", state.packedAddress);
            MemoryInt24 size = MemoryInt24.from(m.group("size"), state.packedAddress);
            String name = m.group("name");
            String format = m.group("format");
            MemoryMapFormat type = format == null ? null : MemoryMapFormat.valueOf(format);
            MemoryMap memorySpace = size == null ? new MemoryMap(MemoryMapType.STRUCT, name, addr, type) : new MemoryMap(MemoryMapType.STRUCT, name, addr, size, type);
            memoryMaps.add(memorySpace);
            state.currentMemoryMap = memorySpace;
        } else {
            throw new MidiError("Unexpected Struct Definition at line " + state.currentLineNumber + ": " + currentLine);
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

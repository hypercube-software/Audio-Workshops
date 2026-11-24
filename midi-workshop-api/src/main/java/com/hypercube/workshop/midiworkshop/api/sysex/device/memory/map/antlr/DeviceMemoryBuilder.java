package com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.antlr;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.DeviceMemory;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.MemorySpace;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryField;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMapFormat;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMapType;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryArray;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryArrayIndex;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryEnum;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
import org.antlr.v4.runtime.ParserRuleContext;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the {@link DeviceMemory} model from the ANTLR parse tree.
 *
 * <p>Syntax is handled by {@link MemoryMapDslParser}; this class only does the
 * semantic work: packed/linear addresses, optional formats, type references,
 * enum references and memory allocation.
 */
public class DeviceMemoryBuilder extends MemoryMapDslBaseVisitor<Void> {
    private final File file;
    private final List<MemoryMap> memoryMaps = new ArrayList<>();
    private final Map<String, List<MemoryMap>> memorySpaces = new LinkedHashMap<>();
    private final Set<String> declaredMemorySpaces = new HashSet<>();
    private final List<MemoryEnum> memoryEnums = new ArrayList<>();
    private final List<MemoryField> fields = new ArrayList<>();

    private String model;
    private boolean packed;
    private String currentMemorySpace = MemorySpace.DEFAULT_NAME;
    private MemoryMap currentMap;
    private MemoryEnum currentEnum;

    public DeviceMemoryBuilder(File file) {
        this.file = file;
    }

    public DeviceMemory build() {
        resolveTypeReferences();
        resolveAddresses();
        memorySpaces.forEach((name, zones) -> {
            if (zones.isEmpty()) {
                throw new MidiError("%s: empty memory space: %s".formatted(file.getName(), name));
            }
        });
        var spaces = memorySpaces.entrySet()
                .stream()
                .map(entry -> new MemorySpace(entry.getKey(), entry.getValue()))
                .toList();
        spaces.stream()
                .flatMap(memorySpace -> memorySpace.getMemoryMaps()
                        .stream())
                .filter(MemoryMap::isTopLevel)
                .forEach(MemoryMap::allocateMemory);
        return new DeviceMemory(model, spaces);
    }

    // ------------------------------------------------------------------
    // Visitor
    // ------------------------------------------------------------------

    @Override
    public Void visitModelDecl(MemoryMapDslParser.ModelDeclContext ctx) {
        model = join(ctx.words());
        return null;
    }

    @Override
    public Void visitAddressingDecl(MemoryMapDslParser.AddressingDeclContext ctx) {
        packed = ctx.PACKED() != null;
        return null;
    }

    @Override
    public Void visitMemorySpaceDef(MemoryMapDslParser.MemorySpaceDefContext ctx) {
        String name = name(words(ctx.words()), ctx, "memory space");
        if (!declaredMemorySpaces.add(name)) {
            throw error(ctx, "Memory space already declared: " + name);
        }
        List<MemoryMap> zones = memorySpaces.computeIfAbsent(name, key -> new ArrayList<>());
        String previousMemorySpace = currentMemorySpace;
        currentMemorySpace = name;
        try {
            visitChildren(ctx);
        } finally {
            currentMemorySpace = previousMemorySpace;
        }
        return null;
    }

    @Override
    public Void visitZone(MemoryMapDslParser.ZoneContext ctx) {
        String[] range = ctx.range.getText()
                .split("-", 2);
        List<String> words = words(ctx.words());
        MemoryMapFormat format = takeFormat(words, MemoryMapFormat.BYTES);
        MemoryInt28 address = parseHex(range[0], ctx, "address");
        MemoryInt28 size = range.length == 2 ? parseHex(range[1], ctx, "size") : null;
        if (format == MemoryMapFormat.NIBBLES && size != null) {
            size = size.div(2);
        }
        String name = name(words, ctx, "zone");
        currentMap = size == null
                ? new MemoryMap(MemoryMapType.MEMORY, name, address, format)
                : new MemoryMap(MemoryMapType.MEMORY, name, address, size, format);
        memoryMaps.add(currentMap);
        memorySpaces.computeIfAbsent(currentMemorySpace, key -> new ArrayList<>())
                .add(currentMap);
        return visitChildren(ctx);
    }

    @Override
    public Void visitStructDef(MemoryMapDslParser.StructDefContext ctx) {
        List<String> words = words(ctx.words());
        MemoryMapFormat format = takeFormat(words, null);
        MemoryInt28 size = ctx.size == null ? null : parseHex(ctx.size.getText(), ctx, "struct size");
        String name = name(words, ctx, "struct");
        currentMap = size == null
                ? new MemoryMap(MemoryMapType.STRUCT, name, MemoryInt28.from(0), format)
                : new MemoryMap(MemoryMapType.STRUCT, name, MemoryInt28.from(0), size, format);
        memoryMaps.add(currentMap);
        return visitChildren(ctx);
    }

    @Override
    public Void visitEnumDef(MemoryMapDslParser.EnumDefContext ctx) {
        currentEnum = new MemoryEnum(join(ctx.words()));
        memoryEnums.add(currentEnum);
        return visitChildren(ctx);
    }

    @Override
    public Void visitEnumValue(MemoryMapDslParser.EnumValueContext ctx) {
        currentEnum.add(join(ctx.words()));
        return null;
    }

    @Override
    public Void visitField(MemoryMapDslParser.FieldContext ctx) {
        String type = ctx.type.getText();
        boolean typeIsFieldSize = type.length() == 2;
        String fieldType = typeIsFieldSize ? "byte" : type;
        String name = join(ctx.nameWords);
        try {
            // A String uses its array size as byte length, it is not a real array of entries
            MemoryArray array = type.equals("String") ? null : parseArray(ctx);
            MemoryField field = new MemoryField(currentMap, ctx.getStart().getLine(), name, fieldType, array);
            switch (type) {
                case "byte" -> field.setSize(MemoryInt28.from(1));
                case "short" -> field.setSize(MemoryInt28.from(2));
                case "int" -> field.setSize(MemoryInt28.from(4));
                case "long" -> field.setSize(MemoryInt28.from(8));
                case "String" -> field.setSize(MemoryInt28.from(stringSize(ctx, name)));
                default -> {
                    if (typeIsFieldSize) {
                        field.setSize(MemoryInt28.from(Integer.parseInt(type, 16)));
                    } else {
                        // resolved in the second pass (struct/enum reference)
                        field.setSize(null);
                    }
                }
            }
            fields.add(field);
            currentMap.add(field);
            return null;
        } catch (NumberFormatException e) {
            throw error(ctx, "Unexpected field definition: " + ctx.getText(), e);
        }
    }

    private int stringSize(MemoryMapDslParser.FieldContext ctx, String name) {
        if (ctx.array() == null || ctx.array()
                .arrayDim()
                .isEmpty()) {
            throw error(ctx, "String field '%s' requires a size".formatted(name));
        }
        return Integer.parseInt(ctx.array()
                .arrayDim(0)
                .size.getText());
    }

    private MemoryArray parseArray(MemoryMapDslParser.FieldContext ctx) {
        if (ctx.array() == null) {
            return null;
        }
        List<MemoryArrayIndex> indexes = new ArrayList<>();
        for (var dim : ctx.array()
                .arrayDim()) {
            indexes.add(new MemoryArrayIndex(Integer.parseInt(dim.size.getText()),
                    dim.indexName == null ? "" : dim.indexName.getText()));
        }
        return new MemoryArray(indexes);
    }

    // ------------------------------------------------------------------
    // Semantic passes (same rules as the former line-based parser)
    // ------------------------------------------------------------------

    /**
     * Since a memory map contains types defined later in the file, references are
     * resolved in a second pass.
     *
     * <p>The stream order matters: zones are declared before structs in the file,
     * and resolving a zone increments the reference count of the structs it uses.
     * {@link MemoryMap#isTopLevel()} is evaluated lazily, so structs are no longer
     * top-level when the stream reaches them.
     */
    private void resolveTypeReferences() {
        memoryMaps.stream()
                .filter(MemoryMap::isTopLevel)
                .forEach(memoryMap -> {
                    if (memoryMap.getType() == MemoryMapType.STRUCT) {
                        throw new MidiError("Unused Struct: " + memoryMap.getName());
                    }
                    resolveTypeReferences(memoryMap, memoryMap.getFormat());
                });
    }

    private void resolveTypeReferences(MemoryMap memoryMap, MemoryMapFormat parentFormat) {
        resolveEnumReferences();
        updateMemoryFormat(memoryMap, parentFormat);
        memoryMap.getFields()
                .forEach(field -> {
                    if (field.getSize() == null) {
                        resolveTypeReference(memoryMap, field);
                    }
                });
        updateDeviceMemoryMapSize(memoryMap);
    }

    private void updateMemoryFormat(MemoryMap memoryMap, MemoryMapFormat parentFormat) {
        if (memoryMap.getFormat() == null) {
            memoryMap.setFormat(parentFormat);
        }
        // A child may declare its own format and override the parent one. This
        // is required by composite structs such as the SE-02 Preset, which
        // mixes NIBBLES parameters with BYTES name and reserved fields.
    }

    private void resolveTypeReference(MemoryMap memoryMap, MemoryField field) {
        if (field.getSize() != null) {
            return;
        }
        var type = field.getType();
        var referencedMemoryMap = memoryMaps.stream()
                .filter(dms -> dms.getName()
                        .equals(type))
                .findFirst()
                .orElseThrow(() -> new MidiError("Type reference %s not found at line %d".formatted(type, field.getLine())));
        if (referencedMemoryMap.getFormat() == null) {
            referencedMemoryMap.setFormat(memoryMap.getFormat());
        }
        referencedMemoryMap.incReferenceCount();
        resolveTypeReferences(referencedMemoryMap, memoryMap.getFormat());
        field.setReference(referencedMemoryMap);
        field.setSize(referencedMemoryMap.getSize());
    }

    private void resolveEnumReferences() {
        memoryEnums.forEach(e -> fields.stream()
                .filter(f -> e.getName()
                        .equals(f.getType()))
                .forEach(f -> {
                    f.setEnumReference(e);
                    f.setSize(MemoryInt28.from(1));
                }));
    }

    /**
     * Knowing the size of each field, compute the size of the memory map.
     */
    private void updateDeviceMemoryMapSize(MemoryMap memoryMap) {
        if (memoryMap.getSize() != null) {
            return;
        }
        int size = 0;
        for (MemoryField f : memoryMap.getFields()) {
            if (f.getSize() == null) {
                throw new MidiError("Field without size: %s at line %d".formatted(f.getName(), f.getLine()));
            }
            size += f.getTotalSize()
                    .value();
        }
        memoryMap.setSize(size);
    }

    private void resolveAddresses() {
        memoryMaps.forEach(dms -> {
            MemoryInt28 fieldAddress = dms.getBaseAddress();
            for (var field : dms.getFields()) {
                field.setAddress(fieldAddress);
                fieldAddress = fieldAddress.add(field.getEffectiveTotalSize());
            }
        });
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private MemoryMapFormat takeFormat(List<String> words, MemoryMapFormat defaultFormat) {
        if (!words.isEmpty()) {
            String first = words.get(0);
            if ("BYTES".equals(first) || "NIBBLES".equals(first)) {
                words.remove(0);
                return MemoryMapFormat.valueOf(first);
            }
        }
        return defaultFormat;
    }

    private String name(List<String> words, ParserRuleContext ctx, String kind) {
        if (words.isEmpty()) {
            throw error(ctx, "Missing %s name".formatted(kind));
        }
        return String.join(" ", words);
    }

    private MemoryInt28 parseHex(String value, ParserRuleContext ctx, String what) {
        try {
            return MemoryInt28.from(value, packed);
        } catch (MidiError e) {
            throw error(ctx, "Invalid %s '%s': %s".formatted(what, value, e.getMessage()), e);
        }
    }

    private static List<String> words(MemoryMapDslParser.WordsContext ctx) {
        List<String> result = new ArrayList<>();
        for (var word : ctx.word()) {
            result.add(word.getText());
        }
        return result;
    }

    private static String join(MemoryMapDslParser.WordsContext ctx) {
        return ctx == null ? null : String.join(" ", words(ctx));
    }

    private MidiError error(ParserRuleContext ctx, String message) {
        return new MidiError("%s:%d: %s".formatted(file.getName(), ctx.getStart()
                .getLine(), message));
    }

    private MidiError error(ParserRuleContext ctx, String message, Throwable cause) {
        return new MidiError("%s:%d: %s".formatted(file.getName(), ctx.getStart()
                .getLine(), message), cause);
    }
}

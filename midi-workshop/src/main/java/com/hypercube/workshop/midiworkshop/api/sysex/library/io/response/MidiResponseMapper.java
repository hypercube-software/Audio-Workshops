package com.hypercube.workshop.midiworkshop.api.sysex.library.io.response;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.alesis.AlesisSysExParser;
import com.hypercube.workshop.midiworkshop.api.sysex.parser.ManufacturerSysExParser;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Getter
@Setter
@Slf4j
public class MidiResponseMapper {
    public static final String BRAND_ALESIS = "Alesis";
    private final AlesisSysExParser alesisSysExParser = new AlesisSysExParser();
    private String name;
    private MidiDeviceDefinition device;
    private Map<String, MidiResponseField> fields;

    public void extract(MidiDeviceMode mode, ExtractedFields currentResponse, CustomMidiEvent event) {
        fields.values()
                .forEach(f -> extract(mode, currentResponse, event, f));
        if (currentResponse.getCategory() == null && currentResponse.getPatchName() != null) {
            searchCategoryInName(mode, currentResponse, currentResponse.getPatchName());
        }
    }

    public void dumpFields(ExtractedFields extractedFields) {
        log.info("Extracted field:");
        fields.keySet()
                .stream()
                .forEach(field -> log.info("%10s: %s".formatted(field, extractedFields.getField(field))));
    }

    private void extract(MidiDeviceMode mode, ExtractedFields currentResponse, CustomMidiEvent event, MidiResponseField field) {
        byte[] payload = event.getMessage()
                .getMessage();

        ManufacturerSysExParser sysExParser = new ManufacturerSysExParser();
        payload = sysExParser.unpackMidiBuffer(device, payload);
        try {
            Files.write(Path.of("unpack.dat"), payload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        switch (field.getType()) {
            case STRING -> extractString(currentResponse, payload, field);
            case NIBBLE_STRING -> extractNibbleString(currentResponse, payload, field);
            case INTEGER -> extractInteger(currentResponse, payload, field);
            case ALESIS_STRING -> extractAlesisString(currentResponse, payload, field);
            case CATEGORY -> extractCategory(mode, currentResponse, payload, field);
        }
    }

    private void extractCategory(MidiDeviceMode mode, ExtractedFields currentResponse, byte[] payload, MidiResponseField field) {
        int categoryIndex = readInteger(payload, field);
        MidiPresetCategory category = device.getCategory(mode, categoryIndex);
        currentResponse.setField(field.getName(), category.name());
    }

    private int readInteger(byte[] payload, MidiResponseField field) {
        if ((field.getUnit() == MidiResponseUnit.BYTE && field.getOffset() >= payload.length) ||
                (field.getUnit() == MidiResponseUnit.BIT && field.getOffset() / 8 >= payload.length)) {
            return 0;
        }
        return switch (field.getUnit()) {
            case BYTE -> readByteInteger(ByteBuffer.wrap(payload), field);
            case BIT -> readBitInteger(field, payload);
        };
    }

    private void searchCategoryInName(MidiDeviceMode mode, ExtractedFields currentResponse, String presetName) {
        currentResponse.setField(ExtractedFields.MIDI_PRESET_CATEGORY, mode.getCategories()
                .stream()
                .filter(c -> c.matches(presetName))
                .findFirst()
                .map(MidiPresetCategory::name)
                .orElse(MidiPresetCategory.UNKNOWN));
    }

    private void extractString(ExtractedFields currentResponse, byte[] payload, MidiResponseField field) {
        String value = "";
        for (int i = field.getOffset(); value.length() < field.getSize(); i++) {
            if (i >= payload.length) {
                value += "❌";
            } else if (payload[i] >= 32 && payload[i] <= 127) {
                value += (char) payload[i];
            } else if (payload[i] == 0) {
                value += " "; // happen in Motif Rack XS for instance
            } else {
                value += "❌";
            }
        }
        value = value.replace("|nitial", "Initial"); // Weird case from Mininova
        value = value.replace("|", " ");// we use this character to store presets, so it is reserved
        currentResponse.setField(field.getName(), value.trim());
    }

    private void extractNibbleString(ExtractedFields currentResponse, byte[] payload, MidiResponseField field) {
        String value = "";
        for (int i = field.getOffset(); value.length() < field.getSize(); i += 2) {
            if (i >= payload.length || i + 1 >= payload.length) {
                value += "❌";
            } else {
                int asciiCode = (payload[i] << 4 | payload[i + 1]);
                if (asciiCode >= 32 && asciiCode <= 127) {
                    value += (char) asciiCode;
                } else {
                    value += "❌";
                }
            }
        }
        value = value.replace("|", " ");// we use this character to store presets, so it is reserved
        currentResponse.setField(field.getName(), value.trim());
    }

    private void extractInteger(ExtractedFields currentResponse, byte[] payload, MidiResponseField field) {
        int value = readInteger(payload, field);
        currentResponse.setField(field.getName(), "" + value);
    }

    private void extractAlesisString(ExtractedFields currentResponse, byte[] payload, MidiResponseField field) {
        String strValue = alesisSysExParser.getString(field, payload)
                .trim();
        currentResponse.setField(field.getName(), strValue);
    }

    private int readByteInteger(ByteBuffer byteBuffer, MidiResponseField location) {
        byteBuffer.position(location.getOffset());
        return switch (location.getSize()) {
            case 1 -> byteBuffer.get();
            case 2 -> byteBuffer.getShort();
            case 4 -> byteBuffer.getInt();
            default -> throw new MidiError("Unexpected value: " + location.getSize());
        };
    }

    private int readBitInteger(MidiResponseField field, byte[] payload) {
        BitStreamReader bsr = new BitStreamReader(payload);
        bsr.readBits(field.getOffset());
        if (field.isLsbFirst()) {
            return bsr.readInvertedBits(field.getSize());
        } else {
            return bsr.readBits(field.getSize());
        }
    }
}

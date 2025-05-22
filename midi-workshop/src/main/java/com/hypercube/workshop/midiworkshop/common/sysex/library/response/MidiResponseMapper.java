package com.hypercube.workshop.midiworkshop.common.sysex.library.response;

import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.common.sysex.manufacturer.alesis.AlesisSysExParser;
import com.hypercube.workshop.midiworkshop.common.sysex.parser.ManufacturerSysExParser;
import com.hypercube.workshop.midiworkshop.common.sysex.util.BitStreamReader;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.Map;

@Getter
@Setter
@Slf4j
public class MidiResponseMapper {
    public static final String BRAND_ALESIS = "Alesis";
    private String name;
    private MidiDeviceDefinition device;
    private Map<String, MidiResponseField> fields;
    private final AlesisSysExParser alesisSysExParser = new AlesisSysExParser();

    public void extract(MidiDeviceMode mode, MidiResponse currentResponse, CustomMidiEvent event) {
        fields.values()
                .forEach(f -> extract(mode, currentResponse, event, f));
        if (currentResponse.getCategory() == null && currentResponse.getPatchName() != null) {
            searchCategoryInName(mode, currentResponse, currentResponse.getPatchName());
        }
    }

    public void dumpFields(MidiResponse midiResponse) {
        log.info("Extracted field:");
        fields.keySet()
                .stream()
                .forEach(field -> log.info("%10s: %s".formatted(field, midiResponse.getField(field))));
    }

    private void extract(MidiDeviceMode mode, MidiResponse currentResponse, CustomMidiEvent event, MidiResponseField field) {
        byte[] payload = event.getMessage()
                .getMessage();

        ManufacturerSysExParser sysExParser = new ManufacturerSysExParser();
        payload = sysExParser.unpackMidiBuffer(device, payload);

        switch (field.getType()) {
            case STRING -> extractString(currentResponse, payload, field);
            case INTEGER -> extractInteger(currentResponse, payload, field);
            case ALESIS_STRING -> extractAlesisString(currentResponse, payload, field);
            case CATEGORY -> extractCategory(mode, currentResponse, payload, field);
        }
    }

    private void extractCategory(MidiDeviceMode mode, MidiResponse currentResponse, byte[] payload, MidiResponseField field) {
        int categoryIndex = readInteger(payload, field);
        MidiPresetCategory category = device.getCategory(mode, categoryIndex);
        currentResponse.addField(field.getName(), category.name());
    }

    private int readInteger(byte[] payload, MidiResponseField field) {
        return switch (field.getUnit()) {
            case BYTE -> readByteInteger(ByteBuffer.wrap(payload), field);
            case BIT -> readBitInteger(field, payload);
        };
    }

    private void searchCategoryInName(MidiDeviceMode mode, MidiResponse currentResponse, String presetName) {
        currentResponse.addField(MidiResponse.MIDI_PRESET_CATEGORY, mode.getCategories()
                .stream()
                .filter(c -> c.matches(presetName))
                .findFirst()
                .map(MidiPresetCategory::name)
                .orElse("Unknown"));
    }

    private void extractString(MidiResponse currentResponse, byte[] payload, MidiResponseField field) {
        String value = "";
        for (int i = field.getOffset(); value.length() < field.getSize(); i++) {
            if (payload[i] >= 32 && payload[i] <= 127) {
                value += (char) payload[i];
            } else if (payload[i] == 0) {
                value += " "; // happen in Motif Rack XS for instance
            } else {
                value += "❌";
            }
        }
        currentResponse.addField(field.getName(), value.trim());
    }

    private void extractInteger(MidiResponse currentResponse, byte[] payload, MidiResponseField field) {
        int value = readInteger(payload, field);
        currentResponse.addField(field.getName(), "" + value);
    }

    private void extractAlesisString(MidiResponse currentResponse, byte[] payload, MidiResponseField field) {
        String strValue = alesisSysExParser.getString(field, payload)
                .trim();
        currentResponse.addField(field.getName(), strValue);
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
        if (device.getBrand()
                .equals(BRAND_ALESIS)) {
            return bsr.readInvertedBits(field.getSize());
        } else {
            return bsr.readBits(field.getSize());
        }
    }
}

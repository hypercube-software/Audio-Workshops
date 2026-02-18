package com.hypercube.workshop.midiworkshop.api.sysex.library.io.response;

import java.util.HashMap;
import java.util.Map;

/**
 * Collect all fields extracted from the MIDI payload with {@link MidiResponseMapper}
 */
public class ExtractedFields {
    public static final String MID_PRESET_NAME = "name";
    public static final String MIDI_PRESET_CATEGORY = "category";
    private final Map<String, String> fields = new HashMap<>();

    public String getField(String fieldName) {
        return fields.get(fieldName);
    }

    public void setField(String fieldName, String value) {
        fields.put(fieldName, value);
    }

    public String getPatchName() {
        return getField(MID_PRESET_NAME);
    }

    public String getCategory() {
        return getField(MIDI_PRESET_CATEGORY);
    }
}

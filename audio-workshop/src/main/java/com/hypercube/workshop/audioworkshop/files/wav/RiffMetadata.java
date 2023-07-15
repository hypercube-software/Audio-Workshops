package com.hypercube.workshop.audioworkshop.files.wav;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RiffMetadata {

    private final Map<String, String> values = new HashMap<>();

    public void put(MetadataField field, String value) {
        if (value == null)
            return;
        value = value.trim();
        if (value.length() == 0)
            return;

        // cleanup
        if (field == MetadataField.VENDOR && value.toLowerCase()
                .startsWith("logic"))
            field = MetadataField.SOFTWARE;

        values.put(field.name(), value);
    }

    public String get(MetadataField field) {
        return values.get(field.name());
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(values);
    }
}

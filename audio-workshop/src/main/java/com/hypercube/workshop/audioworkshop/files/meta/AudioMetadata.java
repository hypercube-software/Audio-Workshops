package com.hypercube.workshop.audioworkshop.files.meta;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AudioMetadata {

    private final Map<String, String> values = new HashMap<>();

    public void put(MetadataField field, String value) {
        if (value == null)
            return;
        if (values.containsKey(field)) {
            log.warn("Metadata field {} already set", field);
            return;
        }
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

    public boolean contains(MetadataField field) {
        return values.containsKey(field.toString());
    }

    public void merge(AudioMetadata metadata) {
        metadata.values.forEach((k, v) -> {
            if (!values.containsKey(k)) {
                values.put(k, v);
            }
        });
    }
}

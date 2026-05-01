package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

@JsonIgnoreType
public record RawData(byte[] content, long position) {
    public int size() {
        return content.length;
    }

    @JsonIgnore
    public BitStreamReader getBitStream() {
        return new BitStreamReader(content);
    }
}

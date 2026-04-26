package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model;

import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public record RawData(byte[] content, long position) {
    public int size() {
        return content.length;
    }

    public BitStreamReader getBitStream() {
        return new BitStreamReader(content);
    }
}

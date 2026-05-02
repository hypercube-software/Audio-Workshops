package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import lombok.Getter;
import lombok.experimental.Accessors;

@JsonIgnoreType
@Getter()
@Accessors(fluent = true)
public final class RawData {
    private final byte[] content;
    private final long position;
    private final BitStreamReader bitStreamReader;

    public RawData(byte[] content, long position) {
        this.content = content;
        this.position = position;
        this.bitStreamReader = new BitStreamReader(content);
    }

    public int size() {
        return content.length;
    }

    public RawData readChildBlock(int size) {
        long childPos = position + bitStreamReader.getBytePos();
        byte[] content = new byte[size];
        RawData childContent = new RawData(content, childPos);
        for (int i = 0; i < size; i++) {
            content[i] = (byte) bitStreamReader.readByte();
        }
        return childContent;
    }
}

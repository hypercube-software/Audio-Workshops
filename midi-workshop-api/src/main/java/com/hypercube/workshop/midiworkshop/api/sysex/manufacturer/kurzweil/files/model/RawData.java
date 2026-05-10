package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.json.HexDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.json.HexSerializer;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class RawData {
    @JsonSerialize(using = HexSerializer.class)
    @JsonDeserialize(using = HexDeserializer.class)
    private byte[] content;
    private long position;
    @JsonIgnore
    private BitStreamReader bitStreamReader;

    public RawData(@JsonDeserialize(using = HexDeserializer.class) byte[] content, long position) {
        this.content = content;
        this.position = position;
    }

    public BitStreamReader bitStreamReader() {
        if (bitStreamReader == null) {
            bitStreamReader = new BitStreamReader(content);
        }
        return bitStreamReader;
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

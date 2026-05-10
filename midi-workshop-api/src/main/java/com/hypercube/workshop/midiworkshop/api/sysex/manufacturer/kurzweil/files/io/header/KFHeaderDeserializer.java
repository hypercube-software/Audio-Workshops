package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.header;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.KFDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.header.KFHeader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class KFHeaderDeserializer extends KFDeserializer {
    public KFHeader deserialize(RawData data) {
        ByteBuffer buffer = ByteBuffer.wrap(data.getContent());
        buffer.order(ByteOrder.BIG_ENDIAN);
        String magic = readMagic(buffer);
        long sampleOffset = readUnsignedInt(buffer);
        return new KFHeader(data, magic, sampleOffset);
    }


}

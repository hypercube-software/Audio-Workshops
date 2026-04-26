package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFHeader;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class KFHeaderDeserializer extends KFDeserializer {
    public static KFHeader deserialize(RawData data) {
        ByteBuffer buffer = ByteBuffer.wrap(data.content());
        buffer.order(ByteOrder.BIG_ENDIAN);
        String magic = readMagic(buffer);
        long sampleOffset = readUnsignedInt(buffer);
        return new KFHeader(data, magic, sampleOffset);
    }


}

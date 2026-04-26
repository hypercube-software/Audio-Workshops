package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import java.nio.ByteBuffer;

public abstract class KFDeserializer {
    protected static String readMagic(ByteBuffer buffer) {
        return "%c%c%c%c".formatted(buffer.getChar(), buffer.getChar(), buffer.getChar(), buffer.getChar());
    }

    protected static int readUnsignedShort(ByteBuffer byteBuffer) {
        return byteBuffer.getShort() & 0xFFFF;
    }

    protected static long readUnsignedInt(ByteBuffer byteBuffer) {
        return byteBuffer.getInt() & 0xFFFFFFFFL;
    }
}

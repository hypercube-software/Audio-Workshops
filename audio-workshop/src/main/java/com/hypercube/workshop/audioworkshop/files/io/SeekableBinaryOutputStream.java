package com.hypercube.workshop.audioworkshop.files.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Provides an abstraction on top of FileChannel and FileOutputStream
 * to be more convenient
 */
public class SeekableBinaryOutputStream implements AutoCloseable {

    private final FileOutputStream out;
    private final FileChannel ch;
    private final ByteBuffer buffer;
    private static final int MAX_BUFFER_SIZE = 8;

    public SeekableBinaryOutputStream(FileOutputStream out) {
        this.out = out;
        ch = out.getChannel();
        buffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
    }

    public void writeByte(int value) throws IOException {
        buffer.clear();
        buffer.rewind();
        buffer.put((byte) value);
        ch.write(buffer.slice(0, 1));
    }

    public void writeShortBE(int value) throws IOException {
        buffer.clear();
        buffer.rewind();
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) value);
        ch.write(buffer.slice(0, 2));
    }

    public void writeShortLE(int value) throws IOException {
        buffer.clear();
        buffer.rewind();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) value);
        ch.write(buffer.slice(0, 2));
    }

    public void writeIntBE(int value) throws IOException {
        buffer.clear();
        buffer.rewind();
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(value);
        ch.write(buffer.slice(0, 4));
    }

    public void writeIntLE(int value) throws IOException {
        buffer.clear();
        buffer.rewind();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
        ch.write(buffer.slice(0, 4));
    }

    public void writeChunkId(String value) throws IOException {
        buffer.clear();
        buffer.rewind();
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        buffer.put(bytes);
        ch.write(buffer.slice(0, 4));
    }

    public void writeLongBE(long value) throws IOException {
        buffer.clear();
        buffer.rewind();
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(value);
        ch.write(buffer.slice(0, 8));
    }

    public void writeLongLE(long value) throws IOException {
        buffer.clear();
        buffer.rewind();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(value);
        ch.write(buffer.slice(0, 8));
    }

    public void writeInt24BE(int value) throws IOException {
        buffer.clear();
        buffer.rewind();
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(value);
        byte[] data = Arrays.copyOfRange(buffer.array(), 1, 4);
        ch.write(ByteBuffer.wrap(data));
    }

    public void writeInt24LE(int value) throws IOException {
        buffer.clear();
        buffer.rewind();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
        byte[] data = Arrays.copyOfRange(buffer.array(), 0, 3);
        ch.write(ByteBuffer.wrap(data));
    }

    public void writeBytes(byte[] value) throws IOException {
        writeBytes(value, value.length);
    }

    public void writeBytes(byte[] value, int size) throws IOException {
        ch.write(ByteBuffer.wrap(value, 0, size));
    }

    public void writeBytes(byte[] value, int offset, int size) throws IOException {
        ch.write(ByteBuffer.wrap(value, offset, size));
    }

    public void seek(long position) throws IOException {
        ch.position(position);
    }

    public long position() throws IOException {
        return ch.position();
    }

    @Override
    public void close() throws IOException {
        ch.close();
        out.close();
    }

    public void writeZeros(int size) throws IOException {
        byte[] pad = new byte[size]; // Guaranteed to be filled by zero by Java spec
        ch.write(ByteBuffer.wrap(pad));
    }

    public void writeFloatLE(float value) throws IOException {
        buffer.clear();
        buffer.rewind();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(value);
        ch.write(buffer.slice(0, 4));
    }

    public void writeFloatBE(float value) throws IOException {
        buffer.clear();
        buffer.rewind();
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putFloat(value);
        ch.write(buffer.slice(0, 4));
    }
}

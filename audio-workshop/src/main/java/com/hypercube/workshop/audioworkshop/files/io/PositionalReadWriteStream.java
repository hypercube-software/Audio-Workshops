package com.hypercube.workshop.audioworkshop.files.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Class used to load or write various audio file formats
 * <p>
 * - We provide the ability to read and write "Unsigned int" because it is often usefull
 * despite the fact Java does not support this kind of type.
 * - We also support indianness using ByteBuffer
 */
public class PositionalReadWriteStream implements Closeable {

    private final RandomAccessFile stream;

    private final FileChannel channel;

    public long positionLong() throws IOException {
        return channel.position();
    }

    public int positionUInt() throws IOException {
        return (int) channel.position();
    }

    public PositionalReadWriteStream(File file) throws FileNotFoundException {
        if (file.exists()) {
            file.setWritable(true);
        }
        stream = new RandomAccessFile(file, "rw");
        channel = stream.getChannel();
    }

    public int getIntBE() throws IOException {
        byte[] data = readNBytes(4);
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getInt();

    }

    public int getIntLE() throws IOException {
        byte[] data = readNBytes(4);
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();

    }

    public long getUIntLE() throws IOException {
        return getIntLE() & 0xFFFFFFFFL;
    }

    public long getUIntBE() throws IOException {
        return getIntBE() & 0xFFFFFFFFL;
    }

    public long getLongBE() throws IOException {
        byte[] data = readNBytes(8);
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getLong();
    }

    public long getLongLE() throws IOException {
        byte[] data = readNBytes(8);
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getLong();

    }

    public float getFloatBE() throws IOException {
        byte[] data = readNBytes(4);
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getFloat();
    }

    public float getFloatLE() throws IOException {
        byte[] data = readNBytes(4);
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getFloat();
    }

    public short getShortBE() throws IOException {
        byte[] data = readNBytes(2);
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getShort();
    }

    public short getShortLE() throws IOException {
        byte[] data = readNBytes(2);
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort();
    }

    public int getByte() throws IOException {
        return stream.read();
    }

    public int read() throws IOException {
        return stream.read();
    }

    public int read(byte[] b) throws IOException {
        return stream.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return stream.read(b, off, len);
    }

    public byte[] readNBytes(int len) throws IOException {
        byte[] result = new byte[len];
        stream.readFully(result);
        return result;
    }

    public long skip(int n) throws IOException {
        return stream.skipBytes(n);
    }

    /**
     * Convert the int offset in unsigned int then go there
     *
     * @param n
     * @throws IOException
     */
    public void seekUInt(int n) throws IOException {
        long longPosition = n & 0xFFFFFFFFL;
        channel.position(longPosition);
    }

    public void seekLong(long n) throws IOException {
        channel.position(n);
    }

    public void readNBytes(byte[] b, int off, int len) throws IOException {
        stream.readFully(b, off, len);
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    public LocalDateTime getDateTime() throws IOException {
        int timeInSeconds = getIntBE();
        return LocalDateTime.ofEpochSecond(timeInSeconds, 0, ZoneOffset.UTC);
    }

    public int capacity() throws IOException {
        return (int) channel.size();
    }

    public void putIntBE(int i) throws IOException {
        byte[] data = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(i);
        stream.write(data);
    }

    public void putIntLE(int i) throws IOException {
        byte[] data = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(i);
        stream.write(data);
    }
}

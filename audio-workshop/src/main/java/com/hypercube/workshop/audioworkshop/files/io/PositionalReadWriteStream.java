package com.hypercube.workshop.audioworkshop.files.io;

import com.hypercube.workshop.audioworkshop.common.utils.CachedRegExp;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.regex.Matcher;

/**
 * Class used to load or write various audio file formats
 * <p>
 * - We provide the ability to read and write "Unsigned int" because it is often useful
 * despite the fact Java does not support this kind of type.
 * - We also support endianness using ByteBuffer
 */
@SuppressWarnings("java:S117")
public class PositionalReadWriteStream implements Closeable {

    private final RandomAccessFile stream;

    private final FileChannel channel;

    public long positionLong() throws IOException {
        return channel.position();
    }

    public int positionUInt() throws IOException {
        return (int) channel.position();
    }

    public PositionalReadWriteStream(File file, boolean canWrite) throws IOException {
        if (canWrite) {
            if (file.exists()) {
                if (!file.setWritable(true)) {
                    throw new IOException("Unable to make the file writable:" + file.getAbsolutePath());
                }
            }
            stream = new RandomAccessFile(file, "rw");
        } else {
            stream = new RandomAccessFile(file, "r");
        }
        channel = stream.getChannel();
    }

    public UUID getUUID() throws IOException {
        // https://devblogs.microsoft.com/oldnewthing/20220928-00/?p=107221
        int time_low = getIntLE();
        int time_mid = getShortLE();
        int time_hi_and_version = getShortLE();
        int clock_seq_hi_and_reserved = getByte();
        int cloc_seq_low = getByte();
        byte[] node = new byte[6];
        stream.readFully(node);
        String UUIDStr = String.format("%08X-%04X-%04X-%02X%02X-%02X%02X%02X%02X%02X%02X",
                time_low, time_mid, time_hi_and_version, clock_seq_hi_and_reserved, cloc_seq_low
                , node[0], node[1], node[2], node[3], node[4], node[5]);
        return UUID.fromString(UUIDStr);
    }

    public void putUUID(UUID uuid) throws IOException {
        // https://devblogs.microsoft.com/oldnewthing/20220928-00/?p=107221
        Matcher m = CachedRegExp.get("([^- ]{8})-([^- ]{4})-([^- ]{4})-([^- ]{2})([^- ]{2})-([^- ]{2})([^- ]{2})([^- ]{2})([^- ]{2})([^- ]{2})([^- ]{2})", uuid.toString());
        if (m.find()) {
            int time_low = Integer.parseInt(m.group(1), 16);
            int time_mid = Integer.parseInt(m.group(2), 16);
            int time_hi_and_version = Integer.parseInt(m.group(3), 16);
            int clock_seq_hi_and_reserved = Integer.parseInt(m.group(4), 16);
            int clock_seq_low = Integer.parseInt(m.group(5), 16);
            int node0 = Integer.parseInt(m.group(6), 16);
            int node1 = Integer.parseInt(m.group(7), 16);
            int node2 = Integer.parseInt(m.group(8), 16);
            int node3 = Integer.parseInt(m.group(9), 16);
            int node4 = Integer.parseInt(m.group(10), 16);
            int node5 = Integer.parseInt(m.group(11), 16);

            putIntLE(time_low);
            putShortLE((short) time_mid);
            putShortLE((short) time_hi_and_version);
            putByte(clock_seq_hi_and_reserved);
            putByte(clock_seq_low);
            putByte(node0);
            putByte(node1);
            putByte(node2);
            putByte(node3);
            putByte(node4);
            putByte(node5);
        }
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

    public double getdoubleBE() throws IOException {
        byte[] data = readNBytes(4);
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getDouble();
    }

    public double getdoubleLE() throws IOException {
        byte[] data = readNBytes(4);
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getDouble();
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

    public void putByte(int v) throws IOException {
        stream.write(v);
    }

    public void putShortLE(short i) throws IOException {
        byte[] data = new byte[2];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort(i);
        stream.write(data);
    }

    public void putShortBE(short i) throws IOException {
        byte[] data = new byte[2];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putShort(i);
        stream.write(data);
    }
    //

    /**
     * Read a 80 bit IEEE-754 number, and convert it to double
     * <p>
     * See <a href ="https://stackoverflow.com/a/35670539">This Stackoverflow response</a>
     */
    public double getLongDoubleBE() throws IOException {
        // This code is enough to read a sample rate, but does not cover all cases
        // It doesn't round properly, doesn't check for overflow, doesn't handle unnormalized values and probably fails to handle special values like NaN.
        long high = getShortBE();
        long low = getLongBE();
        long e = (((high & 0x7FFFL) - 16383) + 1023) & 0x7FFL;
        long ld = ((high & 0x8000L) << 48)
                | (e << 52)
                | ((low >>> 11) & 0xF_FFFF_FFFF_FFFFL);
        return Double.longBitsToDouble(ld);
    }

    public String getPascalString() throws IOException {
        int count = read();
        byte[] data = new byte[count];
        read(data);
        if (count % 2 != 0) {
            read(); // padding (not included in count)
        }
        return new String(data, StandardCharsets.ISO_8859_1);
    }

    public String getASCIIString(int size) throws IOException {
        byte[] data = readNBytes(size);
        return new String(data, StandardCharsets.US_ASCII);
    }

    public String getBEString() throws IOException {
        int size = getIntBE();
        String str = getASCIIString(size - 1);
        int zero = getByte();
        if (zero != 0) {
            throw new AssertionError("BEString not ended with zero at %08X".formatted(positionUInt()));
        }
        return str;
    }
}

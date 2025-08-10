package com.hypercube.workshop.audioworkshop.files.riff;

import com.hypercube.workshop.audioworkshop.api.errors.AudioError;
import com.hypercube.workshop.audioworkshop.api.utils.CachedRegExp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;

/**
 * We are using UUID to store GUID.
 * <p>
 * See {@link com.hypercube.workshop.audioworkshop.files.io.PositionalReadWriteStream#getUUID PositionalReadWriteStream::readUUID()} to read it from disk
 * </p>
 * <p>Readings:
 * <a href="https://learn.microsoft.com/en-us/windows/win32/wmformat/media-type-identifiers">media-type-identifiers</a>
 * <a href="https://learn.microsoft.com/en-us/windows/win32/directshow/fourccmap">fourccmap</a>
 * <a href="https://learn.microsoft.com/en-us/windows/win32/directshow/audio-subtypes">audio-subtypes</a>
 * <a href="https://gix.github.io/media-types/>media-types"></a>
 * <a href="https://github.com/de-husk/mpc-hc/blob/master/include/moreuuids.h">more uuids.h</a>
 */
@SuppressWarnings("unused")
public class WaveGUIDCodecs {
    public static final UUID WMMEDIASUBTYPE_PCM = UUID.fromString("00000001-0000-0010-8000-00aa00389b71");
    public static final UUID WMMEDIASUBTYPE_IEEE754_LE_double = UUID.fromString("00000003-0000-0010-8000-00aa00389b71");
    public static final UUID WMMEDIASUBTYPE_PCM_BE_INT24 = UUID.fromString("34326E69-0000-0010-8000-00AA00389B71");
    public static final UUID WMMEDIASUBTYPE_PCM_BE_INT32 = UUID.fromString("32336E69-0000-0010-8000-00AA00389B71");
    public static final UUID WMMEDIASUBTYPE_PCM_BE_FL32 = UUID.fromString("32336C66-0000-0010-8000-00AA00389B71");
    public static final UUID WMMEDIASUBTYPE_PCM_BE_FL64 = UUID.fromString("34366C66-0000-0010-8000-00AA00389B71");
    public static final UUID WMMEDIASUBTYPE_PCM_LE_INT24 = UUID.fromString("696E3234-0000-0010-8000-00AA00389B71");
    public static final UUID WMMEDIASUBTYPE_PCM_LE_INT32 = UUID.fromString("696E3332-0000-0010-8000-00AA00389B71");
    public static final UUID WMMEDIASUBTYPE_PCM_LE_FL32 = UUID.fromString("666C3332-0000-0010-8000-00AA00389B71");
    public static final UUID WMMEDIASUBTYPE_PCM_LE_FL64 = UUID.fromString("666C3634-0000-0010-8000-00AA00389B71");

    public static final List<UUID> PCM_CODECS = List.of(WMMEDIASUBTYPE_PCM,
            WMMEDIASUBTYPE_PCM_BE_INT32,
            WMMEDIASUBTYPE_PCM_BE_INT24,
            WMMEDIASUBTYPE_PCM_BE_FL32,
            WMMEDIASUBTYPE_PCM_BE_FL64);

    private WaveGUIDCodecs() {
    }

    public static byte[] getBytes(UUID uuid) {
        Matcher m = CachedRegExp.get("([^- ]{8})-([^- ]{4})-([^- ]{4})-([^- ]{2})([^- ]{2})-([^- ]{2})([^- ]{2})([^- ]{2})([^- ]{2})([^- ]{2})([^- ]{2})", uuid.toString());
        if (m.find()) {
            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
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
            buffer.putInt(time_low);
            buffer.putShort((short) time_mid);
            buffer.putShort((short) time_hi_and_version);
            buffer.put((byte) clock_seq_hi_and_reserved);
            buffer.put((byte) clock_seq_low);
            buffer.put((byte) node0);
            buffer.put((byte) node1);
            buffer.put((byte) node2);
            buffer.put((byte) node3);
            buffer.put((byte) node4);
            buffer.put((byte) node5);
            return buffer.array();
        }
        throw new AudioError("Illegal UUID " + uuid.toString());
    }
}

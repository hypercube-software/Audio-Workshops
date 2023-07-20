package com.hypercube.workshop.audioworkshop.files.riff;

import java.util.List;
import java.util.UUID;

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
    public static final UUID WMMEDIASUBTYPE_IEEE754_LE_FLOAT = UUID.fromString("00000003-0000-0010-8000-00aa00389b71");
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
}

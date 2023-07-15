package com.hypercube.workshop.audioworkshop.files.wav;

import java.util.UUID;

/**
 * https://learn.microsoft.com/en-us/windows/win32/wmformat/media-type-identifiers
 * https://learn.microsoft.com/en-us/windows/win32/directshow/fourccmap
 * https://learn.microsoft.com/en-us/windows/win32/directshow/audio-subtypes
 */
public class WaveGUIDCodecs {
    public static final UUID WMMEDIASUBTYPE_PCM_BE = UUID.fromString("00000001-0000-0010-8000-00aa00389b71");
    public static final UUID WMMEDIASUBTYPE_PCM_LE = UUID.fromString("01000000-0000-1000-8000-00aa00389b71");
    public static final UUID WMMEDIASUBTYPE_IEEE754_FLOAT = UUID.fromString("00000003-0000-1000-8000-00aa00389b71");

    private WaveGUIDCodecs() {
    }
}

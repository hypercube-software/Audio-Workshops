package com.hypercube.workshop.midiworkshop.api.sysex.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SysExConstants {
    public static final int SYSEX_START = 0xF0;
    public static final int SYSEX_END = 0xF7;

    public static final int SYSEX_NON_REALTIME = 0x7E;
    public static final int SYSEX_REALTIME = 0x7F;
    public static final int SYSEX_GENERAL_INFORMATION = 0x06;
    public static final int SYSEX_IDENTITY_RESPONSE = 0x02;
    public static final int SYSEX_IDENTITY_REQUEST = 0x01; // aka Device Inquiry request

    public static final int ROLAND_SOUND_CANVAS_DEVICE_MULTI = 0x42;
    public static final int ROLAND_SOUND_CANVAS_DEVICE_SINGLE = 0x55;
}

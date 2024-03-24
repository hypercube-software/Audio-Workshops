package com.hypercube.workshop.midiworkshop.sysex.util;

public class SystemExclusiveConstants {
    public static final int SYSEX_START = 0xF0;
    public static final int SYSEX_END = 0xF7;

    public static final int SYSEX_NON_REALTIME = 0x7E;
    public static final int SYSEX_GENERAL_INFORMATION = 0x06;
    public static final int SYSEX_IDENTITY_RESPONSE = 0x02;
    public static final int SYSEX_IDENTITY_REQUEST = 0x01; // aka Device Inquiry request

}

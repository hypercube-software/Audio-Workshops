package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil;

import java.util.Arrays;
import java.util.Optional;

public enum KObject {
    PROGRAM(0x0104),
    KEYMAP(0x0105),
    STUDIO(0x0071),
    SONG(0x0070),
    SETUP(0x0107),
    SOUND_BLOCK(0x0106),
    VELOCITY_MAP(0x0068),
    PRESSURE_MAP(0x0069),
    QUICK_ACCESS_MAP(0x6F),
    INTONATION_TABLE(0x0067),
    MASTER_PARAMETER(0x0064);

    private final int type;

    private KObject(int type) {
        this.type = type;
    }

    public static Optional<KObject> fromType(int type) {
        return Arrays.stream(KObject.values())
                .filter(c -> c.type == type)
                .findFirst();
    }
}

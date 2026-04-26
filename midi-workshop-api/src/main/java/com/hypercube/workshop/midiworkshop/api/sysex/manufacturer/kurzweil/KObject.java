package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil;

import java.util.Arrays;
import java.util.Optional;

/**
 * Kurzweil designates an Object Type in two ways: with its id or with its type.
 * They don't explain why so it is very confusing. In reality, it is very simple:
 * <ul>
 *     <li>The type, is a value encoded (packed) for MIDI, so the bit 8 is always 0</li>
 *     <li>The id, is the real value (unpacked)</li>
 * </ul>
 * <p>You can easily convert a type to its id for instance type 0x0104 has id 0x0084
 * <p>00000001 00000100</p>
 * <p>x0000001 x0000100</p>
 * <p>00000010000100</p>
 * <p>0x0084</p>
 */
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
    MASTER_PARAMETER(0x0064),
    UNKNOWN(0x0000);

    private final int type;

    private KObject(int type) {
        this.type = type;
    }

    public static Optional<KObject> fromType(int type) {
        return Arrays.stream(KObject.values())
                .filter(c -> c.type == type)
                .findFirst();
    }

    public static Optional<KObject> fromId(int id) {
        return Arrays.stream(KObject.values())
                .filter(c -> c.getId() == id)
                .findFirst();
    }

    public int getId() {
        return ((type & 0x7F00) >>> 1) | (type & 0x7F);
    }
}

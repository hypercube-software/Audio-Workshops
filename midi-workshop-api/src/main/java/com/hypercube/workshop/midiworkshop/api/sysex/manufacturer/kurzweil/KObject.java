package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil;

import lombok.Getter;

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
 * <p>Object Types can be found in the official objtypes.h</p>
 */
@Getter
public enum KObject {
    /**
     * Index type. ID: 97 (0x61)
     */
    INDEX(97),
    /**
     * Executable code type. ID: 98 (0x62)
     */
    X_CODE(98),
    /**
     * Miscellaneous table type. ID: 100 (0x64)
     */
    TABLE(100),
    /**
     * C&H wiring diagram type. ID: 102 (0x66)
     */
    ALG_DESC(102),
    /**
     * Intonation table type. ID: 103 (0x67)
     */
    INTONATION_TABLE(103),
    /**
     * Velocity map type. ID: 104 (0x68)
     */
    VELOCITY_MAP(104),
    /**
     * Pressure map type. ID: 105 (0x69)
     */
    PRESSURE_MAP(105),
    /**
     * Editor table type. ID: 106 (0x6A)
     */
    EDIT(106),
    /**
     * Edit menu type. ID: 107 (0x6B)
     */
    EDIT_MENU(107),
    /**
     * Edit menu group type. ID: 108 (0x6C)
     */
    MENU_GROUP(108),
    /**
     * Menu entry list type. ID: 109 (0x6D)
     */
    MENU_ENTRY_LIST(109),
    /**
     * PList type. ID: 110 (0x6E)
     */
    P_LIST(110),
    /**
     * BList type. ID: 111 (0x6F)
     */
    B_LIST(111),
    /**
     * Song type. ID: 112 (0x70)
     */
    SONG(112),
    /**
     * Effect type. ID: 113 (0x71)
     */
    EFFECT(113),
    /**
     * New LFO shape function type. ID: 114 (0x72)
     */
    SHAPE(114),
    /**
     * Function proc type. ID: 115 (0x73)
     */
    F_PROC(115),
    /**
     * Program type.
     * ID: 132 (0x84), Packed Value: 0x0104
     */
    PROGRAM(132),
    /**
     * Keymap type.
     * ID: 133 (0x85), Packed Value: 0x0105
     */
    KEYMAP(133),
    /**
     * Sound block type.
     * ID: 134 (0x86), Packed Value: 0x0106
     */
    SOUND_BLOCK(134),
    /**
     * Setup type.
     * ID: 135 (0x87), Packed Value: 0x0107
     */
    SETUP(135),
    /**
     * Marge type.
     * ID: 136 (0x88), Packed Value: 0x0108
     */
    MARGE(136),
    /**
     * Custom Studio type. Not explicitly defined in objtypes.h.
     * ID: 140 (0x8C), Packed Value: 0x010C
     */
    STUDIO(140),
    UNKNOWN(0);

    private final int id;

    private KObject(int id) {
        this.id = id;
    }

    public int getType() {
        int upper7BitsOfId = (id >>> 7) & 0x7F; // Get bits 7-13 of id
        int lower7BitsOfId = id & 0x7F; // Get bits 0-6 of id

        return (upper7BitsOfId << 8) | lower7BitsOfId;
    }

    public static Optional<KObject> fromType(int packedType) {
        return Arrays.stream(KObject.values())
                .filter(c -> c.getType() == packedType)
                .findFirst();
    }

    public static Optional<KObject> fromId(int id) {
        return Arrays.stream(KObject.values())
                .filter(c -> c.id == id)
                .findFirst();
    }
}

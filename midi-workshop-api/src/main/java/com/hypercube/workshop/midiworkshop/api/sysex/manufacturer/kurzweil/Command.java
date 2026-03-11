package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil;

import java.util.Arrays;
import java.util.Optional;

public enum Command {
    DUMP(0x00),
    LOAD(0x01),
    DACK(0x02),
    DNACK(0x03),
    DIR(0x04),
    INFO(0x05),
    NEW(0x06),
    DEL(0x07),
    CHANGE(0x08),
    WRITE(0x09),
    READ(0x0A),
    READ_BANK(0x0B),
    DIR_BANK(0x0C),
    END_OF_BANK(0x0D),
    DEL_BANK(0x0E),
    MOVE_BANK(0x0F),
    LOAD_MACRO(0x10),
    MACRO_DONE(0x11),
    PANEL(0x14),
    ALL_TEXT(0x15),
    PARAM_VALUE(0x16),
    PARAM_NAME(0x17),
    GET_GRAPHICS(0x18),
    SCREEN_REPLY(0x19);

    private final int id;

    private Command(int id) {
        this.id = id;
    }

    public static Optional<Command> fromCode(int commandId) {
        return Arrays.stream(Command.values())
                .filter(c -> c.id == commandId)
                .findFirst();
    }
}

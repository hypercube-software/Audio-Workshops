package com.hypercube.workshop.midiworkshop.common.sysex.macro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandMacroTest {

    @Test
    void parseMacro() {
        CommandMacro actual = CommandMacro.parse("name(p1,p2,p3) : FF F8 p1 000000 p2 p3 F7");
        assertEquals(3, actual.parameters()
                .size());
        assertEquals("name", actual.name());
        assertEquals("p1", actual.parameters()
                .get(0));
        assertEquals("p2", actual.parameters()
                .get(1));
        assertEquals("p3", actual.parameters()
                .get(2));
    }

    @Test
    void expand() {
        CommandMacro macro = CommandMacro.parse("name(p1,p2,p3) : FF F8 p1 000000 p2 p3 F7");
        CommandCall actual = CommandCall.parse("name(12,FE,45)");
        String result = macro.expand(actual);
        assertEquals("FF F8 12 000000 FE 45 F7", result);
    }

    @Test
    void expandWithSpaces() {
        CommandMacro macro = CommandMacro.parse("name(p1,p2,p3) : FF F8 p1 000000 p2 p3 F7");
        CommandCall actual = CommandCall.parse("name (12 ,FE , 45)");
        String result = macro.expand(actual);
        assertEquals("FF F8 12 000000 FE 45 F7", result);
    }
}
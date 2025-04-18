package com.hypercube.workshop.midiworkshop.common.sysex.macro;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandMacroTest {
    File configFile = new File("config.yml");
    File definitionFile = new File("def.yml");

    @Test
    void parseMacro() {
        CommandMacro actual = CommandMacro.parse(definitionFile, "name(p1,p2,p3) : FF F8 p1 000000 p2 p3 F7");
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
    void expandByte() {
        CommandMacro macro = CommandMacro.parse(definitionFile, "name(p1,p2,p3) : FF F8 p1 000000 p2 p3 F7");
        CommandCall actual = CommandCall.parse(configFile, "name(12,0xFE,45)");
        String result = macro.expand(actual);
        assertEquals("FF F8 0C 000000 FE 2D F7", result);
    }

    @Test
    void expandWord() {
        CommandMacro macro = CommandMacro.parse(definitionFile, "name(p1,p2,p3) : FF F8 p1 000000 p2 p3 F7");
        CommandCall actual = CommandCall.parse(configFile, "name(12,0x0000FE,$0045)");
        String result = macro.expand(actual);
        assertEquals("FF F8 0C 000000 0000FE 0045 F7", result);
    }

    @Test
    void expandWithSpaces() {
        CommandMacro macro = CommandMacro.parse(definitionFile, "name(p1,p2,p3) : FF F8 p1 000000 p2 p3 F7");
        CommandCall actual = CommandCall.parse(configFile, "name (12 ,$FE, 45)");
        String result = macro.expand(actual);
        assertEquals("FF F8 0C 000000 FE 2D F7", result);
    }

    @Test
    void expandWithSpacesAndSize() {
        CommandMacro macro = CommandMacro.parse(definitionFile, "name(p1,p2,p3) : 420 : FF F8 p1 000000 p2 p3 F7");
        CommandCall actual = CommandCall.parse(configFile, "name (12 ,$FE, 45)");
        String result = macro.expand(actual);
        assertEquals("FF F8 0C 000000 FE 2D F7", result);
    }
}
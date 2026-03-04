package com.hypercube.workshop.midiworkshop.api.sysex.macro;

import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandMacroTest {
    File configFile;
    File definitionFile;
    MidiDeviceDefinition device;

    @BeforeEach
    void init() {
        configFile = new File("config.yml");
        definitionFile = new File("def.yml");
        device = new MidiDeviceDefinition();
        device.setDefinitionFile(definitionFile);
    }

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
        CommandCall actual = forgeCommandCall("name(p1,p2,p3) : FF F8 p1 000000 p2 p3 F7", "name(12,0xFE,45)");
        String result = actual.macro()
                .expand(actual);
        assertEquals("FF F8 0C 000000 FE 2D F7", result);
    }

    @Test
    void expandWord() {
        CommandCall actual = forgeCommandCall("name(p1,p2,p3) : FF F8 p1 000000 p2 p3 F7", "name(12,0x0000FE,$0045)");
        String result = actual.macro()
                .expand(actual);
        assertEquals("FF F8 0C 000000 0000FE 0045 F7", result);
    }

    @Test
    void expandWithSpaces() {
        CommandCall actual = forgeCommandCall("name(p1,p2,p3) : FF F8 p1 000000 p2 p3 F7", "name (12 ,$FE, 45)");
        String result = actual.macro()
                .expand(actual);
        assertEquals("FF F8 0C 000000 FE 2D F7", result);
    }

    @Test
    void expandWithSpacesAndSize() {
        CommandCall actual = forgeCommandCall("name(p1,p2,p3) : 420 : FF F8 p1 000000 p2 p3 F7", "name (12 ,$FE, 45)");
        String result = actual.macro()
                .expand(actual);
        assertEquals("FF F8 0C 000000 FE 2D F7", result);
    }

    CommandCall forgeCommandCall(String macro, String call) {
        CommandMacro commandMacro = CommandMacro.parse(definitionFile, macro);
        CommandCall commandCall = CommandCall.parse(device.getDefinitionFile(), null, call)
                .getFirst();
        commandCall.macro(commandMacro);
        return commandCall;
    }
}

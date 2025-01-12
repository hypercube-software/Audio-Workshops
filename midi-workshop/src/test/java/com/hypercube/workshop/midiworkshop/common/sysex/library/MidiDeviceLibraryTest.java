package com.hypercube.workshop.midiworkshop.common.sysex.library;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MidiDeviceLibraryTest {
    public static final String DEVICE_NAME = "TG-500";
    public static final File APP_CONFIGFILE = new File("MyAppConfigFile");
    MidiDeviceLibrary midiDeviceLibrary;

    @BeforeEach
    void init() {
        midiDeviceLibrary = new MidiDeviceLibrary();
        midiDeviceLibrary.load(new File("./src/test/resources/devices-library"));
    }

    @Test
    void load() {
        // WHEN
        MidiDeviceDefinition midiDeviceDefinition = midiDeviceLibrary.getDevice(DEVICE_NAME)
                .get();

        // THEN
        assertEquals(DEVICE_NAME, midiDeviceDefinition.getDeviceName());
        assertEquals(14, midiDeviceDefinition.getMacros()
                .size());
    }

    @Test
    void forgeRequestsWithoutMacro() {
        // GIVEN
        CommandMacro commandMacro = CommandMacro.parse(APP_CONFIGFILE, "System Setup() : 142 : F0 43 20 7A 'LM  0066SY' 0000000000000000000000000000 00 00 F7");
        // WHEN
        var actual = midiDeviceLibrary.forgeMidiRequestSequence(APP_CONFIGFILE, DEVICE_NAME, commandMacro);
        // THEN
        assertEquals(1, actual.getMidiRequests()
                .size());
        assertEquals("System Setup", actual.getMidiRequests()
                .get(0)
                .getName());
    }

    @Test
    void forgeRequestsWithSequenceOfMacro() {
        // GIVEN
        CommandMacro commandMacro = CommandMacro.parse(APP_CONFIGFILE, "getAll() : --- : AllMulti();AllPerformances()");
        // WHEN
        var actual = midiDeviceLibrary.forgeMidiRequestSequence(APP_CONFIGFILE, DEVICE_NAME, commandMacro);
        // THEN
        assertEquals("getAll", actual.getName());
        assertEquals(2, actual.getMidiRequests()
                .size());
        MidiRequest firstRequest = actual.getMidiRequests()
                .get(0);
        MidiRequest secondOne = actual.getMidiRequests()
                .get(1);

        assertEquals("F0 43 20 7A 'LM  0065MU' 0000000000000000000000000000 00 [0-15] F7", firstRequest
                .getValue());
        assertEquals(0x120, firstRequest.getSize());

        assertEquals("F0 43 20 7A 'LM  0065PF' 0000000000000000000000000000 00 [0-63]    F7", secondOne
                .getValue());
        assertEquals(0x120, firstRequest.getSize());

        // this assert is tricky, AllMulti() is also based on the macro Multi() this is why we get the final macro as name
        assertEquals("Multi", firstRequest
                .getName());
        // same thing here, AllPerformances() is also based on the macro Performance() this is why we get the final macro as name
        assertEquals("Performance", secondOne
                .getName());
    }

    @Test
    void forgeRequestsWithSequenceOfMacroAndRawPayload() {
        // GIVEN
        CommandMacro commandMacro = CommandMacro.parse(APP_CONFIGFILE, "getAll() : --- : 142 : F0 43 20 7A 'LM  0066SY' 0000000000000000000000000000 00 00 F7;AllMulti();AllPerformances()");
        // WHEN
        var actual = midiDeviceLibrary.forgeMidiRequestSequence(APP_CONFIGFILE, DEVICE_NAME, commandMacro);
        // THEN
        assertEquals("getAll", actual.getName());
        assertEquals(3, actual.getMidiRequests()
                .size());
        assertEquals(0x142, actual.getMidiRequests()
                .get(0)
                .getSize());
        assertEquals(0x120, actual.getMidiRequests()
                .get(1)
                .getSize());
        assertEquals(0x122, actual.getMidiRequests()
                .get(2)
                .getSize());
    }

    @Test
    void forgeRequestsWithoutSequence() {
        // GIVEN
        CommandMacro commandMacro = CommandMacro.parse(APP_CONFIGFILE, "getAll() : --- : AllMulti()");
        // WHEN
        var actual = midiDeviceLibrary.forgeMidiRequestSequence(APP_CONFIGFILE, DEVICE_NAME, commandMacro);
        // THEN
        assertEquals("getAll", actual.getName());
        assertEquals(1, actual.getMidiRequests()
                .size());
        MidiRequest firstRequest = actual.getMidiRequests()
                .get(0);
        assertEquals("F0 43 20 7A 'LM  0065MU' 0000000000000000000000000000 00 [0-15] F7", firstRequest
                .getValue());
        assertEquals(0x120, firstRequest.getSize());
    }

    @Test
    void forgeRequestsWithoutParenthesisFail() {
        // WHEN
        Executable actual = () -> CommandMacro.parse(APP_CONFIGFILE, "getAll : --- : AllMulti()");
        // THEN
        assertThrows(MidiConfigError.class, actual);
    }

    @Test
    void forgeRequestsWithParametersFail() {
        // GIVEN
        CommandMacro commandMacro = CommandMacro.parse(APP_CONFIGFILE, "Multi(channel) : 120 : F0 43 20 7A 'LM  0065MU' 0000000000000000000000000000 00 channel F7");
        // WHEN
        Executable actual = () -> midiDeviceLibrary.forgeMidiRequestSequence(APP_CONFIGFILE, DEVICE_NAME, commandMacro);
        // THEN
        assertThrows(MidiConfigError.class, actual);
    }

}
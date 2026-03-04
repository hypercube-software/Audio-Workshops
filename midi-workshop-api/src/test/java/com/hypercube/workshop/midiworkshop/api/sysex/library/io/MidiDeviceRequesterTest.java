package com.hypercube.workshop.midiworkshop.api.sysex.library.io;

import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.request.MidiRequest;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.MidiResponseMapper;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandMacro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MidiDeviceRequesterTest {
    public static final String DEVICE_NAME = "TG-500";
    public static final File APP_CONFIGFILE = new File("MyAppConfigFile");

    MidiDeviceRequester midiDeviceRequester;
    MidiDeviceDefinition device;

    private CommandCall forgeCommandCall(String macro, String call) {
        CommandMacro commandMacro = CommandMacro.parse(APP_CONFIGFILE, macro);
        CommandCall commandCall = CommandCall.parse(APP_CONFIGFILE, null, call)
                .getFirst();
        commandCall.macro(commandMacro);
        return commandCall;
    }

    @BeforeEach
    void init() {
        midiDeviceRequester = new MidiDeviceRequester();
        device = new MidiDeviceDefinition();
        device.setDefinitionFile(APP_CONFIGFILE);
        device.setDeviceName(DEVICE_NAME);
        device.getMappers()
                .put("YamahaMapper", new MidiResponseMapper());
        device.getMacros()
                .add(new CommandMacro(APP_CONFIGFILE, "AllMulti", List.of(), 0x120, "body of AllMulti", null));
        device.getMacros()
                .add(new CommandMacro(APP_CONFIGFILE, "AllPerformances", List.of(), 0x122, "body of AllPerformances", null));
    }

    @Test
    void forgeRequestsWithoutMacro() {
        // GIVEN
        CommandCall commandCall = forgeCommandCall("noname() : 142 : F0 43 20 7A 'LM  0066SY' 0000000000000000000000000000 00 00 F7", "noname()");

        // WHEN

        var actual = midiDeviceRequester.forgeMidiRequestSequence(device, commandCall);
        // THEN
        assertEquals(1, actual.getMidiRequests()
                .size());
        assertEquals("/noname", actual.getMidiRequests()
                .getFirst()
                .getName());
    }

    @Test
    void forgeRequestsWithSequenceOfMacroAndMapper() {
        // GIVEN
        CommandCall commandCall = forgeCommandCall("noname() : --- : AllMulti();AllPerformances() : YamahaMapper", "noname()");
        // WHEN
        var actual = midiDeviceRequester.forgeMidiRequestSequence(device, commandCall);
        // THEN
        assertEquals(2, actual.getMidiRequests()
                .size());
        MidiRequest firstRequest = actual.getMidiRequests()
                .get(0);
        assertEquals("/noname/AllMulti", firstRequest
                .getName());
        assertEquals("body of AllMulti", firstRequest
                .getValue());
        assertEquals(0x120, firstRequest.getResponseSize());


        MidiRequest secondOne = actual.getMidiRequests()
                .get(1);
        assertEquals("/noname/AllPerformances", secondOne
                .getName());
        assertEquals("body of AllPerformances", secondOne
                .getValue());
        assertEquals(0x122, secondOne.getResponseSize());

    }

    @Test
    void forgeRequestsWithSequenceOfMacro() {
        // GIVEN
        CommandCall commandCall = forgeCommandCall("noname() : --- : AllMulti();AllPerformances()", "noname()");
        // WHEN
        var actual = midiDeviceRequester.forgeMidiRequestSequence(device, commandCall);
        // THEN
        assertEquals(2, actual.getMidiRequests()
                .size());
        MidiRequest firstRequest = actual.getMidiRequests()
                .get(0);
        MidiRequest secondOne = actual.getMidiRequests()
                .get(1);

        assertEquals("body of AllMulti", firstRequest
                .getValue());
        assertEquals(0x120, firstRequest.getResponseSize());

        assertEquals("body of AllPerformances", secondOne
                .getValue());
        assertEquals(0x120, firstRequest.getResponseSize());

        assertEquals("/noname/AllMulti", firstRequest
                .getName());
        assertEquals("/noname/AllPerformances", secondOne
                .getName());
    }

    @Test
    void forgeRequestsWithSequenceOfMacroAndRawPayload() {
        // GIVEN
        CommandCall commandCall = forgeCommandCall("noname() : 242 : F0 43 20 7A 'LM  0066SY' 0000000000000000000000000000 00 00 F7;AllMulti();AllPerformances()", "noname()");
        // WHEN
        var actual = midiDeviceRequester.forgeMidiRequestSequence(device, commandCall);
        // THEN
        assertEquals(0x242, actual.getTotalSize());
        assertEquals(3, actual.getMidiRequests()
                .size());
        assertNull(actual.getMidiRequests()
                .get(0)
                .getResponseSize());
        assertEquals(0x120, actual.getMidiRequests()
                .get(1)
                .getResponseSize());
        assertEquals(0x122, actual.getMidiRequests()
                .get(2)
                .getResponseSize());
    }

    @Test
    void wrongResponseSize() {
        // GIVEN
        CommandCall commandCall = forgeCommandCall("noname() : 100 : F0 43 20 7A 'LM  0066SY' 0000000000000000000000000000 00 00 F7;AllMulti();AllPerformances()", "noname()");
        // WHEN
        Executable actual = () -> midiDeviceRequester.forgeMidiRequestSequence(device, commandCall);
        // THEN
        MidiConfigError exception = assertThrows(MidiConfigError.class, actual);
        assertEquals("Response size of the macro 'noname' is not the same as the calculated one: 0x100 (defined) != 0x242 (computed)", exception.getMessage());
    }

    @Test
    void forgeRequestsWithoutSequence() {
        // GIVEN
        CommandCall commandCall = forgeCommandCall("noname() : 120 : AllMulti()", "noname()");

        // WHEN
        var actual = midiDeviceRequester.forgeMidiRequestSequence(device, commandCall);
        // THEN
        assertEquals(0x120, actual.getTotalSize());
        assertEquals(1, actual.getMidiRequests()
                .size());
        MidiRequest firstRequest = actual.getMidiRequests()
                .get(0);
        assertEquals("body of AllMulti", firstRequest
                .getValue());
        assertEquals(0x120, firstRequest.getResponseSize());
    }

    @Test
    void forgeRequestsWithParametersFail() {
        // GIVEN
        CommandCall commandCall = forgeCommandCall("Multi(channel) : 120 : F0 43 20 7A 'LM  0065MU' 0000000000000000000000000000 00 channel F7", "Multi()");

        // WHEN
        Executable actual = () -> midiDeviceRequester.forgeMidiRequestSequence(device, commandCall);
        // THEN
        assertThrows(MidiConfigError.class, actual);
    }
}

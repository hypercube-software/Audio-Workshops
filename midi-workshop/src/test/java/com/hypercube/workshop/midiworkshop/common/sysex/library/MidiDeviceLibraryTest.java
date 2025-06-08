package com.hypercube.workshop.midiworkshop.common.sysex.library;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.library.importer.PatchImporter;
import com.hypercube.workshop.midiworkshop.common.sysex.library.request.MidiRequest;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class MidiDeviceLibraryTest {
    public static final String DEVICE_NAME = "TG-500";
    public static final File APP_CONFIGFILE = new File("MyAppConfigFile");
    MidiDeviceLibrary midiDeviceLibrary;
    PatchImporter patchImporter;

    @BeforeEach
    void init() {
        midiDeviceLibrary = new MidiDeviceLibrary();
        midiDeviceLibrary.load(new File("./src/test/resources/devices-library"));
        patchImporter = new PatchImporter(midiDeviceLibrary);
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
        CommandMacro commandMacro = CommandMacro.parse(APP_CONFIGFILE, "noname() : 142 : F0 43 20 7A 'LM  0066SY' 0000000000000000000000000000 00 00 F7");
        CommandCall commandCall = CommandCall.parse(APP_CONFIGFILE, "noname()")
                .getFirst();
        // WHEN
        var actual = midiDeviceLibrary.forgeMidiRequestSequence(APP_CONFIGFILE, DEVICE_NAME, commandMacro, commandCall);
        // THEN
        assertEquals(1, actual.getMidiRequests()
                .size());
        assertEquals("", actual.getMidiRequests()
                .getFirst()
                .getName());
    }

    @Test
    void forgeRequestsWithSequenceOfMacroAndMapper() {
        // GIVEN
        CommandMacro commandMacro = CommandMacro.parse(APP_CONFIGFILE, "noname() : --- : AllMulti();AllPerformances() : patchName");
        CommandCall commandCall = CommandCall.parse(APP_CONFIGFILE, "noname()")
                .getFirst();
        // WHEN
        var actual = midiDeviceLibrary.forgeMidiRequestSequence(APP_CONFIGFILE, DEVICE_NAME, commandMacro, commandCall);
        // THEN
        assertEquals(2, actual.getMidiRequests()
                .size());
        MidiRequest firstRequest = actual.getMidiRequests()
                .get(0);
        assertEquals("/AllMulti/Multi", firstRequest
                .getName());
        assertEquals("F0 43 20 7A 'LM  0065MU' 0000000000000000000000000000 00 [0-15] F7", firstRequest
                .getValue());
        assertEquals(0x120, firstRequest.getResponseSize());


        MidiRequest secondOne = actual.getMidiRequests()
                .get(1);
        assertEquals("/AllPerformances/Performance", secondOne
                .getName());
        assertEquals("F0 43 20 7A 'LM  0065PF' 0000000000000000000000000000 00 [0-63]    F7", secondOne
                .getValue());
        assertEquals(0x122, secondOne.getResponseSize());

    }

    @Test
    void forgeRequestsWithSequenceOfMacro() {
        // GIVEN
        CommandMacro commandMacro = CommandMacro.parse(APP_CONFIGFILE, "noname() : --- : AllMulti();AllPerformances()");
        CommandCall commandCall = CommandCall.parse(APP_CONFIGFILE, "noname()")
                .getFirst();
        // WHEN
        var actual = midiDeviceLibrary.forgeMidiRequestSequence(APP_CONFIGFILE, DEVICE_NAME, commandMacro, commandCall);
        // THEN
        assertEquals(2, actual.getMidiRequests()
                .size());
        MidiRequest firstRequest = actual.getMidiRequests()
                .get(0);
        MidiRequest secondOne = actual.getMidiRequests()
                .get(1);

        assertEquals("F0 43 20 7A 'LM  0065MU' 0000000000000000000000000000 00 [0-15] F7", firstRequest
                .getValue());
        assertEquals(0x120, firstRequest.getResponseSize());

        assertEquals("F0 43 20 7A 'LM  0065PF' 0000000000000000000000000000 00 [0-63]    F7", secondOne
                .getValue());
        assertEquals(0x120, firstRequest.getResponseSize());

        assertEquals("/AllMulti/Multi", firstRequest
                .getName());
        assertEquals("/AllPerformances/Performance", secondOne
                .getName());
    }

    @Test
    void forgeRequestsWithSequenceOfMacroAndRawPayload() {
        // GIVEN
        CommandMacro commandMacro = CommandMacro.parse(APP_CONFIGFILE, "noname() : 142 : F0 43 20 7A 'LM  0066SY' 0000000000000000000000000000 00 00 F7;AllMulti();AllPerformances()");
        CommandCall commandCall = CommandCall.parse(APP_CONFIGFILE, "noname()")
                .getFirst();
        // WHEN
        var actual = midiDeviceLibrary.forgeMidiRequestSequence(APP_CONFIGFILE, DEVICE_NAME, commandMacro, commandCall);
        // THEN
        assertEquals(0x142, actual.getTotalSize());
        assertEquals(3, actual.getMidiRequests()
                .size());
        assertEquals(null, actual.getMidiRequests()
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
    void forgeRequestsWithoutSequence() {
        // GIVEN
        CommandMacro commandMacro = CommandMacro.parse(APP_CONFIGFILE, "noname() : --- : AllMulti()");
        CommandCall commandCall = CommandCall.parse(APP_CONFIGFILE, "noname()")
                .getFirst();
        // WHEN
        var actual = midiDeviceLibrary.forgeMidiRequestSequence(APP_CONFIGFILE, DEVICE_NAME, commandMacro, commandCall);
        // THEN
        assertEquals(0x120, actual.getTotalSize());
        assertEquals(1, actual.getMidiRequests()
                .size());
        MidiRequest firstRequest = actual.getMidiRequests()
                .get(0);
        assertEquals("F0 43 20 7A 'LM  0065MU' 0000000000000000000000000000 00 [0-15] F7", firstRequest
                .getValue());
        assertEquals(0x120, firstRequest.getResponseSize());
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
        CommandCall commandCall = CommandCall.parse(APP_CONFIGFILE, "Multi()")
                .getFirst();
        // WHEN
        Executable actual = () -> midiDeviceLibrary.forgeMidiRequestSequence(APP_CONFIGFILE, DEVICE_NAME, commandMacro, commandCall);
        // THEN
        assertThrows(MidiConfigError.class, actual);
    }

    @Test
    void importMininovaSysEx() {
        // GIVEN
        MidiDeviceDefinition device = midiDeviceLibrary.getDevice("Mininova")
                .orElseThrow();
        // WHEN
        patchImporter.importSysex(device, "SingleMode", new File("src/test/resources/SysEx/Novation/Mininova/SN Patches II.syx"));
        // THEN
        File dest = new File("src/test/resources/devices-library/Mininova/SingleMode/SN Patches II");
        assertTrue(dest.exists());
        assertEquals(128, dest.listFiles().length);
    }

    @Test
    void importTG500SysExSuper() {
        // GIVEN
        MidiDeviceDefinition device = midiDeviceLibrary.getDevice("TG-500")
                .orElseThrow();
        // WHEN
        patchImporter.importSysex(device, "VoiceMode", new File("src/test/resources/SysEx/Yamaha/TG-500/super.mid"));
        // THEN
        File dest = new File("src/test/resources/devices-library/TG-500/VoiceMode/Super");
        assertTrue(dest.exists());
        assertEquals(385, dest.listFiles().length);
    }

    @Test
    void importTG500SysExTop40() {
        // GIVEN
        MidiDeviceDefinition device = midiDeviceLibrary.getDevice("TG-500")
                .orElseThrow();
        // WHEN
        patchImporter.importSysex(device, "VoiceMode", new File("src/test/resources/SysEx/Yamaha/TG-500/top40.mid"));
        // THEN
        File dest = new File("src/test/resources/devices-library/TG-500/VoiceMode/Top40");
        assertTrue(dest.exists());
        assertEquals(385, dest.listFiles().length);
    }
}
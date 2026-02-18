package com.hypercube.workshop.midiworkshop.api.sysex.library;

import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.ControllerValueType;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiControllerValue;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceController;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.importer.PatchImporter;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.MidiDeviceRequester;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandMacro;
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
        MidiDeviceRequester midiDeviceRequester = new MidiDeviceRequester();
        midiDeviceLibrary = new MidiDeviceLibrary(midiDeviceRequester);
        midiDeviceLibrary.load(new File("./src/test/resources/devices-library"));
        patchImporter = new PatchImporter(midiDeviceLibrary, midiDeviceRequester);
    }

    @Test
    void load() {
        // WHEN
        MidiDeviceDefinition midiDeviceDefinition = midiDeviceLibrary.getDevice(DEVICE_NAME)
                .get();

        // THEN
        assertEquals(DEVICE_NAME, midiDeviceDefinition.getDeviceName());
        assertEquals(21, midiDeviceDefinition.getMacros()
                .size());
    }

    @Test
    void loadDrumkits() {
        // WHEN
        MidiDeviceDefinition midiDeviceDefinition = midiDeviceLibrary.getDevice("CS1x")
                .get();

        // THEN
        var mode = midiDeviceDefinition.getMode("XG")
                .orElseThrow();
        var bank = mode.getBank("XG DRUM KITS")
                .orElseThrow();
        var preset = bank.getPresets()
                .get(0);
        assertEquals(9, bank.getPresets()
                .size());
        assertEquals("Standard Kit", preset.name());
        assertEquals(72, preset.drumMap()
                .size());
    }


    @Test
    void forgeRequestsWithoutParenthesisFail() {
        // WHEN
        Executable actual = () -> CommandMacro.parse(APP_CONFIGFILE, "getAll : --- : AllMulti()");
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
    void parseMininovaControllers() {
        // GIVEN
        MidiDeviceDefinition device = midiDeviceLibrary.getDevice("Mininova")
                .orElseThrow();
        // THEN
        assertEquals(5, device.getControllers()
                .size());
        MidiDeviceController firstController = device.getControllers()
                .get(0);
        MidiDeviceController lastController = device.getControllers()
                .get(4);
        assertEquals("45", firstController
                .getIdentity());
        assertEquals(7, firstController
                .getIdBitDepth());
        assertEquals(ControllerValueType.CC, firstController
                .getType());
        assertEquals("004C", lastController
                .getIdentity());
        assertEquals(14, lastController
                .getIdBitDepth());
        assertEquals(ControllerValueType.NRPN_MSB_LSB, lastController
                .getType());
    }

    @Test
    void parseSYSEXControllers() {
        // GIVEN
        MidiDeviceDefinition device = midiDeviceLibrary.getDevice("TG-500")
                .orElseThrow();
        // THEN
        assertEquals(1, device.getControllers()
                .size());
        MidiDeviceController firstController = device.getControllers()
                .get(0);

        assertEquals(ControllerValueType.SYSEX, firstController
                .getType());
        assertNotNull(firstController.getSysExTemplate());

        for (int i = 0; i < 10; i++) {
            byte[] payload = firstController.getSysExTemplate()
                    .forgePayload(MidiControllerValue.from32BitsSignedValue(i));
            assertEquals(11, payload.length);
            assertEquals(i, payload[9]);
        }
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
        assertEquals(257, dest.listFiles().length);
        dest = new File("src/test/resources/devices-library/TG-500/PerformanceMode/Super");
        assertTrue(dest.exists());
        assertEquals(128, dest.listFiles().length);
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
        assertEquals(257, dest.listFiles().length);
        dest = new File("src/test/resources/devices-library/TG-500/PerformanceMode/Top40");
        assertTrue(dest.exists());
        assertEquals(128, dest.listFiles().length);
    }
}

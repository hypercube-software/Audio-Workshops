package com.hypercube.workshop.synthripper.config;

import com.hypercube.workshop.synthripper.config.presets.IConfigMidiPreset;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SynthRipperConfigurationTest {
    /**
     * The test configuration use a macro to set the tempo of various presets via MIDI SySex
     */
    @Test
    void canLoadConfig() {
        var rsc = this.getClass()
                .getClassLoader()
                .getResource("config/config-test.yml")
                .getFile();
        var cfg = SynthRipperConfiguration.loadConfig(new File(rsc));
        cfg.getMidi()
                .getPresets()
                .forEach(p -> {
                    var mp = p.forgeMidiPreset(cfg.getMidi());
                    assertNotNull(mp.commands());
                    assertNotNull(mp.controlChanges());
                    assertNotNull(mp.drumKitNotes());
                });
        //
        // test the macro expansion for tempo
        //
        IConfigMidiPreset secondPreset = cfg.getMidi()
                .getPresets()
                .get(1);
        var preset2 = secondPreset.forgeMidiPreset(cfg.getMidi());
        var msg = preset2.commands()
                .get(0);
        assertEquals(110, msg.getMessage()[msg.getLength() - 2]); // expected tempo


        //
        // test the drumkit notes
        //
        IConfigMidiPreset lastPreset = cfg.getMidi()
                .getPresets()
                .getLast();
        assertEquals("Capital Drumkit", lastPreset
                .getTitle());

        var preset = lastPreset.forgeMidiPreset(cfg.getMidi());

        assertEquals("Capital Drumkit", preset.title());
        assertEquals(84, preset.drumKitNotes()
                .size());

    }

}
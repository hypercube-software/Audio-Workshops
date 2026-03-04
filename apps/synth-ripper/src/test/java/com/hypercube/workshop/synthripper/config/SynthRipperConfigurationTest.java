package com.hypercube.workshop.synthripper.config;

import com.hypercube.workshop.synthripper.config.yaml.IConfigMidiPreset;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SynthRipperConfigurationTest {
    /**
     * The test configuration use a macro to set the tempo of various presets via MIDI SysEx
     */
    @Test
    @Disabled
    void canLoadConfig() {
        var rsc = Objects.requireNonNull(this.getClass()
                        .getClassLoader()
                        .getResource("config/config-test.yml"))
                .getFile();
        var cfg = SynthRipperConfiguration.loadConfig(new File(rsc));
        cfg.getMidi()
                .getPresets()
                .forEach(p -> {
                    var mp = p.forgeMidiPreset(cfg);
                    assertNotNull(mp.getCommands());
                    assertNotNull(mp.getControlChanges());
                    assertNotNull(mp.getDrumKitNotes());
                });
        //
        // test the macro expansion for tempo
        //
        IConfigMidiPreset secondPreset = cfg.getMidi()
                .getPresets()
                .get(1);
        var preset2 = secondPreset.forgeMidiPreset(cfg);
        var msg = preset2.getCommands()
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

        var preset = lastPreset.forgeMidiPreset(cfg);

        assertEquals("Capital Drumkit", preset.getId()
                .name());
        assertEquals(84, preset.getDrumKitNotes()
                .size());

    }

}

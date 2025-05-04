package com.hypercube.workshop.synthripper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import com.hypercube.workshop.synthripper.config.yaml.CommandMacroDeserializer;
import com.hypercube.workshop.synthripper.config.yaml.ConfigMidiPresetDeserializer;
import com.hypercube.workshop.synthripper.config.yaml.IConfigMidiPreset;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Setter
@Getter
public class SynthRipperConfiguration {
    private String projectName;
    private DevicesSettings devices;
    private MidiSettings midi;
    private AudioSettings audio;
    private File configFile; // loaded config file corresponding to this class

    public static SynthRipperConfiguration loadConfig(File configFile) {
        try {
            var mapper = new ObjectMapper(new YAMLFactory());
            SimpleModule module = new SimpleModule();
            module.addDeserializer(IConfigMidiPreset.class, new ConfigMidiPresetDeserializer());
            module.addDeserializer(CommandMacro.class, new CommandMacroDeserializer(configFile));
            mapper.registerModule(module);

            SynthRipperConfiguration conf = mapper.readValue(configFile, SynthRipperConfiguration.class);
            conf.configFile = configFile;
            return conf;
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }

    public String getOutputDir() {
        return "output/" + projectName;
    }

    private static final List<String> notes = List.of("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");

    public static String noteNameFromPitch(int input) {
        int octave = (input - 12) / 12;
        int offset = input % 12;
        return notes.get(offset) + octave;
    }

    public List<MidiPreset> getSelectedPresets() {
        return midi.getSelectedPresets(this);
    }
}

package com.hypercube.workshop.synthripper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.synthripper.config.presets.ConfigMidiPresetDeserializer;
import com.hypercube.workshop.synthripper.config.presets.IConfigMidiPreset;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;

@Setter
@Getter
public class SynthRipperConfiguration {
    private String projectName;
    private DevicesSettings devices;
    private MidiSettings midi;
    private AudioSettings audio;

    public static SynthRipperConfiguration loadConfig(File configFile) {
        try {
            var mapper = new ObjectMapper(new YAMLFactory());
            SimpleModule module = new SimpleModule();
            module.addDeserializer(IConfigMidiPreset.class, new ConfigMidiPresetDeserializer());
            mapper.registerModule(module);

            return mapper.readValue(configFile, SynthRipperConfiguration.class);
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }

    public String getOutputDir() {
        return "output/" + projectName;
    }
}

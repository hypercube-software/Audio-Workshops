package com.hypercube.workshop.synthripper.config;

import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import lombok.Getter;
import lombok.Setter;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Setter
@Getter
public class SynthRipperConfiguration {
    private String projectName;
    private DevicesSettings devices;
    private MidiSettings midi;
    private AudioSettings audio;
    
    public static SynthRipperConfiguration loadConfig(File configFile) {
        Yaml yaml = new Yaml(new Constructor(SynthRipperConfiguration.class, new LoaderOptions()));
        try (FileInputStream inputStream = new FileInputStream(configFile)) {
            SynthRipperConfiguration cfg = yaml.load(inputStream);
            cfg.getMidi()
                    .buildFilesNames();
            return cfg;
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }
}

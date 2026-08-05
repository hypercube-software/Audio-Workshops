package com.hypercube.workshop.synthripper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.workshop.midiworkshop.api.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.synthripper.SynthRipperApplication;
import com.hypercube.workshop.synthripper.model.SynthRipperError;
import com.hypercube.workshop.synthripper.model.config.SynthRipperConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ConfigFactory {
    private final MidiDeviceLibrary midiDeviceLibrary;

    public SynthRipperConfiguration loadConfig(File configFile) {
        try {
            midiDeviceLibrary.load(ConfigHelper.getApplicationFolder(SynthRipperApplication.class));

            var mapper = new ObjectMapper(new YAMLFactory());

            SynthRipperConfiguration conf = mapper.readValue(configFile, SynthRipperConfiguration.class);
            conf.setConfigFile(configFile);
            conf.setMidiDeviceLibrary(midiDeviceLibrary);
            return conf;
        } catch (IOException e) {
            throw new SynthRipperError(e);
        }
    }
}

package com.hypercube.workshop.synthripper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.MidiDeviceRequester;
import com.hypercube.workshop.synthripper.model.config.SynthRipperConfiguration;
import com.hypercube.workshop.synthripper.preset.decent.DecentSamplerPresetGenerator;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AbstractSynthRipperTest {
    protected MidiDeviceLibrary midiDeviceLibrary;

    protected File getApplicationFolder() {
        return new File("../../");
    }

    protected SynthRipper forgeSynthRipper(String config) throws IOException {
        SynthRipperConfiguration conf = loadConfig(new File(config));
        conf.setMidiDeviceLibrary(midiDeviceLibrary);
        SynthRipper synthRipper = new SynthRipper(List.of(new DecentSamplerPresetGenerator()), new MidiDeviceRequester());
        synthRipper.init(conf);
        return synthRipper;
    }

    private SynthRipperConfiguration loadConfig(File configFile) throws IOException {
        var mapper = new ObjectMapper(new YAMLFactory());
        SynthRipperConfiguration conf = mapper.readValue(configFile, SynthRipperConfiguration.class);
        conf.setConfigFile(configFile);
        return conf;
    }

    @BeforeEach
    void init() {
        midiDeviceLibrary = new MidiDeviceLibrary(new MidiDeviceRequester());
        midiDeviceLibrary.load(getApplicationFolder());
    }
}

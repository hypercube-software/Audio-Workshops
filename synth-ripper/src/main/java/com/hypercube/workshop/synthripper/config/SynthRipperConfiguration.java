package com.hypercube.workshop.synthripper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.workshop.midiworkshop.api.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandMacro;
import com.hypercube.workshop.synthripper.SynthRipperApplication;
import com.hypercube.workshop.synthripper.config.yaml.CommandMacroDeserializer;
import com.hypercube.workshop.synthripper.config.yaml.ConfigMidiPresetDeserializer;
import com.hypercube.workshop.synthripper.config.yaml.IConfigMidiPreset;
import com.hypercube.workshop.synthripper.model.SynthRipperError;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Setter
@Getter
@Configuration
public class SynthRipperConfiguration {
    private static final List<String> notes = List.of("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");
    private String projectName;
    private String device;
    private DevicesSettings ports;
    private MidiSettings midi;
    private AudioSettings audio;
    private File configFile; // loaded config file corresponding to this class
    private MidiDeviceLibrary midiDeviceLibrary;

    public static SynthRipperConfiguration loadConfig(File configFile) {
        try {
            // TODO: use mostly device library instead of what is in the config
            MidiDeviceLibrary midiDeviceLibrary = new MidiDeviceLibrary(null);
            midiDeviceLibrary.load(ConfigHelper.getApplicationFolder(SynthRipperApplication.class));

            var mapper = new ObjectMapper(new YAMLFactory());
            SimpleModule module = new SimpleModule();
            module.addDeserializer(IConfigMidiPreset.class, new ConfigMidiPresetDeserializer());
            module.addDeserializer(CommandMacro.class, new CommandMacroDeserializer(configFile));
            mapper.registerModule(module);

            SynthRipperConfiguration conf = mapper.readValue(configFile, SynthRipperConfiguration.class);
            conf.configFile = configFile;
            conf.midiDeviceLibrary = midiDeviceLibrary;
            return conf;
        } catch (IOException e) {
            throw new SynthRipperError(e);
        }
    }

    public static String noteNameFromPitch(int input) {
        int octave = (input - 12) / 12;
        int offset = input % 12;
        return notes.get(offset) + octave;
    }

    public String getOutputDir() {
        return "output/" + projectName;
    }

    public List<MidiPreset> getSelectedPresets() {
        return midi.getSelectedPresets(this);
    }

    public MidiDeviceDefinition getDevice() {
        return midiDeviceLibrary != null ? midiDeviceLibrary.getDevice(device)
                .orElseThrow() : null;
    }
}

package com.hypercube.workshop.midiworkshop.common.sysex.library;

import com.hypercube.workshop.midiworkshop.common.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetDomain;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetNaming;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetNumbering;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MidiDeviceDefinition {
    private File definitionFile;
    private String deviceName;
    private String brand;
    private String inputMidiDevice;
    private String outputMidiDevice;
    private Integer outputBandWidth;
    private Integer sysExPauseMs;
    private Integer inactivityTimeoutMs;
    private MidiBankFormat presetFormat;
    private MidiPresetNumbering presetNumbering;
    private MidiPresetNaming midiPresetNaming;
    private List<CommandMacro> macros = new ArrayList<>();
    private List<MidiPresetDomain> presetDomains = new ArrayList<>();
}

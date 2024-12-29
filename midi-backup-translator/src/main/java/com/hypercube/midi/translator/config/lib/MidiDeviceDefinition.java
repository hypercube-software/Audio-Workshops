package com.hypercube.midi.translator.config.lib;

import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MidiDeviceDefinition {
    private String deviceName;
    private String brand;
    private String inputMidiDevice;
    private String outputMidiDevice;
    private Integer outputBandWidth;
    private Integer sysExPauseMs;
    private Integer inactivityTimeoutMs;
    private List<CommandMacro> macros;
}

package com.hypercube.midi.translator.config.project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiRequestSequence;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * A Project device is defined outside the midi devices library and can override various settings
 * <p>Typically it is stored in a "project folder" and you can define many whereas the folder of the device library is unique</p>
 *
 * @see MidiDeviceDefinition
 */
@Setter
@Getter
public class ProjectDevice {
    private String name;
    private Boolean enabled;
    private String inputMidiDevice;
    private String outputMidiDevice;
    private Integer outputBandWidth;
    private Integer sysExPauseMs;
    private Integer inactivityTimeoutMs;
    private List<String> dumpRequests;
    @JsonIgnore
    private List<MidiRequestSequence> dumpRequestTemplates;

}

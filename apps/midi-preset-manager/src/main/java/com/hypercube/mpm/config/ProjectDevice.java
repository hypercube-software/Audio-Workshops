package com.hypercube.mpm.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiRequestSequence;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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

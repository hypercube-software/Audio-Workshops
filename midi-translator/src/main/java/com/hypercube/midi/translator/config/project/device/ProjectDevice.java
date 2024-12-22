package com.hypercube.midi.translator.config.project.device;

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
    private List<DumpRequestTemplate> dumpRequests;
}

package com.hypercube.workshop.midiworkshop.common.sysex.library.device;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MidiDeviceMode {
    private String name;
    private String command;
    private String queryName;
    private Map<String, MidiDeviceBank> banks = new HashMap<>();
    private List<String> categories = new ArrayList<>();
}

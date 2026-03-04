package com.hypercube.workshop.midiworkshop.api.presets.crawler;

import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;

import java.util.HashSet;
import java.util.Set;

public record CrawlingDomain(String device, Set<String> modes, Set<String> banks) {
    public static CrawlingDomain everything(String device) {
        return new CrawlingDomain(device, new HashSet<>(), new HashSet<>());
    }

    public boolean matches(MidiDeviceMode mode) {
        return modes.isEmpty() || modes.contains(mode.getName());
    }

    public boolean matches(MidiDeviceBank bank) {
        return banks.isEmpty() || banks.contains(bank.getName());
    }
}

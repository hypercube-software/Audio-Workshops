package com.hypercube.workshop.midiworkshop.sysex.device.memory.primitives;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * MemoryEnum are like Java enums. The value index is the MIDI code found in the Sysex
 */
@Getter
@RequiredArgsConstructor
public class MemoryEnum {
    private final String name;
    private final List<String> values = new ArrayList<>();

    public void add(String value) {
        values.add(value);
    }
}

package com.hypercube.workshop.midiworkshop.common.sysex.library;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MidiRequest {
    private final String name;
    private final String value;
    private final Integer size;
}

package com.hypercube.workshop.midiworkshop.common.sysex.library;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class MidiRequestsSequence {
    private final String name;
    private final List<MidiRequest> midiRequests;
}

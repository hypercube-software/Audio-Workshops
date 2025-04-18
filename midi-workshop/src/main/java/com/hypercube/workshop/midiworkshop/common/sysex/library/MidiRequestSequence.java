package com.hypercube.workshop.midiworkshop.common.sysex.library;

import com.hypercube.workshop.midiworkshop.common.sysex.library.request.MidiRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class MidiRequestSequence {
    /**
     * Total response size if known (null otherwise)
     */
    private final Integer totalSize;
    /**
     * List of requests to execute
     */
    private final List<MidiRequest> midiRequests;
}

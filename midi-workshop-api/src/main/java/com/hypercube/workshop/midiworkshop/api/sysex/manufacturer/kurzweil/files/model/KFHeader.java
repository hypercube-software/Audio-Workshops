package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public final class KFHeader {
    private final RawData data;
    private final String magic;
    private final long offsetSampleData;
}

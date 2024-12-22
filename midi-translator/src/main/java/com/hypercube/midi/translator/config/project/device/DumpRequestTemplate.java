package com.hypercube.midi.translator.config.project.device;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class DumpRequestTemplate {
    private final String name;
    private final String value;
    private final Integer size;
}
